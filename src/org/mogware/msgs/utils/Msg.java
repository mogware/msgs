package org.mogware.msgs.utils;

import java.nio.ByteBuffer;

public class Msg {
    public final ChunkRef hdrs;
    public final ChunkRef body;

    public Msg(int size) {
        hdrs = new ChunkRef(0);
        body = new ChunkRef(size);
    }

    public Msg(byte body[]) {
        hdrs = new ChunkRef(0);
        this.body = new ChunkRef(body);
    }

    public Msg(ByteBuffer body) {
        hdrs = new ChunkRef(0);
        this.body = new ChunkRef(body);
    }

    public Msg(Msg src) {
        hdrs = new ChunkRef(src.hdrs);
        body = new ChunkRef(src.body);
    }
}
