package org.mogware.msgs;

import org.mogware.msgs.core.Global;

public class Socket {
    private final int socket;

    public Socket(final int domain, final int protocol) {
        this.socket = Global.socket(domain, protocol);
    }

    public void close() throws IOException {
        if (Global.close(this.socket) < 0)
            throw new IOException(Errmsg.error(), Errmsg.errno());
    }

    public void bind(final String addr) throws IOException {
        if (Global.bind(this.socket, addr) < 0)
            throw new IOException(Errmsg.error(), Errmsg.errno());
    }

    public void connect(final String addr) throws IOException {
        if (Global.connect(this.socket, addr) < 0)
            throw new IOException(Errmsg.error(), Errmsg.errno());
    }

    public int sendString(final String data, final boolean blocking)
            throws IOException {
        return 0;
    }

    public int sendString(final String data) throws IOException {
        return this.sendString(data, true);
    }

    public int sendBytes(final byte[] data, final boolean blocking)
            throws IOException {
        return 0;
    }

    public int sendBytes(final byte[] data) throws IOException {
        return this.sendBytes(data, true);
    }

    public String recvString(final boolean blocking) throws IOException {
        return null;
    }

    public String recvString() throws IOException {
        return this.recvString(true);
    }

    public byte[] recvBytes(boolean blocking) throws IOException {
        return null;
    }

    public byte[] recvBytes() throws IOException {
        return this.recvBytes(true);
    }

    public int send(final Message msg) throws IOException {
        return sendBytes(msg.toBytes());
    }

    public int send(final Message msg, boolean blocking) throws IOException {
        return this.sendBytes(msg.toBytes(), blocking);
    }

    public Message recv() throws IOException {
        return new Message(this.recvBytes());
    }

    public Message recv(final boolean blocking) throws IOException {
        return new Message(this.recvBytes(blocking));
    }
}
