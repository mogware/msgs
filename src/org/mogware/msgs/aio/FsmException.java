package org.mogware.msgs.aio;

public class FsmException extends RuntimeException {
    public FsmException(String message, int state, int src, int type) {
        super(String.format(
                "%s: state=%d source=%d action=%d", message, state, src, type
        ));
    }
}
