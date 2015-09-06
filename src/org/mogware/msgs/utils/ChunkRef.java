package org.mogware.msgs.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ChunkRef {
    private static final ByteOrder byteorder = ByteOrder.BIG_ENDIAN;
    private final ByteBuffer ref;

    public ChunkRef(int size) {
        this.ref = ByteBuffer.wrap(new byte[size]).order(ChunkRef.byteorder);
    }

    public ChunkRef(byte[] chunk) {
        this.ref = ByteBuffer.wrap(chunk).order(ChunkRef.byteorder);
    }

    public ChunkRef(final ByteBuffer chunk) {
        this.ref = chunk != null ? chunk.duplicate() : null;
    }

    public ChunkRef(final ChunkRef chunk) {
        this.ref = chunk.ref != null ? chunk.ref.duplicate() : null;
    }

    public int size() {
        return this.ref.remaining();
    }

    public byte[] bytes() {
        int length = this.ref.remaining();
        byte[] bytes = new byte[length];
        this.ref.duplicate().get(bytes);
        return bytes;
    }

    public ByteBuffer data() {
        return this.ref.duplicate();
    }

    public ByteBuffer ref() {
        return this.ref;
    }
}
