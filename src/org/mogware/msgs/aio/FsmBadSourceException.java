package org.mogware.msgs.aio;

public class FsmBadSourceException extends FsmException {
    public FsmBadSourceException(int state, int src, int type) {
        super("Unexpected source", state, src, type);
    }
}
