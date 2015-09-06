// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   Atcp.java

package org.mogware.msgs.transports.tcp;

import java.net.SocketOption;
import java.net.StandardSocketOptions;
import org.mogware.msgs.aio.*;
import org.mogware.msgs.core.EndpointBase;
import org.mogware.msgs.core.Global;
import org.mogware.msgs.transports.Transport;
import org.mogware.msgs.utils.ErrnoException;

// Referenced classes of package org.mogware.msgs.transports.tcp:
//            Stcp, Tcp

public class Atcp
{

    public Atcp(final int src, EndpointBase epbase, Fsm owner)
    {
        Atcp atcp = this;
        fsm = new Fsm(this, owner, atcp) {

            protected void onProgress(int src, int type, Object srcObj)
                throws ErrnoException
            {
                switch(atcp.state)
                {
                case 1: // '\001'
                    switch(src)
                    {
                    case -2: 
                        switch(type)
                        {
                        case -2: 
                            atcp.usock.accept(atcp.listener);
                            atcp.state = 2;
                            return;
                        }
                        throw new FsmBadActionException(atcp.state, src, type);
                    }
                    throw new FsmBadSourceException(atcp.state, src, type);

                case 2: // '\002'
                    switch(src)
                    {
                    case 1: // '\001'
                        switch(type)
                        {
                        case 2: // '\002'
                            atcp.usock.opt(StandardSocketOptions.SO_SNDBUF, Integer.valueOf(((Integer)atcp.epbase.opt(1, 2)).intValue()));
                            atcp.usock.opt(StandardSocketOptions.SO_RCVBUF, Integer.valueOf(((Integer)atcp.epbase.opt(1, 3)).intValue()));
                            atcp.usock.opt(StandardSocketOptions.TCP_NODELAY, Boolean.valueOf(((Integer)atcp.epbase.opt(-3, 1)).intValue() == 0));
                            atcp.listener.swapOwner(atcp.owner);
                            atcp.listener = null;
                            atcp.owner.src = -1;
                            atcp.owner.fsm = null;
                            atcp.fsm.raise(atcp.accepted, 34231);
                            atcp.usock.activate();
                            atcp.stcp.start(atcp.usock);
                            atcp.state = 3;
                            atcp.epbase.statIncrement(102, 1);
                            return;
                        }
                        throw new FsmBadActionException(atcp.state, src, type);

                    case 3: // '\003'
                        switch(type)
                        {
                        case 6: // '\006'
                            atcp.epbase.setError(atcp.listener.errno());
                            atcp.epbase.statIncrement(107, 1);
                            atcp.usock.accept(listener);
                            return;
                        }
                        throw new FsmBadActionException(atcp.state, src, type);
                    }
                    throw new FsmBadSourceException(atcp.state, src, type);

                case 3: // '\003'
                    switch(src)
                    {
                    case 2: // '\002'
                        switch(type)
                        {
                        case 1: // '\001'
                            atcp.stcp.stop();
                            atcp.state = 4;
                            atcp.epbase.statIncrement(104, 1);
                            return;
                        }
                        throw new FsmBadActionException(atcp.state, src, type);
                    }
                    throw new FsmBadSourceException(atcp.state, src, type);

                case 4: // '\004'
                    switch(src)
                    {
                    case 2: // '\002'
                        switch(type)
                        {
                        case 8: // '\b'
                            return;

                        case 2: // '\002'
                            atcp.usock.stop();
                            atcp.state = 5;
                            return;
                        }
                        throw new FsmBadActionException(atcp.state, src, type);
                    }
                    throw new FsmBadSourceException(atcp.state, src, type);

                case 5: // '\005'
                    switch(src)
                    {
                    case 1: // '\001'
                        switch(type)
                        {
                        case 8: // '\b'
                            return;

                        case 7: // '\007'
                            atcp.fsm.raise(atcp.done, 34232);
                            atcp.state = 6;
                            return;
                        }
                        throw new FsmBadActionException(atcp.state, src, type);
                    }
                    throw new FsmBadSourceException(atcp.state, src, type);
                }
                throw new FsmBadStateException(atcp.state, src, type);
            }

            protected void onShutdown(int src, int type, Object srcObj)
                throws ErrnoException
            {
                if(src == -2 && type == -3)
                {
                    if(!atcp.stcp.isIdle())
                    {
                        atcp.epbase.statIncrement(103, 1);
                        atcp.stcp.stop();
                    }
                    atcp.state = 7;
                }
                if(atcp.state == 7)
                {
                    if(!atcp.stcp.isIdle())
                        return;
                    atcp.usock.stop();
                    atcp.state = 8;
                }
                if(atcp.state == 8)
                {
                    if(!atcp.usock.isIdle())
                        return;
                    if(atcp.listener != null)
                    {
                        if(!$assertionsDisabled && atcp.owner.fsm == null)
                            throw new AssertionError();
                        atcp.listener.swapOwner(atcp.owner);
                        atcp.listener = null;
                        atcp.owner.src = -1;
                        atcp.owner.fsm = null;
                    }
                    atcp.state = 1;
                    atcp.fsm.stopped(34233);
                    return;
                } else
                {
                    throw new FsmBadStateException(atcp.state, src, type);
                }
            }

            static final boolean $assertionsDisabled = !org/mogware/msgs/transports/tcp/Atcp.desiredAssertionStatus();
            final Atcp val$atcp;
            final Atcp this$0;


            
            {
                this.this$0 = Atcp.this;
                atcp = atcp1;
                super(src, srcObj, owner);
            }
        }
;
        state = 1;
        this.epbase = epbase;
        usock = new SockClient(1, fsm);
        listener = null;
        this.owner.src = -1;
        this.owner.fsm = null;
        stcp = new Stcp(2, this.epbase, fsm);
    }

    public boolean isIdle()
    {
        return fsm.isIdle();
    }

    public void start(SockServer listener)
        throws ErrnoException
    {
        if(!$assertionsDisabled && state != 1)
        {
            throw new AssertionError();
        } else
        {
            this.listener = listener;
            owner.src = 3;
            owner.fsm = fsm;
            this.listener.swapOwner(owner);
            fsm.start();
            return;
        }
    }

    public void stop()
        throws ErrnoException
    {
        fsm.stop();
    }

    public static final int ACCEPTED = 34231;
    public static final int ERROR = 34232;
    public static final int STOPPED = 34233;
    private static final int STATE_IDLE = 1;
    private static final int STATE_ACCEPTING = 2;
    private static final int STATE_ACTIVE = 3;
    private static final int STATE_STOPPING_STCP = 4;
    private static final int STATE_STOPPING_USOCK = 5;
    private static final int STATE_DONE = 6;
    private static final int STATE_STOPPING_STCP_FINAL = 7;
    private static final int STATE_STOPPING = 8;
    private static final int SRC_USOCK = 1;
    private static final int SRC_STCP = 2;
    private static final int SRC_LISTENER = 3;
    private final Fsm fsm;
    private final EndpointBase epbase;
    private SockClient usock;
    private final Stcp stcp;
    private SockServer listener;
    private final org.mogware.msgs.aio.Fsm.Owner owner = new org.mogware.msgs.aio.Fsm.Owner();
    private final FsmEvent accepted = new FsmEvent();
    private final FsmEvent done = new FsmEvent();
    private int state;
    static final boolean $assertionsDisabled = !org/mogware/msgs/transports/tcp/Atcp.desiredAssertionStatus();












}
