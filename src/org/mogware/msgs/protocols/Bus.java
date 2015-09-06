package org.mogware.msgs.protocols;

import org.mogware.msgs.core.Global;

public class Bus extends XBus {
    @Override
    public int domain() {
        return Global.AF_SP;
    }
}
