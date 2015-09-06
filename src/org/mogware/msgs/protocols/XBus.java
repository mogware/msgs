package org.mogware.msgs.protocols;

import org.mogware.msgs.aio.Ctx;
import org.mogware.msgs.core.Global;
import org.mogware.msgs.core.PipeBase;
import org.mogware.msgs.core.Sock;
import org.mogware.msgs.protocols.utils.Dist;
import org.mogware.msgs.protocols.utils.Fq;
import org.mogware.msgs.utils.ErrnoException;
import org.mogware.msgs.utils.Msg;

public class XBus implements SockType {
    @Override
    public int domain() {
        return Global.AF_SP_RAW;
    }

    @Override
    public int protocol() {
        return Global.BUS;
    }

    @Override
    public int flags() {
        return 0;
    }

    @Override
    public SockBase create(Object hint) throws ErrnoException {
        return new XBusImpl(hint);
    }

    @Override
    public boolean isPeer(int socktype) {
        return socktype == Global.BUS;
    }

    public static class XBusImpl implements SockBase {
        private final Sock sock;
        private final Dist outPipes;
        private final Fq inPipes;


        public XBusImpl(Object hint) {
            this.sock = (Sock)hint;
            this.outPipes = new Dist();
            this.inPipes = new Fq();
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
            return this.sock.optInner(1, option);
        }

        @Override
        public void statIncrement(int name, int increment) {
            this.sock.statIncrement(name, increment);
        }

        @Override
        public void add(PipeBase pipe) throws ErrnoException {
            int rcvprio = (Integer)pipe.opt(Global.SOL_SOCKET, Global.RCVBUF);
            assert(rcvprio >= 1 && rcvprio <= 16);
            Data data = new Data();
            this.inPipes.add(data.initem, pipe, rcvprio);
            this.outPipes.add(data.outitem, pipe);
            pipe.setData(data);
        }

        @Override
        public void remove(PipeBase pipe) {
            Data data = (Data)pipe.getData();
            this.inPipes.remove(data.initem);
            this.outPipes.remove(data.outitem);
        }

        @Override
        public void in(PipeBase pipe) {
            Data data = (Data)pipe.getData();
            this.inPipes.in(data.initem);
        }

        @Override
        public void out(PipeBase pipe) {
            Data data = (Data)pipe.getData();
            this.outPipes.out(data.outitem);
        }

        @Override
        public int events() {
            return (this.inPipes.canRecv() ? SockBase.EVENT_IN : 0) |
                    SockBase.EVENT_OUT;
        }

        @Override
        public void send(Msg msg) throws ErrnoException {
            this.outPipes.send(msg);
        }

        @Override
        public Msg recv() throws ErrnoException {
            return this.inPipes.recv();
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
            private final Dist.Data outitem;
            private final Fq.Data initem;

            private Data() {
                this.outitem = new Dist.Data();
                this.initem = new Fq.Data();
            }
        }
    }
}
