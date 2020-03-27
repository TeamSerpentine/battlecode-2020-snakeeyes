package serpentineSnakeEyes.robot;

import battlecode.common.GameActionException;
import battlecode.common.RobotType;

import static battlecode.common.RobotType.DELIVERY_DRONE;
import static battlecode.common.RobotType.DESIGN_SCHOOL;
import static serpentineSnakeEyes.util.Constants.CREATE_MIN_DRONES_PER_FULFILLMENT_CENTER;
import static serpentineSnakeEyes.util.Constants.MIN_NEARBY_LANDSCAPERS_FOR_DRONE_BUILDING;
import static serpentineSnakeEyes.util.Constants.PLENTY_OF_SOUP;
import static serpentineSnakeEyes.util.Constants.STRATEGY_CHANGE_ROUND;

public class FulfillmentCenter extends Robot {

    private int dronesCreated = 0;

    @Override
    public void runRobot() throws GameActionException {
        updateBuildings();
        if ((enoughSoup() || roundNum > STRATEGY_CHANGE_ROUND) &&
                (dronesCreated < CREATE_MIN_DRONES_PER_FULFILLMENT_CENTER || enoughLandscapersNearby() || rc.getTeamSoup() > PLENTY_OF_SOUP)
                && spawnSuccessful()) {
            dronesCreated++;
            System.out.println("Spawned a " + DELIVERY_DRONE.name());
        }
    }

    private boolean enoughLandscapersNearby() throws GameActionException {
        return getNumberOfUnitsOfTypeNearby(RobotType.LANDSCAPER) > MIN_NEARBY_LANDSCAPERS_FOR_DRONE_BUILDING;
    }

    private boolean enoughSoup() {
        return soupLeftForRefinery(DELIVERY_DRONE) && roundNum > 100;
    }

    private boolean spawnSuccessful() throws GameActionException {
        return builder.buildAnywhere(DELIVERY_DRONE);
    }

    private boolean designSchoolBuilt() {
        return !locatedBuildings.get(DESIGN_SCHOOL).isEmpty();
    }
}
