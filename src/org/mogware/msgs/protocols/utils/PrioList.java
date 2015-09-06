package org.mogware.msgs.protocols.utils;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.mogware.msgs.core.PipeBase;

public class PrioList {
    private static final int SLOTS = 16;
    private final Slot slots[];
    private int current;

    public PrioList() {
        this.slots = new Slot[PrioList.SLOTS];
        for(int i = 0; i < PrioList.SLOTS; i++)
            slots[i] = new Slot();
        current = -1;
    }

    public void add(Data data, PipeBase pipe, int priority) {
        assert(priority <= PrioList.SLOTS);
        data.pipe = pipe;
        data.priority = priority;
    }

    public void remove(Data data) {
        Slot slot = this.slots[data.priority - 1];
        if (slot.current != data)
            slot.pipes.remove(data);
        else {
            slot.advance(true);
            if (this.current != data.priority)
                return;
            while (slot.pipes.isEmpty()) {
                this.current++;
                if (this.current > PrioList.SLOTS) {
                    this.current = -1;
                    return;
                }
                slot = this.slots[this.current - 1];
            }
        }
    }

    public void activate(Data data) {
        Slot slot = this.slots[data.priority - 1];
        if(!slot.pipes.isEmpty())
            slot.pipes.add(data);
        else {
            slot.pipes.add(data);
            slot.current = data;
            if (this.current == -1 || this.current > data.priority)
                this.current = data.priority;
        }
    }

    public boolean isActive() {
        return current != -1;
    }

    public PipeBase getPipe() {
        if (this.current == -1)
            return null;
        assert(this.current > 0);
        return this.slots[this.current - 1].current.pipe;
    }

    public void advance(boolean release)
    {
        assert(this.current > 0);
        Slot slot = this.slots[this.current - 1];
        slot.advance(release);
        while(slot.pipes.isEmpty()) {
            this.current++;
            if (this.current > PrioList.SLOTS) {
                this.current = -1;
                return;
            }
            slot = this.slots[this.current - 1];
        }
    }

    public int getPriotity() {
        return this.current;
    }

    private static class Slot {
        private List<Data> pipes;
        private Data current;

        private Slot() {
            this.pipes = new LinkedList<>();
            this.current = null;
        }

        private void advance(boolean release) {
            Iterator<Data> it = this.pipes.iterator();
            while (it.hasNext()) {
                Data data = it.next();
                if (data != this.current)
                    continue;
                if (release)
                    it.remove();
                this.current = it.hasNext() ? it.next() :
                        this.pipes.isEmpty() ? null : pipes.get(0);
                break;
            }
        }
    }

    public static class Data {
        private PipeBase pipe;
        private int priority;
    }
}
