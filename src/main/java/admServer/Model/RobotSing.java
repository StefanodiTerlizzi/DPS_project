package admServer.Model;

import cleaningRobot.CleaningRobot;
import cleaningRobot.*;
import admServer.Model.greenfield.Greenfield;
import utils.Tuple;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@XmlRootElement
public class RobotSing {
    private static RobotSing instance = null;

    private Greenfield greenfield = new Greenfield();

    private ArrayList<Tuple.TwoTuple<Position, ArrayList<CleaningRobot>>> matrix = new ArrayList<>();

    public static synchronized RobotSing getInstance(){
        if (instance==null) instance = new RobotSing();
        return instance;
    }


    public synchronized Tuple.TwoTuple<Position, ArrayList<CleaningRobot>> addRobot(CleaningRobot robot) throws Exception {
        ArrayList<CleaningRobot> list = greenfield.getListOfRobots();
        if (list.stream().anyMatch(
                r -> r.getId() == robot.getId()
        )) throw new Exception("robot ID already present");
        ArrayList<CleaningRobot> listCopy = (ArrayList<CleaningRobot>) list.clone();
        return new Tuple.TwoTuple<>(greenfield.getNewPosition(robot),listCopy);
    }
    public synchronized void deleteNotAvailableRobots(List<Integer> notAvailableRobots) {greenfield.deleteNotAvailableRobots(notAvailableRobots);}

    public synchronized List<Tuple.TwoTuple> getRobots() {
        return greenfield.getMatrix().stream()
                .filter(el -> el.two.size()>0)
                .map(e -> new Tuple.TwoTuple(
                        e.one,
                        e.two.stream().map(r -> r.getId()).collect(Collectors.toList())
                        )
                )
                .collect(Collectors.toList());
    }

    public synchronized void printGreenfield() {
        System.out.println("network");
        System.out.println(
                greenfield.getMatrix().stream()
                        .filter(e -> e.two.size()>0)
                        .map(el -> String.format("%s -> %s\n", el.one, el.two.stream().map(r -> r.getId()).collect(Collectors.toList())))
                        .collect(Collectors.toList())
        );
    }

}
