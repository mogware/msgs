package org.mogware.msgs.protocols;

import org.mogware.msgs.core.Global;

public class Pub extends XPub {
    @Override
    public int domain() {
        return Global.AF_SP;
    }
}
