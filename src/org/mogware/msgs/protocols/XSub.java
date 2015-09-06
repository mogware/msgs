package org.mogware.msgs.protocols;

import org.mogware.msgs.aio.Ctx;
import org.mogware.msgs.core.Global;
import org.mogware.msgs.core.PipeBase;
import org.mogware.msgs.core.Sock;
import org.mogware.msgs.protocols.utils.Fq;
import org.mogware.msgs.utils.ErrnoException;
import org.mogware.msgs.utils.Msg;

public class XSub implements SockType {
    @Override
    public int domain() {
        return Global.AF_SP_RAW;
    }

    @Override
    public int protocol() {
        return Global.SUB;
    }

    @Override
    public int flags() {
        return SockType.FLAG_NOSEND;
    }

    @Override
    public SockBase create(Object hint) throws ErrnoException {
        return new XSubImpl(hint);
    }

    @Override
    public boolean isPeer(int socktype) {
        return socktype == Global.PUB;
    }

    public static class XSubImpl implements SockBase {
        private final Sock sock;
        private final Fq fq;

        public XSubImpl(Object hint) {
            this.sock = (Sock)hint;
            this.fq = new Fq();
        }

        @Override
        public void stopped() {
            this.sock.stopped();
        }

        @Override
        public Ctx ctx() {
            return this.sock.ctx();
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
            this.fq.add(data.fq, pipe, rcvprio);
            pipe.setData(data);
        }

        @Override
        public void remove(PipeBase pipe) {
            Data data = (Data)pipe.getData();
            this.fq.remove(data.fq);
        }

        @Override
        public void in(PipeBase pipe) {
            Data data = (Data)pipe.getData();
            this.fq.in(data.fq);
        }

        @Override
        public void out(PipeBase pipe) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public int events() {
            return this.fq.canRecv() ? SockBase.EVENT_IN : 0;
        }

        @Override
        public void send(Msg msg) throws ErrnoException {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public Msg recv() throws ErrnoException {
            return this.fq.recv();
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
            private final Fq.Data fq;

            private Data() {
                this.fq = new Fq.Data();
            }
        }
    }
}
