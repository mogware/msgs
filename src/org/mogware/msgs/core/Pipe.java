package org.mogware.msgs.core;

public abstract class Pipe extends PipeBase {
    public static final int IN = 33987;
    public static final int OUT = 33988;

    public Pipe(EndpointBase epbase) {
        super(epbase);
    }
}
