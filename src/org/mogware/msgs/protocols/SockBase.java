package org.mogware.msgs.protocols;

import org.mogware.msgs.aio.Ctx;
import org.mogware.msgs.core.PipeBase;
import org.mogware.msgs.utils.ErrnoException;
import org.mogware.msgs.utils.Msg;

public interface SockBase {
    public static final int EVENT_IN = 1;
    public static final int EVENT_OUT = 2;

    public abstract void stopped();

    public abstract Ctx ctx();

    public abstract Object opt(int i)
        throws ErrnoException;

    public abstract void statIncrement(int i, int j);

    public abstract void add(PipeBase pipebase)
        throws ErrnoException;

    public abstract void remove(PipeBase pipebase);

    public abstract void in(PipeBase pipebase);

    public abstract void out(PipeBase pipebase);

    public abstract int events();

    public abstract void send(Msg msg)
        throws ErrnoException;

    public abstract Msg recv()
        throws ErrnoException;

    public abstract void opt(int i, int j, Object obj)
        throws ErrnoException;

    public abstract Object opt(int i, int j)
        throws ErrnoException;

    public abstract boolean stop();
}
