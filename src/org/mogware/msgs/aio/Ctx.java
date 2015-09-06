package org.mogware.msgs.aio;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.mogware.msgs.utils.ErrnoException;

public abstract class Ctx {
    private final Lock sync;
    private final Pool pool;

    private final Deque<FsmEvent> events;
    private final Deque<FsmEvent> eventsto;

    public Ctx(Pool pool) {
        this.pool = pool;
        this.sync = new ReentrantLock();
        this.events = new ArrayDeque<>();
        this.eventsto = new ArrayDeque<>();
    }

    public void enter() {
        this.sync.lock();
    }

    public void leave() throws ErrnoException {
        while (true) {
            if (this.events.isEmpty())
                break;
            FsmEvent event = this.events.pop();
            event.decref();
            event.process();
        }
        this.onLeave();
        if (this.eventsto.isEmpty()) {
            this.sync.unlock();
            return;
        }
        Deque<FsmEvent> eventsto = new ArrayDeque<>(this.eventsto);
        this.eventsto.clear();
        this.sync.unlock();
        for (FsmEvent event: eventsto) {
            event.fsm.ctx().enter();
            event.decref();
            event.process();
            event.fsm.ctx().leave();
        }
    }

    public Worker chooseWorker() {
        return this.pool.chooseWorker();
    }

    public void raise(FsmEvent event) {
        event.incref();
        this.events.push(event);
    }

    public void raiseTo(FsmEvent event) {
        event.incref();
        this.eventsto.push(event);
    }

    public abstract void onLeave() throws ErrnoException;
}
