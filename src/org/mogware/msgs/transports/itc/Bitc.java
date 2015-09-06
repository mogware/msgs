// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   Bitc.java

package org.mogware.msgs.transports.itc;

import java.util.*;
import org.mogware.msgs.aio.*;
import org.mogware.msgs.core.*;
import org.mogware.msgs.transports.Transport;
import org.mogware.msgs.utils.ErrnoException;

// Referenced classes of package org.mogware.msgs.transports.itc:
//            Sitc, Registry

public class Bitc extends Registry.Entry
{

    private Bitc(final Endpoint ep)
        throws ErrnoException
    {
        sitcs = new LinkedList();
        Bitc bitc = this;
        state = 1;
        epbase = new EndpointBase(bitc) {

            protected void onStop()
                throws ErrnoException
            {
                bitc.fsm.stop();
            }

            final Bitc val$bitc;
            final Bitc this$0;

            
            {
                this.this$0 = Bitc.this;
                bitc = bitc1;
                super(ep);
            }
        }
;
        fsm = new Fsm(bitc) {

            protected void onProgress(int src, int type, Object srcObj)
                throws ErrnoException
            {
                switch(bitc.state)
                {
                case 1: // '\001'
                    switch(src)
                    {
                    case -2: 
                        switch(type)
                        {
                        case -2: 
                            bitc.state = 2;
                            return;
                        }
                        throw new FsmBadActionException(bitc.state, src, type);
                    }
                    throw new FsmBadSourceException(bitc.state, src, type);

                case 2: // '\002'
                    switch(src)
                    {
                    case 27713: 
                        switch(type)
                        {
                        case 1: // '\001'
                            Sitc peer = (Sitc)srcObj;
                            Sitc sitc = new Sitc(1, bitc.epbase, bitc.fsm);
                            bitc.sitcs.add(sitc);
                            sitc.accept(peer);
                            return;
                        }
                        throw new FsmBadActionException(bitc.state, src, type);
                    }
                    throw new FsmBadSourceException(bitc.state, src, type);
                }
                throw new FsmBadStateException(bitc.state, src, type);
            }

            protected void onShutdown(int src, int type, Object srcObj)
                throws ErrnoException
            {
                if(src == -2 && type == -3)
                {
                    Registry.instance().unbind(bitc);
                    Sitc sitc;
                    for(Iterator iterator = bitc.sitcs.iterator(); iterator.hasNext(); sitc.stop())
                        sitc = (Sitc)iterator.next();

                    bitc.state = 3;
                } else
                if(bitc.state == 3)
                {
                    if(!$assertionsDisabled && (src != 1 || type != 7))
                        throw new AssertionError();
                    Sitc sitc = (Sitc)srcObj;
                    bitc.sitcs.remove(sitc);
                } else
                {
                    throw new FsmBadStateException(bitc.state, src, type);
                }
                if(!bitc.sitcs.isEmpty())
                {
                    return;
                } else
                {
                    bitc.state = 1;
                    bitc.fsm.stoppedNoEvent();
                    bitc.epbase.stopped();
                    return;
                }
            }

            static final boolean $assertionsDisabled = !org/mogware/msgs/transports/itc/Bitc.desiredAssertionStatus();
            final Bitc val$bitc;
            final Bitc this$0;


            
            {
                this.this$0 = Bitc.this;
                bitc = bitc1;
                super(ctx);
            }
        }
;
        connects = 0;
        protocol = ((Integer)epbase.opt(1, 13)).intValue();
        fsm.start();
    }

    protected void onConnect(Registry.Entry peer)
        throws ErrnoException
    {
        if(!$assertionsDisabled && state != 2)
        {
            throw new AssertionError();
        } else
        {
            Sitc sitc = new Sitc(1, epbase, fsm);
            sitcs.add(sitc);
            sitc.connect(peer.fsm);
            epbase.statIncrement(102, 1);
            return;
        }
    }

    public static EndpointBase create(Endpoint ep)
        throws ErrnoException
    {
        Bitc bitc = new Bitc(ep);
        Registry.instance().bind(bitc);
        return bitc.epbase;
    }

    private static final int STATE_IDLE = 1;
    private static final int STATE_ACTIVE = 2;
    private static final int STATE_STOPPING = 3;
    private static final int SRC_SITC = 1;
    private List sitcs;
    private int state;
    static final boolean $assertionsDisabled = !org/mogware/msgs/transports/itc/Bitc.desiredAssertionStatus();




}
