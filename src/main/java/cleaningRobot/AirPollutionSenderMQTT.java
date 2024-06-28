package cleaningRobot;

import cleaningRobot.simulator.Measurement;

import java.util.List;

public interface AirPollutionSenderMQTT {
    void registertoMQTT();
    void sendAirPollution();
}
