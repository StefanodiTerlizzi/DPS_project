package admServer.services;

import admServer.Model.RobotSing;
import cleaningRobot.CleaningRobot;
import cleaningRobot.Position;
import com.google.gson.Gson;
import utils.Tuple;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Path("cleaningRobotManagement")
public class CleaningRobotManagement {
    @Path("add")
    @POST
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public synchronized Response addCleaningRobot(CleaningRobot robot){
        System.out.println("addCleaningRobot "+robot.getId());
        try {
            Tuple.TwoTuple<Position, ArrayList<CleaningRobot>> res = RobotSing.getInstance().addRobot(robot);
            RobotSing.getInstance().printGreenfield();
            return Response.ok(new Gson().toJson(res)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.CONFLICT).entity(e.getLocalizedMessage()).build();
        }
    }

    @Path("updateNetwork")
    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    public synchronized Response updateNetwork(List<Integer> notAvailableRobots){
        System.out.println("updateNetwork");
        RobotSing.getInstance().deleteNotAvailableRobots(notAvailableRobots);
        RobotSing.getInstance().printGreenfield();
        return Response.status(Response.Status.OK).build();
    }


}
