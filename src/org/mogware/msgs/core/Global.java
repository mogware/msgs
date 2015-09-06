package org.mogware.msgs.core;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.mogware.msgs.aio.Ctx;
import org.mogware.msgs.aio.Fsm;
import org.mogware.msgs.aio.FsmBadActionException;
import org.mogware.msgs.aio.FsmBadSourceException;
import org.mogware.msgs.aio.FsmBadStateException;
import org.mogware.msgs.aio.Pool;
import org.mogware.msgs.aio.Timer;
import org.mogware.msgs.protocols.Bus;
import org.mogware.msgs.protocols.Pair;
import org.mogware.msgs.protocols.SockType;
import org.mogware.msgs.protocols.XBus;
import org.mogware.msgs.protocols.XPair;
import org.mogware.msgs.transports.Transport;
import org.mogware.msgs.transports.itc.Itc;
import org.mogware.msgs.transports.tcp.Tcp;
import org.mogware.msgs.transports.udp.Udp;
import org.mogware.msgs.utils.ErrnoException;
import org.mogware.msgs.utils.Msg;

public final class Global {
    public static final int AF_SP = 1;
    public static final int AF_SP_RAW = 2;

    public static final int PAIR = (1 * 16 + 0);
    public static final int PUB = (2 * 16 + 0);
    public static final int SUB = (2 * 16 + 1);
    public static final int REQ = (3 * 16 + 0);
    public static final int REP = (3 * 16 + 1);
    public static final int PUSH = (5 * 16 + 0);
    public static final int PULL = (5 * 16 + 1);
    public static final int BUS = (7 * 16 + 0);

    public static final int SOL_SOCKET = 1;

    public static final int LINGER = 1;
    public static final int SNDBUF = 2;
    public static final int RCVBUF = 3;
    public static final int SNDTIMEO = 4;
    public static final int RCVTIMEO = 5;
    public static final int RECONNECT_IVL = 6;
    public static final int RECONNECT_IVL_MAX = 7;
    public static final int SNDPRIO = 8;
    public static final int RCVPRIO = 9;
    public static final int DOMAIN = 12;
    public static final int PROTOCOL = 13;
    public static final int IPV4ONLY = 14;

    public static final int DONTWAIT = 1;

    private static final int MAX_SOCKETS = 512;

    private static final int HAUSNUMERO = 156384712;

    /*  POSIX error codes.
    */
    public static final int ENOTSUP = HAUSNUMERO + 1;
    public static final int EPROTONOSUPPORT = HAUSNUMERO + 2;
    public static final int ENOBUFS  = HAUSNUMERO + 3;
    public static final int ENETDOWN = HAUSNUMERO + 4;
    public static final int EADDRINUSE = HAUSNUMERO + 5;
    public static final int EADDRNOTAVAIL = HAUSNUMERO + 6;
    public static final int ECONNREFUSED = HAUSNUMERO + 7;
    public static final int EINPROGRESS = HAUSNUMERO + 8;
    public static final int ENOTSOCK = HAUSNUMERO + 9;
    public static final int EAFNOSUPPORT = HAUSNUMERO + 10;
    public static final int EPROTO = HAUSNUMERO + 11;
    public static final int EAGAIN = HAUSNUMERO + 12;
    public static final int EBADF = HAUSNUMERO + 13;
    public static final int EINTR = HAUSNUMERO + 14;
    public static final int EINVAL = HAUSNUMERO + 15;
    public static final int EMFILE = HAUSNUMERO + 16;
    public static final int EFAULT = HAUSNUMERO + 17;
    public static final int EACCESS = HAUSNUMERO + 18;
    public static final int ENETRESET = HAUSNUMERO + 19;
    public static final int ENETUNREACH = HAUSNUMERO + 20;
    public static final int EHOSTUNREACH = HAUSNUMERO + 21;
    public static final int ENOTCONN = HAUSNUMERO + 22;
    public static final int EMSGSIZE = HAUSNUMERO + 23;
    public static final int ETIMEDOUT = HAUSNUMERO + 24;
    public static final int ECONNABORTED = HAUSNUMERO + 25;
    public static final int ECONNRESET = HAUSNUMERO + 26;
    public static final int ENOPROTOOPT = HAUSNUMERO + 27;
    public static final int EISCONN = HAUSNUMERO + 28;
    public static final int ENODEV = HAUSNUMERO + 29;
    public static final int EIO = HAUSNUMERO + 30;

    /*  Native error codes.
    */
    public static final int ETERM = HAUSNUMERO + 53;
    public static final int EFSM = HAUSNUMERO + 54;

    private static int errno = 0;

    public static synchronized int socket(int domain, int protocol) {
        try {
            Context.instance().start();
            return Context.instance().createSocket(domain, protocol);
        } catch(ErrnoException ex) {
            Global.errno = ex.errno();
            Context.instance().term();
            System.out.println("socket: " + ex.getMessage());
        }
        catch(RuntimeException ex) {
            Global.errno = Global.EINTR;
            Context.instance().term();
            System.out.println("socket: " + ex.getMessage());
        }
        return -1;
    }

    public static synchronized int close(int s) {
        try {
            Context.instance().closeSocket(s);
            Context.instance().stop();
            return 0;
        } catch(ErrnoException ex) {
            Global.errno = ex.errno();
            System.out.println("close: " + ex.getMessage());
        } catch(RuntimeException ex) {
            Global.errno = Global.EINTR;
            System.out.println("close: " + ex.getMessage());
        }
        return -1;
    }

    public static int setsockopt(int s, int level, int option, Object val) {
        try {
            Context.instance().sock(s).opt(level, option, val);
            return 0;
        } catch(ErrnoException ex) {
            Global.errno = ex.errno();
            System.out.println("setsockopt: " + ex.getMessage());
        } catch(RuntimeException ex) {
            Global.errno = Global.EINTR;
            System.out.println("setsockopt: " + ex.getMessage());
        }
        return -1;
    }

    public static Object getsockopt(int s, int level, int option) {
        try {
            return Context.instance().sock(s).opt(level, option);
        } catch(ErrnoException ex) {
            Global.errno = ex.errno();
            System.out.println("getsockopt: " + ex.getMessage());
        } catch(RuntimeException ex) {
            Global.errno = Global.EINTR;
            System.out.println("getsockopt: " + ex.getMessage());
        }
        return null;
    }

    public static synchronized int bind(int s, String addr) {
        try {
            Context.instance().createEndpoint(s, addr, true);
            return 0;
        } catch(ErrnoException ex) {
            Global.errno = ex.errno();
            System.out.println("bind: " + ex.getMessage());
        } catch(RuntimeException ex) {
            Global.errno = Global.EINTR;
            System.out.println("bind: " + ex.getMessage());
        }
        return -1;
    }

    public static synchronized int connect(int s, String addr) {
        try {
            Context.instance().createEndpoint(s, addr, false);
            return 0;
        } catch(ErrnoException ex) {
            Global.errno = ex.errno();
            System.out.println("connect: " + ex.getMessage());
        } catch(RuntimeException ex) {
            Global.errno = Global.EINTR;
            System.out.println("connect: " + ex.getMessage());
        }
        return -1;
    }

    public static int shutdown(int s, int how) {
        try {
            Context.instance().sock(s).removeEndpoint(how);
            return 0;
        } catch(ErrnoException ex) {
            Global.errno = ex.errno();
            System.out.println("shutdown: " + ex.getMessage());
        } catch(RuntimeException ex) {
            Global.errno = Global.EINTR;
            System.out.println("shutdown: " + ex.getMessage());
        }
        return -1;
    }

    public static int sendmsg(int s, Msg msg, int flags) {
        try {
            Context.instance().sock(s).send(msg, flags);
            return 0;
        } catch(ErrnoException ex) {
            Global.errno = ex.errno();
            System.out.println("sendmsg: " + ex.getMessage());
        } catch(RuntimeException ex) {
            Global.errno = Global.EINTR;
            System.out.println("sendmsg: " + ex.getMessage());
        }
        return -1;
    }

    public static Msg recvmsg(int s, int flags) {
        try {
            return Context.instance().sock(s).recv(flags);
        } catch(ErrnoException ex) {
            Global.errno = ex.errno();
            System.out.println("recvmsg: " + ex.getMessage());
        } catch(RuntimeException ex) {
            Global.errno = Global.EINTR;
            System.out.println("recvmsg: " + ex.getMessage());
        }
        return null;
    }

    public static int errno() {
        return Global.errno;
    }

public static String errname(int errnum) {
        switch (errnum) {
        case ENOTSUP:
            return "ENOTSUP";
        case EPROTONOSUPPORT:
            return "EPROTONOSUPPORT";
        case ENOBUFS:
            return "ENOBUFS";
        case ENETDOWN:
            return "ENETDOWN";
        case EADDRINUSE:
            return "EADDRINUSE";
        case EADDRNOTAVAIL:
            return "EADDRNOTAVAIL";
        case ECONNREFUSED:
            return "ECONNREFUSED";
        case EINPROGRESS:
            return "EINPROGRESS";
        case ENOTSOCK:
            return "ENOTSOCK";
        case EAFNOSUPPORT:
            return "EAFNOSUPPORT";
        case EPROTO:
            return "EPROTO";
        case EAGAIN:
            return "EAGAIN";
        case EBADF:
            return "EBADF";
        case EINTR:
            return "EINTR";
        case EINVAL:
            return "EINVAL";
        case EMFILE:
            return "EMFILE";
        case EFAULT:
            return "EFAULT";
        case EACCESS:
            return "EACCESS";
        case ENETRESET:
            return "ENETRESET";
        case ENETUNREACH:
            return "ENETUNREACH";
        case EHOSTUNREACH:
            return "EHOSTUNREACH";
        case ENOTCONN:
            return "ENOTCONN";
        case EMSGSIZE:
            return "EMSGSIZE";
        case ETIMEDOUT:
            return "ETIMEDOUT";
        case ECONNABORTED:
            return "ECONNABORTED";
        case ECONNRESET:
            return "ECONNRESET";
        case ENOPROTOOPT:
            return "ENOPROTOOPT";
        case EISCONN:
            return "EISCONN";
        case ENODEV:
            return "ENODEV";
        case EIO:
            return "EIO";
        case ETERM:
            return "ETERM";
        case EFSM:
            return "EFSM";
        }
        return null;
    }

public static String strerror(int errnum) {
        switch (errnum) {
        case ENOTSUP:
            return "Not supported";
        case EPROTONOSUPPORT:
            return "Protocol not supported";
        case ENOBUFS:
            return "No buffer space available";
        case ENETDOWN:
            return "Network is down";
        case EADDRINUSE:
            return "Address in use";
        case EADDRNOTAVAIL:
            return "Address not available";
        case ECONNREFUSED:
            return "Connection refused";
        case EINPROGRESS:
            return "Operation in progress";
        case ENOTSOCK:
            return "Not a socket";
        case EAFNOSUPPORT:
            return "Address family not supported";
        case EPROTO:
            return "Protocol error";
        case EAGAIN:
            return "Resource unavailable, try again";
        case EBADF:
            return "Bad file descriptor";
        case EINTR:
            return "Interrupted function";
        case EINVAL:
            return "Invalid argument";
        case EMFILE:
            return "Too many open files";
        case EFAULT:
            return "Bad address";
        case EACCESS:
            return "Permission denied";
        case ENETRESET:
            return "Connection aborted by network";
        case ENETUNREACH:
            return "Network unreachable";
        case EHOSTUNREACH:
            return "Host is unreachable";
        case ENOTCONN:
            return "The socket is not connected";
        case EMSGSIZE:
            return "Message too large";
        case ETIMEDOUT:
            return "Timed out";
        case ECONNABORTED:
            return "Connection aborted";
        case ECONNRESET:
            return "Connection reset";
        case ENOPROTOOPT:
            return "Protocol not available";
        case EISCONN:
            return "Socket is connected";
        case ENODEV:
            return "Operation not supported by device";
        case EIO:
            return "I/O error";
        case ETERM:
            return "Nanomsg library was terminated";
        case EFSM:
            return "Operation cannot be performed in this state";
        }
        return "";
    }

    public static Transport transport(int id) {
        return Context.instance().transport(id);
    }

    public static Pool pool() {
        return Context.instance().pool();
    }

    private static class Context {
        private static final int STATE_IDLE = 1;
        private static final int STATE_ACTIVE = 2;
        private static final int STATE_STOPPING_TIMER = 3;

        private static final int SRC_STAT_TIMER = 1;

        private static Context instance = null;

        private static final Pattern PSEUDO_URI_PARSER =
                Pattern.compile("^([^:]+)://(.*)$");

        private final Sock socks[];
        private final int unused[];

        private int nsocks;
        private int flags;

        private final List<Transport> transports;
        private final List<SockType> socktypes;

        private Ctx ctx;
        private Fsm fsm;
        private Pool pool;
        private Timer timer;

        private int state;

        private boolean printStatistics;

        protected Context() {
            this.nsocks = 0;
            this.flags = 0;

            this.socks = new Sock[MAX_SOCKETS];
            for (int i = 0; i < MAX_SOCKETS; i++)
                this.socks[i] = null;
            this.unused = new int[MAX_SOCKETS];
            for (int i = 0; i < MAX_SOCKETS; i++)
                this.unused[i] = MAX_SOCKETS - i - 1;

            this.transports = new LinkedList<>();
            this.socktypes = new LinkedList<>();

            this.transports.add(new Itc());
            this.transports.add(new Tcp());
            this.transports.add(new Udp());

            this.socktypes.add(new Bus());
            this.socktypes.add(new XBus());
            this.socktypes.add(new Pair());
            this.socktypes.add(new XPair());

            this.printStatistics = true;
        }

        public void start() throws ErrnoException {
            if (this.nsocks != 0)
                return;
            this.pool = new Pool();
            this.ctx = new Ctx(this.pool) {
                @Override
                public void onLeave() {
                }
            };
            final Context cont = this;
            this.fsm = new Fsm(this.ctx) {
                @Override
                protected void onProgress(int src, int type, Object srcObj)
                        throws ErrnoException {
                    switch (cont.state) {
                    case Context.STATE_IDLE:
                        switch (src) {
                        case Fsm.ACTION:
                            switch (type) {
                            case Fsm.START:
                                cont.state = Context.STATE_ACTIVE;
                                if (cont.printStatistics)
                                    cont.timer.start(10000);
                                return;
                            default:
                                throw new FsmBadActionException(cont.state, src, type);
                            }
                        default:
                            throw new FsmBadSourceException(cont.state, src, type);
                        }
                    case Context.STATE_ACTIVE:
                        switch (src) {
                        case Context.SRC_STAT_TIMER:
                            switch (type) {
                            case Timer.TIMEOUT:
                                cont.submitStatistics();
                                cont.timer.stop();
                                return;
                            case Timer.STOPPED:
                                cont.timer.start(10000);
                                return;
                            default:
                                throw new FsmBadActionException(cont.state, src, type);
                            }
                        default:
                            throw new FsmBadSourceException(cont.state, src, type);
                        }
                    default:
                        throw new FsmBadStateException(cont.state, src, type);
                    }
                }
                @Override
                protected void onShutdown(int src, int type, Object srcObj)
                        throws ErrnoException {
                    if (cont.state == Context.STATE_ACTIVE) {
                        if (!cont.timer.isIdle())
                            cont.timer.stop();
                    }
                }
            };
            this.timer = new Timer(Context.SRC_STAT_TIMER, this.fsm);
            this.state = STATE_IDLE;
            this.fsm.start();
        }

        public void stop() throws ErrnoException {
            if (this.nsocks > 0)
                return;
            this.ctx.enter();
            this.fsm.stop();
            this.ctx.leave();
            this.pool.dispose();
        }

        public void term() {
            try {
                this.stop();
            } catch (ErrnoException ex) { }
        }

        public int createSocket(int domain, int protocol)
                throws ErrnoException {
            if (domain != AF_SP && domain != AF_SP_RAW)
                throw new ErrnoException(Global.EAFNOSUPPORT);
            if (this.nsocks >= MAX_SOCKETS)
                throw new ErrnoException(Global.EMFILE);
            int s = this.unused[MAX_SOCKETS - this.nsocks - 1];
            for (SockType stype: this.socktypes)
                if (stype.domain() == domain && stype.protocol() == protocol) {
                    Sock sock = new Sock(stype);
                    this.socks[s] = sock;
                    this.nsocks++;
                    return s;
                }
            throw new ErrnoException(Global.EINVAL);
        }

        public void closeSocket(int s) throws ErrnoException {
            this.socks[s].dispose();
            this.socks[s] = null;
            this.unused[MAX_SOCKETS - this.nsocks] = s;
            this.nsocks--;
        }

        public void createEndpoint(int s, String addr, boolean bind)
                throws ErrnoException {
            if (addr == null)
                throw new ErrnoException(Global.EINVAL);
            Matcher match = Context.PSEUDO_URI_PARSER.matcher(addr);
            if (!match.matches())
                throw new ErrnoException(Global.EINVAL);
            String proto = match.group(1);
            addr = match.group(2);
            for (Transport trans: this.transports) {
                if (!trans.name().equals(proto))
                    continue;
                this.socks[s].addEndpoint(trans, bind, addr);
                return;
            }
            throw new ErrnoException(Global.EPROTONOSUPPORT);
        }

        public Sock sock(int s) {
            return this.socks[s];
        }

        public Transport transport(int id) {
            for (Transport trans: this.transports)
                if (trans.id() == id)
                    return trans;
            return null;
        }

        public Pool pool() {
            return this.pool;
        }

        public static Context instance() {
            if (Context.instance == null)
                Context.instance = new Context();
            return Context.instance;
        }

        private void submitStatistics() throws ErrnoException {
            for (int i = 0; i < MAX_SOCKETS; i++) {
                Sock s = this.socks[i];
                if (s == null)
                    continue;
                s.ctx().enter();
                this.submitCounter(i, s, "established_connections",
                        s.statistics.established_connections);
                this.submitCounter(i, s, "accepted_connections",
                        s.statistics.accepted_connections);
                this.submitCounter(i, s, "dropped_connections",
                        s.statistics.dropped_connections);
                this.submitCounter(i, s, "broken_connections",
                        s.statistics.broken_connections);
                this.submitCounter(i, s, "connect_errors",
                        s.statistics.connect_errors);
                this.submitCounter(i, s, "bind_errors",
                        s.statistics.bind_errors);
                this.submitCounter(i, s, "accept_errors",
                        s.statistics.accept_errors);
                this.submitCounter(i, s, "messages_sent",
                        s.statistics.messages_sent);
                this.submitCounter(i, s, "messages_received",
                        s.statistics.messages_received);
                this.submitCounter(i, s, "bytes_sent",
                        s.statistics.bytes_sent);
                this.submitCounter(i, s, "bytes_received",
                        s.statistics.bytes_received);
                this.submitLevel(i, s, "current_connections",
                        s.statistics.current_connections);
                this.submitLevel(i, s, "inprogress_connections",
                        s.statistics.inprogress_connections);
                this.submitLevel(i, s, "current_snd_priority",
                        s.statistics.current_snd_priority);
                this.submitErrors (i, s, "current_ep_errors",
                        s.statistics.current_ep_errors);
                s.ctx().leave();
            }
        }

        private void submitCounter(int i, Sock s, String name, int value) {
            if (this.printStatistics)
                System.err.printf("msgs: socket: %s: %d\n", name, value);
        }

        private void submitLevel(int i, Sock s, String name, int value) {
            if (this.printStatistics)
                System.err.printf("msgs: socket: %s: %d\n", name, value);
        }

        private void submitErrors(int i, Sock s, String name, int value) {
        }
    }
}
