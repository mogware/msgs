package org.mogware.msgs;

import org.mogware.msgs.core.Global;

public final class Errmsg {
    public static final int errno() {
        return Global.errno();
    }

    public static final String error() {
        int errno = errno();
        String errnoName = Global.errname(errno);
        if (errnoName == null)
            errnoName = "errno " + errno;
        String description = Global.strerror(errno);
        return "Operation failed: " + errnoName + " (" + description + ")";
    }
}
