package org.mogware.msgs.protocols.utils;

import org.mogware.msgs.core.Global;
import org.mogware.msgs.core.PipeBase;
import org.mogware.msgs.utils.ErrnoException;
import org.mogware.msgs.utils.Msg;
import org.mogware.msgs.utils.Pair;

public class Fq {
    private final PrioList priolist;

    public Fq() {
        this.priolist = new PrioList();
    }

    public void add(Data data, PipeBase pipe, int priority) {
        priolist.add(data.priodata, pipe, priority);
    }

    public void remove(Data data) {
        priolist.remove(data.priodata);
    }

    public void in(Data data) {
        priolist.activate(data.priodata);
    }

    public boolean canRecv() {
        return priolist.isActive();
    }

    public Msg recv() throws ErrnoException {
        PipeBase pipe = priolist.getPipe();
        if (pipe == null)
            throw new ErrnoException(Global.EAGAIN);
        Pair<Integer, Msg> ret = pipe.recv();
        int rc = ret.val0();
        Msg msg = ret.val1();
        priolist.advance((rc & PipeBase.RELEASE) != 0);
        rc &= ~PipeBase.RELEASE;
        if (rc != 0)
            throw new ErrnoException(rc);
        return msg;
    }

    public static class Data {
        private PrioList.Data priodata;

        public Data() {
            this.priodata = new PrioList.Data();
        }
    }
}
