package org.mogware.msgs.aio;

import org.mogware.msgs.utils.ErrnoException;

public class Timer {
    private static final int STATE_IDLE = 1;
    private static final int STATE_ACTIVE = 2;
    private static final int STATE_STOPPING = 3;

    private static final int SRC_START_TASK = 1;
    private static final int SRC_STOP_TASK = 2;

    public static final int TIMEOUT = 1;
    public static final int STOPPED = 2;

    private final Fsm fsm;
    private final Worker.Task start;
    private final Worker.Task stop;
    private final Worker.Timer timer;
    private final FsmEvent done;
    private final Worker worker;

    private int state;
    private long timeout;

    public Timer(int src, Fsm owner) {
        final Timer timer = this;
        this.fsm = new Fsm(src, this, owner) {
            @Override
            protected void onProgress(int src, int type, Object srcObj)
                throws ErrnoException {
                switch (timer.state)
                {
                case Timer.STATE_IDLE:
                    switch (src) {
                    case Fsm.ACTION:
                        switch (type) {
                        case Fsm.START:
                            timer.state = 2;
                            timer.worker.execute(timer.start);
                            return;
                        default:
                            throw new FsmBadActionException(timer.state, src, type);
                        }
                    default:
                        throw new FsmBadSourceException(timer.state, src, type);
                    }
                case Timer.STATE_ACTIVE:
                    if (src == SRC_START_TASK) {
                        assert(type == Worker.TASK_EXECUTE);
                        assert(timer.timeout >= 0);
                        timer.worker.addTimer(timer.timeout, timer.timer);
                        timer.timeout = -1L;
                        return;
                    }
                    if (srcObj == timer.timer) {
                        switch (type) {
                        case Worker.TIMER_TIMEOUT:
                            assert(timer.timeout == -1);
                            timer.fsm.raise(timer.done, TIMEOUT);
                            return;
                        default:
                            throw new FsmBadActionException(timer.state, src, type);
                        }
                    }
                    throw new FsmBadSourceException(timer.state, src, type);
                default:
                    throw new FsmBadStateException(timer.state, src, type);
                }
            }
            @Override
            protected void onShutdown(int src, int type, Object srcObj)
                    throws ErrnoException {
                if (src == Fsm.ACTION && type == Fsm.STOP) {
                    timer.state = Timer.STATE_STOPPING;
                    timer.worker.execute(timer.stop);
                }
                else if (timer.state == Timer.STATE_STOPPING) {
                    if (src != Timer.SRC_STOP_TASK)
                        return;
                    assert(type == Worker.TASK_EXECUTE);
                    timer.worker.removeTimer(timer.timer);
                    timer.state = Timer.STATE_IDLE;
                    timer.fsm.stopped(Timer.STOPPED);
                }
                else
                    throw new FsmBadStateException(timer.state, src, type);
            }
        };
        this.start = new Worker.Task(Timer.SRC_START_TASK, this.fsm);
        this.stop = new Worker.Task(Timer.SRC_STOP_TASK, this.fsm);
        this.timer = new Worker.Timer(this.fsm);
        this.done = new FsmEvent();
        this.worker = this.fsm.chooseWorker();
        this.state = Timer.STATE_IDLE;
        this.timeout = -1;
    }

    public boolean isIdle() {
        return this.fsm.isIdle();
    }

    public void start(long timeout) throws ErrnoException {
        assert(timeout >= 0);
        this.timeout = timeout;
        this.fsm.start();
    }

    public void stop() throws ErrnoException {
        this.fsm.stop();
    }
}
