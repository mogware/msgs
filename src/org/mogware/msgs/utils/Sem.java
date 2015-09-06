package org.mogware.msgs.utils;

public class Sem {
    private int value;

    public Sem(int startingValue) {
        this.value = startingValue;
    }

    public synchronized void semWait() throws InterruptedException {
        if (this.value == 0)
            wait();
        this.value--;
    }

    public synchronized void semSignal() {
        this.value++;
        notify();
    }
}
