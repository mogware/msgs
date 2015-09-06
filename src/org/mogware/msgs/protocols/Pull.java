package org.mogware.msgs.protocols;

import org.mogware.msgs.core.Global;

public class Pull extends XPull {
    @Override
    public int domain() {
        return Global.AF_SP;
    }
}
