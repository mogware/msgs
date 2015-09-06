package org.mogware.msgs.aio;

import org.mogware.msgs.utils.ErrnoException;

public abstract class Fsm {
    private static final int STATE_IDLE = 1;
    private static final int STATE_ACTIVE = 2;
    private static final int STATE_STOPPING = 3;

    public static final int ACTION = -2;
    public static final int START = -2;
    public static final int STOP = -3;

    private int state;

    private int src;
    private Fsm owner;

    private final Object srcObj;
    private final Ctx ctx;
    private final FsmEvent stopped;

    public Fsm(Ctx ctx) {
        this.state = Fsm.STATE_IDLE;
        this.src = -1;
        this.srcObj = null;
        this.owner = null;
        this.ctx = ctx;
        this.stopped = new FsmEvent();
    }

    public Fsm(int src, Object srcObj, Fsm owner) {
        this.state = Fsm.STATE_IDLE;
        this.src = src;
        this.srcObj = srcObj;
        this.owner = owner;
        this.ctx = owner.ctx;
        this.stopped = new FsmEvent();
    }

    public Ctx ctx() {
        return this.ctx;
    }

    public void feed(int src, int type, Object srcObj) throws ErrnoException {
        if (this.state != Fsm.STATE_STOPPING)
            this.onProgress(src, type, srcObj);
        else
            this.onShutdown(src, type, srcObj);
    }

    public void start() throws ErrnoException {
        assert(this.isIdle());
        this.onProgress(Fsm.ACTION, Fsm.START, null);
        this.state = Fsm.STATE_ACTIVE;
    }

    public boolean isIdle() {
      return this.state == Fsm.STATE_IDLE && !this.stopped.active();
    }

    public void stop() throws ErrnoException {
        if (this.state != Fsm.STATE_ACTIVE)
            return;
        this.state = Fsm.STATE_STOPPING;
        this.onShutdown(Fsm.ACTION, Fsm.STOP, null);
    }

    public void stopped(int type) {
        assert(this.state == Fsm.STATE_STOPPING);
        this.raise(this.stopped, type);
        this.state = Fsm.STATE_IDLE;
    }

    public void stoppedNoEvent() {
        assert(this.state == Fsm.STATE_STOPPING);
        this.state = Fsm.STATE_IDLE;
    }

    public void stoppedAction(int type) {
        this.stopped.fsm = this;
        this.stopped.src = Fsm.ACTION;
        this.stopped.srcObj = null;
        this.stopped.type = type;
        ctx.raise(this.stopped);
    }

    public void swapOwner(Owner owner) {
        int oldsrc = this.src;
        Fsm oldowner = this.owner;
        this.src = owner.src;
        this.owner = owner.fsm;
        owner.src = oldsrc;
        owner.fsm = oldowner;
    }

    public Worker chooseWorker() {
        return this.ctx.chooseWorker();
    }

    public void action(int type) throws ErrnoException {
        assert(type > 0);
        this.feed(Fsm.ACTION, type, null);
    }

    public void raise(FsmEvent event, int type) {
        event.fsm = owner;
        event.src = src;
        event.srcObj = srcObj;
        event.type = type;
        this.ctx.raise(event);
    }

    public void raiseTo(Fsm dst, FsmEvent event, int src,
            int type, Object srcObj) {
        event.fsm = dst;
        event.src = src;
        event.srcObj = srcObj;
        event.type = type;
        this.ctx.raiseTo(event);
    }

    public static class Owner {
        public int src;
        public Fsm fsm;
    }

    protected abstract void onProgress(int i, int j, Object obj)
        throws ErrnoException;

    protected abstract void onShutdown(int i, int j, Object obj)
        throws ErrnoException;
}
