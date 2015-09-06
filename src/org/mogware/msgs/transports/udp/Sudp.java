// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   Sudp.java

package org.mogware.msgs.transports.udp;

import java.io.PrintStream;
import org.mogware.msgs.aio.*;
import org.mogware.msgs.core.EndpointBase;
import org.mogware.msgs.utils.ErrnoException;

public class Sudp
{

    public Sudp(final int src, EndpointBase epbase, Fsm owner)
    {
        Sudp sudp = this;
        fsm = new Fsm(this, owner, sudp) {

            protected void onProgress(int src, int type, Object srcObj)
                throws ErrnoException
            {
                System.out.printf("Sudp p: state=%d, src=%s, type=%d\n", new Object[] {
                    Integer.valueOf(sudp.state), Integer.valueOf(src), Integer.valueOf(type)
                });
            }

            protected void onShutdown(int src, int type, Object srcObj)
                throws ErrnoException
            {
                System.out.printf("Sudp s: state=%d, src=%s, type=%d\n", new Object[] {
                    Integer.valueOf(sudp.state), Integer.valueOf(src), Integer.valueOf(type)
                });
                if(src == -2 && type == -3)
                    sudp.state = 7;
                if(sudp.state == 7)
                {
                    sudp.usock.swapOwner(sudp.owner);
                    sudp.usock = null;
                    sudp.owner.src = -1;
                    sudp.owner.fsm = null;
                    sudp.state = 1;
                    sudp.fsm.stopped(2);
                    return;
                } else
                {
                    throw new FsmBadActionException(sudp.state, src, type);
                }
            }

            final Sudp val$sudp;
            final Sudp this$0;

            
            {
                this.this$0 = Sudp.this;
                sudp = sudp1;
                super(src, srcObj, owner);
            }
        }
;
        state = 1;
        usock = null;
        this.owner.src = -1;
        this.owner.fsm = null;
    }

    public boolean isIdle()
    {
        return fsm.isIdle();
    }

    public void start(SockDatag usock)
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
    private static final int STATE_STOPPING = 7;
    private static final int SRC_USOCK = 1;
    public static final int ERROR = 1;
    public static final int STOPPED = 2;
    private final Fsm fsm;
    private final org.mogware.msgs.aio.Fsm.Owner owner = new org.mogware.msgs.aio.Fsm.Owner();
    private SockDatag usock;
    private int state;
    static final boolean $assertionsDisabled = !org/mogware/msgs/transports/udp/Sudp.desiredAssertionStatus();







}
