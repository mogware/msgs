package org.mogware.msgs.aio;

public class FsmBadStateException extends FsmException {
    public FsmBadStateException(int state, int src, int type) {
        super("Unexpected state", state, src, type);
    }
}
