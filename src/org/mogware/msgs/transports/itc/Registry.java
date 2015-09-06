// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   Registry.java

package org.mogware.msgs.transports.itc;

import java.util.*;
import org.mogware.msgs.aio.Fsm;
import org.mogware.msgs.core.EndpointBase;
import org.mogware.msgs.core.Global;
import org.mogware.msgs.utils.ErrnoException;

public class Registry
{
    public static abstract class Entry
    {

        protected abstract void onConnect(Entry entry)
            throws ErrnoException;

        protected Fsm fsm;
        protected EndpointBase epbase;
        protected int protocol;
        protected int connects;

        public Entry()
        {
        }
    }


    protected Registry()
    {
    }

    public synchronized void bind(Entry item)
        throws ErrnoException
    {
        for(Iterator iterator = bounded.iterator(); iterator.hasNext();)
        {
            Entry bitem = (Entry)iterator.next();
            if(item.epbase.addr().equals(bitem.epbase.addr()))
                throw new ErrnoException(0x9523dcd);
        }

        bounded.add(item);
        Iterator iterator1 = connected.iterator();
        do
        {
            if(!iterator1.hasNext())
                break;
            Entry citem = (Entry)iterator1.next();
            if(item.epbase.addr().equals(citem.epbase.addr()) && item.epbase.isPeer(citem.protocol))
            {
                citem.connects = 1;
                item.onConnect(citem);
            }
        } while(true);
    }

    public synchronized void connect(Entry item)
        throws ErrnoException
    {
        connected.add(item);
        Iterator iterator = bounded.iterator();
        do
        {
            if(!iterator.hasNext())
                break;
            Entry bitem = (Entry)iterator.next();
            if(!item.epbase.addr().equals(bitem.epbase.addr()))
                continue;
            if(item.epbase.isPeer(bitem.protocol))
            {
                bitem.connects++;
                item.onConnect(bitem);
            }
            break;
        } while(true);
    }

    public synchronized void disconnect(Entry item)
    {
        connected.remove(item);
    }

    public synchronized void unbind(Entry item)
    {
        bounded.remove(item);
    }

    public static Registry instance()
    {
        if(instance == null)
            instance = new Registry();
        return instance;
    }

    private static Registry instance = null;
    private final List bounded = new LinkedList();
    private final List connected = new LinkedList();

}
