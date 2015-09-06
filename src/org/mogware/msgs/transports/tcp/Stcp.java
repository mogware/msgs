// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   Stcp.java

package org.mogware.msgs.transports.tcp;

import java.nio.ByteBuffer;
import org.mogware.msgs.aio.*;
import org.mogware.msgs.core.EndpointBase;
import org.mogware.msgs.core.PipeBase;
import org.mogware.msgs.transports.utils.StreamHdr;
import org.mogware.msgs.utils.*;

public class Stcp
{

    public Stcp(final int src, final EndpointBase epbase, Fsm owner)
    {
        inhdr = ByteBuffer.allocate(8);
        outhdr = ByteBuffer.allocate(8);
        Stcp stcp = this;
        fsm = new Fsm(this, owner, stcp) {

            protected void onProgress(int src, int type, Object srcObj)
                throws ErrnoException
            {
                switch(stcp.state)
                {
                case 1: // '\001'
                    switch(src)
                    {
                    case -2: 
                        switch(type)
                        {
                        case -2: 
                            stcp.streamhdr.start(stcp.usock, stcp.pipebase);
                            stcp.state = 2;
                            return;
                        }
                        throw new FsmBadActionException(stcp.state, src, type);
                    }
                    throw new FsmBadSourceException(stcp.state, src, type);

                case 2: // '\002'
                    switch(src)
                    {
                    case 2: // '\002'
                        switch(type)
                        {
                        case 1: // '\001'
                            stcp.streamhdr.stop();
                            stcp.state = 3;
                            return;

                        case 2: // '\002'
                            stcp.state = 6;
                            stcp.fsm.raise(stcp.done, 1);
                            return;
                        }
                        throw new FsmBadActionException(stcp.state, src, type);
                    }
                    throw new FsmBadSourceException(stcp.state, src, type);

                case 3: // '\003'
                    switch(src)
                    {
                    case 2: // '\002'
                        switch(type)
                        {
                        case 3: // '\003'
                            try
                            {
                                stcp.pipebase.start();
                                stcp.instate = 1;
                                stcp.inhdr.clear();
                                stcp.usock.recv(stcp.inhdr);
                                stcp.outstate = 1;
                                stcp.state = 4;
                            }
                            catch(ErrnoException ex)
                            {
                                stcp.state = 6;
                                stcp.fsm.raise(stcp.done, 1);
                            }
                            return;
                        }
                        throw new FsmBadActionException(stcp.state, src, type);
                    }
                    throw new FsmBadSourceException(stcp.state, src, type);

                case 4: // '\004'
                    switch(src)
                    {
                    case 1: // '\001'
                        switch(type)
                        {
                        case 3: // '\003'
                            if(!$assertionsDisabled && stcp.outstate != 2)
                            {
                                throw new AssertionError();
                            } else
                            {
                                stcp.outstate = 1;
                                stcp.outmsg = new Msg(0);
                                stcp.pipebase.sent();
                                return;
                            }

                        case 4: // '\004'
                            switch(stcp.instate)
                            {
                            case 1: // '\001'
                                size = (int)stcp.inhdr.getLong();
                                stcp.inmsg = new Msg(size);
                                if(size == 0)
                                {
                                    stcp.instate = 3;
                                    stcp.pipebase.received();
                                } else
                                {
                                    stcp.instate = 2;
                                    stcp.usock.recv(stcp.inmsg.body.ref());
                                }
                                return;

                            case 2: // '\002'
                                stcp.instate = 3;
                                stcp.pipebase.received();
                                return;
                            }
                            throw new FsmBadErrorException(stcp.state, src, type);

                        case 8: // '\b'
                            stcp.pipebase.stop();
                            stcp.state = 5;
                            return;

                        case 5: // '\005'
                            stcp.pipebase.stop();
                            stcp.state = 6;
                            return;

                        case 6: // '\006'
                        case 7: // '\007'
                        default:
                            throw new FsmBadActionException(stcp.state, src, type);
                        }
                    }
                    throw new FsmBadSourceException(stcp.state, src, type);

                case 5: // '\005'
                    switch(src)
                    {
                    case 1: // '\001'
                        switch(type)
                        {
                        case 5: // '\005'
                            stcp.state = 6;
                            stcp.fsm.raise(stcp.done, 1);
                            return;
                        }
                        throw new FsmBadActionException(stcp.state, src, type);
                    }
                    throw new FsmBadSourceException(stcp.state, src, type);

                case 6: // '\006'
                    throw new FsmBadSourceException(stcp.state, src, type);
                }
                throw new FsmBadStateException(stcp.state, src, type);
            }

            protected void onShutdown(int src, int type, Object srcObj)
                throws ErrnoException
            {
                if(src == -2 && type == -3)
                {
                    stcp.pipebase.stop();
                    stcp.streamhdr.stop();
                    stcp.state = 7;
                }
                if(stcp.state == 7)
                {
                    if(!stcp.streamhdr.isIdle())
                    {
                        return;
                    } else
                    {
                        stcp.usock.swapOwner(stcp.owner);
                        stcp.usock = null;
                        stcp.owner.src = -1;
                        stcp.owner.fsm = null;
                        stcp.state = 1;
                        stcp.fsm.stopped(2);
                        return;
                    }
                } else
                {
                    throw new FsmBadActionException(stcp.state, src, type);
                }
            }

            int size;
            static final boolean $assertionsDisabled = !org/mogware/msgs/transports/tcp/Stcp.desiredAssertionStatus();
            final Stcp val$stcp;
            final Stcp this$0;


            
            {
                this.this$0 = Stcp.this;
                stcp = stcp1;
                super(src, srcObj, owner);
            }
        }
;
        state = 1;
        streamhdr = new StreamHdr(2, fsm);
        usock = null;
        this.owner.src = -1;
        this.owner.fsm = null;
        pipebase = new PipeBase(stcp) {

            protected void onSend(Msg msg)
                throws ErrnoException
            {
                if(!$assertionsDisabled && stcp.state != 4)
                    throw new AssertionError();
                if(!$assertionsDisabled && stcp.outstate != 1)
                {
                    throw new AssertionError();
                } else
                {
                    stcp.outmsg = msg;
                    stcp.outhdr.clear();
                    stcp.outhdr.putLong(stcp.outmsg.body.size());
                    stcp.outhdr.flip();
                    ByteBuffer iov[] = new ByteBuffer[2];
                    iov[0] = stcp.outhdr;
                    iov[1] = stcp.outmsg.body.data();
                    stcp.usock.send(iov);
                    stcp.outstate = 2;
                    return;
                }
            }

            protected Msg onRecv()
                throws ErrnoException
            {
                if(!$assertionsDisabled && stcp.state != 4)
                    throw new AssertionError();
                if(!$assertionsDisabled && stcp.instate != 3)
                {
                    throw new AssertionError();
                } else
                {
                    Msg msg = stcp.inmsg;
                    stcp.inmsg = new Msg(0);
                    stcp.instate = 1;
                    stcp.inhdr.clear();
                    stcp.usock.recv(stcp.inhdr);
                    return msg;
                }
            }

            static final boolean $assertionsDisabled = !org/mogware/msgs/transports/tcp/Stcp.desiredAssertionStatus();
            final Stcp val$stcp;
            final Stcp this$0;


            
            {
                this.this$0 = Stcp.this;
                stcp = stcp1;
                super(epbase);
            }
        }
;
        instate = -1;
        inmsg = new Msg(0);
        outstate = -1;
        outmsg = new Msg(0);
    }

    public boolean isIdle()
    {
        return fsm.isIdle();
    }

    public void start(SockClient usock)
        throws ErrnoException
    {
        if(!$assertionsDisabled && (this.usock != null || owner.fsm != null))
        {
            throw new AssertionError();
        } else
        {
            owner.src = 1;
            owner.fsm = fsm;
            usock.swapOwner(owner);
            this.usock = usock;
            fsm.start();
            return;
        }
    }

    public void stop()
        throws ErrnoException
    {
        fsm.stop();
    }

    private static final int STATE_IDLE = 1;
    private static final int STATE_PROTOHDR = 2;
    private static final int STATE_STOPPING_STREAMHDR = 3;
    private static final int STATE_ACTIVE = 4;
    private static final int STATE_SHUTTING_DOWN = 5;
    private static final int STATE_DONE = 6;
    private static final int STATE_STOPPING = 7;
    private static final int INSTATE_HDR = 1;
    private static final int INSTATE_BODY = 2;
    private static final int INSTATE_HASMSG = 3;
    private static final int OUTSTATE_IDLE = 1;
    private static final int OUTSTATE_SENDING = 2;
    private static final int SRC_USOCK = 1;
    private static final int SRC_STREAMHDR = 2;
    public static final int ERROR = 1;
    public static final int STOPPED = 2;
    private final Fsm fsm;
    private final org.mogware.msgs.aio.Fsm.Owner owner = new org.mogware.msgs.aio.Fsm.Owner();
    private SockClient usock;
    private final StreamHdr streamhdr;
    private final PipeBase pipebase;
    private int instate;
    private ByteBuffer inhdr;
    private Msg inmsg;
    private int outstate;
    private ByteBuffer outhdr;
    private Msg outmsg;
    private final FsmEvent done = new FsmEvent();
    private int state;
    static final boolean $assertionsDisabled = !org/mogware/msgs/transports/tcp/Stcp.desiredAssertionStatus();




















}
