package org.mogware.msgs.utils;

import org.mogware.msgs.core.Global;

public final class ErrnoException extends Exception {
    private final int errno;

    public ErrnoException(int errno) {
        this.errno = errno;
    }

    public ErrnoException(int errno, Throwable cause) {
        super(cause);
        this.errno = errno;
    }

    @Override
    public String getMessage() {
        String errnoName = Global.errname(errno);
        if (errnoName == null)
            errnoName = "errno " + errno;
        String description = Global.strerror(errno);
        return "Operation failed: " + errnoName + " (" + description + ")";
    }

    public int errno() {
        return this.errno;
    }
}

