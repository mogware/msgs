package org.mogware.msgs.aio;

public class FsmBadActionException extends FsmException {
    public FsmBadActionException(int state, int src, int type) {
        super("Unexpected action", state, src, type);
    }
}
