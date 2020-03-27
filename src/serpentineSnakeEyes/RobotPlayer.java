package serpentineSnakeEyes;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import serpentineSnakeEyes.robot.DeliveryDrone;
import serpentineSnakeEyes.robot.DesignSchool;
import serpentineSnakeEyes.robot.FulfillmentCenter;
import serpentineSnakeEyes.robot.Hq;
import serpentineSnakeEyes.robot.Landscaper;
import serpentineSnakeEyes.robot.Miner;
import serpentineSnakeEyes.robot.NetGun;
import serpentineSnakeEyes.robot.Refinery;
import serpentineSnakeEyes.robot.Robot;
import serpentineSnakeEyes.robot.Vaporator;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

import static battlecode.common.RobotType.DELIVERY_DRONE;
import static battlecode.common.RobotType.DESIGN_SCHOOL;
import static battlecode.common.RobotType.FULFILLMENT_CENTER;
import static battlecode.common.RobotType.HQ;
import static battlecode.common.RobotType.LANDSCAPER;
import static battlecode.common.RobotType.MINER;
import static battlecode.common.RobotType.NET_GUN;
import static battlecode.common.RobotType.REFINERY;
import static battlecode.common.RobotType.VAPORATOR;

public strictfp class RobotPlayer {
    public static int turnCount;

	public static Direction[] directions = {
			Direction.NORTH,
			Direction.NORTHEAST,
			Direction.EAST,
			Direction.SOUTHEAST,
			Direction.SOUTH,
			Direction.SOUTHWEST,
			Direction.WEST,
			Direction.NORTHWEST
	};

    private static Map<RobotType, Supplier<? extends Robot>> robotSuppliers;
    static {
        robotSuppliers = new HashMap<>();
        robotSuppliers.put(HQ, Hq::new);
        robotSuppliers.put(MINER, Miner::new);
        robotSuppliers.put(REFINERY, Refinery::new);
        robotSuppliers.put(VAPORATOR, Vaporator::new);
        robotSuppliers.put(DESIGN_SCHOOL, DesignSchool::new);
        robotSuppliers.put(FULFILLMENT_CENTER, FulfillmentCenter::new);
        robotSuppliers.put(LANDSCAPER, Landscaper::new);
        robotSuppliers.put(DELIVERY_DRONE, DeliveryDrone::new);
        robotSuppliers.put(NET_GUN, NetGun::new);
    }

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        try {
            Supplier<? extends Robot> robotSupplier = robotSuppliers.get(rc.getType());
            Robot robot = robotSupplier.get();
            robot.setRc(rc);
            while (true) {
                turnCount++;
                robot.run();
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();
            }
        } catch (Exception e) {
            System.out.println(rc.getType() + " Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}