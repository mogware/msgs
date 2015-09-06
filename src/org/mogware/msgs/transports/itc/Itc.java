// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   Itc.java

package org.mogware.msgs.transports.itc;

import org.mogware.msgs.core.*;
import org.mogware.msgs.transports.Transport;
import org.mogware.msgs.utils.ErrnoException;

// Referenced classes of package org.mogware.msgs.transports.itc:
//            Bitc, Citc

public class Itc
    implements Transport
{

    public Itc()
    {
    }

    public String name()
    {
        return "itc";
    }

    public int id()
    {
        return -1;
    }

    public EndpointBase bind(Endpoint ep)
        throws ErrnoException
    {
        return Bitc.create(ep);
    }

    public EndpointBase connect(Endpoint ep)
        throws ErrnoException
    {
        return Citc.create(ep);
    }

    public OptSet optset()
    {
        return null;
    }

    public static final int ITC = -1;
}
