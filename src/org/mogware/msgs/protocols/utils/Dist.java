package org.mogware.msgs.protocols.utils;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.mogware.msgs.core.PipeBase;
import org.mogware.msgs.utils.ErrnoException;
import org.mogware.msgs.utils.Msg;

public class Dist
{
    private final List<Data> pipes;

    public Dist() {
        this.pipes = new LinkedList<>();
    }

    public void add(Data data, PipeBase pipe) {
        data.pipe = pipe;
    }

    public void remove(Data data) {
        this.pipes.remove(data);
    }

    public void out(Data data) {
        this.pipes.add(data);
    }

    public void send(Msg msg) throws ErrnoException {
        if (pipes.isEmpty())
            return;
        Iterator<Data> it = this.pipes.iterator();
        while (it.hasNext()) {
            Data data = it.next();
            int rc = data.pipe.send(new Msg(msg));
            if((rc & PipeBase.RELEASE) != 0)
                it.remove();
            rc &= ~PipeBase.RELEASE;
            if(rc != 0)
                throw new ErrnoException(rc);
        }
    }

    public static class Data {
        private PipeBase pipe;
    }
}
