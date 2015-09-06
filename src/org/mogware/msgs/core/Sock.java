// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3)
// Source File Name:   Sock.java

package org.mogware.msgs.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.mogware.msgs.aio.Ctx;
import org.mogware.msgs.aio.Fsm;
import org.mogware.msgs.aio.FsmBadActionException;
import org.mogware.msgs.aio.FsmBadSourceException;
import org.mogware.msgs.aio.FsmBadStateException;
import org.mogware.msgs.protocols.SockBase;
import org.mogware.msgs.protocols.SockType;
import org.mogware.msgs.transports.Transport;
import org.mogware.msgs.utils.Clock;
import org.mogware.msgs.utils.Efd;
import org.mogware.msgs.utils.ErrnoException;
import org.mogware.msgs.utils.Msg;
import org.mogware.msgs.utils.Sem;

public class Sock {
    private static final int FLAG_IN = 1;
    private static final int FLAG_OUT = 2;

    private static final int STATE_INIT = 1;
    private static final int STATE_ACTIVE = 2;
    private static final int STATE_ZOMBIE = 3;
    private static final int STATE_STOPPING_EPS = 4;
    private static final int STATE_STOPPING = 5;

    private static final int ACTION_ZOMBIFY = 1;
    private static final int ACTION_STOPPED = 2;

    private static final int SRC_EP = 1;

    private static final int STAT_MESSAGES_SENT = 301;
    private static final int STAT_MESSAGES_RECEIVED = 302;
    private static final int STAT_BYTES_SENT = 303;
    private static final int STAT_BYTES_RECEIVED = 304;

    private final Fsm fsm;
    private final Ctx ctx;
    private final Efd sndfd;
    private final Efd rcvfd;
    private final Sem termsem;
    private final SockType socktype;
    private final SockBase sockbase;

    private final List<Endpoint> eps = new LinkedList<>();
    private final List<Endpoint> sdeps = new LinkedList<>();

    private final Map<Integer, OptSet> optsets = new HashMap<>();

    private int state;
    private int flags;
    private int eid;

    private int linger;
    private int sndbuf;
    private int rcvbuf;
    private int sndtimeo;
    private int rcvtimeo;
    private int ipv4only;
    private int sndprio;
    private int rcvprio;
    private int reconnectIvl;
    private int reconnectIvlMax;

    public final class Statistics {
        public int established_connections;
        public int accepted_connections;
        public int dropped_connections;
        public int broken_connections;
        public int connect_errors;
        public int bind_errors;
        public int accept_errors;
        public int messages_sent;
        public int messages_received;
        public int bytes_sent;
        public int bytes_received;
        public int current_connections;
        public int inprogress_connections;
        public int current_snd_priority;
        public int current_ep_errors;
    }
    public Statistics statistics = new Statistics();

    public Sock(SockType socktype) throws ErrnoException {
        assert((socktype.flags() & SockType.FLAG_NOSEND) == 0 ||
                (socktype.flags() & SockType.FLAG_NORECV) == 0);
        final Sock sock = this;
        this.ctx = new Ctx(Global.pool()) {
            @Override
            public void onLeave() throws ErrnoException {
                if (sock.state != Sock.STATE_ACTIVE)
                    return;
                int events = sock.sockbase.events();
                if ((socktype.flags() & SockType.FLAG_NORECV) == 0) {
                    if ((events & SockBase.EVENT_IN) != 0) {
                        if ((sock.flags & Sock.FLAG_IN) == 0) {
                            sock.flags |= Sock.FLAG_IN;
                            sock.rcvfd.signal();
                        }
                    } else {
                        if ((sock.flags & Sock.FLAG_IN) != 0) {
                            sock.flags &= ~Sock.FLAG_IN;
                            sock.rcvfd.unsignal();
                        }
                    }
                }
                if ((socktype.flags() & SockType.FLAG_NOSEND) == 0) {
                    if ((events & SockBase.EVENT_OUT) != 0) {
                        if ((sock.flags & Sock.FLAG_OUT) == 0) {
                            sock.flags |= Sock.FLAG_OUT;
                            sock.sndfd.signal();
                        }
                    } else {
                        if ((sock.flags & Sock.FLAG_OUT) != 0) {
                            sock.flags &= ~Sock.FLAG_OUT;
                            sock.sndfd.unsignal();
                        }
                    }
                }
            }
        };
        this.fsm = new Fsm(this.ctx) {
            Endpoint ep;
            @Override
            protected void onProgress(int src, int type, Object srcObj)
                    throws ErrnoException {
                switch (sock.state) {
                case Sock.STATE_INIT:
                    switch (src) {
                    case Fsm.ACTION:
                        switch (type) {
                        case Fsm.START:
                            sock.state = Sock.STATE_ACTIVE;
                            return;
                        case Sock.ACTION_ZOMBIFY:
                            sock.zombifyAction();
                            return;
                        default:
                            throw new FsmBadActionException(sock.state, src, type);
                        }
                    default:
                        throw new FsmBadSourceException(sock.state, src, type);
                    }
                case Sock.STATE_ACTIVE:
                    switch (src) {
                    case Fsm.ACTION:
                        switch (type) {
                        case Sock.ACTION_ZOMBIFY:
                            sock.zombifyAction();
                            return;
                        default:
                            throw new FsmBadActionException(sock.state, src, type);
                        }
                    case Sock.SRC_EP:
                        switch (type) {
                        case Endpoint.STOPPED:
                            ep = (Endpoint) srcObj;
                            sock.sdeps.remove(ep);
                            return;
                        default:
                            throw new FsmBadActionException(sock.state, src, type);
                        }
                    default:
                        switch (type) {
                        case Pipe.IN:
                            sock.sockbase.in((PipeBase) srcObj);
                            return;
                        case Pipe.OUT:
                            sock.sockbase.out((PipeBase) srcObj);
                           return;
                        default:
                            throw new FsmBadActionException(sock.state, src, type);
                        }
                    }
                case Sock.STATE_ZOMBIE:
                    throw new FsmBadStateException(sock.state, src, type);
                default:
                    throw new FsmBadStateException(sock.state, src, type);
                }
            }
            protected void onShutdown(int src, int type, Object srcObj)
                    throws ErrnoException {
                if (src == Fsm.ACTION && type == Fsm.STOP) {
                    assert(sock.state == Sock.STATE_ACTIVE ||
                            sock.state == Sock.STATE_ZOMBIE);
                    if ((sock.socktype.flags() & SockType.FLAG_NORECV) == 0)
                        sock.rcvfd.close();
                    if ((sock.socktype.flags() & SockType.FLAG_NOSEND) == 0)
                        sock.sndfd.close();
                    Iterator<Endpoint> it = sock.eps.iterator();
                    while (it.hasNext()) {
                        Endpoint ep = it.next();
                        it.remove();
                        sock.sdeps.add(ep);
                        ep.stop();
                    }
                    sock.state = Sock.STATE_STOPPING_EPS;
                    if (!sock.sdeps.isEmpty())
                        return;
                    sock.state = Sock.STATE_STOPPING;
                    if (sock.sockbase.stop())
                        return;
                    sock.state = Sock.STATE_INIT;
                    sock.termsem.semSignal();
                    return;
                }
                if (sock.state == Sock.STATE_STOPPING_EPS) {
                    Endpoint ep = (Endpoint) srcObj;
                    sock.sdeps.remove(ep);
                    if (!sock.sdeps.isEmpty())
                        return;
                    assert(sock.eps.isEmpty());
                    sock.state = Sock.STATE_STOPPING;
                    if (sock.sockbase.stop())
                        return;
                    sock.state = Sock.STATE_INIT;
                    sock.termsem.semSignal();
                    return;
                }
                if (sock.state == Sock.STATE_STOPPING) {
                    assert(src == Fsm.ACTION && type == Sock.ACTION_STOPPED);
                    sock.state = Sock.STATE_INIT;
                    sock.termsem.semSignal();
                    return;
                }
                throw new FsmBadStateException(sock.state, src, type);
            }
        };

        if ((socktype.flags() & SockType.FLAG_NOSEND) != 0)
            this.sndfd = null;
        else
            this.sndfd = new Efd();

        if ((socktype.flags() & SockType.FLAG_NORECV) != 0)
            this.rcvfd = null;
        else {
            try {
                this.rcvfd = new Efd();
            } catch (ErrnoException ex) {
                if ((socktype.flags() & SockType.FLAG_NOSEND) == 0)
                    this.sndfd.close();
                throw ex;
            }
        }
        this.termsem = new Sem(0);

        /*  Default values for NN_SOL_SOCKET options. */
        this.linger = 1000;
        this.sndbuf = 128 * 1024;
        this.rcvbuf = 128 * 1024;
        this.sndtimeo = -1;
        this.rcvtimeo = -1;
        this.sndprio = 8;
        this.rcvprio = 8;
        this.ipv4only = 1;
        this.reconnectIvl = 100;
        this.reconnectIvlMax = 0;

        /* Initialize statistic entries */
        this.statistics.established_connections = 0;
        this.statistics.accepted_connections = 0;
        this.statistics.dropped_connections = 0;
        this.statistics.broken_connections = 0;
        this.statistics.connect_errors = 0;
        this.statistics.bind_errors = 0;
        this.statistics.accept_errors = 0;

        this.statistics.messages_sent = 0;
        this.statistics.messages_received = 0;
        this.statistics.bytes_sent = 0;
        this.statistics.bytes_received = 0;

        this.statistics.current_connections = 0;
        this.statistics.inprogress_connections = 0;
        this.statistics.current_snd_priority = 0;
        this.statistics.current_ep_errors = 0;

        this.state = Sock.STATE_INIT;
        this.flags = 0;
        this.eid = 1;

        this.sockbase = socktype.create(sock);
        this.socktype = socktype;

        this.ctx.enter();
        this.fsm.start();
        this.ctx.leave();
    }

    public void stopped() {
        this.fsm.stoppedAction(Sock.ACTION_STOPPED);
    }

    public void zombify() throws ErrnoException {
        this.ctx.enter();
        this.fsm.action(Sock.ACTION_ZOMBIFY);
        this.ctx.leave();
    }

    public void dispose() throws ErrnoException {
        this.ctx.enter();
        this.fsm.stop();
        this.ctx.leave();

        try {
            this.termsem.semWait();
        } catch (InterruptedException ex) {
            throw new ErrnoException(Global.EINTR);
        }

        this.ctx.enter();
        this.ctx.leave();

        this.fsm.stoppedNoEvent();
    }

    public Ctx ctx() {
        return this.ctx;
    }

    public Fsm fsm() {
        return this.fsm;
    }

    public boolean isPeer(int socktype) {
        if ((this.socktype.protocol() & 0xfff0) != (socktype & 0xfff0))
            return false;
        return this.socktype.isPeer(socktype);
    }

    public void opt(int level, int option, Object val)
            throws ErrnoException {
        this.ctx.enter();
        if (this.state == Sock.STATE_ZOMBIE) {
            this.ctx.leave();
            throw new ErrnoException(Global.ETERM);
        }
        this.optInner(level, option, val);
        this.ctx.leave();
    }

    public void optInner(int level, int option, Object val)
        throws ErrnoException {
        if (level > Global.SOL_SOCKET)
            this.sockbase.opt(level, option, val);
        else if (level < Global.SOL_SOCKET) {
            OptSet optset = this.optset(level);
            if (optset == null)
                throw new ErrnoException(Global.ENOPROTOOPT);
            optset.opt(option, val);
        } else {
            switch (option) {
            case Global.LINGER:
                this.linger = (Integer) val;
                break;
            case Global.SNDBUF:
                this.sndbuf = (Integer) val;
                break;
            case Global.RCVBUF:
                this.rcvbuf = (Integer) val;
                break;
            case Global.SNDTIMEO:
                this.sndtimeo = (Integer) val;
                break;
            case Global.RCVTIMEO:
                this.rcvtimeo = (Integer) val;
                break;
            case Global.SNDPRIO:
                this.sndprio = (Integer)val;
                break;
            case Global.RCVPRIO:
                this.rcvprio = (Integer)val;
                break;
            case Global.IPV4ONLY:
                this.ipv4only = (Integer) val;
                break;
            case Global.RECONNECT_IVL:
                this.reconnectIvl = (Integer) val;
                break;
            case Global.RECONNECT_IVL_MAX:
                this.reconnectIvlMax = (Integer) val;
                break;
            }
        }
    }

    public Object opt(int level, int option) throws ErrnoException {
        this.ctx.enter();
        if (this.state == Sock.STATE_ZOMBIE) {
            this.ctx.leave();
            throw new ErrnoException(Global.ETERM);
        }
        Object retval = this.optInner(level, option);
        this.ctx.leave();
        return retval;
    }

    public Object optInner(int level, int option) throws ErrnoException {
        if (level > Global.SOL_SOCKET)
            return this.sockbase.opt(level, option);
        if (level < Global.SOL_SOCKET) {
            OptSet optset = this.optset(level);
            if (optset == null)
                throw new ErrnoException(Global.ENOPROTOOPT);
            return optset.opt(option);
        }
        switch (option) {
        case Global.DOMAIN:
            return this.socktype.domain();
        case Global.PROTOCOL:
            return this.socktype.protocol();
        case Global.LINGER:
            return this.linger;
        case Global.SNDBUF:
            return this.sndbuf;
        case Global.RCVBUF:
            return this.rcvbuf;
        case Global.SNDTIMEO:
            return this.sndtimeo;
        case Global.RCVTIMEO:
            return this.rcvtimeo;
        case Global.SNDPRIO:
            return this.sndprio;
        case Global.RCVPRIO:
            return this.rcvprio;
        case Global.IPV4ONLY:
            return this.ipv4only;
        case Global.RECONNECT_IVL:
            return this.reconnectIvl;
        case Global.RECONNECT_IVL_MAX:
            return this.reconnectIvlMax;
        default:
            throw new ErrnoException(Global.ENOPROTOOPT);
        }
    }

    public int addEndpoint(Transport transport, boolean bind, String addr)
            throws ErrnoException {
        this.ctx.enter();
        try {
            Endpoint ep = new Endpoint(Sock.SRC_EP, this, this.eid,
                    transport, bind, addr);
            ep.start();
            this.eps.add(ep);
        } catch (ErrnoException ex) {
            this.ctx.leave();
            throw ex;
        }
        int eid = this.eid;
        this.eid++;
        this.ctx.leave();
        return eid;
    }

    public void removeEndpoint(int eid) throws ErrnoException {
        this.ctx.enter();
        Iterator<Endpoint> it = eps.iterator();
        while (it.hasNext()) {
            Endpoint ep = it.next();
            if (ep.id() == eid) {
                it.remove();
                this.sdeps.add(ep);
                ep.stop();
                this.ctx.leave();
            }
        }
        this.ctx.leave();
        throw new ErrnoException(Global.EINVAL);
    }

    public void send(Msg msg, int flags) throws ErrnoException {
        long deadline, now;
        int timeout;

        if ((this.socktype.flags() & SockType.FLAG_NOSEND) != 0)
            throw new ErrnoException(Global.ENOTSUP);
        if (this.sndtimeo < 0) {
            deadline = -1;
            timeout = -1;
        } else {
            deadline = Clock.now() + this.sndtimeo;
            timeout = this.sndtimeo;
        }

        this.ctx.enter();
        while (true) {
            if (this.state == Sock.STATE_ZOMBIE) {
                this.ctx.leave();
                throw new ErrnoException(Global.ETERM);
            }
            try {
                this.sockbase.send(msg);
                this.ctx.leave();
                return;
            } catch (ErrnoException ex) {
                int errno = ex.errno();
                if (errno != Global.EAGAIN) {
                    this.ctx.leave();
                    throw ex;
                }
                if ((flags & Global.DONTWAIT) != 0) {
                    this.ctx.leave();
                    throw ex;
                }
                this.ctx.leave();
                if (! this.sndfd.await(timeout))
                    throw new ErrnoException(Global.EAGAIN);
                this.ctx.enter();
                if (! this.sndfd.await(0))
                    this.flags |= Sock.FLAG_OUT;
                if (this.sndtimeo >= 0) {
                    now = Clock.now();
                    timeout = (int)(now > deadline ? 0 : deadline - now);
                }
            }
        }
    }

    public Msg recv(int flags) throws ErrnoException {
        long deadline, now;
        int timeout;

        if ((this.socktype.flags() & SockType.FLAG_NORECV) != 0)
            throw new ErrnoException(Global.ENOTSUP);
        if (this.rcvtimeo < 0) {
            deadline = -1;
            timeout = -1;
        } else {
            deadline = Clock.now() + this.rcvtimeo;
            timeout = this.rcvtimeo;
        }

        this.ctx.enter();
        while (true) {
            if (this.state == Sock.STATE_ZOMBIE) {
                this.ctx.leave();
                throw new ErrnoException(Global.ETERM);
            }
            try {
                Msg msg = this.sockbase.recv();
                this.ctx.leave();
                return msg;
            } catch (ErrnoException ex) {
                int errno = ex.errno();
                if (errno != Global.EAGAIN) {
                    this.ctx.leave();
                    throw ex;
                }
                if ((flags & Global.DONTWAIT) != 0) {
                    this.ctx.leave();
                    throw ex;
                }
                this.ctx.leave();
                if (! this.rcvfd.await(timeout))
                    throw new ErrnoException(Global.EAGAIN);
                this.ctx.enter();
                if (! this.rcvfd.await(0))
                    this.flags |= Sock.FLAG_IN;
                if (this.rcvtimeo >= 0) {
                    now = Clock.now();
                    timeout = (int)(now > deadline ? 0 : deadline - now);
                }
            }
        }
    }

    public void add(PipeBase pipebase) throws ErrnoException {
        this.sockbase.add(pipebase);
        this.statIncrement(Transport.STAT_CURRENT_CONNECTIONS, 1);
    }

    public void remove(PipeBase pipebase) {
        this.sockbase.remove(pipebase);
        this.statIncrement(Transport.STAT_CURRENT_CONNECTIONS, -1);
    }

    private OptSet optset(int id) {
        if (this.optsets.containsKey(id))
            return this.optsets.get(id);
        Transport trans = Global.transport(id);
        if (trans == null)
            return null;
        OptSet optset = trans.optset();
        if (optset == null)
            return null;
        this.optsets.put(id, optset);
        return optset;
    }

    private void zombifyAction() throws ErrnoException {
        this.state = Sock.STATE_ZOMBIE;
        if ((this.flags & Sock.FLAG_IN) == 0) {
            this.flags |= Sock.FLAG_IN;
            if ((socktype.flags() & SockType.FLAG_NORECV) == 0)
                this.rcvfd.signal();
        }
        if ((this.flags & Sock.FLAG_OUT) == 0) {
            this.flags |= Sock.FLAG_OUT;
            if ((socktype.flags() & SockType.FLAG_NOSEND) == 0)
                this.sndfd.signal();
        }
    }

    public void reportError(Endpoint ep, int errnum) {
        if (errnum == 0)
            return;
        if (ep != null)
            System.err.printf("msgs: socket[%s]: Error: %s\n",
                    ep.addr(), Global.strerror(errnum));
        else
            System.err.printf("msgs: socket: Error: %s\n",
                    Global.strerror(errnum));
    }

    public void statIncrement(int name, int increment) {
        switch (name) {
        case Transport.STAT_ESTABLISHED_CONNECTIONS:
            assert(increment > 0);
            this.statistics.established_connections += increment;
            break;
        case Transport.STAT_ACCEPTED_CONNECTIONS:
            assert(increment > 0);
            this.statistics.accepted_connections += increment;
            break;
        case Transport.STAT_DROPPED_CONNECTIONS:
            assert(increment > 0);
            this.statistics.dropped_connections += increment;
            break;
        case Transport.STAT_BROKEN_CONNECTIONS:
            assert(increment > 0);
            this.statistics.broken_connections += increment;
            break;
        case Transport.STAT_CONNECT_ERRORS:
            assert(increment > 0);
            this.statistics.connect_errors += increment;
            break;
        case Transport.STAT_BIND_ERRORS:
            assert(increment > 0);
            this.statistics.bind_errors += increment;
            break;
        case Transport.STAT_ACCEPT_ERRORS:
            assert(increment > 0);
            this.statistics.accept_errors += increment;
            break;
        case Sock.STAT_MESSAGES_SENT:
            assert(increment > 0);
            this.statistics.messages_sent += increment;
            break;
        case Sock.STAT_MESSAGES_RECEIVED:
            assert(increment > 0);
            this.statistics.messages_received += increment;
            break;
        case Sock.STAT_BYTES_SENT:
            assert(increment > 0);
            this.statistics.bytes_sent += increment;
            break;
        case Sock.STAT_BYTES_RECEIVED:
            assert(increment > 0);
            this.statistics.bytes_received += increment;
            break;
        case Transport.STAT_CURRENT_CONNECTIONS:
            assert(increment > 0 ||
                    this.statistics.current_connections >= -increment);
            this.statistics.current_connections += increment;
            break;
        case Transport.STAT_INPROGRESS_CONNECTIONS:
            assert(increment > 0 ||
                    this.statistics.inprogress_connections >= -increment);
            this.statistics.inprogress_connections += increment;
            break;
        case SockType.STAT_CURRENT_SND_PRIORITY:
            assert(increment > 0 && increment <= 16);
            this.statistics.current_snd_priority = increment;
            break;
        case Transport.STAT_CURRENT_EP_ERRORS:
            assert(increment > 0 ||
                    this.statistics.current_ep_errors >= -increment);
            this.statistics.current_ep_errors += increment;
            break;
        }
    }
}
