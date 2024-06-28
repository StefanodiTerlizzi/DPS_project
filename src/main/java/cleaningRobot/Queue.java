package cleaningRobot;

import java.util.ArrayList;

public class Queue {
    public ArrayList<String> buffer = new ArrayList<>();

    public synchronized void add(String msg) {
        buffer.add(msg);
        notify();
    }

    public synchronized String get() throws InterruptedException {
        while (buffer.size()==0) {this.wait();}
        String msg = buffer.get(0);
        buffer.remove(0);
        return msg;
    }
}