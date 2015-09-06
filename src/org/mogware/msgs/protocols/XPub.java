package org.mogware.msgs.protocols;

import org.mogware.msgs.aio.Ctx;
import org.mogware.msgs.core.Global;
import org.mogware.msgs.core.PipeBase;
import org.mogware.msgs.core.Sock;
import org.mogware.msgs.protocols.utils.Dist;
import org.mogware.msgs.utils.ErrnoException;
import org.mogware.msgs.utils.Msg;

public class XPub implements SockType {
    @Override
    public int domain() {
        return Global.AF_SP_RAW;
    }

    @Override
    public int protocol() {
        return Global.PUB;
    }

    @Override
    public int flags() {
        return SockType.FLAG_NORECV;
    }

    @Override
    public SockBase create(Object hint) throws ErrnoException {
        return new XPubImpl(hint);
    }

    @Override
    public boolean isPeer(int socktype) {
        return socktype == Global.SUB;
    }

    public static class XPubImpl implements SockBase {
        private final Sock sock;
        private final Dist outPipes;

        public XPubImpl(Object hint) {
            this.sock = (Sock)hint;
            this.outPipes = new Dist();
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
            return this.sock.optInner(Global.SOL_SOCKET, option);
        }

        @Override
        public void statIncrement(int name, int increment) {
            this.sock.statIncrement(name, increment);
        }

        @Override
        public void add(PipeBase pipe) throws ErrnoException {
            Data data = new Data();
            this.outPipes.add(data.item, pipe);
            pipe.setData(data);
        }

        @Override
        public void remove(PipeBase pipe) {
            Data data = (Data)pipe.getData();
            this.outPipes.remove(data.item);
        }

        @Override
        public void in(PipeBase pipe) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public void out(PipeBase pipe) {
            Data data = (Data)pipe.getData();
            this.outPipes.out(data.item);
        }

        @Override
        public int events() {
            return SockBase.EVENT_OUT;
        }

        @Override
        public void send(Msg msg) throws ErrnoException {
            this.outPipes.send(msg);
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
            private final Dist.Data item;

            private Data() {
                this.item = new Dist.Data();
            }
        }
    }
}
