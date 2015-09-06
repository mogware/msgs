package org.mogware.msgs.core;

import org.mogware.msgs.utils.ErrnoException;

public interface OptSet {
    public abstract void opt(int i, Object obj)
        throws ErrnoException;

    public abstract Object opt(int i)
        throws ErrnoException;
}
