package org.mogware.msgs.protocols;

import org.mogware.msgs.utils.ErrnoException;

public interface SockType {
    public static final int FLAG_NORECV = 1;
    public static final int FLAG_NOSEND = 2;
    public static final int STAT_CURRENT_SND_PRIORITY = 401;

    public abstract int domain();

    public abstract int protocol();

    public abstract int flags();

    public abstract SockBase create(Object obj)
        throws ErrnoException;

    public abstract boolean isPeer(int i);
}
