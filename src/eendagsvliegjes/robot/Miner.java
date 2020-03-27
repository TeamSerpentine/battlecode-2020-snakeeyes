package eendagsvliegjes.robot;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class Miner extends AbstractRobot {

    // TODO: Check all operations for safety (e.g. check if sensed locations are on the map, check if action is possible).
    // TODO: Make sure all log messages have a debug level (maybe create logger).
    public enum State {
        FIND_HQ,
        SEARCH_SOUP,
        SEARCH_REFINERY,
        MINE_SOUP,
        REFINE_SOUP,
        BUILD_REFINERY,
        BUILD_DESIGN_SCHOOL
    }

    public enum Keys {
        HQ, SOUP, REFINERY
    }

    // TODO: change to an easier data structure (ewoud's opninion)
    // Variable to store the location, valid keys are in Keys)
    private HashMap<Keys, ArrayList<MapLocation>> information = new HashMap<>();

    private State state;

    public Miner(RobotController rc) {
        super(rc);
        state = State.FIND_HQ; // start with saving the HQ location
    }

    @Override
    protected void run() throws GameActionException {

        // TODO: make this giant switch more readable.
        if (state == State.FIND_HQ) {
            if (searchHQ()) {
                state = State.SEARCH_SOUP;
            }
        }

        if (state == State.SEARCH_SOUP) {
            if (searchSoup()) {
                state = State.SEARCH_REFINERY;
            }
        }

        if (state == State.SEARCH_REFINERY) {
            if (refineryNearby()) {
                state = State.MINE_SOUP;
            } else {
                state = State.BUILD_REFINERY;
            }
        }

        /*
        If we cant build a refinery we want to go mining;
         */
        if (state == State.BUILD_REFINERY) {
            if (buildRefinery()) {
                state = State.MINE_SOUP;
            } else {
                if (mineSoup()) {
                    state = State.REFINE_SOUP;
                }
            }
        } else if (state == State.MINE_SOUP) {
            if (mineSoup()) {
                state = State.REFINE_SOUP;
            }
        }

        if (state == State.REFINE_SOUP) {
            if (refineSoup()) {
                state = State.BUILD_DESIGN_SCHOOL;
            }
        }

        if (state == State.BUILD_DESIGN_SCHOOL){
            System.out.println("Entering build design school  " + state);
            if (buildDesignSchool()){
                state = State.SEARCH_SOUP;
            }
        }

        if (DEBUG_LEVEL >= 5) {
            System.out.println("Miner current state: " + state);
        }
    }
    /**
     * The boolean methods below should return True whenever the performed method is completed.
     */


    /**
     * Locates the Headquarters and stores this in the miner memory.
     *
     * @return true if the HQ location is stored.
     */
    private boolean searchHQ() {
        if (information.containsKey("HQ")) {
            return true;
        } else {
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots) {
                if (robot.type == RobotType.HQ && robot.team == rc.getTeam()) {

                    // We append HQ also to refinery since it has a double function
                    addLocationToMemory(Keys.HQ, robot.getLocation());
                    addLocationToMemory(Keys.REFINERY, robot.getLocation());

                    if (DEBUG_LEVEL >= 3) {
                        System.out.println("HQ is located at " + robot.location);
                    }
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * Find a soup location and if newly found store it in miners memory.
     *
     * @return true if miner is adjacent to soup.
     */
    private boolean searchSoup() throws GameActionException {

        if (nextToSoup()) {
            System.out.println("Next to soup " + rc.getLocation());
            return true;
        }

        if (!information.containsKey(Keys.SOUP) || !moveToSoupLocationFromMemory()) {
            System.out.println("Moving in random location from " + rc.getLocation());
            tryMove(randomDirection());
        }

        return false;
    }


    /**
     * Move to a Soup location located in Memory.
     *
     * @return true if moving to a Soup location in memory.
     */
    private boolean moveToSoupLocationFromMemory() throws GameActionException {

        Iterator it = information.get(Keys.SOUP).iterator();

        while (it.hasNext()) {
            MapLocation soupLocation = (MapLocation) it.next();

            // If at the correct location but there is no Soup, remove the location and try next one
            if (rc.getLocation() == soupLocation) {
                it.remove();
                continue;
            }

            if (tryMoveToMapLocation(soupLocation)) {
                System.out.println("Moving to soup at " + soupLocation);
                return true;
            }
        }
        return false;
    }

    /**
     * Add a new Soup location to memory.
     *
     * @param key Key of HashMap to use.
     * @param location Location to add to HashMap
     */
    private void addLocationToMemory(Keys key, MapLocation location) {
        if (information.containsKey(key)) {
            if (!information.get(key).contains(location)) {
                information.get(key).add(location);
            }
        } else {
            ArrayList<MapLocation> loc = new ArrayList<>();
            information.put(key, loc);
            information.get(key).add(location);
        }
        System.out.println(" key: " + key + " new location: " + location);
    }
    /**
     * @return True if refinery in vision. False if no refinery in vision.
     */
    private boolean refineryNearby() {
        for (RobotInfo robot : rc.senseNearbyRobots()) {
            if (robot.getType() == RobotType.REFINERY || robot.getType() == RobotType.HQ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Mines the soup until full or no more soup in vision
     *
     * @return True if miner is full at method call or no more soup adjacent to the robot at method call
     */
    private boolean mineSoup() throws GameActionException {
        // First check if miner is done, before mining
        if (rc.getSoupCarrying() > 93 || !nextToSoup()) {
            System.out.println("I am full of soup! " + rc.getSoupCarrying());
            return true;
        }

        for (Direction dir : directions) {
            if (tryMine(dir)) {
                System.out.println("I mined soup! " + rc.getSoupCarrying());
                break;
            }
        }
        return false;
    }

    /**
     * Find HQ or Refinery to refine crude soup.
     *
     * @retrun True if no more soup in inventory.
     */
    private boolean refineSoup() throws GameActionException {
        // TODO: Redo method in a more readible way, maybe split travel and deposit actions into separate methods.

        if (rc.getSoupCarrying() == 0){
            return true;
        }

        // Walk to refinery (check information HashMap)
        int squareDistance;
        int bestDistance = 4096; // 64^2 (max map size)
        MapLocation closestRefinery = information.get(Keys.REFINERY).get(0); // default refinery is HQ

        for (MapLocation location : information.get(Keys.REFINERY)){
             squareDistance = location.distanceSquaredTo(rc.getLocation());
             if (squareDistance < bestDistance){
                 closestRefinery = location;
                 bestDistance = squareDistance;
             }
        }

        // Dump all soup until inventory is empty (at start of call)
        if (closestRefinery.isAdjacentTo(rc.getLocation())) {
            for (Direction dir : directions){
                if (rc.canDepositSoup(dir)){
                    rc.depositSoup(dir, rc.getSoupCarrying());
                    System.out.println("Dumping at refinery at location " + closestRefinery + " soup left " + rc.getSoupCarrying());
                    break;
                }
            }
        } else {
            tryMoveToMapLocation(closestRefinery);
            System.out.println("From location " + rc.getLocation() + " moving to refinery at " + closestRefinery);
        }

        return false;
    }

    /**
     * Build a refinery.
     *
     * @return true if refinery in vision.
     */
    private boolean buildRefinery() throws GameActionException {
        // check if a refinery is already in vision
        if (refineryNearby()) {
            return true;
        }
        // check if soup is nearby, if so build a refinery in that direction
        if (nextToSoup()) {
            // if there is soup on the adjacent location
            for (Direction direction : directions) {
                MapLocation adjacentLocation = rc.getLocation().add(direction);
                if (rc.canBuildRobot(RobotType.REFINERY, direction) && rc.canSenseLocation(adjacentLocation) && rc.senseSoup(adjacentLocation) == 0) {
                    // if a refinery can be built in direction2 build it.
                    rc.buildRobot(RobotType.REFINERY, direction);
                    addLocationToMemory(Keys.REFINERY, adjacentLocation);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Build a design school two tiles next to HQ (in order of "directions array").
     *
     * @return true if Design school in vision.
     */
    private boolean buildDesignSchool() throws GameActionException {
        MapLocation designSchoolLocation = information.get(Keys.HQ).get(0).translate(offsetDSX, offsetDSY);

        // Skip building if design school location not in vision range
        if (!rc.canSenseLocation(designSchoolLocation) ||  rc.isLocationOccupied(designSchoolLocation)) {
            return true;
        }

        // If the location is empty, move to adjacent location and try to build factory
        if (!rc.isLocationOccupied(designSchoolLocation)) {
            // Build design school if adjacent to tile
            if (designSchoolLocation.isAdjacentTo(rc.getLocation())) {
                if (tryBuild(RobotType.DESIGN_SCHOOL, rc.getLocation().directionTo(designSchoolLocation))){
                    return true;
                }
                return false;
            }

            // Move to map location
            tryMoveToMapLocation(designSchoolLocation);
            return false;
        }
        return false;
    }

    /**
     * Check if robot is standing next to Soup.
     *
     * @return True if robot is next to soup, false if not.
     */
    private boolean nextToSoup() throws GameActionException {
        MapLocation currentLocation = rc.getLocation();
        for (Direction direction : directions) {
            MapLocation adjacentLocation = currentLocation.add(direction);
            if (rc.canSenseLocation(adjacentLocation) && rc.senseSoup(adjacentLocation) > 0) {
                return true;
            }
        }
        return false;
    }
}
