// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   Citc.java

package org.mogware.msgs.transports.itc;

import org.mogware.msgs.aio.*;
import org.mogware.msgs.core.*;
import org.mogware.msgs.transports.Transport;
import org.mogware.msgs.utils.ErrnoException;

// Referenced classes of package org.mogware.msgs.transports.itc:
//            Sitc, Registry

public class Citc extends Registry.Entry
{

    private Citc(final Endpoint ep)
        throws ErrnoException
    {
        Citc citc = this;
        state = 1;
        epbase = new EndpointBase(citc) {

            protected void onStop()
                throws ErrnoException
            {
                citc.fsm.stop();
            }

            final Citc val$citc;
            final Citc this$0;

            
            {
                this.this$0 = Citc.this;
                citc = citc1;
                super(ep);
            }
        }
;
        fsm = new Fsm(citc) {

            protected void onProgress(int src, int type, Object srcObj)
                throws ErrnoException
            {
                switch(citc.state)
                {
                case 1: // '\001'
                    switch(src)
                    {
                    case -2: 
                        switch(type)
                        {
                        case -2: 
                            citc.state = 2;
                            citc.epbase.statIncrement(202, 1);
                            return;
                        }
                        throw new FsmBadActionException(citc.state, src, type);
                    }
                    throw new FsmBadSourceException(citc.state, src, type);

                case 2: // '\002'
                    switch(src)
                    {
                    case -2: 
                        switch(type)
                        {
                        case 1: // '\001'
                            citc.state = 3;
                            citc.epbase.statIncrement(202, -1);
                            citc.epbase.statIncrement(101, 1);
                            return;
                        }
                        throw new FsmBadActionException(citc.state, src, type);

                    case 27713: 
                        Sitc sitc = (Sitc)srcObj;
                        switch(type)
                        {
                        case 1: // '\001'
                            citc.sitc.accept(sitc);
                            citc.state = 3;
                            citc.epbase.statIncrement(202, -1);
                            citc.epbase.statIncrement(101, 1);
                            return;
                        }
                        throw new FsmBadActionException(citc.state, src, type);
                    }
                    throw new FsmBadSourceException(citc.state, src, type);

                case 3: // '\003'
                    throw new FsmBadSourceException(citc.state, src, type);
                }
                throw new FsmBadStateException(citc.state, src, type);
            }

            protected void onShutdown(int src, int type, Object srcObj)
                throws ErrnoException
            {
                if(src == -2 && type == -3)
                {
                    Registry.instance().disconnect(citc);
                    citc.sitc.stop();
                    citc.state = 4;
                }
                if(citc.state == 4)
                {
                    if(!citc.sitc.isIdle())
                    {
                        return;
                    } else
                    {
                        citc.state = 1;
                        citc.fsm.stoppedNoEvent();
                        citc.epbase.stopped();
                        return;
                    }
                } else
                {
                    throw new FsmBadStateException(citc.state, src, type);
                }
            }

            final Citc val$citc;
            final Citc this$0;

            
            {
                this.this$0 = Citc.this;
                citc = citc1;
                super(ctx);
            }
        }
;
        sitc = new Sitc(1, epbase, fsm);
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
            sitc.connect(peer.fsm);
            fsm.action(1);
            return;
        }
    }

    public static EndpointBase create(Endpoint ep)
        throws ErrnoException
    {
        Citc citc = new Citc(ep);
        Registry.instance().connect(citc);
        return citc.epbase;
    }

    private static final int STATE_IDLE = 1;
    private static final int STATE_DISCONNECTED = 2;
    private static final int STATE_ACTIVE = 3;
    private static final int STATE_STOPPING = 4;
    private static final int ACTION_CONNECT = 1;
    private static final int SRC_SITC = 1;
    private int state;
    private Sitc sitc;
    static final boolean $assertionsDisabled = !org/mogware/msgs/transports/itc/Citc.desiredAssertionStatus();




}
