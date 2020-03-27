package eendagsvliegjes.robot;
import battlecode.common.*;
import java.lang.Math;

import java.awt.*;

abstract public strictfp class AbstractRobot {
    //TODO: reorder auxiliarry methods in a more clear way, maybe split into separate file
    //TODO: switch to composite design pattern
    protected RobotController rc;

    protected static int DEBUG_LEVEL = 5;

    final static protected Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST
    };
    final static protected RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL,
            RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};

    // constants
    protected final int offsetDSX = -2;
    protected final int offsetDSY = 2;

    protected int turnCount;

    /**
     * Constructor
     * @param rc RobotController for this robot.
     */
    public AbstractRobot(RobotController rc) {
        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        this.rc = rc;
        turnCount = 0;

        if (DEBUG_LEVEL >= 3) {
            System.out.println("I'm a " + rc.getType() + " and I just got created!");
        }
    }

    /**
     * Main robot loop.
     * @throws GameActionException
     */
    public void loop() throws GameActionException {
        while (true) {
            turnCount += 1;

            try {
                if (DEBUG_LEVEL >= 5) {
                    System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
                }

                run();
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                if (DEBUG_LEVEL >= 1) {
                    System.out.println(rc.getType() + " Exception");
                }
                e.printStackTrace();
            }
        }
    }

    /**
     * Robot specific behavior goes here.
     * @throws GameActionException
     */
    abstract protected void run() throws GameActionException;

    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    protected Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    /**
     * Returns a random RobotType spawned by miners.
     *
     * @return a random RobotType
     */
    protected RobotType randomSpawnedByMiner() {
        return spawnedByMiner[(int) (Math.random() * spawnedByMiner.length)];
    }

    /**
     * Move in the first direction you can move to in de directions array.
     * @return True if moved in a direction. False if unable to move in any direction.
     * @throws GameActionException
     */
    protected boolean tryMove() throws GameActionException {
        for (Direction dir : directions)
            if (tryMove(dir))
                return true;
        return false;
    }

    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    protected boolean tryMove(Direction dir) throws GameActionException {
        if (DEBUG_LEVEL >= 5) {
            System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        }
        if (rc.isReady() && rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to build a given robot in a given direction.
     *
     * @param type The type of the robot to build
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    protected boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to mine soup in a given direction.
     *
     * @param dir The intended direction of mining
     * @return true if a move was performed
     * @throws GameActionException
     */
    protected boolean tryMine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMineSoup(dir)) {
            rc.mineSoup(dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to refine soup in a given direction.
     *
     * @param dir The intended direction of refining
     * @return true if a move was performed
     * @throws GameActionException
     */
    protected boolean tryRefine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canDepositSoup(dir)) {
            rc.depositSoup(dir, rc.getSoupCarrying());
            return true;
        } else return false;
    }

    /**
     * Try to shoot at the given robot.
     *
     * @return True if shot was executed
     * @throws GameActionException
     */
    protected boolean tryShoot(RobotInfo robot) throws GameActionException {
        int unitId = robot.getID();
        if (rc.isReady() && rc.canShootUnit(unitId)) {
            rc.shootUnit(unitId);
            return true;
        } else return false;
    }

    /**
     * Shoot at an enemy drone within range
     *
     * @return True on successful action execution
     * @throws GameActionException
     */
    protected boolean shootNearbyEnemyDrone() throws GameActionException {
        RobotInfo[] nearbyEnemyRobots = rc.senseNearbyRobots(-1, getOpponentTeam());
        for (RobotInfo robot : nearbyEnemyRobots) {
            if (robot.type == RobotType.DELIVERY_DRONE) {
                if (tryShoot(robot)) {
                    break;
                }
            }
        }
        return true;
    }

    /**
     * Try to submit a transaction on the blockchain.
     *
     * @throws GameActionException
     */
    protected void tryBlockchain() throws GameActionException {
        if (turnCount < 3) {
            int[] message = new int[7];
            for (int i = 0; i < 7; i++) {
                message[i] = 123;
            }
            if (rc.canSubmitTransaction(message, 10))
                rc.submitTransaction(message, 10);
        }
        // System.out.println(rc.getRoundMessages(turnCount-1));
    }

    /**
     * Move in the direction of a MapLocation.
     * @param location Location to move towards
     * @return true if moved in direction of location.
     * @throws GameActionException
     */
    protected boolean tryMoveToMapLocation(MapLocation location) throws GameActionException {
        return tryMove(rc.getLocation().directionTo(location));
    }

    /**
     * Get the opponent team, not taking team "Neutral" into account
     *
     * @return Opponent team
     */
    protected Team getOpponentTeam() {
        if (rc.getTeam() == Team.A) {
            return Team.B;
        } else return Team.A;
    }
}
