package org.mogware.msgs.core;

import org.mogware.msgs.aio.Ctx;
import org.mogware.msgs.utils.ErrnoException;

public abstract class EndpointBase {
    private Endpoint ep;

    public EndpointBase(Endpoint ep) {
        this.ep = ep;
    }

    public void stopped() {
        ep.stopped();
    }

    public Ctx ctx() {
        return ep.ctx();
    }

    public Endpoint ep() {
        return ep;
    }

    public String addr() {
        return ep.addr();
    }

    public Object opt(int level, int option) throws ErrnoException {
        return ep.opt(level, option);
    }

    public boolean isPeer(int socktype) {
        return ep.isPeer(socktype);
    }

    public void setError(int errnum) {
        ep.setError(errnum);
    }

    public void clearError() {
        ep.clearError();
    }

    public void statIncrement(int name, int increment) {
        ep.statIncrement(name, increment);
    }

    protected abstract void onStop()  throws ErrnoException;
}
