package org.mogware.msgs.aio;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import org.mogware.msgs.core.Global;
import org.mogware.msgs.utils.ErrnoException;

public class SockServer extends SockBase {
    private ServerSocketChannel s;
    private SockClient asock;
    private final Worker worker;
    private final Worker.Fd fd;
    private final Worker.Task accept;
    private final Worker.Task stop;
    private FsmEvent error;

    public SockServer(int src, Fsm owner) {
        final SockServer usock = this;
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
                        }
                        throw new FsmBadActionException(usock.state, src, type);
                    }
                    throw new FsmBadSourceException(usock.state, src, type);
                case SockBase.STATE_STARTING:
                    switch (src) {
                    case Fsm.ACTION:
                        switch (type) {
                        case SockBase.ACTION_LISTEN:
                            usock.state = SockBase.STATE_LISTENING;
                            return;
                        }
                        throw new FsmBadActionException(usock.state, src, type);
                    }
                    throw new FsmBadSourceException(usock.state, src, type);
                case SockBase.STATE_LISTENING:
                    switch (src) {
                    case Fsm.ACTION:
                        switch (type) {
                        case SockBase.ACTION_ACCEPT:
                            usock.state = SockBase.STATE_ACCEPTING;
                            return;
                        }
                        throw new FsmBadActionException(usock.state, src, type);
                    }
                    throw new FsmBadSourceException(usock.state, src, type);
                case SockBase.STATE_ACCEPTING:
                    switch (src) {
                    case Fsm.ACTION:
                        switch (type) {
                        case SockBase.ACTION_DONE:
                            usock.state = usock.STATE_LISTENING;
                            return;
                        case SockBase.ACTION_CANCEL:
                            usock.state = usock.STATE_CANCELLING;
                            usock.worker.execute(usock.stop);
                            return;
                        }
                        throw new FsmBadActionException(usock.state, src, type);
                    case SockBase.SRC_FD:
                        switch (type) {
                        case Worker.FD_IN:
                            try {
                                SocketChannel s = usock.s.accept();
                                usock.asock.init(s);
                                usock.asock.accepted();
                                usock.asock.asock(null);
                                usock.asock = null;
                                usock.worker.removeFd(usock.s);
                                usock.state = SockBase.STATE_LISTENING;
                            } catch(IOException ex) {
                                usock.errno = Global.ECONNREFUSED;
                                usock.worker.removeFd(usock.s);
                                usock.state = SockBase.STATE_ACCEPTING_ERROR;
                                usock.fsm.raise(usock.error, SockBase.ACCEPT_ERROR);
                            }
                            return;
                        }
                        break;
                    }
                    throw new FsmBadSourceException(usock.state, src, type);
                case SockBase.STATE_ACCEPTING_ERROR:
                    switch (src) {
                    case Fsm.ACTION:
                        switch (type) {
                        case SockBase.ACTION_ACCEPT:
                            usock.state = SockBase.STATE_ACCEPTING;
                            return;
                        }
                        throw new FsmBadActionException(usock.state, src, type);
                    }
                    throw new FsmBadSourceException(usock.state, src, type);
                case SockBase.STATE_CANCELLING:
                    switch (src) {
                    case SockBase.SRC_TASK_STOP:
                        switch (type) {
                        case Worker.TASK_EXECUTE:
                            usock.worker.removeFd(usock.s);
                            usock.state = SockBase.STATE_LISTENING;
                            usock.asock.fsm.action(SockBase.ACTION_DONE);
                            return;
                        }
                        throw new FsmBadActionException(usock.state, src, type);
                    case SockBase.SRC_FD:
                        switch (type) {
                        case Worker.FD_IN:
                            return;
                        default:
                            throw new FsmBadActionException(usock.state, src, type);
                        }
                    default:
                        throw new FsmBadSourceException(usock.state, src, type);
                    }
                default:
                    throw new FsmBadStateException(usock.state, src, type);
                }
            }

            protected void onShutdown(int src, int type, Object srcObj)
                    throws ErrnoException {
                if (usock.internalTasks(src, type))
                    return;
                if (src == Fsm.ACTION && type == Fsm.STOP) {
                    assert(usock.state != SockBase.STATE_ACCEPTING &&
                            usock.state != SockBase.STATE_CANCELLING);
                    usock.errno = 0;
                    if (usock.state == SockBase.STATE_IDLE)
                        return;
                    if (usock.state == SockBase.STATE_STARTING ||
                            usock.state == SockBase.STATE_ACCEPTING_ERROR ||
                            usock.state == SockBase.STATE_LISTENING) {
                        try {
                            usock.s.close();
                        } catch(IOException ex) { }
                        usock.s = null;
                        usock.state = SockBase.STATE_IDLE;
                        usock.fsm.stopped(SockBase.STOPPED);
                        return;
                    }
                }
                throw new FsmBadStateException(usock.state, src, type);
            }
        };
        this.state = SockBase.STATE_IDLE;
        this.worker = this.fsm.chooseWorker();
        this.fd = new Worker.Fd(SockBase.SRC_FD, this.fsm);
        this.accept = new Worker.Task(SockBase.SRC_TASK_ACCEPT, this.fsm);
        this.stop = new Worker.Task(SockBase.SRC_TASK_STOP, this.fsm);
        this.error = new FsmEvent();
        this.s = null;
        this.asock = null;
    }

    public SockClient asock() {
        return this.asock;
    }

    public void asock(SockClient asock) {
        assert(this.asock == null || this.asock == asock || asock == null);
        this.asock = asock;
    }

    public void start() throws ErrnoException {
        try {
            this.s = ServerSocketChannel.open();
            this.s.configureBlocking(false);
            this.fsm.start();
        } catch(IOException ex) {
            throw new ErrnoException(Global.EINVAL);
        }
    }

    public void bind(SocketAddress addr, int backlog) throws ErrnoException {
        assert(this.state == SockBase.STATE_STARTING);
        try {
            if (!SockBase.isWindows)
                this.s.socket().setReuseAddress(true);
            this.s.socket().bind(addr, backlog);
        } catch(IOException ex) {
            throw new ErrnoException(Global.EADDRINUSE);
        }
        this.fsm.action(SockBase.ACTION_LISTEN);
    }

    public SocketChannel accept() throws ErrnoException {
        try {
            return this.s.accept();
        } catch (ClosedChannelException ex) {
            this.errno = Global.EBADF;
            this.state = SockBase.STATE_ACCEPTING_ERROR;
            this.fsm.raise(this.error, SockBase.ACTION_ERROR);
        } catch (SecurityException ex) {
            this.errno = Global.EACCESS;
            this.state = SockBase.STATE_ACCEPTING_ERROR;
            this.fsm.raise(this.error, SockBase.ACTION_ERROR);
        } catch (IOException ex) {
            this.errno = Global.EIO;
            this.state = SockBase.STATE_ACCEPTING_ERROR;
            this.fsm.raise(this.error, SockBase.ACTION_ERROR);
        }
        return null;
    }

    public void awaitAccept() throws ErrnoException {
        this.worker.execute(accept);
    }

    private boolean internalTasks(int src, int type) {
        switch (src) {
        case SockBase.SRC_TASK_ACCEPT:
            this.worker.addFd(s, fd);
            this.worker.setAccept(s);
            return true;
        }
        return false;
    }
}
