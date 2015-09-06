package org.mogware.msgs.protocols;

import org.mogware.msgs.core.Global;

public class Push extends XPush {
    @Override
    public int domain() {
        return Global.AF_SP;
    }
}
