package eendagsvliegjes.robot;

import battlecode.common.*;

public class Landscaper extends AbstractRobot {

    // Constants
    private MapLocation HQLocation = rc.getLocation().translate(-offsetDSX, -(offsetDSY+1));
    private int WALL_HEIGHT = 3;

    // Global parameters
    private boolean digFlag = true; // True when digger needs to dig

    public Landscaper(RobotController rc) {
        super(rc);
    }

    @Override
    protected void run() throws GameActionException {
        if (digFlag) {
            digDirt();
        } else {
            buildDirtWall();
        }

    }

    /**
     * Dig dirt until max dirt load has been reached.
     *
     * @return True when action execution successful
     */
    private boolean digDirt() throws GameActionException {
        // If dirt cannot be digged, assume digger is full
        if (rc.getDirtCarrying() == RobotType.LANDSCAPER.dirtLimit){
            digFlag = false;
            return false;
        }

        // TODO: Do not only dig north
        if(rc.isReady() && rc.canDigDirt(Direction.NORTH)){
            rc.digDirt(Direction.NORTH);
            return true;
        }

        return false;
    }

    /**
     * Build a dirt wall of WALL_HEIGHT against our base
     *
     * @return True if action successful
     * @throws GameActionException
     */
    private boolean buildDirtWall() throws GameActionException {
        // Put down 2 dirt blocks on every tile touching the base
        // Go to a tile adjacent to base
        int HQElevation = rc.senseElevation(HQLocation);
        int wallElevation = 0;
        // Put dirt down on each tile
        for (Direction dir : directions) {
            // Go to tile if not enough dirt
            wallElevation = rc.senseElevation(HQLocation.add(dir));
            // Check elevation level
            if (wallElevation - HQElevation < WALL_HEIGHT) {
                if (!HQLocation.add(dir).isAdjacentTo(rc.getLocation())) {
                    tryMoveToMapLocation(HQLocation.add(dir));
                } else {
                    tryDepositDirt(HQLocation.add(dir));
                }
            }
        }
        return false;
    }

    /**
     * Try to deposit dirt if possible
     *
     * @param location Location of dirt deposit
     * @return True if action successful
     * @throws GameActionException
     */
    protected boolean tryDepositDirt(MapLocation location) throws GameActionException {
        Direction dir = rc.getLocation().directionTo(location);
        if (rc.canDepositDirt(dir)) {
            rc.depositDirt(dir);
            return true;
        }
        return false;
    }

}
