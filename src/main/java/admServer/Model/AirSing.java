package admServer.Model;

import org.codehaus.jackson.JsonToken;
import utils.Tuple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AirSing {
    private static AirSing instance = null;
    private List<Tuple.ThreeTuple> district1 = new ArrayList<>();
    private List<Tuple.ThreeTuple> district2 = new ArrayList<>();
    private List<Tuple.ThreeTuple> district3 = new ArrayList<>();
    private List<Tuple.ThreeTuple> district4 = new ArrayList<>();

    public static synchronized AirSing getInstance(){
        if (instance==null) instance = new AirSing();
        return instance;
    }
    public void addToDistrict1(Tuple.ThreeTuple<Integer, Long, ArrayList<Double>> message) {if (message!=null) synchronized (district1) {
        //System.out.println("addToDistrict1");
        district1.add(message);}}
    public void addToDistrict2(Tuple.ThreeTuple<Integer, Long, ArrayList<Double>> message) {if (message!=null) synchronized (district2) {
        //System.out.println("addToDistrict2");
        district2.add(message);}}
    public void addToDistrict3(Tuple.ThreeTuple<Integer, Long, ArrayList<Double>> message) {if (message!=null) synchronized (district3) {
        //System.out.println("addToDistrict3");
        district3.add(message);}}
    public void addToDistrict4(Tuple.ThreeTuple<Integer, Long, ArrayList<Double>> message) {if (message!=null) synchronized (district4) {
        //System.out.println("addToDistrict4");
        district4.add(message);}}
    public synchronized List<Double> getLastNitemsOfRobotR(Integer N, Integer RID) {
        List<Tuple.ThreeTuple> all = new ArrayList();
        for (List<Tuple.ThreeTuple> d:Arrays.asList(district1, district2, district3, district4)) {
            all.addAll(d.stream().filter(e -> e.one==RID).collect(Collectors.toList()));
        }

        List<Double> result = all.stream()
                .sorted((a, b) -> (int) ((Long)a.two - (Long)b.two))
                .map(e -> {
                    List<Double> values = (List<Double>) e.three;
                    return values.stream().reduce(0.0, Double::sum) / values.size();
                })
                .collect(Collectors.toList());

        /*List<Double> result = (List<Double>) all.stream()
                .sorted((a, b) -> (int) ((Long)a.two - (Long)b.two))
                .flatMap(threeTuple -> ((List) threeTuple.three).stream())
                .collect(Collectors.toList());*/

        if (N < result.size()) result = result.stream().skip(result.size()-N).collect(Collectors.toList());

        return result;
    }

    public synchronized List<Double> getAVGfromT1toT2(Long start, Long end) {
        List<Double> result = new ArrayList();
        for (List<Tuple.ThreeTuple> d:Arrays.asList(district1, district2, district3, district4)) {
            //System.out.println(d.stream().filter(e -> (Long)e.two>=start && (Long)e.two<=end).collect(Collectors.toList()));

            result.addAll(
                    d.stream()
                    .filter(e -> (Long)e.two>=start && (Long)e.two<=end)
                    .map(e -> {
                        List<Double> values = (List<Double>) e.three;
                        return values.stream().reduce(0.0, Double::sum) / values.size();
                    })
                    .collect(Collectors.toList())
            );

            /*result.addAll(
                    (List<Double>) d.stream()
                            .filter(e -> (Long)e.two>=start && (Long)e.two<=end)
                            .flatMap(el -> ((List)el.three).stream())
                            .collect(Collectors.toList())
            );*/
        }

        return result;
    }
}
