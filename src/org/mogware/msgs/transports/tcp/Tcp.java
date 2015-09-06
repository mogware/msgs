// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   Tcp.java

package org.mogware.msgs.transports.tcp;

import org.mogware.msgs.core.*;
import org.mogware.msgs.transports.Transport;
import org.mogware.msgs.utils.ErrnoException;

// Referenced classes of package org.mogware.msgs.transports.tcp:
//            Btcp, Ctcp

public class Tcp
    implements Transport
{

    public Tcp()
    {
        nodelay = 0;
    }

    public String name()
    {
        return "tcp";
    }

    public int id()
    {
        return -3;
    }

    public EndpointBase bind(Endpoint ep)
        throws ErrnoException
    {
        return Btcp.create(ep);
    }

    public EndpointBase connect(Endpoint ep)
        throws ErrnoException
    {
        return Ctcp.create(ep);
    }

    public OptSet optset()
    {
        final Tcp tcp = this;
        return new OptSet() {

            public void opt(int option, Object val)
                throws ErrnoException
            {
                switch(option)
                {
                case 1: // '\001'
                    tcp.nodelay = ((Integer)val).intValue();
                    break;

                default:
                    throw new ErrnoException(0x9523de3);
                }
            }

            public Object opt(int option)
                throws ErrnoException
            {
                switch(option)
                {
                case 1: // '\001'
                    return Integer.valueOf(tcp.nodelay);
                }
                throw new ErrnoException(0x9523de3);
            }

            final Tcp val$tcp;
            final Tcp this$0;

            
            {
                this.this$0 = Tcp.this;
                tcp = tcp1;
                super();
            }
        }
;
    }

    public static final int TCP = -3;
    public static final int NODELAY = 1;
    private int nodelay;


}
