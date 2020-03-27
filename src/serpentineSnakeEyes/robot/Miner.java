package serpentineSnakeEyes.robot;

import java.util.HashSet;
import java.util.Iterator;

import serpentineSnakeEyes.building.Building;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;

import static serpentineSnakeEyes.RobotPlayer.directions;
import static serpentineSnakeEyes.RobotPlayer.turnCount;
import static battlecode.common.RobotType.DESIGN_SCHOOL;
import static battlecode.common.RobotType.FULFILLMENT_CENTER;
import static battlecode.common.RobotType.NET_GUN;
import static battlecode.common.RobotType.REFINERY;
import static battlecode.common.RobotType.VAPORATOR;
import static serpentineSnakeEyes.util.Constants.*;

public class Miner extends Robot {

	private int timesHqBlocked;
	private int timesMovedForUnstuck;
	private boolean immunity;
	private boolean gettingUnstuck;

	@Override
	public void runRobot() throws GameActionException {
		if (justSpawned) {
			justSpawned = false;
			if (roundNum > STRATEGY_CHANGE_ROUND) {
				immunity = true;
				readAllBuildingLocations();
			}
		}
		updateBuildings();
		updateSoupLocations();
		removeDepletedSoup();

		// If it is on the wall after round 700, remove itself
		if (isOnWall(myLocation) && roundNum > ROUND_NUM_MINIERS_ON_WALL_SUICIDE) {
			suicide();
		}

		if (roundNum < 150 || !locatedBuildings.get(DESIGN_SCHOOL).isEmpty()
				|| rc.getTeamSoup() < DESIGN_SCHOOL.cost) {
			for (Direction direction : directions) {
				tryRefine(direction);
			}
			for (Direction direction : directions) {
				tryMine(direction);
			}
		}

		if (roundNum < STRATEGY_CHANGE_ROUND || locatedBuildings.get(DESIGN_SCHOOL).isEmpty()) {
			builder.buildNextToHQ(DESIGN_SCHOOL, 1);
		}
		if (roundNum < STRATEGY_CHANGE_ROUND || locatedBuildings.get(FULFILLMENT_CENTER).isEmpty()) {
			builder.buildNextToHQ(FULFILLMENT_CENTER, 1);
		}
		builder.buildNextToHQ(VAPORATOR, 4);
		if (locatedBuildings.get(VAPORATOR).size() > 2) {
			builder.buildNextToHQ(NET_GUN, 1);
		}

		if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
			returnSoup();
		} else if (!soupLocations.isEmpty()) {
			moveTowardsSoup();
		}
		move();
		if (roundNum > 500 && logistics.withinWalls() && !immunity) {
			suicide();
		}
	}

	private boolean moveTowardsSoup() throws GameActionException {
		MapLocation closestSoup = null;
		for (MapLocation soup : soupLocations) {
			if (closestSoup == null || logistics.isCloser(soup, closestSoup)) {
				closestSoup = soup;
			}
		}
		if (closestSoup.isWithinDistanceSquared(myLocation, SOUP_TOO_FAR)) {
			rc.setIndicatorLine(myLocation, closestSoup, 255, 255, 0);
			if (logistics.moveTowards(closestSoup)) {
				timesStuck = 0;
				return true;
			} else {
				timesStuck++; // Unable to reach the desired soup
			}
		}
		return false;
	}

	private boolean isStuck() throws GameActionException {
		if (turnCount < 100) {
			return false;
		}
		if (gettingUnstuck && timesMovedForUnstuck < MOVE_BEFORE_RESUMING_COURSE) {
			move();
			timesMovedForUnstuck++;
			return true;
		} else if (new HashSet<>(previousLocations).size() < 3) {
			directionBundle = logistics.getRandomDirectionBundle();
			gettingUnstuck = true;
			move();
			return true;
		}
		gettingUnstuck = false;
		timesMovedForUnstuck = 0;
		return false;
	}

	private void returnSoup() throws GameActionException {
		if (new HashSet<>(previousLocations).size() < 3) {
			builder.buildAnywhere(REFINERY);
		}
		MapLocation refinery = null;
		for (Building ref : locatedBuildings.get(REFINERY)) {
			MapLocation location = ref.getLocation();
			if (refinery == null || logistics.isCloser(location, refinery)) {
				refinery = location;
			}
		}

		if (refinery != null) {
			if (!myLocation.isWithinDistanceSquared(refinery, SOUP_DEPOSIT_RADIUS_THRESHOLD)) { // Refinery too far, build another
				if (!builder.buildAnywhere(REFINERY)) {  // HQ too far, build refinery
					logistics.moveTowards(refinery); // Unable to build, return to initial one
				}
			} else {
				logistics.moveTowards(refinery);
				rc.setIndicatorLine(myLocation, refinery, 0, 255, 255);
			}
		} else if (!myLocation.isWithinDistanceSquared(hqLoc, SOUP_DEPOSIT_RADIUS_THRESHOLD)) { // TODO Find suitable distance
			if (!builder.buildAnywhere(REFINERY)) {  // HQ too far, build refinery
				logistics.moveTowards(hqLoc); // Unable to build, return to HQ
				rc.setIndicatorLine(myLocation, hqLoc, 0, 255, 255);
			}
		} else if (logistics.moveTowards(hqLoc)) {
			timesStuck = 0;
			timesHqBlocked = 0;
		} else if (timesHqBlocked == SAME_LOCATION_THRESHOLD) {
			if (builder.buildAnywhere(REFINERY)) { // Can no longer refine soup in HQ, need a refinery
				timesHqBlocked = 0;
				timesStuck = 0;
			}
		} else {
			timesHqBlocked++;
			timesStuck++;
		}
	}

	private void removeDepletedSoup() throws GameActionException {
		Iterator<MapLocation> iterator = soupLocations.iterator();
		while (iterator.hasNext()) {
			MapLocation soup = iterator.next();
			if (rc.canSenseLocation(soup) && rc.senseSoup(soup) == 0) {
				iterator.remove();
			}
		}
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
			MapLocation soupLoc = myLocation.add(dir);
			if (!soupLocations.contains(soupLoc)) {
				broadcastLocation(soupLoc, SOUP_TYPE);
			}
			return true;
		}
		return false;
	}
}
