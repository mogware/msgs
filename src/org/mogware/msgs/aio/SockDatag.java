package org.mogware.msgs.aio;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import org.mogware.msgs.core.Global;
import org.mogware.msgs.utils.ErrnoException;

public class SockDatag extends SockBase {
    private DatagramChannel s;
    private final Worker worker;
    private final Worker.Fd fd;

    public SockDatag(int src, Fsm owner) {
        final SockDatag usock = this;
        this.fsm = new Fsm(src,  this, owner) {
            protected void onProgress(int src, int type, Object srcObj)
                    throws ErrnoException {
System.out.printf("SockDatag p: state=%d, src=%s, type=%d\n", usock.state, src, type);
                switch (usock.state)
                {
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
                    }
                case SockBase.STATE_STARTING:
                    switch (src) {
                    case Fsm.ACTION:
                        switch (type) {
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
                case SockBase.STATE_ACTIVE:
                default:
                    throw new FsmBadStateException(usock.state, src, type);
                }
            }
            @Override
            protected void onShutdown(int src, int type, Object srcObj)
                    throws ErrnoException {
System.out.printf("SockDatag s: state=%d, src=%s, type=%d\n", usock.state, src, type);
            }
        };
        this.state = SockBase.STATE_IDLE;
        this.worker = this.fsm.chooseWorker();
        this.fd = new Worker.Fd(SockBase.SRC_FD, this.fsm);
        this.s = null;
    }

    public void start() throws ErrnoException {
        try {
            this.s = DatagramChannel.open();
            this.s.configureBlocking(false);
            this.fsm.start();
        } catch(IOException ex) {
            throw new ErrnoException(Global.EINVAL);
        }
    }

    public void bind(SocketAddress addr) throws ErrnoException {
      assert(this.state == SockBase.STATE_STARTING);
      try {
            if (!SockBase.isWindows)
                this.s.socket().setReuseAddress(true);
            this.s.socket().bind(addr);
        } catch(IOException ex) {
            throw new ErrnoException(Global.EADDRINUSE);
        }
    }

    public void connect(SocketAddress addr) throws ErrnoException {
        try {
            this.s.connect(addr);
            this.fsm.action(SockBase.ACTION_STARTED);
        }
        catch(ClosedChannelException ex) {
            throw new ErrnoException(Global.EBADF);
        } catch(SecurityException ex) {
            throw new ErrnoException(Global.EACCESS);
        } catch(IOException ex) {
            throw new ErrnoException(Global.EIO);
        }
    }

    public void listen() throws ErrnoException {
        fsm.action(SockBase.ACTION_STARTED);
    }
}
