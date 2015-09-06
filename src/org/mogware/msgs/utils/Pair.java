package org.mogware.msgs.utils;

public final class Pair<A,B> {
    private final A val0;
    private final B val1;

    public Pair(final A val0, final B val1) {
        this.val0 = val0;
        this.val1 = val1;
    }

    public A val0() {
        return this.val0;
    }

    public B val1() {
        return this.val1;
    }

    public static <A,B> Pair<A,B> with(final A val0, final B val1) {
        return new Pair<>(val0, val1);
    }
}
