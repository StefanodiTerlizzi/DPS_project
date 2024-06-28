package admServer.Model.greenfield;

import cleaningRobot.CleaningRobot;
import cleaningRobot.Position;
import utils.Tuple;

import java.util.*;
import java.util.stream.Collectors;

public class Greenfield {

    MatrixGUI matrixGUI;
    private ArrayList<Tuple.TwoTuple<Position, ArrayList<CleaningRobot>>> matrix = new ArrayList<>();

    public synchronized ArrayList<Tuple.TwoTuple<Position, ArrayList<CleaningRobot>>> getMatrix() {return matrix;}

    public void setMatrix(ArrayList<Tuple.TwoTuple<Position, ArrayList<CleaningRobot>>> matrix) {this.matrix = matrix;}

    public synchronized ArrayList<Tuple.TwoTuple<Position, ArrayList<CleaningRobot>>> getDistrict(int i) {
        int startRow = (i<=2) ? 0 : 5;
        int startColumn = (i==1 || i==4) ? 0 : 5;
        return (ArrayList<Tuple.TwoTuple<Position, ArrayList<CleaningRobot>>>) matrix.stream()
                .filter(e ->
                        e.one.getRow() >= startRow &&
                        e.one.getRow() < startRow+5 &&
                        e.one.getCol() >= startColumn &&
                        e.one.getCol() < startColumn+5
                ).collect(Collectors.toList());
    }

    public Greenfield() {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                matrix.add(new Tuple.TwoTuple<>(new Position(i, j), new ArrayList<>()));
            }
        }
        matrixGUI = new MatrixGUI(matrix);
    }

    public synchronized Position getNewPosition(CleaningRobot robot) {
        TreeMap<Integer, Integer> nRobot_district = new TreeMap<>();
        for (Integer district: Arrays.asList(1, 2, 3, 4)) {
            nRobot_district.put(
                    getDistrict(district).stream()
                            .map(a -> a.two.size())
                            .reduce((a, b) -> a+b)
                            .get(),
                    district
            );
        }
        List<Tuple.TwoTuple<Position, ArrayList<CleaningRobot>>> orderCells = getDistrict(nRobot_district.firstEntry().getValue()).stream().sorted(Comparator.comparingInt(e -> e.two.size())).collect(Collectors.toList());
        int minSize = orderCells.stream().findFirst().get().two.size();
        List<Position> minCells = orderCells.stream().filter(e -> e.two.size() == minSize).map(e -> e.one).collect(Collectors.toList());
        Position p = minCells.get(new Random().nextInt(minCells.size()));
        matrix.stream().filter(e -> e.one.equals(p)).findFirst().get().two.add(robot);
        matrixGUI.updateMatrix(matrix);
        return p;
    }

    public synchronized ArrayList<CleaningRobot> getListOfRobots() {
        return (ArrayList<CleaningRobot>) matrix.stream()
                .flatMap(e -> e.two.stream())
                .collect(Collectors.toList());
    }

    public synchronized void deleteNotAvailableRobots(List<Integer> notAvailableRobots) {
        for (Integer id:notAvailableRobots) {
            //find correct element of the grid
            Tuple.TwoTuple<Position, ArrayList<CleaningRobot>> element = matrix.stream()
                    .filter(e -> e.two.stream()
                            .filter(e1 -> e1.getId() == id).collect(Collectors.toList()).size() > 0
                    )
                    .collect(Collectors.toList()).get(0);
            //delete element
            matrix = (ArrayList<Tuple.TwoTuple<Position, ArrayList<CleaningRobot>>>) matrix.stream().filter(e -> !e.one.equals(element.one)).collect(Collectors.toList());
            //update
            element.two = (ArrayList<CleaningRobot>) element.two.stream().filter(e -> e.getId()!=id).collect(Collectors.toList());
            //add
            matrix.add(element);
        }
        matrixGUI.updateMatrix(matrix);
    }

    @Override
    public String toString() {
        return "Greenfield{" +
                "matrix=" + matrix +
                '}';
    }

}
