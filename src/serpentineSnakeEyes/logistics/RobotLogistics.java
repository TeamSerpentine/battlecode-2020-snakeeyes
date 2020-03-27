package serpentineSnakeEyes.logistics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import serpentineSnakeEyes.util.StreamUtils;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import static serpentineSnakeEyes.RobotPlayer.directions;
import static serpentineSnakeEyes.robot.Robot.hqLoc;
import static serpentineSnakeEyes.robot.Robot.wallLocations;
import static serpentineSnakeEyes.util.Constants.ADJACENCY_DISTANCE;
import static serpentineSnakeEyes.util.Constants.DISTANCE_OF_WALL_FROM_HQ;
import static battlecode.common.Direction.EAST;
import static battlecode.common.Direction.NORTH;
import static battlecode.common.Direction.NORTHEAST;
import static battlecode.common.Direction.NORTHWEST;
import static battlecode.common.Direction.SOUTH;
import static battlecode.common.Direction.SOUTHEAST;
import static battlecode.common.Direction.SOUTHWEST;
import static battlecode.common.Direction.WEST;
import static battlecode.common.RobotType.DELIVERY_DRONE;

public class RobotLogistics {

	public static List<Direction[]> directionBundles;

	static {
		directionBundles = new ArrayList<>();
		directionBundles.add(new Direction[] { NORTHWEST, NORTH, NORTHEAST });
		directionBundles.add(new Direction[] { NORTH, NORTHEAST, EAST });
		directionBundles.add(new Direction[] { NORTHEAST, EAST, SOUTHEAST });
		directionBundles.add(new Direction[] { EAST, SOUTHEAST, SOUTH });
		directionBundles.add(new Direction[] { SOUTHEAST, SOUTH, SOUTHWEST });
		directionBundles.add(new Direction[] { SOUTH, SOUTHWEST, WEST });
		directionBundles.add(new Direction[] { SOUTHWEST, WEST, NORTHWEST });
		directionBundles.add(new Direction[] { WEST, NORTHWEST, NORTH });
	}

	private RobotController rc;

	public RobotLogistics(RobotController rc) {
		this.rc = rc;
	}

	/**
	 * Get a random direction
	 * @return
	 */
	public Direction randomDirection() {
		return directions[(int) (Math.random() * directions.length)];
	}

	/**
	 * Try to move in a random direction
	 * @return
	 * @throws GameActionException
	 */
	public boolean moveSomewhere() throws GameActionException {
		return tryMove(randomDirection());
	}

	/**
	 * Try to move in one of the given directions
	 * @param directionBundle
	 * @return
	 * @throws GameActionException
	 */
	public boolean moveSomewhere(Direction[] directionBundle) throws GameActionException {
		return tryMove(directionBundle[(int) (Math.random() * directionBundle.length)]);
	}

	/**
	 * Try to move in all of the directions
	 * @return
	 * @throws GameActionException
	 */
	public boolean moveAnywhere() throws GameActionException {
		return Arrays.stream(directions).anyMatch(StreamUtils.rethrowPredicate(this::tryMove));
	}

	/**
	 * Try to move to a given destination
	 * @param destination
	 * @return
	 * @throws GameActionException
	 */
	public boolean moveTowards(MapLocation destination) throws GameActionException {
		return go(rc.getLocation().directionTo(destination));
	}

	/**
	 * Try to move sideways in either direction
	 * @param location
	 * @return
	 * @throws GameActionException
	 */
	public boolean moveLaterally(MapLocation location) throws GameActionException {
		Direction direction = rc.getLocation().directionTo(location);
		List<Direction> directions = Arrays.asList(new Direction[] {
				direction.rotateLeft().rotateLeft(),
				direction.rotateRight().rotateRight(),
		});
		Collections.shuffle(directions);
		for (Direction dir : directions) {
			if (tryMove(dir)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Try to move to a specified direction
	 * @param direction
	 * @return
	 * @throws GameActionException
	 */
	public boolean go(Direction direction) throws GameActionException {
		Direction[] toTry = { direction, direction.rotateLeft(), direction.rotateRight(), direction.rotateLeft().rotateLeft(), direction.rotateRight().rotateRight() };
		for (Direction dir : toTry) {
			if (tryMove(dir)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks whether the robot is standing on of the edge coordinates of the map
	 * @return
	 */
	public boolean onTheEdgeOfTheMap() {
		MapLocation location = rc.getLocation();
		return location.x == 0 || location.y == 0 || location.x == rc.getMapWidth() - 1 || location.y == rc.getMapHeight() - 1;
	}

	/**
	 * Verify whether the robot is standing on one of the wall locations
	 * @return
	 */
	public boolean standingOnWall() {
		MapLocation myLocation = rc.getLocation();
		for (MapLocation wall : wallLocations) {
			if (myLocation.equals(wall)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Get a random bundle of directions
	 * @return
	 */
	public Direction[] getRandomDirectionBundle() {
		return directionBundles.get((int) (Math.random() * directionBundles.size()));
	}

	/**
	 * Check whether the first location is closer than the second
	 * @param loc1
	 * @param loc2
	 * @return
	 */
	public boolean isCloser(MapLocation loc1, MapLocation loc2) {
		MapLocation location = rc.getLocation();
		return location.distanceSquaredTo(loc1) < location.distanceSquaredTo(loc2);
	}

	public boolean isNextToHQ() {
		return isNextToHQ(rc.getLocation());
	}

	public boolean isNextToHQ(MapLocation location) {
		return location.isWithinDistanceSquared(hqLoc, ADJACENCY_DISTANCE);
	}

	public boolean withinWalls() {
		return rc.getLocation().isWithinDistanceSquared(hqLoc, DISTANCE_OF_WALL_FROM_HQ - 1);
	}

	public boolean isWall(MapLocation location) {
		for (MapLocation wall : wallLocations) {
			if (wall.equals(location)) {
				return true;
			}
		}
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
		MapLocation location = rc.getLocation().add(dir);
		if (rc.isReady() && rc.canMove(dir) && rc.canSenseLocation(location) &&
				(!rc.senseFlooding(location) || rc.getType() == DELIVERY_DRONE)) {
			rc.move(dir);
			return true;
		}
		return false;
	}
}
