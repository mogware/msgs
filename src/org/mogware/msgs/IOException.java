package org.mogware.msgs;

public class IOException extends java.io.IOException {
    protected int errno = -1;

    public IOException(final String message) {
        super(message);
    }

    public IOException(final String message, final int errno) {
        super(message);
        this.errno = errno;
    }

    public IOException(Throwable cause) {
        super(cause);
    }

    public int errno() {
        return this.errno;
    }
}
