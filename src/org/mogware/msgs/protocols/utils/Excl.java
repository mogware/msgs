package org.mogware.msgs.protocols.utils;

import org.mogware.msgs.core.Global;
import org.mogware.msgs.core.PipeBase;
import org.mogware.msgs.utils.*;

public class Excl {
    private PipeBase pipe;
    private PipeBase inPipe;
    private PipeBase outPipe;

    public Excl() {
        this.pipe = null;
        this.inPipe = null;
        this.outPipe = null;
    }

    public void add(PipeBase pipe) throws ErrnoException {
        if (this.pipe != null)
            throw new ErrnoException(Global.EISCONN);
        this.pipe = pipe;
    }

    public void remove(PipeBase pipe) {
        this.pipe = null;
        this.inPipe = null;
        this.outPipe = null;
    }

    public void in(PipeBase pipe) {
        this.inPipe = pipe;
    }

    public void out(PipeBase pipe) {
        this.outPipe = pipe;
    }

    public void send(Msg msg) throws ErrnoException {
        if (outPipe == null)
            throw new ErrnoException(Global.EAGAIN);
        int rc = outPipe.send(msg);
        if ((rc & PipeBase.RELEASE) != 0)
            outPipe = null;
        rc &= ~PipeBase.RELEASE;
        if (rc != 0)
            throw new ErrnoException(rc);
    }

    public Msg recv() throws ErrnoException {
        if (this.inPipe == null)
            throw new ErrnoException(Global.EAGAIN);
        Pair<Integer, Msg> ret = this.inPipe.recv();
        int rc = ret.val0();
        Msg msg = ret.val1();
        if((rc & PipeBase.RELEASE) != 0)
            this.inPipe = null;
        rc &= ~PipeBase.RELEASE;
        if (rc != 0)
            throw new ErrnoException(rc);
        return msg;
    }

    public boolean canSend() {
        return this.outPipe != null;
    }

    public boolean canRecv() {
        return this.inPipe != null;
    }
}
