package cleaningRobot;

import cleaningRobot.simulator.*;
import com.google.gson.Gson;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import utils.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class AirPollutionSender extends Thread implements AirPollutionSenderMQTT {
    private volatile boolean stopCondition = false;
    int id;
    int district;
    MqttClient client;
    String broker = "tcp://localhost:1883";
    String clientId = MqttClient.generateClientId();
    String topic = "greenfield/pollution/district";
    int qos = 2;
    MqttConnectOptions connOpts = new MqttConnectOptions();
    Simulator simulator;
    private ArrayList<Double> means = new ArrayList<>();

    private final BufferImpl buffer = new BufferImpl(8, 0.5);

    public AirPollutionSender(int id, int district) {
        this.id = id;
        this.district = district;
        connOpts.setCleanSession(true);
        this.registertoMQTT();
        simulator = new PM10Simulator(buffer);
        simulator.start();
    }

    @Override
    public void run() {
        super.run();
        new Timer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {if (!stopCondition) sendAirPollution();}
                },
                0,
                15000
        );
        while(!stopCondition) {
            try {
                if (buffer.getMeasurements().size()<buffer.getBufferSize()) synchronized(simulator.getBuffer()) {simulator.getBuffer().wait();}
                synchronized(means) {
                    List<Measurement> clone = simulator.getBuffer().readAllAndClean();
                    means.add(
                            clone.stream()
                                    .map(Measurement::getValue)
                                    .reduce(0.0, Double::sum) / buffer.getBufferSize()
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void stopThread() {
            stopCondition = true;
            simulator.stopMeGently();
            //simulator.interrupt();
    }

    public synchronized void restartThread() {
        stopCondition = false;
        simulator = new PM10Simulator(buffer);
        simulator.start();
    }


    @Override
    public void registertoMQTT() {
        try {
            client = new MqttClient(broker, clientId, new MqttDefaultFilePersistence("/tmp"));
            client.connect(connOpts);
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println(clientId + " Connectionlost! cause:" + cause.getMessage());

                }
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // Not used Here
                }
                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Until the delivery is completed, messages with QoS 1 or 2 are retained from the client
                    // Delivery for a message is completed when all acknowledgments have been received
                    // When the callback returns from deliveryComplete to the main thread, the client removes the retained messages with QoS 1 or 2.
                    if (token.isComplete()) {
                        means.clear();
                        //System.out.println(clientId + " Message delivered - Thread PID: " + Thread.currentThread().getId());
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void sendAirPollution() {
        synchronized (means) {
            if (means.size() == 0) {return;}
            try {
                if (!client.isConnected()) {client.connect(connOpts);}
                MqttMessage message = new MqttMessage(
                        new Gson().toJson(
                                new Tuple.ThreeTuple<>(id, System.currentTimeMillis(), means)
                        ).getBytes()
                );
                message.setQos(qos);
                //System.out.println(clientId + " Publishing message: " + means.size() + " ...");
                client.publish(topic + district, message);
                //System.out.println(clientId + " Message published - Thread PID: " + Thread.currentThread().getId());
                //if (client.isConnected()) client.disconnect();
                //System.out.println("Publisher " + clientId + " disconnected - Thread PID: " + Thread.currentThread().getId());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
