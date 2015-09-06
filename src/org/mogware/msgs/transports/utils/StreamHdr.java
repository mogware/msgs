package org.mogware.msgs.transports.utils;

import java.nio.ByteBuffer;
import org.mogware.msgs.aio.Fsm;
import org.mogware.msgs.aio.FsmBadActionException;
import org.mogware.msgs.aio.FsmBadSourceException;
import org.mogware.msgs.aio.FsmBadStateException;
import org.mogware.msgs.aio.FsmEvent;
import org.mogware.msgs.aio.SockBase;
import org.mogware.msgs.aio.SockClient;
import org.mogware.msgs.aio.Timer;
import org.mogware.msgs.core.Global;
import org.mogware.msgs.core.PipeBase;
import org.mogware.msgs.utils.ErrnoException;

public class StreamHdr {
    private static final int STATE_IDLE = 1;
    private static final int STATE_SENDING = 2;
    private static final int STATE_RECEIVING = 3;
    private static final int STATE_STOPPING_TIMER_ERROR = 4;
    private static final int STATE_STOPPING_TIMER_DONE = 5;
    private static final int STATE_DONE = 6;
    private static final int STATE_STOPPING = 7;

    private static final int SRC_USOCK = 1;
    private static final int SRC_TIMER = 2;

    public static final int OK = 1;
    public static final int ERROR = 2;
    public static final int STOPPED = 3;

    private static final int MAGIC = 0x535000;

    private final Fsm fsm;
    private final Fsm.Owner owner = new Fsm.Owner();
    private final Timer timer;

    private final ByteBuffer protohdr = ByteBuffer.allocate(8);

    private int state;
    private SockClient usock;
    private PipeBase pipebase;

    private final FsmEvent done;

    public StreamHdr(final int src, Fsm owner) {
        final StreamHdr streamhdr = this;
        this.fsm = new Fsm(src, this, owner) {
            @Override
            protected void onProgress(int src, int type, Object srcObj)
                    throws ErrnoException {
                int magic, protocol;
                switch (streamhdr.state) {
                case StreamHdr.STATE_IDLE:
                    switch (src) {
                    case Fsm.ACTION:
                        switch (type) {
                        case Fsm.START:
                            streamhdr.timer.start(1000);
                            streamhdr.usock.send(streamhdr.protohdr);
                            streamhdr.state = StreamHdr.STATE_SENDING;
                            return;
                        default:
                            throw new FsmBadActionException(streamhdr.state, src, type);
                        }
                    default:
                        throw new FsmBadSourceException(streamhdr.state, src, type);
                    }
                case StreamHdr.STATE_SENDING:
                    switch (src) {
                    case StreamHdr.SRC_USOCK:
                        switch (type) {
                        case SockBase.SENT:
                            streamhdr.protohdr.clear();
                            streamhdr.usock.recv(streamhdr.protohdr);
                            streamhdr.state = StreamHdr.STATE_RECEIVING;
                            return;
                        case SockBase.SHUTDOWN:
                            return;
                        case SockBase.ERROR:
                            streamhdr.timer.stop();
                            streamhdr.state = StreamHdr.STATE_STOPPING_TIMER_ERROR;
                            return;
                        default:
                            throw new FsmBadActionException(streamhdr.state, src, type);
                        }
                    case StreamHdr.SRC_TIMER:
                        switch (type) {
                        case Timer.TIMEOUT:
                            streamhdr.timer.stop();
                            streamhdr.state = StreamHdr.STATE_STOPPING_TIMER_ERROR;
                            return;
                        default:
                            throw new FsmBadActionException(streamhdr.state, src, type);
                        }
                    default:
                        throw new FsmBadSourceException(streamhdr.state, src, type);
                    }
                case StreamHdr.STATE_RECEIVING:
                    switch (src) {
                    case StreamHdr.SRC_USOCK:
                        switch (type) {
                        case SockBase.RECEIVED:
                            magic = protohdr.getInt();
                            protocol = protohdr.getInt();
                            if (magic != StreamHdr.MAGIC)
                                streamhdr.state = StreamHdr.STATE_STOPPING_TIMER_ERROR;
                            else if (!streamhdr.pipebase.isPeer(protocol))
                                streamhdr.state = StreamHdr.STATE_STOPPING_TIMER_ERROR;
                            else
                                streamhdr.state = StreamHdr.STATE_STOPPING_TIMER_DONE;
                            streamhdr.timer.stop();
                            return;
                        case SockBase.SHUTDOWN:
                            return;
                        case SockBase.ERROR:
                            streamhdr.timer.stop();
                            streamhdr.state = StreamHdr.STATE_STOPPING_TIMER_ERROR;
                            return;
                        default:
                            throw new FsmBadActionException(streamhdr.state, src, type);
                        }
                    case StreamHdr.SRC_TIMER:
                        switch (type) {
                        case Timer.TIMEOUT:
                            streamhdr.timer.stop();
                            streamhdr.state = StreamHdr.STATE_STOPPING_TIMER_ERROR;
                            return;
                        default:
                            throw new FsmBadActionException(streamhdr.state, src, type);
                        }
                    default:
                        throw new FsmBadSourceException(streamhdr.state, src, type);
                    }
                case StreamHdr.STATE_STOPPING_TIMER_ERROR:
                    switch (src) {
                    case StreamHdr.SRC_USOCK:
                        return;
                    case StreamHdr.SRC_TIMER:
                        switch (type) {
                        case Timer.STOPPED:
                            streamhdr.usock.swapOwner(streamhdr.owner);
                            streamhdr.usock = null;
                            streamhdr.owner.src = -1;
                            streamhdr.owner.fsm = null;
                            streamhdr.state = StreamHdr.STATE_DONE;
                            streamhdr.fsm.raise(streamhdr.done, StreamHdr.ERROR);
                            return;
                        default:
                            throw new FsmBadActionException(streamhdr.state, src, type);
                        }
                    default:
                        throw new FsmBadSourceException(streamhdr.state, src, type);
                    }
                case StreamHdr.STATE_STOPPING_TIMER_DONE:
                    switch (src) {
                    case StreamHdr.SRC_TIMER:
                        switch (type) {
                        case Timer.STOPPED:
                            streamhdr.usock.swapOwner(streamhdr.owner);
                            streamhdr.usock = null;
                            streamhdr.owner.src = -1;
                            streamhdr.owner.fsm = null;
                            streamhdr.state = StreamHdr.STATE_DONE;
                            streamhdr.fsm.raise(streamhdr.done, StreamHdr.OK);
                            return;
                        default:
                            throw new FsmBadActionException(streamhdr.state, src, type);
                        }
                    default:
                        throw new FsmBadSourceException(streamhdr.state, src, type);
                    }
                case StreamHdr.STATE_DONE:
                    throw new FsmBadSourceException(streamhdr.state, src, type);
                default:
                    throw new FsmBadStateException(streamhdr.state, src, type);
                }
            }
            @Override
            protected void onShutdown(int src, int type, Object srcObj)
                throws ErrnoException {
                if (src == ACTION && type == STOP) {
                    streamhdr.timer.stop();
                    streamhdr.state = StreamHdr.STATE_STOPPING;
                }
                if (streamhdr.state == StreamHdr.STATE_STOPPING) {
                    if (!streamhdr.timer.isIdle())
                        return;
                    streamhdr.state = StreamHdr.STATE_IDLE;
                    streamhdr.fsm.stopped(StreamHdr.STOPPED);
                    return;
                }
                throw new FsmBadStateException(streamhdr.state, src, type);
            }
        };
        this.state = StreamHdr.STATE_IDLE;
        this.timer = new Timer(StreamHdr.SRC_TIMER, this.fsm);
        this.done = new FsmEvent();
        this.usock = null;
        this.owner.src = -1;
        this.owner.fsm = null;
        this.pipebase = null;
    }

    public boolean isIdle() {
        return this.fsm.isIdle();
    }

    public void start(SockClient usock, PipeBase pipebase)
            throws ErrnoException {
        assert(this.usock == null && this.owner.fsm == null);
        this.owner.src = StreamHdr.SRC_USOCK;
        this.owner.fsm = this.fsm;
        this.usock = usock;
        this.usock.swapOwner(owner);
        this.pipebase = pipebase;
        Integer protocol = (Integer) this.pipebase.opt(
                Global.SOL_SOCKET, Global.PROTOCOL);
        this.protohdr.putInt(StreamHdr.MAGIC);
        this.protohdr.putInt(protocol);
        this.protohdr.flip();
        this.fsm.start();

    }

    public void stop() throws ErrnoException {
        this.fsm.stop();
    }
}
