package admServer;

import admServer.Model.AirSing;
import cleaningRobot.CleaningRobot;
import cleaningRobot.Position;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.net.httpserver.HttpServer;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import utils.Tuple;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Scanner;

public class AdmServer {
    private static final String HOST = "localhost";
    private static final int PORT = 2001;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServerFactory.create("http://"+HOST+":"+PORT+"/");
        server.start();
        System.out.println("Server started on: http://"+HOST+":"+PORT);
        startSubriber();
    }

    private static void startSubriber() {
        MqttClient client;
        String broker = "tcp://localhost:1883";
        String clientId = "pahoAdmServer01";
        String topicArray  [] = new String[] {
                "greenfield/pollution/district1",
                "greenfield/pollution/district2",
                "greenfield/pollution/district3",
                "greenfield/pollution/district4"
        };
        int qosArray [] = new int[] {2,2,2,2};

        try {
            client = new MqttClient(broker, clientId, new MqttDefaultFilePersistence("/tmp"));
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(false);

            // Connect the client
            System.out.println(clientId + " Connecting Broker " + broker);
            client.connect(connOpts);
            System.out.println(clientId + " Connected - Thread PID: " + Thread.currentThread().getId());

            // Callback
            client.setCallback(new MqttCallback() {
                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    // Called when a message arrives from the server that matches any subscription made by the client
                    //String time = new Timestamp(System.currentTimeMillis()).toString();
                    String receivedMessage = new String(message.getPayload());
                    int district = Integer.parseInt(topic.substring(topic.length() - 1));
                    Tuple.ThreeTuple<Integer, Long, ArrayList<Double>> parsed = new Gson().fromJson(
                            receivedMessage,
                            new TypeToken<Tuple.ThreeTuple<Integer, Long, ArrayList<Double>>>() {}.getType()
                    );
                    switch(district) {
                        case 1:
                            AirSing.getInstance().addToDistrict1(parsed);
                            break;
                        case 2:
                            AirSing.getInstance().addToDistrict2(parsed);
                            break;
                        case 3:
                            AirSing.getInstance().addToDistrict3(parsed);
                            break;
                        case 4:
                            AirSing.getInstance().addToDistrict4(parsed);
                            break;
                    }
                }
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println(clientId + " Connectionlost! cause:" + cause.getMessage()+ "-  Thread PID: " + Thread.currentThread().getId());
                }
                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {}

            });
            client.subscribe(topicArray,qosArray);
            System.out.println("\n ***  Press a random key to disconnect the subscriber *** \n");
            Scanner command = new Scanner(System.in);
            command.nextLine();
            client.disconnect();

        } catch (MqttException me) {
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("excep " + me);
            me.printStackTrace();
        }
    }

}
