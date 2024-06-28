package AdmClient;

import cleaningRobot.CleaningRobot;
import cleaningRobot.Position;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import utils.Tuple;
import utils.Utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Scanner;

public class AdmClient {

    private final static String PATH = "/admClientServices";
    private final static String SERVER_ADDRESS = "http://localhost:2001";

    public static void main(String[] args) {
        while (true) {
            System.out.printf("\ncommands: (insert the number)\n" +
                    "1. The list of the cleaning robots currently located in Greenfield\n" +
                    "2. The average of the last N air pollution levels sent to the server by a given robot R\n" +
                    "3. The average of the air pollution levels sent by all the robots to the server and occurred from timestamps t1 and t2\n" +
                    "4. exit\n"
            );
            try {
                String command = new BufferedReader(new InputStreamReader(System.in)).readLine();
                if (command.equals("1")) listofCleaningRobots();
                if (command.equals("2")) {
                    System.out.print("insert N (n of air pollution measumerents): ");
                    String N = new BufferedReader(new InputStreamReader(System.in)).readLine();
                    System.out.print("insert R (robot id): ");
                    String R = new BufferedReader(new InputStreamReader(System.in)).readLine();
                    avglastNmeasurementsfromRobotR(N, R);
                }
                if (command.equals("3")) {
                    System.out.print("insert start timestamp: ");
                    String A = new BufferedReader(new InputStreamReader(System.in)).readLine();
                    System.out.print("insert end timestamp: ");
                    String B = new BufferedReader(new InputStreamReader(System.in)).readLine();
                    avgfromAtoB(A, B);
                }
                if (command.equals("4")) break;
            } catch (Exception e) {}
        }
    }

    private synchronized static void avgfromAtoB(String start, String end) {
        //System.out.printf("get measuments from %s to %s\n", A, B);
        ClientResponse clientResponse = Utils.postRequest(
                Client.create(),
                SERVER_ADDRESS+PATH+"/avgFromt1Tot2",
                Arrays.asList(start, end)
        );
        if (clientResponse.getStatus() != 200) {
            System.err.println(clientResponse.getStatus() + " " + clientResponse.getEntity(String.class));
            return;
        }
        InputStream inputStream = clientResponse.getEntityInputStream();
        String responseBody = new Scanner(inputStream, "UTF-8").useDelimiter("\\A").next();
        Double result = new Gson().fromJson(
                responseBody,
                (Type) Double.class
        );
        System.out.println("average: "+result);
    }

    private synchronized static void avglastNmeasurementsfromRobotR(String N, String RID) {
        //System.out.printf("get %s measuments of robot %s\n", N, RID);
        ClientResponse clientResponse = Utils.postRequest(
                Client.create(),
                SERVER_ADDRESS+PATH+"/NmeasurementsFromRobotRID",
                Arrays.asList(N, RID)
                );
        if (clientResponse.getStatus() != 200) {
            System.err.println(clientResponse.getStatus() + " " + clientResponse.getEntity(String.class));
            return;
        }
        InputStream inputStream = clientResponse.getEntityInputStream();
        String responseBody = new Scanner(inputStream, "UTF-8").useDelimiter("\\A").next();
        Double result = new Gson().fromJson(
                responseBody,
                (Type) Double.class
        );
        System.out.println("average: "+result);
    }

    private synchronized static void listofCleaningRobots() {
        ClientResponse clientResponse = Utils.getRequest(Client.create(),SERVER_ADDRESS+PATH+"/robotList");
        if (clientResponse.getStatus() != 200) {
            System.err.println(clientResponse.getStatus() + " " + clientResponse.getEntity(String.class));
            return;
        }

        InputStream inputStream = clientResponse.getEntityInputStream();
        String responseBody = new Scanner(inputStream, "UTF-8").useDelimiter("\\A").next();
        ArrayList<Tuple.TwoTuple<Position, ArrayList<Integer>>> list = new Gson().fromJson(
                responseBody,
                new TypeToken<ArrayList<Tuple.TwoTuple<Position, ArrayList<Integer>>>>() {
                }.getType()
        );
        list.stream().sorted(Comparator.comparing(e -> e.one)).map(el -> String.format("%s -> robots: %s", el.one, el.two)).forEach(System.out::println);
    }


}
