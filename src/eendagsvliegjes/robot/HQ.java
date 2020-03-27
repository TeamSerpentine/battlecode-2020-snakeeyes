package eendagsvliegjes.robot;

import battlecode.common.*;

public class HQ extends AbstractRobot {

    // Constant strategy variables
    static final int MAX_MINERS = 5;  // Maximum amount of miners produced

    // Robot parameters
    static int builtMiners = 0; // Amount of miners built by HQ
    public HQ(RobotController rc) {
        super(rc);
    }
    RobotInfo[] nearbyRobots; // Info structure containing senseNearbyRobot info

    @Override
    protected void run() throws GameActionException {
        shootNearbyEnemyDrone();
        buildMiner();
    }

    /**
     * Build miners provided maximum number has not been reached
     *
     * @return True on successful action execution
     * @throws GameActionException
     */
    private boolean buildMiner() throws GameActionException {
        boolean built = false;
        if (builtMiners < MAX_MINERS) {
            for (Direction dir : directions) {
                built = tryBuild(RobotType.MINER, dir);
                if (built) {
                    builtMiners += 1;
                    break;
                }
            }
        }
        return built;
    }

    /**
     * Sense robots in the area of the view radius and store them for use
     *
     * @return True on successful action execution
     * @throws GameActionException
     */
    private boolean senseRobots() throws GameActionException {
        nearbyRobots = rc.senseNearbyRobots();
        return true;
    }
}
