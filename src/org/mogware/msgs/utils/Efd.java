package org.mogware.msgs.utils;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.concurrent.atomic.AtomicInteger;
import org.mogware.msgs.core.Global;

public class Efd implements Closeable {
    private final Pipe.SinkChannel w;
    private final Pipe.SourceChannel r;
    private final Selector selector;

    private final AtomicInteger wcursor = new AtomicInteger(0);
    private int rcursor = 0;

    public Efd() throws ErrnoException {
        try {
            Pipe pipe = Pipe.open();
            this.r = pipe.source();
            this.r.configureBlocking(false);
            this.w = pipe.sink();
            this.w.configureBlocking(false);
            this.selector = Selector.open();
            this.r.register(this.selector, SelectionKey.OP_READ);
        } catch (IOException ex) {
            throw new ErrnoException(Global.EMFILE);
        }
    }

    @Override
    public void close() {
        try {
            this.r.close();
            this.w.close();
            this.selector.close();
        } catch (IOException ex) {
        }
    }

    public SelectableChannel fd() {
        return this.r;
    }

    public void signal() throws ErrnoException {
        int nbytes;
        ByteBuffer bb = ByteBuffer.allocate(1);
        while (true) {
            try {
                nbytes = this.w.write(bb);
            } catch (IOException ex) {
                throw new ErrnoException(Global.EINTR);
            }
            if (nbytes == 0)
                continue;
            this.wcursor.incrementAndGet();
            break;
        }
    }

    public void unsignal() throws ErrnoException {
        int nbytes;
        ByteBuffer bb = ByteBuffer.allocate(1);
        while (true) {
            try {
                nbytes = this.r.read(bb);
            } catch (IOException ex) {
                throw new ErrnoException(Global.EINTR);
            }
            if (nbytes == 0)
                continue;
            this.rcursor++;
            break;
        }
    }

    public boolean await(long timeout) throws ErrnoException {
        if (timeout == 0)
            return this.rcursor < this.wcursor.get();
        try {
            int rc;
            if (timeout < 0)
                rc = this.selector.select(0);
            else
                rc = this.selector.select(timeout);
            if (rc == 0)
                return false;
            this.selector.selectedKeys().clear();
            return true;
        } catch (IOException ex) {
            throw new ErrnoException(Global.EINTR);
        }
    }
}
