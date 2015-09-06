package org.mogware.msgs.transports;

import org.mogware.msgs.core.Endpoint;
import org.mogware.msgs.core.EndpointBase;
import org.mogware.msgs.core.OptSet;
import org.mogware.msgs.utils.ErrnoException;

public interface Transport {
    public static final int STAT_ESTABLISHED_CONNECTIONS = 101;
    public static final int STAT_ACCEPTED_CONNECTIONS = 102;
    public static final int STAT_DROPPED_CONNECTIONS = 103;
    public static final int STAT_BROKEN_CONNECTIONS = 104;
    public static final int STAT_CONNECT_ERRORS = 105;
    public static final int STAT_BIND_ERRORS = 106;
    public static final int STAT_ACCEPT_ERRORS = 107;

    public static final int STAT_CURRENT_CONNECTIONS = 201;
    public static final int STAT_INPROGRESS_CONNECTIONS = 202;
    public static final int STAT_CURRENT_EP_ERRORS = 203;

    public abstract String name();

    public abstract int id();

    public abstract EndpointBase bind(Endpoint endpoint)
        throws ErrnoException;

    public abstract EndpointBase connect(Endpoint endpoint)
        throws ErrnoException;

    public abstract OptSet optset();
}
