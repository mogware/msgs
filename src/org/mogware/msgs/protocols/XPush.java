package org.mogware.msgs.protocols;

import org.mogware.msgs.aio.Ctx;
import org.mogware.msgs.core.Global;
import org.mogware.msgs.core.PipeBase;
import org.mogware.msgs.core.Sock;
import org.mogware.msgs.protocols.utils.Lb;
import org.mogware.msgs.utils.ErrnoException;
import org.mogware.msgs.utils.Msg;

public class XPush implements SockType {
    @Override
    public int domain() {
        return Global.AF_SP_RAW;
    }

    @Override
    public int protocol() {
        return Global.PUSH;
    }

    @Override
    public int flags() {
        return SockType.FLAG_NORECV;
    }

    @Override
    public SockBase create(Object hint)
        throws ErrnoException {
        return new XPushImpl(hint);
    }

    @Override
    public boolean isPeer(int socktype) {
        return socktype == Global.PULL;
    }

    public static class XPushImpl implements SockBase {
        private final Sock sock;
        private final Lb lb;

        public XPushImpl(Object hint) {
            this.sock = (Sock)hint;
            this.lb = new Lb();
        }

        @Override
        public void stopped() {
            sock.stopped();
        }

        @Override
        public Ctx ctx() {
            return sock.ctx();
        }

        @Override
        public Object opt(int option) throws ErrnoException {
            return sock.optInner(Global.SOL_SOCKET, option);
        }

        @Override
        public void statIncrement(int name, int increment) {
            sock.statIncrement(name, increment);
        }

        @Override
        public void add(PipeBase pipe) throws ErrnoException {
            int rcvprio = (Integer)pipe.opt(Global.SOL_SOCKET, Global.RCVPRIO);
            assert(rcvprio >= 1 && rcvprio <= 16);
            Data data = new Data();
            this.lb.add(data.lb, pipe, rcvprio);
            pipe.setData(data);
        }

        @Override
        public void remove(PipeBase pipe) {
            Data data = (Data)pipe.getData();
            this.lb.remove(data.lb);
        }

        @Override
        public void in(PipeBase pipe) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public void out(PipeBase pipe) {
            Data data = (Data)pipe.getData();
            this.lb.out(data.lb);
            statIncrement(401, this.lb.getPriority());
        }

        @Override
        public int events() {
            return this.lb.canSend() ? SockBase.EVENT_OUT : 0;
        }

        @Override
        public void send(Msg msg) throws ErrnoException {
            this.lb.send(msg);
        }

        @Override
        public Msg recv() throws ErrnoException {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public void opt(int level, int option, Object val)
            throws ErrnoException {
            throw new ErrnoException(Global.ENOPROTOOPT);
        }

        @Override
        public Object opt(int level, int option)
            throws ErrnoException {
            throw new ErrnoException(Global.ENOPROTOOPT);
        }

        @Override
        public boolean stop() {
            return false;
        }

        private static class Data {
            private final Lb.Data lb;

            private Data() {
                this.lb = new Lb.Data();
            }
        }
    }
}
