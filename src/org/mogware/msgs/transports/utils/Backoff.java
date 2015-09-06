package org.mogware.msgs.transports.utils;

import org.mogware.msgs.aio.Fsm;
import org.mogware.msgs.aio.Timer;
import org.mogware.msgs.utils.ErrnoException;

public class Backoff {
    public static final int TIMEOUT = 1;
    public static final int STOPPED = 2;

    private final Timer timer;
    private final int minivl;
    private final int maxivl;
    private int n;

    public Backoff(int src, int minivl, int maxivl, Fsm owner) {
        this.timer = new Timer(src, owner);
        this.minivl = minivl;
        this.maxivl = maxivl;
        this.n = 1;
    }

    public boolean isIdle() {
        return this.timer.isIdle();
    }

    public void start() throws ErrnoException {
        long timeout = (n - 1) * minivl;
        if(timeout > (long)maxivl)
            timeout = maxivl;
        else
            n *= 2;
        this.timer.start(timeout);
    }

    public void stop() throws ErrnoException {
        this.timer.stop();
    }

    public void reset() {
        this. n = 1;
    }
}
