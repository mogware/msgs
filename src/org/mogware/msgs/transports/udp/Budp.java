// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   Budp.java

package org.mogware.msgs.transports.udp;

import org.mogware.msgs.aio.Fsm;
import org.mogware.msgs.aio.SockDatag;
import org.mogware.msgs.core.Endpoint;
import org.mogware.msgs.core.EndpointBase;
import org.mogware.msgs.utils.ErrnoException;

public class Budp
{

    public Budp(Endpoint ep)
        throws ErrnoException
    {
    }

    public static EndpointBase create(Endpoint ep)
        throws ErrnoException
    {
        Budp budp = new Budp(ep);
        return budp.epbase;
    }

    private Fsm fsm;
    private EndpointBase epbase;
    private SockDatag usock;
}
