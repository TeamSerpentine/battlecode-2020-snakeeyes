package serpentineSnakeEyes.builder;

import java.util.Arrays;

import serpentineSnakeEyes.util.StreamUtils;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

import static serpentineSnakeEyes.RobotPlayer.directions;
import static serpentineSnakeEyes.robot.Landscaper.wallLocations;
import static serpentineSnakeEyes.robot.Miner.soupLocations;
import static serpentineSnakeEyes.robot.Robot.buildingTypes;
import static serpentineSnakeEyes.robot.Robot.hqLoc;
import static serpentineSnakeEyes.robot.Robot.locatedBuildings;
import static serpentineSnakeEyes.util.Constants.ADJACENCY_DISTANCE;
import static serpentineSnakeEyes.util.Constants.REFINERY_MAX_DISTANCE_FROM_SOUP;

public class RobotBuilder {

	private RobotController rc;

	public RobotBuilder(RobotController rc) {
		this.rc = rc;
	}

	/**
	 * Build in a random location next to the robot
	 * @param robot
	 * @return
	 * @throws GameActionException
	 */
	public boolean buildSomewhere(RobotType robot) throws GameActionException {
		return tryBuild(robot, randomDirection());
	}

	/**
	 * Build in any location next to the robot
	 * @param building
	 * @return
	 * @throws GameActionException
	 */
	public boolean buildAnywhere(RobotType building) throws GameActionException {
		for (Direction direction : directions) {
			if (tryBuild(building, direction)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Try to build a facility next to the HQ
	 * @param building
	 * @return
	 * @throws GameActionException
	 */
	public boolean buildNextToHQ(RobotType building, int maxBuildings) throws GameActionException {
		MapLocation myLocation = rc.getLocation();
		if (locatedBuildings.get(building).size() < maxBuildings) {
			for (Direction dir : directions) {
				MapLocation location = myLocation.add(dir);
				if (location.isWithinDistanceSquared(hqLoc, ADJACENCY_DISTANCE)) {
					if (tryBuild(building, dir)) {
						System.out.println("Constructed new " + building.name());
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Try to build the specified facility near any of the soup locations
	 * @param building
	 * @return
	 * @throws GameActionException
	 */
	public boolean buildNearSoup(RobotType building) throws GameActionException {
		return Arrays.stream(directions)
				.filter(dir -> soupLocations.stream()
						.anyMatch(loc -> rc.getLocation().add(dir).isWithinDistanceSquared(loc, REFINERY_MAX_DISTANCE_FROM_SOUP)))
				.anyMatch(StreamUtils.rethrowPredicate(dir -> tryBuild(building, dir)));
	}

	/**
	 * Attempts to build a given robot in a given direction.
	 *
	 * @param type The type of the robot to build
	 * @param dir The intended direction of movement
	 * @return true if a move was performed
	 * @throws GameActionException
	 */
	public boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
		MapLocation location = rc.getLocation().add(dir);
		if (rc.isReady() && rc.canSenseLocation(location) && rc.onTheMap(location) &&
				rc.canBuildRobot(type, dir) && !tryingToBuildOnWall(type, dir)) {
			rc.buildRobot(type, dir);
			return true;
		}
		return false;
	}

	private boolean tryingToBuildOnWall(RobotType building, Direction dir) {
		MapLocation buildingLocation = rc.getLocation().add(dir);
		for (RobotType buildingType : buildingTypes) {
			if (building == buildingType) {
				for (MapLocation wall : wallLocations) {
					if (wall.equals(buildingLocation)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private Direction randomDirection() {
		return directions[(int) (Math.random() * directions.length)];
	}
}
