// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   Lb.java

package org.mogware.msgs.protocols.utils;

import org.mogware.msgs.core.Global;
import org.mogware.msgs.core.PipeBase;
import org.mogware.msgs.utils.ErrnoException;
import org.mogware.msgs.utils.Msg;

// Referenced classes of package org.mogware.msgs.protocols.utils:
//            PrioList

public class Lb
{
    public static class Data
    {

        private PrioList.Data priodata;


        public Data()
        {
            priodata = new PrioList.Data();
        }
    }


    public Lb()
    {
    }

    public void add(Data data, PipeBase pipe, int priority)
    {
        priolist.add(data.priodata, pipe, priority);
    }

    public void remove(Data data)
    {
        priolist.remove(data.priodata);
    }

    public void out(Data data)
    {
        priolist.activate(data.priodata);
    }

    public boolean canSend()
    {
        return priolist.isActive();
    }

    public int getPriority()
    {
        return priolist.getPriotity();
    }

    public void send(Msg msg)
        throws ErrnoException
    {
        PipeBase pipe = priolist.getPipe();
        if(pipe == null)
            throw new ErrnoException(0x9523dd4);
        int rc = pipe.send(msg);
        priolist.advance((rc & 1) != 0);
        rc &= -2;
        if(rc != 0)
            throw new ErrnoException(rc);
        else
            return;
    }

    private final PrioList priolist = new PrioList();
}
