package cleaningRobot;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import utils.Tuple;
import utils.Utils;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.*;
import java.text.DateFormat;
import java.util.*;
import java.util.stream.Collectors;

@XmlRootElement
public class CleaningRobot extends CleaningRobotServerGrpc.CleaningRobotServerImplBase {
    private int id;
    private int listeningPort;
    private String localAddress = "localhost";
    private transient String serverAddress;
    private Position position;
    private transient List<CleaningRobot> others;
    private transient AirPollutionSender airPollutionSender;
    private transient Boolean inDamage = false;
    private transient List<Integer> responseForHandlingMyMaintenance = new ArrayList<>();
    private transient Integer canEnterToMech = 0;
    private transient long time;

    private transient final Integer inDamageLock = 0;

    public static void main(String[] args) throws IOException {
        System.out.print("insert id: ");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        int id = Integer.parseInt(br.readLine());
        new CleaningRobot(id, 3000+id, "http://localhost:2001");
    }
    public CleaningRobot(){}

    //only for conversion from CleaningRobotServerOuterClass.Robot to CleaningRobot
    public CleaningRobot(int id, int listeningPort, String localAddress, Position position) {
        this.id = id;
        this.listeningPort = listeningPort;
        this.localAddress = localAddress;
        this.position = position;
    }

    public CleaningRobot(int id, int listeningPort, String serverAddress) {
        this.id = id;
        this.listeningPort = listeningPort;
        this.serverAddress = serverAddress;
        utils.Tuple.TwoTuple<Position, ArrayList<CleaningRobot>> response;
        try {
           response = this.registerToAdmServer();
           position = response.one;
           others = response.two;
        } catch (Exception e) {
            System.err.printf("error: %s\nrobotID: %s\n",e.getLocalizedMessage(), id);
            return;
        }
        Server server = null;
        try {
            server = ServerBuilder.forPort(listeningPort).addService(this).build();
            server.start();
        } catch (Exception e) {
            System.err.println("cannot start the server functions of cleaning robot with id: "+id);
            e.printStackTrace();
        }
        airPollutionSender = new AirPollutionSender(this.id, getDistrictFromPosition(position));
        airPollutionSender.start();
        if (others.size()>0) this.presentToOthers();

        new Thread(() -> {
            while (true) {
                try {
                    String command = new BufferedReader(new InputStreamReader(System.in)).readLine();
                    if (command.equals("quit")) exitInControlledWay();
                    if (command.equals("fix")) handleDamage();
                } catch (Exception e) {}
            }
        }).start();

        new Timer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        synchronized (inDamageLock) {
                            try {
                                if (inDamage) {inDamageLock.wait();}
                                if (new Random().nextDouble()<=0.10) {handleDamage();}
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                },
                0,
                10000
        );
        printNetwork();


        try {
            server.awaitTermination();
        }catch (Exception e) {
            System.err.println("cannot stop the server functions of cleaning robot with id: "+id);
            e.printStackTrace();
        }

    }

    private void exitInControlledWay() throws InterruptedException {
        //complete any operation at the mechanic
        synchronized (inDamageLock) {if (inDamage) inDamageLock.wait();}
        //notify the other robots of Greenfield
        //request the Administrator Server to leave Greenfield
        notifyUpdatedNetwork(Arrays.asList(id), false);
        airPollutionSender.stopThread();
        airPollutionSender.interrupt();
    }

    private void handleDamage() {
        time = System.currentTimeMillis();
        synchronized (inDamageLock) {
            inDamage = true;
            System.out.println("damage "+ DateFormat.getTimeInstance().format(time));
            airPollutionSender.stopThread(); //stop sending and generation of pollution Measurement
        }
        // distributed mutual Exclusion
        System.out.println("request to go to mech to "+others.stream().map(e -> e.getId()).collect(Collectors.toList()));
        for (CleaningRobot r:others) {
            final ManagedChannel channel = ManagedChannelBuilder.forAddress(r.getLocalAddress(), r.getListeningPort()).usePlaintext().build();
            CleaningRobotServerGrpc.CleaningRobotServerStub stub = CleaningRobotServerGrpc.newStub(channel);
            CleaningRobotServerOuterClass.IdTimestamp request = CleaningRobotServerOuterClass.IdTimestamp.newBuilder()
                    .setTimestamp(time)
                    .setId(id)
                    .build();
            stub.handleMaintenance(request, new StreamObserver<CleaningRobotServerOuterClass.Response>() {
                @Override
                public void onNext(CleaningRobotServerOuterClass.Response value) {
                    //System.out.printf("response from robot %s: %s\n",r.id, value);
                    responseForHandlingMyMaintenance.add(r.id);
                    synchronized (canEnterToMech) {if (responseForHandlingMyMaintenance.size() == others.size()) canEnterToMech.notify();}
                }
                @Override
                public void onError(Throwable t) {
                    System.out.printf("robot %s error on try to contact him, need to delete from network\n", r.id);
                    notifyUpdatedNetwork(Arrays.asList(r.id), true);
                    synchronized (canEnterToMech) {if (responseForHandlingMyMaintenance.size() == others.size()) canEnterToMech.notify();}
                    channel.shutdown();
                }
                @Override
                public void onCompleted() {channel.shutdown();}
            });
        }
        //wait for all responses
        if (others.size() > 0) {synchronized (canEnterToMech) {
            try {
                canEnterToMech.wait();
            } catch (Exception e) {}
        }}
        System.out.println("enter in mech");
        responseForHandlingMyMaintenance.clear();
        try {Thread.sleep(10*1000);} catch (InterruptedException e) {} //simulate damage management
        synchronized (inDamageLock) {
            System.out.println("exit from damage");
            airPollutionSender.restartThread();
            inDamage = false;
            inDamageLock.notifyAll(); //restart: damage probability generator, exit in controlled way, response for other want to enter in mech
        }
    }

    public utils.Tuple.TwoTuple<Position, ArrayList<CleaningRobot>> registerToAdmServer() throws Exception {
        String postPath = "/cleaningRobotManagement/add";
        ClientResponse clientResponse = Utils.postRequest(Client.create(),serverAddress+postPath,this);

        assert clientResponse != null;
        if (clientResponse.getStatus() != 200) throw new Exception(clientResponse.getStatus()+" "+clientResponse.getEntity(String.class));

        InputStream inputStream = clientResponse.getEntityInputStream();
        String responseBody = new Scanner(inputStream, "UTF-8").useDelimiter("\\A").next();
        return new Gson().fromJson(
                responseBody,
                new TypeToken<Tuple.TwoTuple<Position, ArrayList<CleaningRobot>>>() {}.getType()
        );
    }

    public void presentToOthers() {
        List<Integer> toDeleteFromNetwork = new ArrayList<>();
        for (CleaningRobot r:others) {
            //System.out.println("present to "+r.getId());
            final ManagedChannel channel = ManagedChannelBuilder.forAddress(r.getLocalAddress(), r.getListeningPort()).usePlaintext().build();
            CleaningRobotServerGrpc.CleaningRobotServerBlockingStub stub = CleaningRobotServerGrpc.newBlockingStub(channel);
            CleaningRobotServerOuterClass.Robot request = CleaningRobotServerOuterClass.Robot.newBuilder()
                    .setId(id)
                    .setListeningPort(listeningPort)
                    .setLocalAddress(localAddress)
                    .setPositionRow(position.getRow())
                    .setPositionCol(position.getCol())
                    .build();
            CleaningRobotServerOuterClass.Response response = CleaningRobotServerOuterClass.Response.newBuilder().setOk(false).build();
            try {
                response = stub.presentRobot(request);
            } catch (Exception e) {}
            //System.out.println("result from presentation to robot "+r.id+": "+response.getOk());
            if (!response.getOk()) toDeleteFromNetwork.add(r.getId());
            channel.shutdown();
        }
        if (toDeleteFromNetwork.size()!=0) {
            notifyUpdatedNetwork(toDeleteFromNetwork, true);
        }
    }
    public synchronized Boolean updateOthers(CleaningRobot r) {
        others = others.stream().filter(e -> e.getId() != r.getId()).collect(Collectors.toList());
        return others.add(r);
    }
    public int getDistrictFromPosition(Position position) {
        return Arrays.asList(1, 2, 3, 4).stream()
                .filter(e -> (position.getRow()<=4) ? e<=2 : e>2)
                .filter(e -> (position.getCol()<=4) ? (e==1||e==4) : (e==2||e==3))
                .collect(Collectors.toList())
                .get(0);
    }
    @Override
    public String toString() {
        return "{" +
                "id=" + id +
                ", listeningPort=" + listeningPort +
                ", localAddress='" + localAddress + '\'' +
                ", position=" + position +
                '}';
    }

    public void printNetwork() {
        System.out.printf("network %s %s\n", id, others.stream().map(e -> e.id).collect(Collectors.toList()));
        //if (inDamage) System.out.printf("reponse for hendling maintencance %s over total others %s\n", responseForHandlingMyMaintenance.size(), others.size());
    }

    /* ------------------------------------------------ NOTIFY OTHERS OF CHANGE IN THE NETWORK ------------------------------------------ */
    private void notifyUpdatedNetwork(List<Integer> toDeleteFromNetwork, Boolean removeFromMyList) {
        if (removeFromMyList) others = others.stream().filter(e -> !toDeleteFromNetwork.contains(e.getId())).collect(Collectors.toList());
        printNetwork();
        //notify Server
        String postPath = "/cleaningRobotManagement/updateNetwork";
        ClientResponse clientResponse = Utils.postRequest(Client.create(),serverAddress+postPath,toDeleteFromNetwork);
        List<Integer> unreachables = new ArrayList<>();
        assert clientResponse != null;
        if (clientResponse.getStatus() != 200) {
            System.err.println("error update network in AdmServer, "+clientResponse.getStatus());
        }
        //notify Robots
        for (CleaningRobot r:others) {
            final ManagedChannel channel = ManagedChannelBuilder.forAddress(r.getLocalAddress(), r.getListeningPort()).usePlaintext().build();
            CleaningRobotServerGrpc.CleaningRobotServerBlockingStub stub = CleaningRobotServerGrpc.newBlockingStub(channel);
            CleaningRobotServerOuterClass.listOfUnreachableRobots message = CleaningRobotServerOuterClass.listOfUnreachableRobots.newBuilder()
                    .addAllId(toDeleteFromNetwork)
                    .build();
            CleaningRobotServerOuterClass.Response response = CleaningRobotServerOuterClass.Response.newBuilder().setOk(false).build();
            try {
                response = stub.updateNetwork(message);
            } catch (Exception e) {}
            //System.out.println("result from updateNetwork to robot "+r.id+": "+response.getOk());
            if (!response.getOk()) {unreachables.add(r.getId());}
            channel.shutdown();
        }
        if (unreachables.size()!=0) {
            notifyUpdatedNetwork(unreachables, true);
        } else {
            printNetwork();
            //System.out.println("finish updated network from robot: "+id+", "+others.stream().map(e -> e.getId()).collect(Collectors.toList()));
        }
    }
    /* ------------------------------------------------ GETTERS ------------------------------------------ */
    public int getId() {return id;}
    public int getListeningPort() {return listeningPort;}
    public String getLocalAddress() {
        return localAddress;
    }
    public String getServerAddress() {
        return serverAddress;
    }
    public Position getPosition() {return this.position;}
    public List<CleaningRobot> getOthers() {return others;}
    /* ------------------------------------------------ SETTERS ------------------------------------------ */
    public void setId(int id) {this.id = id;}
    public void setListeningPort(int listeningPort) {this.listeningPort = listeningPort;}
    public void setLocalAddress(String localAddress) {this.localAddress = localAddress;}
    public void setPosition(Position position) {this.position = position;}
    public void setServerAddress(String serverAddress) {this.serverAddress = serverAddress;}
    public void setOthers(List<CleaningRobot> others) {this.others = others;}
    /* ------------------------------------------------ SERVER FUNCTIONS ------------------------------------------ */
    @Override
    public void presentRobot(CleaningRobotServerOuterClass.Robot request, StreamObserver<CleaningRobotServerOuterClass.Response> responseObserver) {
        CleaningRobot robot = new CleaningRobot(
                request.getId(),
                request.getListeningPort(),
                request.getLocalAddress(),
                new Position(request.getPositionRow(), request.getPositionCol())
        );
        Boolean ok = updateOthers(robot);
        CleaningRobotServerOuterClass.Response r = CleaningRobotServerOuterClass.Response.newBuilder().setOk(ok).build();
        if (ok && inDamage) {
            responseForHandlingMyMaintenance.add(robot.id);
            //System.out.printf("response from: %s: ok\n", robot.id);
        }
        synchronized (canEnterToMech) {if (responseForHandlingMyMaintenance.size() == others.size()) canEnterToMech.notify();}
        responseObserver.onNext(r);
        printNetwork();
        responseObserver.onCompleted();
    }
    @Override
    public void handleMaintenance(CleaningRobotServerOuterClass.IdTimestamp request, StreamObserver<CleaningRobotServerOuterClass.Response> responseObserver) {
        synchronized (inDamageLock) {
            try {
                if (inDamage && time<request.getTimestamp()) {
                    //System.out.printf("wait to responde to %s, my timestamp is before\n", request.getId());
                    inDamageLock.wait();
                }
            } catch (Exception e) {}
        }
        try {Thread.sleep(2*1000);} catch (InterruptedException e) {} //simulate damage management
        //System.out.printf("reponse for handling maintenance to %s\n", request.getId());
        CleaningRobotServerOuterClass.Response r = CleaningRobotServerOuterClass.Response.newBuilder().setOk(true).build();
        responseObserver.onNext(r);
        responseObserver.onCompleted();
    }
    @Override
    public synchronized void updateNetwork(CleaningRobotServerOuterClass.listOfUnreachableRobots request, StreamObserver<CleaningRobotServerOuterClass.Response> responseObserver) {
        others = others.stream().filter(e -> !request.getIdList().contains(e.getId())).collect(Collectors.toList());
        //System.out.println("finish updated network from robot: "+id+", "+others.stream().map(e -> e.getId()).collect(Collectors.toList()));
        printNetwork();
        CleaningRobotServerOuterClass.Response r = CleaningRobotServerOuterClass.Response.newBuilder().setOk(true).build();
        responseObserver.onNext(r);
        responseObserver.onCompleted();
    }
}
