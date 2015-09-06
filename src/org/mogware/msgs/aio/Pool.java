package org.mogware.msgs.aio;

import org.mogware.msgs.utils.ErrnoException;

public class Pool {
    private final Worker worker;

    public Pool() throws ErrnoException {
        this.worker = new Worker();
    }

    public Worker chooseWorker() {
        return this.worker;
    }

    public void dispose() throws ErrnoException {
        this.worker.dispose();
    }
}
