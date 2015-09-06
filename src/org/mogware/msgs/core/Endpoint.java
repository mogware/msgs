package org.mogware.msgs.core;

import org.mogware.msgs.aio.Ctx;
import org.mogware.msgs.aio.Fsm;
import org.mogware.msgs.aio.FsmBadActionException;
import org.mogware.msgs.aio.FsmBadSourceException;
import org.mogware.msgs.aio.FsmBadStateException;
import org.mogware.msgs.transports.Transport;
import org.mogware.msgs.utils.ErrnoException;

public class Endpoint {
    private static final int STATE_IDLE = 1;
    private static final int STATE_ACTIVE = 2;
    private static final int STATE_STOPPING = 3;

    private static final int ACTION_STOPPED = 1;

    public static final int STOPPED = 1;

    private final int id;
    private final Fsm fsm;
    private final EndpointBase epbase;
    private final Sock sock;

    private int state;
    private final String addr;
    private int errno;

    public Endpoint(int src, Sock sock, int id, Transport transport,
            boolean bind, String addr) throws ErrnoException {
        final Endpoint ep = this;
        this.fsm = new Fsm(src, this, sock.fsm()) {
            @Override
            protected void onProgress(int src, int type, Object srcObj) {
                switch (ep.state) {
                case Endpoint.STATE_IDLE:
                    switch (src) {
                    case Fsm.ACTION:
                        switch (type) {
                        case Fsm.START:
                            ep.state = Endpoint.STATE_ACTIVE;
                            return;
                        default:
                            throw new FsmBadActionException(ep.state, src, type);
                        }
                    default:
                        throw new FsmBadSourceException(ep.state, src, type);
                    }
                case Endpoint.STATE_ACTIVE:
                    throw new FsmBadSourceException(ep.state, src, type);
                default:
                    throw new FsmBadStateException(ep.state, src, type);
                }
            }
            @Override
            protected void onShutdown(int src, int type, Object srcObj)
                    throws ErrnoException {
                if (src == Fsm.ACTION && type == Fsm.STOP) {
                    ep.epbase.onStop();
                    ep.state = 3;
                    return;
                }
                if (ep.state == Endpoint.STATE_STOPPING) {
                    if (src != Fsm.ACTION || type != Endpoint.ACTION_STOPPED)
                        return;
                    ep.state = 1;
                    ep.fsm.stopped(1);
                    return;
                }
                throw new FsmBadStateException(ep.state, src, type);
            }
        };
        this.id = id;
        this.state = Endpoint.STATE_IDLE;
        this.sock = sock;
        this.addr = addr;
        this.errno = 0;
        this.epbase = bind ? transport.bind(ep) : transport.connect(ep);
    }

    public void start() throws ErrnoException {
        this.fsm.start();
    }

    public void stop() throws ErrnoException {
        this.fsm.stop();
    }

    public void stopped() {
        this.fsm.stoppedAction(1);
    }

    public int id() {
        return this.id;
    }

    public Ctx ctx() {
        return this.sock.ctx();
    }

    public Sock sock() {
        return this.sock;
    }

    public String addr() {
        return this.addr;
    }

    public Object opt(int level, int option) throws ErrnoException {
        return this.sock.optInner(level, option);
    }

    public boolean isPeer(int socktype) {
        return this.sock.isPeer(socktype);
    }

    public void setError(int errnum) {
        if (this.errno == errnum)
            return;
        if (this.errno == 0)
            sock.statIncrement(Transport.STAT_CURRENT_EP_ERRORS, 1);
        errno = errnum;
        sock.reportError(this, errnum);
    }

    public void clearError() {
        if (errno == 0)
            return;
        this.sock.statIncrement(Transport.STAT_CURRENT_EP_ERRORS, -1);
        this.errno = 0;
        this.sock.reportError(this, 0);
    }

    public void statIncrement(int name, int increment) {
        this.sock.statIncrement(name, increment);
    }
}
