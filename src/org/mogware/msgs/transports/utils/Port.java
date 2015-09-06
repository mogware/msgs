package org.mogware.msgs.transports.utils;

import org.mogware.msgs.core.Global;
import org.mogware.msgs.utils.ErrnoException;

public final class Port {
    public static int resolve(String portStr) throws ErrnoException {
        int port;
        if (portStr.equals("*") || portStr.equals("0"))
            port = 0;
        else if ((port = Integer.parseInt(portStr)) == 0)
            throw new ErrnoException(Global.EINVAL);
        return port;
    }
}
