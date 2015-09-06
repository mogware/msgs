package org.mogware.msgs.protocols;

import org.mogware.msgs.aio.Ctx;
import org.mogware.msgs.core.Global;
import org.mogware.msgs.core.PipeBase;
import org.mogware.msgs.core.Sock;
import org.mogware.msgs.protocols.utils.Excl;
import org.mogware.msgs.utils.ErrnoException;
import org.mogware.msgs.utils.Msg;

public class XPair implements SockType {
    public int domain() {
        return Global.AF_SP_RAW;
    }

    public int protocol() {
        return Global.PAIR;
    }

    public int flags() {
        return 0;
    }

    public SockBase create(Object hint) throws ErrnoException {
        return new XPairImpl(hint);
    }

    public boolean isPeer(int socktype) {
        return socktype == Global.PAIR;
    }

    public static class XPairImpl implements SockBase {
        private final Sock sock;
        private final Excl excl;

        public XPairImpl(Object hint) {
            this.sock = (Sock)hint;
            this.excl = new Excl();
        }

        public void stopped() {
            this.sock.stopped();
        }

        public Ctx ctx() {
            return this.sock.ctx();
        }

        public Object opt(int option) throws ErrnoException {
            return this.sock.optInner(Global.SOL_SOCKET, option);
        }

        public void statIncrement(int name, int increment) {
            this.sock.statIncrement(name, increment);
        }

        public void add(PipeBase pipe) throws ErrnoException {
            this.excl.add(pipe);
        }

        public void remove(PipeBase pipe) {
            this.excl.remove(pipe);
        }

        public void in(PipeBase pipe) {
            this.excl.in(pipe);
        }

        public void out(PipeBase pipe) {
            this.excl.out(pipe);
        }

        public int events() {
            int events = 0;
            if (this.excl.canRecv())
                events |= SockBase.EVENT_IN;
            if (this.excl.canSend())
                events |= SockBase.EVENT_OUT;
            return events;
        }

        public void send(Msg msg) throws ErrnoException {
            this.excl.send(msg);
        }

        public Msg recv() throws ErrnoException {
            return this.excl.recv();
        }

        public void opt(int level, int option, Object val)
                throws ErrnoException {
            throw new ErrnoException(Global.ENOPROTOOPT);
        }

        public Object opt(int level, int option)
            throws ErrnoException {
            throw new ErrnoException(Global.ENOPROTOOPT);
        }

        public boolean stop() {
            return false;
        }
    }
}
