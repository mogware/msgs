package org.mogware.msgs.aio;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.nio.channels.UnsupportedAddressTypeException;
import org.mogware.msgs.core.Global;
import org.mogware.msgs.utils.ErrnoException;

public class SockClient extends SockBase {
    private SocketChannel s;
    private SockServer asock;

    private ByteBuffer iov[];
    private ByteBuffer inbuf;

    private final Worker worker;
    private final Worker.Fd fd;
    private final Worker.Task connecting;
    private final Worker.Task connected;
    private final Worker.Task send;
    private final Worker.Task recv;
    private final Worker.Task stop;

    private final FsmEvent established;
    private final FsmEvent sent;
    private final FsmEvent received;
    private final FsmEvent error;

    public SockClient(int src, Fsm owner) {
        final SockClient usock = this;
        this.fsm = new Fsm(src, this, owner) {
            @Override
            protected void onProgress(int src, int type, Object srcObj)
                    throws ErrnoException {
                if (usock.internalTasks(src, type))
                    return;
                switch (usock.state) {
                case SockBase.STATE_IDLE:
                    switch (src) {
                    case Fsm.ACTION:
                        switch (type) {
                        case Fsm.START:
                            usock.state = SockBase.STATE_STARTING;
                            return;
                        default:
                            throw new FsmBadActionException(usock.state, src, type);
                        }
                    default:
                        throw new FsmBadSourceException(usock.state, src, type);
                    }                case SockBase.STATE_STARTING:
                    switch (src) {
                    case Fsm.ACTION:
                        switch (type)
                        {
                        case SockBase.ACTION_CONNECT:
                            usock.state = SockBase.STATE_CONNECTING;
                            return;
                        case SockBase.ACTION_BEING_ACCEPTED:
                            usock.state = SockBase.STATE_BEING_ACCEPTED;
                            return;
                        case SockBase.ACTION_STARTED:
                            usock.worker.addFd(usock.s, usock.fd);
                            usock.state = SockBase.STATE_ACTIVE;
                            return;
                        default:
                            throw new FsmBadActionException(usock.state, src, type);
                        }
                    default:
                        throw new FsmBadSourceException(usock.state, src, type);
                    }
                case SockBase.STATE_BEING_ACCEPTED:
                    switch (src) {
                    case Fsm.ACTION:
                        switch (type) {
                        case SockBase.ACTION_DONE:
                            usock.state = SockBase.STATE_ACCEPTED;
                            usock.fsm.raise(usock.established, SockBase.ACCEPTED);
                            return;
                        default:
                            throw new FsmBadActionException(usock.state, src, type);
                        }
                    default:
                        throw new FsmBadSourceException(usock.state, src, type);
                    }
                case SockBase.STATE_ACCEPTED:
                    switch (src) {
                    case Fsm.ACTION:
                        switch (type) {
                        case SockBase.ACTION_ACTIVATE:
                            usock.state = SockBase.STATE_ACTIVE;
                            usock.worker.addFd(usock.s, usock.fd);
                            return;
                        default:
                            throw new FsmBadActionException(usock.state, src, type);
                        }
                    default:
                        throw new FsmBadSourceException(usock.state, src, type);
                    }
                case SockBase.STATE_CONNECTING:
                    switch (src) {
                    case Fsm.ACTION:
                        switch (type) {
                        case SockBase.ACTION_DONE:
                            usock.state = SockBase.STATE_ACTIVE;
                            usock.worker.execute(usock.connected);
                            usock.fsm.raise(usock.established, SockBase.CONNECTED);
                            return;
                        case SockBase.ACTION_ERROR:
                            try {
                                usock.s.close();
                            } catch(IOException ex) { }
                            usock.s = null;
                            usock.state = SockBase.STATE_DONE;
                            usock.fsm.raise(usock.error, SockBase.ERROR);
                            return;
                        default:
                            throw new FsmBadActionException(usock.state, src, type);
                        }
                    case SockBase.SRC_FD:
                        switch (type) {
                        case Worker.FD_OUT:
                            try {
                                usock.s.finishConnect();
                                usock.state = SockBase.STATE_ACTIVE;
                                usock.fsm.raise(usock.established, SockBase.CONNECTED);
                            } catch(IOException ex) {
                                usock.errno = Global.ECONNABORTED;
                                usock.worker.removeFd(usock.s);
                                try {
                                    usock.s.close();
                                } catch(IOException e) { }
                                usock.s = null;
                                usock.state = SockBase.STATE_DONE;
                                usock.fsm.raise(usock.error, SockBase.ERROR);
                            }
                            return;
                        case Worker.FD_ERR:
                            usock.worker.removeFd(usock.s);
                            try {
                                usock.s.close();
                            } catch(IOException ex) { }
                            usock.s = null;
                            usock.state = SockBase.STATE_DONE;
                            usock.fsm.raise(usock.error, SockBase.ERROR);
                            return;
                        default:
                            throw new FsmBadActionException(usock.state, src, type);
                        }
                    default:
                        throw new FsmBadSourceException(usock.state, src, type);
                    }
                case SockBase.STATE_ACTIVE:
                    switch (src) {
                    case SockBase.SRC_FD:
                        switch (type) {
                        case Worker.FD_IN:
                            try {
                                if (SockClient.read(usock.s, usock.inbuf)) {
                                    usock.inbuf.flip();
                                    usock.worker.resetIn(usock.s);
                                    usock.fsm.raise(usock.received, SockBase.RECEIVED);
                                }
                            } catch(IOException ex) {
                                usock.worker.removeFd(usock.s);
                                try {
                                    usock.s.close();
                                } catch(IOException e) { }
                                usock.s = null;
                                usock.state = SockBase.STATE_DONE;
                                usock.fsm.raise(usock.error, SockBase.ERROR);
                            }
                            return;
                        case Worker.FD_OUT:
                            try {
                                if (SockClient.write(usock.s, usock.iov)) {
                                    usock.worker.resetOut(usock.s);
                                    usock.fsm.raise(usock.sent, SockBase.SENT);
                                }
                            } catch(IOException ex) {
                                usock.worker.removeFd(usock.s);
                                try {
                                    usock.s.close();
                                } catch(IOException e) { }
                                usock.s = null;
                                usock.state = SockBase.STATE_DONE;
                                usock.fsm.raise(usock.error, SockBase.ERROR);
                            }
                            return;
                        case Worker.FD_ERR:
                            usock.worker.removeFd(usock.s);
                            try {
                                usock.s.close();
                            } catch(IOException ex) { }
                            usock.s = null;
                            usock.state = SockBase.STATE_DONE;
                            usock.fsm.raise(usock.error, SockBase.ERROR);
                            return;
                        }
                        throw new FsmBadActionException(usock.state, src, type);
                    case Fsm.ACTION:
                        switch (type) {
                        case SockBase.ACTION_ERROR:
                            usock.state = SockBase.STATE_REMOVING_FD;
                            usock.asyncStop();
                            return;
                        default:
                            throw new FsmBadActionException(usock.state, src, type);
                        }
                    default:
                        throw new FsmBadSourceException(usock.state, src, type);
                    }
                case SockBase.STATE_REMOVING_FD:
                    switch (src) {
                    case SockBase.SRC_TASK_STOP:
                        switch (type) {
                        case Worker.TASK_EXECUTE:
                            usock.worker.removeFd(usock.s);
                            try {
                                usock.s.close();
                            } catch(IOException e) { }
                            usock.s = null;
                            usock.state = SockBase.STATE_DONE;
                            usock.fsm.raise(usock.error, SockBase.ERROR);
                            return;
                        default:
                            throw new FsmBadActionException(usock.state, src, type);
                        }
                    case SockBase.SRC_FD:
                        return;
                    default:
                        throw new FsmBadSourceException(usock.state, src, type);
                    }
                case SockBase.STATE_DONE:
                    throw new FsmBadSourceException(usock.state, src, type);
                default:
                    throw new FsmBadStateException(usock.state, src, type);
                }
            }
            @Override
            protected void onShutdown(int src, int type, Object srcObj)
                    throws ErrnoException {
                if (usock.internalTasks(src, type))
                    return;
                if (src == Fsm.ACTION  && type == Fsm.STOP) {
                    usock.errno = 0;
                    switch (usock.state) {
                    case SockBase.STATE_ACCEPTED:
                        try {
                            usock.s.close();
                        } catch(IOException ex) { }
                        usock.s = null;
                    case SockBase.STATE_DONE:
                        usock.state = SockBase.STATE_IDLE;
                        usock.fsm.stopped(SockBase.STOPPED);
                    case SockBase.STATE_IDLE:
                        return;
                    case SockBase.STATE_BEING_ACCEPTED:
                        usock.asock.fsm.action(SockBase.ACTION_CANCEL);
                        usock.state = SockBase.STATE_STOPPING_ACCEPT;
                        return;
                    }
                    if (usock.state != SockBase.STATE_REMOVING_FD) {
                        usock.asyncStop();
                        usock.state = SockBase.STATE_STOPPING;
                        return;
                    }
                }
                switch (usock.state) {
                case SockBase.STATE_STOPPING:
                    if (src != SockBase.SRC_TASK_STOP)
                        return;
                    assert(type == Worker.TASK_EXECUTE);
                    usock.worker.removeFd(usock.s);
                    try {
                        usock.s.close();
                    } catch(IOException ex) { }
                    usock.s = null;
                case SockBase.STATE_STOPPING_ACCEPT:
                    usock.state = SockBase.STATE_IDLE;
                    usock.fsm.stopped(SockBase.STOPPED);
                    return;
                default:
                    throw new FsmBadStateException(usock.state, src, type);
                }
            }
        };
        this.state = SockBase.STATE_IDLE;
        this.worker = this.fsm.chooseWorker();
        this.fd = new Worker.Fd(SockBase.SRC_FD, this.fsm);
        this.connecting = new Worker.Task(SockBase.SRC_TASK_CONNECTING, this.fsm);
        this.connected = new Worker.Task(SockBase.SRC_TASK_CONNECTED, this.fsm);
        this.send = new Worker.Task(SockBase.SRC_TASK_SEND, this.fsm);
        this.recv = new Worker.Task(SockBase.SRC_TASK_RECV, this.fsm);
        this.stop = new Worker.Task(SockBase.SRC_TASK_STOP, this.fsm);
        this.established = new FsmEvent();
        this.sent = new FsmEvent();
        this.received = new FsmEvent();
        this.error = new FsmEvent();
        this.s = null;
        this.asock = null;
    }

    public SockServer asock() {
        return this.asock;
    }

    public void asock(SockServer asock) {
        assert(this.asock == null || this.asock == asock || asock == null);
        this.asock = asock;
    }

    public void start() throws ErrnoException {
        try {
            this.s = SocketChannel.open();
            this.s.configureBlocking(false);
            this.fsm.start();
        } catch(IOException ex) {
            throw new ErrnoException(Global.EINVAL);
        }
    }

    public void start(SocketChannel s) throws ErrnoException {
        try {
            this.s = s;
            this.s.configureBlocking(false);
            this.fsm.start();
            this.fsm.action(SockBase.ACTION_STARTED);
        } catch(IOException ex) {
            throw new ErrnoException(Global.EINVAL);
        }
    }

    public void asyncStop() throws ErrnoException {
        this.worker.execute(this.stop);
        this.fsm.raise(error, SockBase.SHUTDOWN);
    }

    public <T extends Object> void opt(SocketOption<T> so, T val)
            throws ErrnoException {
        try {
            s.setOption(so, val);
        } catch(IOException ex) {
            throw new ErrnoException(Global.EINVAL);
        }
    }

    public void bind(SocketAddress addr) throws ErrnoException {
        assert(this.state == SockBase.STATE_STARTING);
        try {
            if (!SockBase.isWindows)
                s.socket().setReuseAddress(true);
            s.socket().bind(addr);
        } catch(IOException ex) {
            throw new ErrnoException(Global.EADDRINUSE);
        }
    }

    public void accept(SockServer listener) throws ErrnoException {
        if (this.fsm.isIdle()) {
            fsm.start();
            fsm.action(SockBase.ACTION_BEING_ACCEPTED);
        }
        listener.fsm().action(SockBase.ACTION_ACCEPT);
        SocketChannel s = listener.accept();
        if (s != null) {
            this.asock = null;
            this.init(s);
            listener.fsm().action(SockBase.ACTION_DONE);
            this.fsm.action(SockBase.ACTION_DONE);
            return;
        }
        assert(this.asock == null || this.asock == listener);
        this.asock = listener;
        listener.asock(this);
        listener.awaitAccept();
    }

    public void connect(SocketAddress addr) throws ErrnoException {
        this.fsm.action(SockBase.ACTION_CONNECT);
        try {
            if (this.s.connect(addr))
                this.fsm.action(SockBase.ACTION_DONE);
            else
                this.worker.execute(connecting);
        } catch (AlreadyConnectedException ex) {
            this.errno = Global.EISCONN;
            this.fsm.action(SockBase.ACTION_ERROR);
        } catch (ClosedChannelException ex) {
            this.errno = Global.EBADF;
            this.fsm.action(SockBase.ACTION_ERROR);
        } catch (UnresolvedAddressException ex) {
            this.errno = Global.EADDRNOTAVAIL;
            this.fsm.action(SockBase.ACTION_ERROR);
        } catch (UnsupportedAddressTypeException ex) {
            this.errno = Global.EAFNOSUPPORT;
            this.fsm.action(SockBase.ACTION_ERROR);
        } catch (SecurityException ex) {
            this.errno = Global.EACCESS;
            this.fsm.action(SockBase.ACTION_ERROR);
        } catch (IOException ex) {
            this.errno = Global.EIO;
            this.fsm.action(SockBase.ACTION_ERROR);
        }
    }

    public void init(SocketChannel s) throws ErrnoException {
        assert(this.state == SockBase.STATE_IDLE ||
                this.state == SockBase.STATE_BEING_ACCEPTED);
        assert(this.s == null);
        try {
            this.s = s;
            this.s.configureBlocking(false);
        } catch(IOException ex) {
            throw new ErrnoException(Global.EINVAL);
        }
    }

    public void accepted() {
        this.state = SockBase.STATE_ACCEPTED;
        this.fsm.raise(established, SockBase.ACCEPTED);
    }

    public void send(ByteBuffer iov[]) throws ErrnoException {
        assert(this.state == SockBase.STATE_ACTIVE);
        this.iov = new ByteBuffer[iov.length];
        System.arraycopy(iov, 0, this.iov, 0, iov.length);
        try {
            if (!SockClient.write(this.s, this.iov))
                this.worker.execute(this.send);
            else
                this.fsm.raise(this.sent, SockBase.SENT);
        } catch(IOException ex) {
            this.fsm.action(SockBase.ACTION_ERROR);
        }
    }

    public void send(ByteBuffer bb) throws ErrnoException {
        this.iov = new ByteBuffer[1];
        this.iov[0] = bb;
        try {
            if (!SockClient.write(this.s, this.iov))
                this.worker.execute(this.send);
            else
                this.fsm.raise(this.sent, SockBase.SENT);
        } catch(IOException ex) {
            this.fsm.action(SockBase.ACTION_ERROR);
        }
    }

    public void recv(ByteBuffer buf) throws ErrnoException {
        assert(this.state == SockBase.STATE_ACTIVE);
        this.inbuf = buf;
        try {
            if (!SockClient.read(this.s, this.inbuf)) {
                this.worker.execute(this.recv);
            } else {
                this.inbuf.flip();
                this.fsm.raise(this.received, SockBase.RECEIVED);
            }
        } catch(IOException ex) {
            this.fsm.action(SockBase.ACTION_ERROR);
        }
    }

    private boolean internalTasks(int src, int type) {
        switch (src) {
        case SockBase.SRC_TASK_SEND:
            assert(type == Worker.TASK_EXECUTE);
            this.worker.setOut(this.s);
            return true;
        case SockBase.SRC_TASK_RECV:
            assert(type == Worker.TASK_EXECUTE);
            this.worker.setIn(this.s);
            return true;
        case SockBase.SRC_TASK_CONNECTED:
            assert(type == Worker.TASK_EXECUTE);
            this.worker.addFd(this.s, this.fd);
            return true;
        case SockBase.SRC_TASK_CONNECTING:
            assert(type == Worker.TASK_EXECUTE);
            this.worker.addFd(this.s, this.fd);
            this.worker.setConnect(this.s);
            return true;
        }
        return false;
    }

    private static boolean write(SocketChannel s, ByteBuffer iov[])
            throws IOException {
        long iovlen = 0;
        for (ByteBuffer bb: iov)
            iovlen += bb.remaining();
        long nbytes = s.write(iov);
        return nbytes >= iovlen;
    }

    private static boolean read(SocketChannel s, ByteBuffer buf)
        throws IOException {
        long buflen = buf.remaining();
        long nbytes = s.read(buf);
        if (nbytes < 0L)
            throw new ClosedChannelException();
        return nbytes >= buflen;
    }

















}
