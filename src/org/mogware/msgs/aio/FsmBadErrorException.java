package org.mogware.msgs.aio;

public class FsmBadErrorException extends FsmException {
    public FsmBadErrorException(int state, int src, int type) {
        super("Unexpected instate", state, src, type);
    }
}
