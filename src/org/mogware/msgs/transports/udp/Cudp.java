// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   Cudp.java

package org.mogware.msgs.transports.udp;

import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import org.mogware.msgs.aio.*;
import org.mogware.msgs.core.*;
import org.mogware.msgs.transports.Transport;
import org.mogware.msgs.transports.utils.*;
import org.mogware.msgs.utils.ErrnoException;

// Referenced classes of package org.mogware.msgs.transports.udp:
//            Sudp

public class Cudp
{

    public Cudp(final Endpoint ep)
        throws ErrnoException
    {
        Cudp cudp = this;
        epbase = new EndpointBase(cudp) {

            protected void onStop()
                throws ErrnoException
            {
                cudp.fsm.stop();
            }

            final Cudp val$cudp;
            final Cudp this$0;

            
            {
                this.this$0 = Cudp.this;
                cudp = cudp1;
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
        fsm = new Fsm(cudp) {

            protected void onProgress(int src, int type, Object srcObj)
                throws ErrnoException
            {
                System.out.printf("Cudp p: state=%d, src=%s, type=%d\n", new Object[] {
                    Integer.valueOf(cudp.state), Integer.valueOf(src), Integer.valueOf(type)
                });
                switch(cudp.state)
                {
                case 1: // '\001'
                    switch(src)
                    {
                    case -2: 
                        switch(type)
                        {
                        case -2: 
                            cudp.startConnecting();
                            return;
                        }
                        throw new FsmBadActionException(cudp.state, src, type);
                    }
                    throw new FsmBadSourceException(cudp.state, src, type);
                }
                throw new FsmBadStateException(cudp.state, src, type);
            }

            protected void onShutdown(int src, int type, Object srcObj)
                throws ErrnoException
            {
                System.out.printf("Cudp s: state=%d, src=%s, type=%d\n", new Object[] {
                    Integer.valueOf(cudp.state), Integer.valueOf(src), Integer.valueOf(type)
                });
                if(src == -2 && type == -3)
                {
                    if(!cudp.sudp.isIdle())
                    {
                        cudp.epbase.statIncrement(103, 1);
                        cudp.sudp.stop();
                    }
                    cudp.state = 8;
                }
                if(cudp.state == 8)
                {
                    if(!cudp.sudp.isIdle())
                        return;
                    cudp.retry.stop();
                    cudp.usock.stop();
                    cudp.state = 9;
                }
                if(cudp.state == 9)
                {
                    if(!cudp.retry.isIdle() || !cudp.usock.isIdle())
                    {
                        return;
                    } else
                    {
                        cudp.state = 1;
                        cudp.fsm.stoppedNoEvent();
                        cudp.epbase.stopped();
                        return;
                    }
                } else
                {
                    throw new FsmBadStateException(cudp.state, src, type);
                }
            }

            final Cudp val$cudp;
            final Cudp this$0;

            
            {
                this.this$0 = Cudp.this;
                cudp = cudp1;
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
        usock = new SockDatag(1, fsm);
        sudp = new Sudp(2, epbase, fsm);
        fsm.start();
    }

    public static EndpointBase create(Endpoint ep)
        throws ErrnoException
    {
        Cudp cudp = new Cudp(ep);
        return cudp.epbase;
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
        sudp.start(usock);
        state = 3;
        epbase.statIncrement(101, 1);
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
    private final SockDatag usock;
    private final Backoff retry;
    private final Sudp sudp;
    private int state;








}
