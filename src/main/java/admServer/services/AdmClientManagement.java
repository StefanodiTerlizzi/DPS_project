package admServer.services;

import admServer.Model.AirSing;
import admServer.Model.RobotSing;
import cleaningRobot.CleaningRobot;
import cleaningRobot.Position;
import com.google.gson.Gson;
import utils.Tuple;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Path("admClientServices")
public class AdmClientManagement {
    @Path("robotList")
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public synchronized Response robotList(){
        try {
            List<Tuple.TwoTuple> res = RobotSing.getInstance().getRobots();
            return Response.ok(new Gson().toJson(res)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getLocalizedMessage()).build();
        }
    }

    @Path("NmeasurementsFromRobotRID")
    @POST
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public synchronized Response NmeasumerentFromRobotRID(List<Integer> request){
        try {
            List<Double> result = AirSing.getInstance().getLastNitemsOfRobotR(request.get(0), request.get(1));
            if (result.size() == 0) return Response.status(Response.Status.CONFLICT).entity("no values in this period for this robot").build();
            return Response.ok(new Gson().toJson(result.stream()
                    .reduce(0., Double::sum) / result.size()
            )).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getLocalizedMessage()).build();
        }
    }

    @Path("avgFromt1Tot2")
    @POST
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public synchronized Response avgFromt1Tot2(List<Long> request){
        try {
            List<Double> result = AirSing.getInstance().getAVGfromT1toT2(request.get(0), request.get(1));
            if (result.size() == 0) return Response.status(Response.Status.CONFLICT).entity("no values in this period").build();
            return Response.ok(new Gson().toJson(result.stream()
                    .reduce(0., Double::sum) / result.size()
            )).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getLocalizedMessage()).build();
        }
    }



}
