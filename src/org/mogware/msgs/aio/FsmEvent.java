package org.mogware.msgs.aio;

import org.mogware.msgs.utils.ErrnoException;

public class FsmEvent {
    public int src;
    public Object srcObj;
    public int type;
    public Fsm fsm;
    private int ref;

    public FsmEvent() {
        this.fsm = null;
        this.src = -1;
        this.srcObj = null;
        this.type = -1;
        this.ref = 0;
    }

    public void incref() {
        this.ref++;
    }

    public void decref() {
        this.ref--;
    }

    public boolean active() {
        return this.ref != 0;
    }

    public void process() throws ErrnoException {
        int src = this.src;
        int type = this.type;
        Object srcObj = this.srcObj;
        this.src = -1;
        this.type = -1;
        this.srcObj = null;
        fsm.feed(src, type, srcObj);
    }
}
