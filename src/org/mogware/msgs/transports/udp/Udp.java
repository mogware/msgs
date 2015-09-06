// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   Udp.java

package org.mogware.msgs.transports.udp;

import org.mogware.msgs.core.*;
import org.mogware.msgs.transports.Transport;
import org.mogware.msgs.utils.ErrnoException;

// Referenced classes of package org.mogware.msgs.transports.udp:
//            Budp, Cudp

public class Udp
    implements Transport
{

    public Udp()
    {
    }

    public String name()
    {
        return "udp";
    }

    public int id()
    {
        return -4;
    }

    public EndpointBase bind(Endpoint ep)
        throws ErrnoException
    {
        return Budp.create(ep);
    }

    public EndpointBase connect(Endpoint ep)
        throws ErrnoException
    {
        return Cudp.create(ep);
    }

    public OptSet optset()
    {
        return null;
    }

    public static final int UDP = -4;
}
