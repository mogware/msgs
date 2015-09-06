package org.mogware.msgs.aio;

import org.mogware.msgs.utils.ErrnoException;

public abstract class SockBase {
    protected static final int STATE_IDLE = 1;
    protected static final int STATE_STARTING = 2;
    protected static final int STATE_BEING_ACCEPTED = 3;
    protected static final int STATE_ACCEPTED = 4;
    protected static final int STATE_CONNECTING = 5;
    protected static final int STATE_ACTIVE = 6;
    protected static final int STATE_REMOVING_FD = 7;
    protected static final int STATE_DONE = 8;
    protected static final int STATE_LISTENING = 9;
    protected static final int STATE_ACCEPTING = 10;
    protected static final int STATE_CANCELLING = 11;
    protected static final int STATE_STOPPING = 12;
    protected static final int STATE_STOPPING_ACCEPT = 13;
    protected static final int STATE_ACCEPTING_ERROR = 14;

    protected static final int ACTION_ACCEPT = 1;
    protected static final int ACTION_BEING_ACCEPTED = 2;
    protected static final int ACTION_CANCEL = 3;
    protected static final int ACTION_LISTEN = 4;
    protected static final int ACTION_CONNECT = 5;
    protected static final int ACTION_ACTIVATE = 6;
    protected static final int ACTION_DONE = 7;
    protected static final int ACTION_ERROR = 8;
    protected static final int ACTION_STARTED = 9;

    protected static final int SRC_FD = 1;
    protected static final int SRC_TASK_CONNECTING = 2;
    protected static final int SRC_TASK_CONNECTED = 3;
    protected static final int SRC_TASK_ACCEPT = 4;
    protected static final int SRC_TASK_SEND = 5;
    protected static final int SRC_TASK_RECV = 6;
    protected static final int SRC_TASK_STOP = 7;

    public static final int CONNECTED = 1;
    public static final int ACCEPTED = 2;
    public static final int SENT = 3;
    public static final int RECEIVED = 4;
    public static final int ERROR = 5;
    public static final int ACCEPT_ERROR = 6;
    public static final int STOPPED = 7;
    public static final int SHUTDOWN = 8;

    protected Fsm fsm;
    protected int state = STATE_IDLE;;
    protected int errno = 0;

    protected static final boolean isWindows;
    static {
        String os = System.getProperty("os.name").toLowerCase();
        isWindows = os.contains("win");
    }

    public int errno() {
        return this.errno;
    }

    public Fsm fsm() {
        return this.fsm;
    }

    public boolean isIdle() {
        return this.fsm.isIdle();
    }

    public void stop() throws ErrnoException {
        this.fsm.stop();
    }

    public void swapOwner(Fsm.Owner owner) {
        this.fsm.swapOwner(owner);
    }

    public void activate() throws ErrnoException {
        this.fsm.action(SockBase.ACTION_ACTIVATE);

    }
}
