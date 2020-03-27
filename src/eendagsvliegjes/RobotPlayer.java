package eendagsvliegjes;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import eendagsvliegjes.robot.*;

public strictfp class RobotPlayer {
    public static void run(RobotController rc) throws GameActionException {

        AbstractRobot robot;

        switch (rc.getType()) {
            case HQ:                 robot = new HQ(rc);                break;
            case MINER:              robot = new Miner(rc);             break;
            case REFINERY:           robot = new Refinery(rc);          break;
            case VAPORATOR:          robot = new Vaporator(rc);         break;
            case DESIGN_SCHOOL:      robot = new DesignSchool(rc);      break;
            case FULFILLMENT_CENTER: robot = new FulfillmentCenter(rc); break;
            case LANDSCAPER:         robot = new Landscaper(rc);        break;
            case DELIVERY_DRONE:     robot = new DeliveryDrone(rc);     break;
            case NET_GUN:            robot = new NetGun(rc);            break;
            default:                 throw new IllegalStateException();
        }

        // run the loop in the robot.
        robot.loop();
    }
}
