package cleaningRobot.simulator;

import java.util.ArrayList;
import java.util.List;

public class BufferImpl implements Buffer {
    private int bufferSize;
    private double overlap;
    private ArrayList<Measurement> measurements = new ArrayList<>();

    public BufferImpl(int bufferSize, double overlap) {
        this.bufferSize = bufferSize;
        this.overlap = overlap;
    }

    public ArrayList<Measurement> getMeasurements() {return measurements;}

    public int getBufferSize() {return bufferSize;}

    public double getOverlap() {return overlap;}

    @Override
    public synchronized void addMeasurement(Measurement m) {
        measurements.add(m);
        if (measurements.size()>=bufferSize) notify();
    }
    @Override
    public synchronized List<Measurement> readAllAndClean() {
        List<Measurement> clone = new ArrayList<>(measurements.subList(0, bufferSize));
        measurements.subList(0, (int)(bufferSize*overlap)).clear();
        measurements.clear();
        return clone;
    }

}
