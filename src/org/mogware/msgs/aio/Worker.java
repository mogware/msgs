package org.mogware.msgs.aio;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.mogware.msgs.core.Global;
import org.mogware.msgs.utils.*;

public class Worker implements Runnable {
    public static final int TIMER_TIMEOUT = 1;
    public static final int TASK_EXECUTE = 1;

    public static final int FD_IN = 1;
    public static final int FD_OUT = 2;
    public static final int FD_ERR = 3;

    private final Lock sync;
    private final TimerSet timerset;

    private final Efd efd;
    private final Task stop;
    private final List<Task> tasks;

    private final Thread thread;

    private final Map<SelectableChannel, PollSet> pollsets;
    private final Selector selector;
    private boolean retired;

    public Worker() throws ErrnoException {
        this.efd = new Efd();
        this.sync = new ReentrantLock();
        this.timerset = new TimerSet();
        this.tasks = new LinkedList<>();
        this.stop = new Task(0, null);
        this.pollsets = new HashMap<>();
        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            throw new ErrnoException(Global.EIO);
        }
        this.retired = false;
        this.pollsets.put(this.efd.fd(), new PollSet(null));
        this.register(this.efd.fd(), SelectionKey.OP_READ, false);
        this.thread = new Thread(this, "worker");
        this.thread.start();
    }

    public void dispose()
        throws ErrnoException
    {
        this.sync.lock();
        try {
            this.tasks.add(this.stop);
            this.efd.signal();
        } finally {
            this.sync.unlock();
        }
        do {
            try {
                this.thread.join(100);
                if (!this.thread.isAlive())
                    break;
            } catch (final Exception ex) {
                this.thread.interrupt();
            }
        } while (true);
        try {
            this.selector.close();
        } catch (IOException ex) {
            throw new ErrnoException(Global.EIO);
        }
    }

    public void execute(Task task) throws ErrnoException {
        this.sync.lock();
        try {
            this.tasks.add(task);
            this.efd.signal();
        } finally {
            this.sync.unlock();
        }
    }

    public void cancel(Task task) {
        this.sync.lock();
        try {
            this.tasks.remove(task);
        } finally {
            this.sync.unlock();
        }

    }

    public void addFd(SelectableChannel s, Fd fd) {
        this.pollsets.put(s, new PollSet(fd));
    }

    public void removeFd(SelectableChannel s) {
        this.pollsets.get(s).cancelled = true;
        this.retired = true;
    }

    public void setIn(SelectableChannel fd) {
        register(fd, SelectionKey.OP_READ, false);
    }

    public void resetIn(SelectableChannel fd) {
        register(fd, SelectionKey.OP_READ, true);
    }

    public final void setOut(SelectableChannel fd) {
        register(fd, SelectionKey.OP_WRITE, false);
    }

    public final void resetOut(SelectableChannel fd) {
        register(fd, SelectionKey.OP_WRITE, true);
    }

    public final void setConnect(SelectableChannel s) {
        register(s, SelectionKey.OP_CONNECT, false);
    }

    public final void setAccept(SelectableChannel s) {
        register(s, SelectionKey.OP_ACCEPT, false);
    }

    public void addTimer(long timeout, Timer timer) {
        this.timerset.add(timeout, timer);
    }

    public void removeTimer(Timer timer) {
        this.timerset.remove(timer);
    }

    @Override
    public void run() {
        while (true) {
            try {
                this.select(this.timerset.timeout());
                while (true) {
                    Timer timer = this.timerset.event();
                    if (timer == null)
                        break;
                    timer.owner.ctx().enter();
                    timer.owner.feed(-1, Worker.TIMER_TIMEOUT, timer);
                    timer.owner.ctx().leave();
                }
                Iterator<SelectionKey> it = this.selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    Fd fd = (Fd) key.attachment();
                    it.remove();
                    if (key.channel() == this.efd.fd()) {
                        List<Task> tasks;
                        this.sync.lock();
                        try {
                            this.efd.unsignal();
                            tasks = new LinkedList<>(this.tasks);
                            this.tasks.clear();
                        } finally {
                            this.sync.unlock();
                        }
                        for (Task task: tasks) {
                            if (task == this.stop)
                                return;
                            task.owner.ctx().enter();
                            task.owner.feed(task.src, Worker.TASK_EXECUTE, task);
                            task.owner.ctx().leave();
                        }
                        continue;
                    }
                    try {
                        int event = Worker.FD_ERR;
                        if (key.isReadable())
                            event = Worker.FD_IN;
                        else if (key.isAcceptable())
                            event = Worker.FD_IN;
                        else if (key.isConnectable())
                            event = Worker.FD_OUT;
                        else if (key.isWritable())
                            event = Worker.FD_OUT;
                        fd.owner.ctx().enter();
                        fd.owner.feed(fd.src, event, fd);
                        fd.owner.ctx().leave();
                    } catch (CancelledKeyException ex) { }
                }
            } catch (ErrnoException ex) { }
        }
    }

    private void register(SelectableChannel fd, int ops, boolean negate)  {
        PollSet pollset = this.pollsets.get(fd);
        if (negate)
            pollset.ops = pollset.ops & ~ops;
        else
            pollset.ops = pollset.ops | ops;
        if (pollset.key != null)
            pollset.key.interestOps(pollset.ops);
        else
            this.retired = true;
    }

    public void select(long timeout) throws ErrnoException {

        if (this.retired) {
            Iterator<Map.Entry<SelectableChannel, PollSet>> it =
                    this.pollsets.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<SelectableChannel, PollSet> entry = it.next();
                SelectableChannel fd = entry.getKey();
                PollSet pollset = entry.getValue();
                if (pollset.key == null) {
                    try {
                        pollset.key = fd.register(selector, pollset.ops, pollset.fd);
                    } catch(ClosedChannelException ex) {
                        throw new ErrnoException(Global.EIO);
                    }
                }
                if (pollset.cancelled || !fd.isOpen()) {
                    if (pollset.key != null)
                        pollset.key.cancel();
                    it.remove();
                }
            }
            retired = false;
        }
        try {
            if (timeout < 0L)
                this. selector.select(0L);
            else if (timeout == 0L)
                this.selector.selectNow();
            else this.selector.select(timeout);
        } catch(IOException e) {
            throw new ErrnoException(Global.EINTR);
        }
    }

    public static class Task {
        private final int src;
        private final Fsm owner;

        public Task(int src, Fsm owner) {
            this.src = src;
            this.owner = owner;
        }
    }

    public static class Fd {
        private final int src;
        private final Fsm owner;

        public Fd(int src, Fsm owner) {
            this.src = src;
            this.owner = owner;
        }
    }

    public static class Timer {
        private final Fsm owner;

        public Timer(Fsm owner) {
            this.owner = owner;
        }
    }

    private static class PollSet {
        private final Fd fd;
        private SelectionKey key;
        private int ops;
        private boolean cancelled;

        private PollSet(Fd fd) {
            this.fd = fd;
            this.key = null;
            this.cancelled = false;
            this.ops = 0;
        }
    }

    private static class TimerSet {
        private final class TimerInfo {
            private final long timeout;
            private final Timer timer;

            private TimerInfo(long timeout, Timer timer) {
                this.timeout = Clock.now() + timeout;
                this.timer = timer;
            }
        }

        private final List<TimerInfo> timeouts;

        public TimerSet() {
            this.timeouts = new LinkedList<>();
        }

        public synchronized void add(long timeout, Timer timer) {
            TimerInfo info = new TimerInfo(timeout, timer);
            int i = 0;
            for (; i < this.timeouts.size(); i++)
                if (info.timeout < this.timeouts.get(i).timeout)
                    break;
            this.timeouts.add(i, info);
        }

        public synchronized void remove(Timer timer) {
            Iterator<TimerInfo> it = this.timeouts.iterator();
            while (it.hasNext()) {
                TimerInfo info = it.next();
                if (timer == info.timer)
                    it.remove();
            }
        }

        public synchronized long timeout() {
            if (this.timeouts.isEmpty())
                return -1;
            long timeout = this.timeouts.get(0).timeout - Clock.now();
            return timeout < 0 ? 0 : timeout;
        }

        public synchronized Timer event() {
            if (this.timeouts.isEmpty())
                return null;
            if (this.timeouts.get(0).timeout > Clock.now())
                return null;
            return this.timeouts.remove(0).timer;
        }
    }
}
