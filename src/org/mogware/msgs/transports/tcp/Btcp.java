// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   Btcp.java

package org.mogware.msgs.transports.tcp;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import org.mogware.msgs.aio.*;
import org.mogware.msgs.core.*;
import org.mogware.msgs.transports.utils.*;
import org.mogware.msgs.utils.ErrnoException;

// Referenced classes of package org.mogware.msgs.transports.tcp:
//            Atcp

public class Btcp
{

    public Btcp(final Endpoint ep)
        throws ErrnoException
    {
        Btcp btcp = this;
        epbase = new EndpointBase(btcp) {

            protected void onStop()
                throws ErrnoException
            {
                btcp.fsm.stop();
            }

            final Btcp val$btcp;
            final Btcp this$0;

            
            {
                this.this$0 = Btcp.this;
                btcp = btcp1;
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
        int ipv4only = ((Integer)epbase.opt(1, 14)).intValue();
        if(Iface.resolve(addrStr, ipv4only > 0) == null)
            throw new ErrnoException(0x9523de5);
        fsm = new Fsm(btcp) {

            protected void onProgress(int src, int type, Object srcObj)
                throws ErrnoException
            {
                switch(btcp.state)
                {
                case 1: // '\001'
                    switch(src)
                    {
                    case -2: 
                        switch(type)
                        {
                        case -2: 
                            btcp.startListening();
                            return;
                        }
                        throw new FsmBadActionException(btcp.state, src, type);
                    }
                    throw new FsmBadSourceException(btcp.state, src, type);

                case 2: // '\002'
                    if(srcObj == btcp.atcp)
                    {
                        switch(type)
                        {
                        case 34231: 
                            btcp.atcps.add(btcp.atcp);
                            btcp.atcp = null;
                            btcp.startAccepting();
                            return;
                        }
                        throw new FsmBadActionException(btcp.state, src, type);
                    }
                    if(!$assertionsDisabled && src != 2)
                        throw new AssertionError();
                    Atcp atcp = (Atcp)srcObj;
                    switch(type)
                    {
                    case 34232: 
                        atcp.stop();
                        return;

                    case 34233: 
                        btcp.atcps.remove(atcp);
                        return;
                    }
                    throw new FsmBadActionException(btcp.state, src, type);

                case 7: // '\007'
                    switch(src)
                    {
                    case 1: // '\001'
                        switch(type)
                        {
                        case 8: // '\b'
                            return;

                        case 7: // '\007'
                            btcp.retry.start();
                            btcp.state = 6;
                            return;
                        }
                        throw new FsmBadActionException(btcp.state, src, type);
                    }
                    throw new FsmBadSourceException(btcp.state, src, type);

                case 6: // '\006'
                    switch(src)
                    {
                    case 3: // '\003'
                        switch(type)
                        {
                        case 1: // '\001'
                            btcp.retry.stop();
                            btcp.state = 8;
                            return;
                        }
                        throw new FsmBadActionException(btcp.state, src, type);
                    }
                    throw new FsmBadSourceException(btcp.state, src, type);

                case 8: // '\b'
                    switch(src)
                    {
                    case 3: // '\003'
                        switch(type)
                        {
                        case 2: // '\002'
                            btcp.startListening();
                            return;
                        }
                        throw new FsmBadActionException(btcp.state, src, type);
                    }
                    throw new FsmBadSourceException(btcp.state, src, type);

                case 3: // '\003'
                case 4: // '\004'
                case 5: // '\005'
                default:
                    throw new FsmBadStateException(btcp.state, src, type);
                }
            }

            protected void onShutdown(int src, int type, Object srcObj)
                throws ErrnoException
            {
                if(src == -2 && type == -3)
                {
                    btcp.retry.stop();
                    if(btcp.atcp != null)
                    {
                        btcp.atcp.stop();
                        btcp.state = 3;
                    } else
                    {
                        btcp.state = 4;
                    }
                }
                if(btcp.state == 3)
                {
                    if(!btcp.atcp.isIdle())
                        return;
                    btcp.atcp = null;
                    btcp.usock.stop();
                    btcp.state = 4;
                }
                if(btcp.state == 4)
                {
                    if(!btcp.usock.isIdle())
                        return;
                    Atcp atcp;
                    for(Iterator iterator = btcp.atcps.iterator(); iterator.hasNext(); atcp.stop())
                        atcp = (Atcp)iterator.next();

                    btcp.state = 5;
                } else
                if(btcp.state == 5)
                {
                    if(!$assertionsDisabled && (src != 2 || type != 34233))
                        throw new AssertionError();
                    Atcp atcp = (Atcp)srcObj;
                    btcp.atcps.remove(atcp);
                } else
                {
                    throw new FsmBadStateException(btcp.state, src, type);
                }
                if(btcp.atcps.isEmpty())
                {
                    btcp.state = 1;
                    btcp.fsm.stoppedNoEvent();
                    btcp.epbase.stopped();
                }
            }

            static final boolean $assertionsDisabled = !org/mogware/msgs/transports/tcp/Btcp.desiredAssertionStatus();
            final Btcp val$btcp;
            final Btcp this$0;


            
            {
                this.this$0 = Btcp.this;
                btcp = btcp1;
                super(ctx);
            }
        }
;
        state = 1;
        int reconnect_ivl = ((Integer)epbase.opt(1, 6)).intValue();
        int reconnect_ivl_max = ((Integer)epbase.opt(1, 7)).intValue();
        if(reconnect_ivl_max == 0)
            reconnect_ivl_max = reconnect_ivl;
        retry = new Backoff(3, reconnect_ivl, reconnect_ivl_max, fsm);
        usock = new SockServer(1, fsm);
        atcp = null;
        fsm.start();
    }

    public static EndpointBase create(Endpoint ep)
        throws ErrnoException
    {
        Btcp btcp = new Btcp(ep);
        return btcp.epbase;
    }

    private void startListening()
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
        InetAddress sockAddr = Iface.resolve(addrStr, ipv4only > 0);
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
        try
        {
            usock.bind(new InetSocketAddress(sockAddr, port), 100);
        }
        catch(ErrnoException ex)
        {
            usock.stop();
            state = 7;
            return;
        }
        startAccepting();
        state = 2;
    }

    private void startAccepting()
        throws ErrnoException
    {
        atcp = new Atcp(2, epbase, fsm);
        atcp.start(usock);
    }

    private static final int BACKLOG = 100;
    private static final int STATE_IDLE = 1;
    private static final int STATE_ACTIVE = 2;
    private static final int STATE_STOPPING_ATCP = 3;
    private static final int STATE_STOPPING_USOCK = 4;
    private static final int STATE_STOPPING_ATCPS = 5;
    private static final int STATE_WAITING = 6;
    private static final int STATE_CLOSING = 7;
    private static final int STATE_STOPPING_BACKOFF = 8;
    private static final int SRC_USOCK = 1;
    private static final int SRC_ATCP = 2;
    private static final int SRC_RECONNECT_TIMER = 3;
    private final Fsm fsm;
    private final EndpointBase epbase;
    private final SockServer usock;
    private final Backoff retry;
    private Atcp atcp;
    private final List atcps = new LinkedList();
    private int state;











}
