package org.mogware.msgs.protocols;

import org.mogware.msgs.core.Global;

public class Pair extends XPair {
    @Override
    public int domain() {
        return Global.AF_SP;
    }
}
