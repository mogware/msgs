// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   Ctcp.java

package org.mogware.msgs.transports.tcp;

import java.net.*;
import org.mogware.msgs.aio.*;
import org.mogware.msgs.core.*;
import org.mogware.msgs.transports.Transport;
import org.mogware.msgs.transports.utils.*;
import org.mogware.msgs.utils.ErrnoException;

// Referenced classes of package org.mogware.msgs.transports.tcp:
//            Stcp, Tcp

public class Ctcp
{

    public Ctcp(final Endpoint ep)
        throws ErrnoException
    {
        Ctcp ctcp = this;
        epbase = new EndpointBase(ctcp) {

            protected void onStop()
                throws ErrnoException
            {
                ctcp.fsm.stop();
            }

            final Ctcp val$ctcp;
            final Ctcp this$0;

            
            {
                this.this$0 = Ctcp.this;
                ctcp = ctcp1;
                super(ep);
            }
        }
;
        String addr = epbase.addr();
        int colon = addr.lastIndexOf(':');
        if(colon < 0)
            throw new ErrnoException(0x9523dd7);
        String addrStr = addr.substring(0, colon);
        String portStr = addr.substring(colon + 1);
        Port.resolve(portStr);
        int semicolon = addrStr.indexOf(';');
        String hostname = semicolon >= 0 ? addrStr.substring(semicolon + 1) : addrStr;
        int ipv4only = ((Integer)epbase.opt(1, 14)).intValue();
        if(!Dns.checkHostname(hostname) && Literal.resolve(hostname, ipv4only > 0) == null)
            throw new ErrnoException(0x9523dd7);
        if(semicolon > 0)
        {
            addrStr = addrStr.substring(0, semicolon);
            if(Iface.resolve(addrStr, ipv4only > 0) == null)
                throw new ErrnoException(0x9523dd7);
        }
        fsm = new Fsm(ctcp) {

            protected void onProgress(int src, int type, Object srcObj)
                throws ErrnoException
            {
                switch(ctcp.state)
                {
                case 1: // '\001'
                    switch(src)
                    {
                    case -2: 
                        switch(type)
                        {
                        case -2: 
                            ctcp.startConnecting();
                            return;
                        }
                        throw new FsmBadActionException(ctcp.state, src, type);
                    }
                    throw new FsmBadSourceException(ctcp.state, src, type);

                case 2: // '\002'
                    switch(src)
                    {
                    case 1: // '\001'
                        switch(type)
                        {
                        case 1: // '\001'
                            ctcp.stcp.start(ctcp.usock);
                            ctcp.state = 3;
                            ctcp.epbase.statIncrement(202, -1);
                            ctcp.epbase.statIncrement(101, 1);
                            ctcp.epbase.clearError();
                            return;

                        case 5: // '\005'
                            ctcp.epbase.setError(ctcp.usock.errno());
                            ctcp.usock.stop();
                            ctcp.state = 5;
                            ctcp.epbase.statIncrement(202, -1);
                            ctcp.epbase.statIncrement(105, 1);
                            return;
                        }
                        throw new FsmBadActionException(ctcp.state, src, type);
                    }
                    throw new FsmBadSourceException(ctcp.state, src, type);

                case 3: // '\003'
                    switch(src)
                    {
                    case 2: // '\002'
                        switch(type)
                        {
                        case 1: // '\001'
                            ctcp.stcp.stop();
                            ctcp.state = 4;
                            ctcp.epbase.statIncrement(104, 1);
                            return;
                        }
                        throw new FsmBadActionException(ctcp.state, src, type);
                    }
                    throw new FsmBadSourceException(ctcp.state, src, type);

                case 4: // '\004'
                    switch(src)
                    {
                    case 2: // '\002'
                        switch(type)
                        {
                        case 8: // '\b'
                            return;

                        case 2: // '\002'
                            ctcp.usock.stop();
                            ctcp.state = 5;
                            return;
                        }
                        throw new FsmBadActionException(ctcp.state, src, type);
                    }
                    throw new FsmBadSourceException(ctcp.state, src, type);

                case 5: // '\005'
                    switch(src)
                    {
                    case 1: // '\001'
                        switch(type)
                        {
                        case 8: // '\b'
                            return;

                        case 7: // '\007'
                            ctcp.retry.start();
                            ctcp.state = 6;
                            return;
                        }
                        throw new FsmBadActionException(ctcp.state, src, type);
                    }
                    throw new FsmBadSourceException(ctcp.state, src, type);

                case 6: // '\006'
                    switch(src)
                    {
                    case 2: // '\002'
                        switch(type)
                        {
                        case 1: // '\001'
                            ctcp.retry.stop();
                            ctcp.state = 7;
                            return;
                        }
                        throw new FsmBadActionException(ctcp.state, src, type);
                    }
                    throw new FsmBadSourceException(ctcp.state, src, type);

                case 7: // '\007'
                    switch(src)
                    {
                    case 2: // '\002'
                        switch(type)
                        {
                        case 2: // '\002'
                            ctcp.startConnecting();
                            return;
                        }
                        throw new FsmBadActionException(ctcp.state, src, type);
                    }
                    throw new FsmBadSourceException(ctcp.state, src, type);
                }
                throw new FsmBadStateException(ctcp.state, src, type);
            }

            protected void onShutdown(int src, int type, Object srcObj)
                throws ErrnoException
            {
                if(src == -2 && type == -3)
                {
                    if(!ctcp.stcp.isIdle())
                    {
                        ctcp.epbase.statIncrement(103, 1);
                        ctcp.stcp.stop();
                    }
                    ctcp.state = 8;
                }
                if(ctcp.state == 8)
                {
                    if(!ctcp.stcp.isIdle())
                        return;
                    ctcp.retry.stop();
                    ctcp.usock.stop();
                    ctcp.state = 9;
                }
                if(ctcp.state == 9)
                {
                    if(!ctcp.retry.isIdle() || !ctcp.usock.isIdle())
                    {
                        return;
                    } else
                    {
                        ctcp.state = 1;
                        ctcp.fsm.stoppedNoEvent();
                        ctcp.epbase.stopped();
                        return;
                    }
                } else
                {
                    throw new FsmBadStateException(ctcp.state, src, type);
                }
            }

            final Ctcp val$ctcp;
            final Ctcp this$0;

            
            {
                this.this$0 = Ctcp.this;
                ctcp = ctcp1;
                super(ctx);
            }
        }
;
        state = 1;
        int reconnect_ivl = ((Integer)epbase.opt(1, 6)).intValue();
        int reconnect_ivl_max = ((Integer)epbase.opt(1, 7)).intValue();
        if(reconnect_ivl_max == 0)
            reconnect_ivl_max = reconnect_ivl;
        retry = new Backoff(2, reconnect_ivl, reconnect_ivl_max, fsm);
        usock = new SockClient(1, fsm);
        stcp = new Stcp(2, epbase, fsm);
        fsm.start();
    }

    public static EndpointBase create(Endpoint ep)
        throws ErrnoException
    {
        Ctcp ctcp = new Ctcp(ep);
        return ctcp.epbase;
    }

    private void startConnecting()
        throws ErrnoException
    {
        String addr = epbase.addr();
        int colon = addr.lastIndexOf(':');
        if(colon < 0)
            throw new ErrnoException(0x9523dd7);
        String addrStr = addr.substring(0, colon);
        String portStr = addr.substring(colon + 1);
        int port = Port.resolve(portStr);
        int ipv4only = ((Integer)epbase.opt(1, 14)).intValue();
        int semicolon = addrStr.indexOf(';');
        InetAddress local;
        if(semicolon < 0)
            local = Iface.resolve("*", ipv4only > 0);
        else
            local = Iface.resolve(addrStr.substring(0, semicolon), ipv4only > 0);
        if(local == null)
        {
            retry.start();
            state = 6;
            return;
        }
        InetAddress remote;
        if(semicolon < 0)
            remote = Dns.resolve(addrStr, ipv4only > 0);
        else
            remote = Dns.resolve(addrStr.substring(semicolon + 1), ipv4only > 0);
        if(remote == null)
        {
            retry.start();
            state = 6;
            return;
        }
        try
        {
            usock.start();
        }
        catch(ErrnoException ex)
        {
            retry.start();
            state = 6;
            return;
        }
        usock.opt(StandardSocketOptions.SO_SNDBUF, Integer.valueOf(((Integer)epbase.opt(1, 2)).intValue()));
        usock.opt(StandardSocketOptions.SO_RCVBUF, Integer.valueOf(((Integer)epbase.opt(1, 3)).intValue()));
        usock.opt(StandardSocketOptions.TCP_NODELAY, Boolean.valueOf(((Integer)epbase.opt(-3, 1)).intValue() == 0));
        try
        {
            usock.bind(new InetSocketAddress(local, 0));
        }
        catch(ErrnoException ex)
        {
            retry.start();
            state = 6;
            return;
        }
        usock.connect(new InetSocketAddress(remote, port));
        state = 2;
        epbase.statIncrement(202, 1);
    }

    private static final int STATE_IDLE = 1;
    private static final int STATE_CONNECTING = 2;
    private static final int STATE_ACTIVE = 3;
    private static final int STATE_STOPPING_STCP = 4;
    private static final int STATE_STOPPING_USOCK = 5;
    private static final int STATE_WAITING = 6;
    private static final int STATE_STOPPING_BACKOFF = 7;
    private static final int STATE_STOPPING_STCP_FINAL = 8;
    private static final int STATE_STOPPING = 9;
    private static final int SRC_USOCK = 1;
    private static final int SRC_RECONNECT_TIMER = 2;
    private static final int SRC_STCP = 2;
    private final Fsm fsm;
    private final EndpointBase epbase;
    private final SockClient usock;
    private final Backoff retry;
    private final Stcp stcp;
    private int state;








}
