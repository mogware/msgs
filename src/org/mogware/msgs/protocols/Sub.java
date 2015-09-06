package org.mogware.msgs.protocols;

import org.mogware.msgs.core.Global;

public class Sub extends XSub {
    public static final int SUBSCRIBE = 1;
    public static final int UNSUBSCRIBE = 2;

    @Override
    public int domain() {
        return Global.AF_SP;
    }
}
