// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   Sitc.java

package org.mogware.msgs.transports.itc;

import org.mogware.msgs.aio.*;
import org.mogware.msgs.core.*;
import org.mogware.msgs.utils.ErrnoException;
import org.mogware.msgs.utils.Msg;

// Referenced classes of package org.mogware.msgs.transports.itc:
//            MsgQueue

public class Sitc
{

    public Sitc(final int src, final EndpointBase epbase, Fsm owner)
        throws ErrnoException
    {
        Sitc sitc = this;
        fsm = new Fsm(this, owner, sitc) {

            protected void onProgress(int src, int type, Object srcObj)
                throws ErrnoException
            {
                switch(sitc.state)
                {
                case 1: // '\001'
                    switch(src)
                    {
                    case -2: 
                        switch(type)
                        {
                        case -2: 
                            sitc.state = 2;
                            return;
                        }
                        throw new FsmBadActionException(sitc.state, src, type);
                    }
                    throw new FsmBadSourceException(sitc.state, src, type);

                case 2: // '\002'
                    switch(src)
                    {
                    case -2: 
                        switch(type)
                        {
                        case 1: // '\001'
                            sitc.state = 3;
                            return;
                        }
                        throw new FsmBadActionException(sitc.state, src, type);

                    case 27713: 
                        switch(type)
                        {
                        case 2: // '\002'
                            sitc.peer = (Sitc)srcObj;
                            sitc.pipebase.start();
                            sitc.state = 4;
                            sitc.fsm.raiseTo(sitc.peer.fsm, sitc.connect, 27713, 3, sitc);
                            return;
                        }
                        throw new FsmBadActionException(sitc.state, src, type);
                    }
                    throw new FsmBadSourceException(sitc.state, src, type);

                case 3: // '\003'
                    switch(src)
                    {
                    case 27713: 
                        switch(type)
                        {
                        case 2: // '\002'
                            sitc.pipebase.start();
                            sitc.state = 4;
                            return;

                        case 3: // '\003'
                            sitc.pipebase.start();
                            sitc.state = 4;
                            return;
                        }
                        throw new FsmBadActionException(sitc.state, src, type);
                    }
                    throw new FsmBadSourceException(sitc.state, src, type);

                case 4: // '\004'
                    switch(src)
                    {
                    case 27713: 
                        switch(type)
                        {
                        case 4: // '\004'
                            boolean isEmpty = sitc.queue.isEmpty();
                            try
                            {
                                sitc.queue.send(sitc.peer.msg);
                            }
                            catch(ErrnoException ex)
                            {
                                sitc.flags = sitc.flags | 2;
                                return;
                            }
                            sitc.peer.msg = new Msg(0);
                            if(isEmpty)
                                sitc.pipebase.received();
                            sitc.fsm.raiseTo(sitc.peer.fsm, sitc.peer.received, 27713, 5, sitc);
                            return;

                        case 5: // '\005'
                            if(!$assertionsDisabled && (sitc.flags & 1) == 0)
                            {
                                throw new AssertionError();
                            } else
                            {
                                sitc.pipebase.sent();
                                sitc.flags = sitc.flags & -2;
                                return;
                            }

                        case 6: // '\006'
                            sitc.pipebase.stop();
                            sitc.fsm.raiseTo(sitc.peer.fsm, sitc.peer.disconnect, 27713, 6, sitc);
                            sitc.state = 5;
                            return;
                        }
                        throw new FsmBadActionException(sitc.state, src, type);
                    }
                    throw new FsmBadSourceException(sitc.state, src, type);

                case 5: // '\005'
                    switch(src)
                    {
                    case 27713: 
                        switch(type)
                        {
                        case 5: // '\005'
                            return;
                        }
                        throw new FsmBadActionException(sitc.state, src, type);
                    }
                    throw new FsmBadSourceException(sitc.state, src, type);
                }
                throw new FsmBadStateException(sitc.state, src, type);
            }

            protected void onShutdown(int src, int type, Object srcObj)
            {
                sitc.shutdownEvents(src, type, srcObj);
                if(sitc.state != 7)
                    return;
                if(sitc.received.active() || sitc.disconnect.active())
                {
                    return;
                } else
                {
                    sitc.fsm.stopped(7);
                    return;
                }
            }

            static final boolean $assertionsDisabled = !org/mogware/msgs/transports/itc/Sitc.desiredAssertionStatus();
            final Sitc val$sitc;
            final Sitc this$0;


            
            {
                this.this$0 = Sitc.this;
                sitc = sitc1;
                super(src, srcObj, owner);
            }
        }
;
        pipebase = new PipeBase(sitc) {

            protected void onSend(Msg msg)
                throws ErrnoException
            {
                if(sitc.state == 5)
                    throw new ErrnoException(0x9523de2);
                if(!$assertionsDisabled && sitc.state != 4)
                    throw new AssertionError();
                if(!$assertionsDisabled && (sitc.flags & 1) != 0)
                {
                    throw new AssertionError();
                } else
                {
                    sitc.msg = msg;
                    sitc.flags = sitc.flags | 1;
                    sitc.fsm.raiseTo(sitc.peer.fsm, sitc.peer.sent, 27713, 4, sitc);
                    return;
                }
            }

            protected Msg onRecv()
                throws ErrnoException
            {
                if(!$assertionsDisabled && sitc.state != 4 && sitc.state != 5)
                    throw new AssertionError();
                Msg msg = sitc.queue.recv();
                if(sitc.state != 5 && (sitc.flags & 2) != 0)
                    try
                    {
                        sitc.queue.send(sitc.peer.msg);
                        sitc.peer.msg = new Msg(0);
                        sitc.fsm.raiseTo(sitc.peer.fsm, sitc.peer.received, 27713, 5, sitc);
                        sitc.flags = sitc.flags & -3;
                    }
                    catch(ErrnoException ex) { }
                if(!sitc.queue.isEmpty())
                    sitc.pipebase.received();
                return msg;
            }

            static final boolean $assertionsDisabled = !org/mogware/msgs/transports/itc/Sitc.desiredAssertionStatus();
            final Sitc val$sitc;
            final Sitc this$0;


            
            {
                this.this$0 = Sitc.this;
                sitc = sitc1;
                super(epbase);
            }
        }
;
        state = 1;
        flags = 0;
        peer = null;
        int rcvbuf = ((Integer)epbase.opt(1, 3)).intValue();
        queue = new MsgQueue(rcvbuf);
        msg = new Msg(0);
        connect = new FsmEvent();
        sent = new FsmEvent();
        received = new FsmEvent();
        disconnect = new FsmEvent();
    }

    public void connect(Fsm peer)
        throws ErrnoException
    {
        fsm.start();
        fsm.raiseTo(peer, connect, 27713, 1, this);
    }

    public void accept(Sitc peer)
        throws ErrnoException
    {
        if(!$assertionsDisabled && this.peer != null)
        {
            throw new AssertionError();
        } else
        {
            this.peer = peer;
            fsm.raiseTo(peer.fsm, connect, 27713, 2, this);
            fsm.start();
            fsm.action(1);
            return;
        }
    }

    public void stop()
        throws ErrnoException
    {
        fsm.stop();
    }

    public boolean isIdle()
    {
        return fsm.isIdle();
    }

    protected void shutdownEvents(int src, int type, Object srcObj)
    {
        switch(src)
        {
        case -2: 
            switch(type)
            {
            case -3: 
                if(state != 1 && state != 5)
                {
                    pipebase.stop();
                    fsm.raiseTo(peer.fsm, peer.disconnect, 27713, 6, this);
                    state = 6;
                } else
                {
                    state = 7;
                }
                return;
            }
            // fall through

        case 27713: 
            switch(type)
            {
            case 5: // '\005'
                return;
            }
            // fall through

        default:
            switch(state)
            {
            case 6: // '\006'
                switch(src)
                {
                case 27713: 
                    switch(type)
                    {
                    case 6: // '\006'
                        state = 7;
                        return;
                    }
                    throw new FsmBadActionException(state, src, type);
                }
                throw new FsmBadSourceException(state, src, type);
            }
            break;
        }
        throw new FsmBadStateException(state, src, type);
    }

    public static final int CONNECT = 1;
    public static final int READY = 2;
    public static final int ACCEPTED = 3;
    public static final int SENT = 4;
    public static final int RECEIVED = 5;
    public static final int DISCONNECT = 6;
    public static final int STOPPED = 7;
    public static final int SRC_PEER = 27713;
    private static final int STATE_IDLE = 1;
    private static final int STATE_CONNECTING = 2;
    private static final int STATE_READY = 3;
    private static final int STATE_ACTIVE = 4;
    private static final int STATE_DISCONNECTED = 5;
    private static final int STATE_STOPPING_PEER = 6;
    private static final int STATE_STOPPING = 7;
    private static final int ACTION_READY = 1;
    private static final int FLAG_SENDING = 1;
    private static final int FLAG_RECEIVING = 2;
    private final Fsm fsm;
    private final PipeBase pipebase;
    private int state;
    private int flags;
    private Sitc peer;
    private MsgQueue queue;
    private Msg msg;
    private FsmEvent connect;
    private FsmEvent sent;
    private FsmEvent received;
    private FsmEvent disconnect;
    static final boolean $assertionsDisabled = !org/mogware/msgs/transports/itc/Sitc.desiredAssertionStatus();
















}
