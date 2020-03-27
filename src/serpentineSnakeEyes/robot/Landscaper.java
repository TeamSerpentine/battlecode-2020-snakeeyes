package serpentineSnakeEyes.robot;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import serpentineSnakeEyes.util.Constants;

import static serpentineSnakeEyes.RobotPlayer.directions;
import static serpentineSnakeEyes.util.Constants.DIRT_DEPOSITING_RANGE;
import static serpentineSnakeEyes.util.Constants.DISTANCE_OF_WALL_FROM_HQ;
import static serpentineSnakeEyes.util.Constants.ELEVATION_QUANTIFIER;

public class Landscaper extends Robot {

	private boolean positionedOnWall;

	@Override
	public void runRobot() throws GameActionException {
		updateBuildings();
		Set<MapLocation> unoccupied = new HashSet<>();
		for (MapLocation wall : wallLocations) {
			if (myLocation.equals(wall)) {
				positionedOnWall = true;
			}
			if (rc.canSenseLocation(wall) && !rc.isLocationOccupied(wall)) {
				unoccupied.add(wall);
			}
		}
		// Go towards one of the unoccupied walls
		if (!positionedOnWall || roundNum < 200 && Math.random() < 0.1) {
			for (MapLocation wall : unoccupied) {
				if (logistics.moveTowards(wall)) {
					positionedOnWall = true;
					break;
				}
			}
		}

		if (rc.getDirtCarrying() == 0) {
			tryDig();
		}

		if (!positionedOnWall || Math.random() < 0.2) {
			moveToALowWall();
		}

		buildAtBestLocation();
		if (!positionedOnWall) {
			if (hqLoc != null) {
				// Try to get to the hq
				if (logistics.moveTowards(hqLoc)) {
					timesStuck = 0;
				} else {
					timesStuck++;
				}
			} else {
				if (logistics.moveSomewhere()) {
					timesStuck = 0;
				} else {
					timesStuck++;
				}
			}
		}
	}

	private void moveToALowWall() throws GameActionException {
		// Find the elevation of the highest wall
		int highestElevation = Integer.MIN_VALUE;
		for (MapLocation wall : wallLocations) {
			if (rc.canSenseLocation(wall)) {
				int elevation = rc.senseElevation(wall);
				if (elevation > highestElevation) {
					highestElevation = elevation;
				}
			}
		}

		// Find all low walls below threshold
		Set<MapLocation> lowWalls = new HashSet<>();
		if (highestElevation != Integer.MIN_VALUE) {
			for (MapLocation wall : wallLocations) {
				if (rc.canSenseLocation(wall) && rc.senseElevation(wall) < highestElevation * ELEVATION_QUANTIFIER) {
					lowWalls.add(wall);
				}
			}
		}

		// Try to move towards one of the low walls
		for (MapLocation wall : lowWalls) {
			if (!myLocation.isWithinDistanceSquared(wall, 4) && logistics.moveTowards(wall)) {
				if (!logistics.standingOnWall()) {
					positionedOnWall = false;
				}
			}
		}
	}

	private void buildAtBestLocation() throws GameActionException {
		MapLocation lowestLocation = null;
		int lowestElevation = Integer.MAX_VALUE;
		MapLocation bestPlaceToBuildWall = null;
		for (MapLocation wall : wallLocations) {
			Direction direction = myLocation.directionTo(wall);
			int distance = myLocation.distanceSquaredTo(wall);
			if (rc.canSenseLocation(wall) && rc.senseElevation(wall) < lowestElevation) {
				lowestLocation = wall;
				lowestElevation = rc.senseElevation(wall);
			}
			if (rc.canDepositDirt(direction) && distance < DIRT_DEPOSITING_RANGE) {
				if (bestPlaceToBuildWall == null ||
						rc.canSenseLocation(wall) && rc.senseElevation(bestPlaceToBuildWall) > rc.senseElevation(wall)) {
					bestPlaceToBuildWall = wall;
				}
			}
		}

		if (lowestLocation != null && (bestPlaceToBuildWall == null ||
				lowestElevation < rc.senseElevation(bestPlaceToBuildWall) * ELEVATION_QUANTIFIER)) {
			moveOnWallTo(lowestLocation);
		}
		if (bestPlaceToBuildWall != null) {
			// Build the wall
			Direction direction = myLocation.directionTo(bestPlaceToBuildWall);
			if (rc.canDepositDirt(direction)) {
				rc.depositDirt(direction);
				rc.setIndicatorDot(bestPlaceToBuildWall, 0, 255, 0);
			}
		}
	}

	private void moveOnWallTo(MapLocation destination) throws GameActionException {
		int posX = myLocation.x - hqLoc.x;
		int posY = myLocation.y - hqLoc.y;
		int destX = destination.x - hqLoc.x;
		int destY = destination.y - hqLoc.y;

		if (posX == destX && Math.abs(posX) == 2|| posY == destY && Math.abs(posY) == 2) {
			// Can move directly
			if (rc.canMove(myLocation.directionTo(destination))) {
				rc.move(myLocation.directionTo(destination));
			}
		} else if (Math.abs(posX) == 2) {
			// On vertical wall
			if (destY >= 0 && rc.canMove(Direction.NORTH)) {
				rc.move(Direction.NORTH);
			} else if (destY >= 0 && rc.canMove(Direction.SOUTH)) {
				rc.move(Direction.SOUTH);
			}
		} else {
			// On horizontal wall
			if (destX >= 0 && rc.canMove(Direction.EAST)) {
				rc.move(Direction.EAST);
			} else if (destX <= 0 && rc.canMove(Direction.SOUTH)) {
				rc.move(Direction.SOUTH);
			}
		}

	}

	private MapLocation getLowerTile(MapLocation tile1, MapLocation tile2) throws GameActionException {
		return rc.senseElevation(tile1) < rc.senseElevation(tile2) ? tile1 : tile2;
	}

	private MapLocation getHigherTile(MapLocation tile1, MapLocation tile2) throws GameActionException {
		return rc.senseElevation(tile1) > rc.senseElevation(tile2) ? tile1 : tile2;
	}

	private boolean tryDig() throws GameActionException {
		Set<MapLocation> locationsToDig = new HashSet<>();
		outer:
		for (Direction dir : directions) {
			if (rc.canDigDirt(dir)) {
				MapLocation location = myLocation.add(dir);
				if (roundNum < 400 && location.isWithinDistanceSquared(hqLoc, DISTANCE_OF_WALL_FROM_HQ + 1)) {
					continue outer;
				} else {
					locationsToDig.add(location);
				}
				// Try to dig the highest tile to keep the plain even
				MapLocation bestSpotToDig = null;
				for (MapLocation dirt : locationsToDig) {
					if (!logistics.isWall(dirt) && (bestSpotToDig == null ||
							rc.senseElevation(dirt) > rc.senseElevation(bestSpotToDig))) {
						rc.digDirt(dir);
						rc.setIndicatorDot(myLocation.add(dir), 255, 0, 0);
						return true;
					}
				}
			}
		}
		return false;
	}

	private Predicate<MapLocation> isCloseEnoughForDepositing() {
		return tile -> myLocation.distanceSquaredTo(tile) < DIRT_DEPOSITING_RANGE;
	}

	private Predicate<MapLocation> canDeposit() {
		return tile -> rc.canDepositDirt(myLocation.directionTo(tile));
	}
}
