// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   MsgQueue.java

package org.mogware.msgs.transports.itc;

import org.mogware.msgs.core.Global;
import org.mogware.msgs.utils.*;

public class MsgQueue
{
    private static class Chunk
    {

        private final Msg msgs[];
        private Chunk next;




        private Chunk()
        {
            msgs = new Msg[126];
        }

    }


    public MsgQueue(int maxmem)
    {
        mem = 0;
        this.maxmem = maxmem;
        count = 0;
        Chunk chunk = new Chunk();
        chunk.next = null;
        inChunk = chunk;
        inPos = 0;
        outChunk = chunk;
        outPos = 0;
        cache = null;
    }

    public synchronized void send(Msg msg)
        throws ErrnoException
    {
        int msgsz = msg.body.size();
        if(count > 0 && mem + msgsz >= maxmem)
            throw new ErrnoException(0x9523dd4);
        count++;
        mem += msgsz;
        outChunk.msgs[outPos] = msg;
        outPos++;
        if(outPos == 126)
        {
            if(cache == null)
            {
                cache = new Chunk();
                cache.next = null;
            }
            outChunk.next = cache;
            outChunk = cache;
            cache = null;
            outPos = 0;
        }
    }

    public synchronized Msg recv()
        throws ErrnoException
    {
        if(count == 0)
            throw new ErrnoException(0x9523dd4);
        Msg msg = inChunk.msgs[inPos];
        inChunk.msgs[inPos] = null;
        inPos++;
        if(inPos == 126)
        {
            Chunk chunk = inChunk;
            inChunk = inChunk.next;
            inPos = 0;
            if(cache == null)
                cache = chunk;
        }
        count--;
        mem -= msg.body.size();
        return msg;
    }

    public synchronized boolean isEmpty()
    {
        return count == 0;
    }

    private static final int MSGQUEUE_GRANULARITY = 126;
    private final int maxmem;
    private volatile int count;
    private volatile int mem;
    private Chunk inChunk;
    private int inPos;
    private Chunk outChunk;
    private int outPos;
    private volatile Chunk cache;
}
