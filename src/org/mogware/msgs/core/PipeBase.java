package org.mogware.msgs.core;

import org.mogware.msgs.aio.Fsm;
import org.mogware.msgs.aio.FsmEvent;
import org.mogware.msgs.utils.ErrnoException;
import org.mogware.msgs.utils.Msg;
import org.mogware.msgs.utils.Pair;

public abstract class PipeBase {
    private static final int STATE_IDLE = 1;
    private static final int STATE_ACTIVE = 2;
    private static final int STATE_FAILED = 3;

    private static final int INSTATE_DEACTIVATED = 0;
    private static final int INSTATE_IDLE = 1;
    private static final int INSTATE_RECEIVING = 2;
    private static final int INSTATE_RECEIVED = 3;
    private static final int INSTATE_ASYNC = 4;

    private static final int OUTSTATE_DEACTIVATED = 0;
    private static final int OUTSTATE_IDLE = 1;
    private static final int OUTSTATE_SENDING = 2;
    private static final int OUTSTATE_SENT = 3;
    private static final int OUTSTATE_ASYNC = 4;

    public static final int RELEASE = 1;
    public static final int PARSED = 2;

    private final Fsm fsm;
    private final Sock sock;

    private int state;
    private int instate;
    private int outstate;

    private Object data;

    private final FsmEvent in;
    private final FsmEvent out;

    public PipeBase(EndpointBase epbase) {
        assert (epbase.ep().sock() != null);
        this.fsm = new Fsm(0, this, epbase.ep().sock().fsm()) {
            @Override
            protected void onProgress(int i, int j, Object obj) {
            }
            @Override
            protected void onShutdown(int i, int j, Object obj) {
            }
        };
        this.state = PipeBase.STATE_IDLE;
        this.instate = PipeBase.INSTATE_DEACTIVATED;
        this.outstate = PipeBase.OUTSTATE_DEACTIVATED;
        this.sock = epbase.ep().sock();
        this.in = new FsmEvent();
        this.out = new FsmEvent();
        this.data = null;
    }

    public void start() throws ErrnoException {
        assert(this.state == PipeBase.STATE_IDLE);
        this.state = PipeBase.STATE_ACTIVE;
        this.instate = PipeBase.INSTATE_ASYNC;
        this.outstate = PipeBase.OUTSTATE_IDLE;
        if (this.sock != null) {
            try {
                this.sock.add(this);
            } catch (ErrnoException ex) {
                this.state = PipeBase.STATE_FAILED;
                throw ex;
            }
            this.fsm.raise(this.out, Pipe.OUT);
        }
    }

    public void stop() {
        if (this.state == PipeBase.STATE_ACTIVE)
            this.sock.remove(this);
        this.state = PipeBase.STATE_IDLE;
    }

    public boolean isPeer(int socktype) {
        return this.sock.isPeer(socktype);
    }

    public void setData(Object data) {
        this.data = data;
    }

    public Object getData() {
        return this.data;
    }

    public void received() {
        if (this.instate == PipeBase.INSTATE_RECEIVING) {
            this.instate = PipeBase.INSTATE_RECEIVED;
            return;
        }
        assert(this.instate == PipeBase.INSTATE_ASYNC);
        this.instate = PipeBase.INSTATE_IDLE;
        if (this.sock != null)
            this.fsm.raise(this.in, Pipe.IN);    }

    public void sent() {
        if (this.outstate == PipeBase.OUTSTATE_SENDING) {
            this.outstate = PipeBase.OUTSTATE_SENT;
            return;
        }
        assert(this.outstate == PipeBase.OUTSTATE_ASYNC);
        this.outstate = PipeBase.OUTSTATE_IDLE;
        if (this.sock != null)
            this.fsm.raise(this.out, Pipe.OUT);
    }

    public Object opt(int level, int option) throws ErrnoException {
        return this.sock.optInner(level, option);
    }

    public int send(Msg msg) {
        int rc = 0;
        assert(this.outstate == PipeBase.OUTSTATE_IDLE);
        this.outstate = PipeBase.OUTSTATE_SENDING;
        try {
            this.onSend(msg);
        } catch (ErrnoException ex) {
            rc = ex.errno();
        }
        if (this.outstate == PipeBase.OUTSTATE_SENT) {
            this.outstate = PipeBase.OUTSTATE_IDLE;
            return rc;
        }
        assert(this.outstate == PipeBase.OUTSTATE_SENDING);
        this.outstate = PipeBase.OUTSTATE_ASYNC;
        return rc | PipeBase.RELEASE;
    }

    public Pair<Integer, Msg> recv() {
        int rc = 0;
        Msg msg = null;
        assert(this.instate == PipeBase.INSTATE_IDLE);
        this.instate = PipeBase.INSTATE_RECEIVING;
        try {
            msg = this.onRecv();
        } catch (ErrnoException ex) {
            rc = ex.errno();
        }
        if (this.instate == PipeBase.INSTATE_RECEIVED) {
            this.instate = PipeBase.INSTATE_IDLE;
            return Pair.with(rc, msg);
        }
        assert(this.instate == PipeBase.INSTATE_RECEIVING);
        this.instate = PipeBase.INSTATE_ASYNC;
        return Pair.with(rc | PipeBase.RELEASE, msg);
    }

    protected abstract void onSend(Msg msg)
        throws ErrnoException;

    protected abstract Msg onRecv()
        throws ErrnoException;
}
