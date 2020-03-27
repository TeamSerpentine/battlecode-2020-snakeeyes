package serpentineSnakeEyes.robot;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.Transaction;
import serpentineSnakeEyes.building.Building;

import static battlecode.common.RobotType.HQ;
import static battlecode.common.RobotType.LANDSCAPER;
import static battlecode.common.RobotType.MINER;
import static battlecode.common.Team.NEUTRAL;
import static serpentineSnakeEyes.RobotPlayer.directions;
import static serpentineSnakeEyes.util.Constants.*;

public class DeliveryDrone extends Robot {

	private boolean hqBroadcasted;
	private boolean perimeterBroadcasted;
	private boolean attack;
	private int dronesInPerimeter;
	private RobotType carriedUnitType;
	private Team carriedUnitTeam;

	@Override
	public void runRobot() throws GameActionException {
		updateBuildings();
		updateDroneInfo();
		updateSoupLocations();
		updateWaterLocations();
		locateEnemyHQ();
		locateSoup();
		locateWater();

		// Transporting
		if (rc.isCurrentlyHoldingUnit()) {
			if (carriedUnitTeam == NEUTRAL) {
				transportNearEnemyHQ();
			} else if (carriedUnitTeam == myTeam) {
				transportTeammate();
			} else {
				dropInWater();
			}
		}

		// Pick up
		tryToPickUp();

		// Move or scout
		if (!goTowardsEnemyHQ()) {
			move();
		}
	}

	private void locateWater() throws GameActionException {
		if (waterLocations.size() > 10) {
			return; // Plenty of water already located
		}
		for (Direction dir : directions) {
			MapLocation location = myLocation.add(dir);
			if (rc.canSenseLocation(location) && rc.senseFlooding(location)) {
				broadcastLocation(location, WATER_TYPE);
			}
		}
	}

	private void locateSoup() throws GameActionException {
		MapLocation[] nearbySoup = senseNearbySoup();
		for (MapLocation soup : nearbySoup) {
			if (!soupLocations.contains(soup) && rc.canSenseLocation(soup) && !rc.senseFlooding(soup)) {
				broadcastLocation(soup, SOUP_TYPE);
			}
		}
	}

	private boolean goTowardsEnemyHQ() throws GameActionException {
		if (hqLocEnemy != null) {
			if (rc.canSenseLocation(hqLocEnemy) || myLocation.distanceSquaredTo(hqLocEnemy) < DISTANCE_TO_KEEP_FROM_ENEMY_HQ) {
				if (!perimeterBroadcasted) {
					broadcastInPerimeter();
					perimeterBroadcasted = true;
				}
				if (dronesInPerimeter > SUFFICIENT_DRONES_FOR_ATTACK) {
					broadcastAttack();
					logistics.moveTowards(hqLocEnemy);
				} else if (attack || roundNum > KAMIKAZE) {
					logistics.moveTowards(hqLocEnemy);
				} else {
					logistics.moveLaterally(hqLocEnemy);
				}
				return true;
			}
			rc.setIndicatorLine(myLocation, hqLocEnemy, 255, 0, 255);
			return logistics.moveTowards(hqLocEnemy);
		}
		return false;
	}

	private void tryToPickUp() throws GameActionException {
		RobotInfo[] robots = senseNearbyRobots();
		if (robots.length > 0) {
			// Pick up a first robot within range
			for (RobotInfo robot : robots) {
				Team robotTeam = robot.getTeam();
				if (rc.canPickUpUnit(robot.getID()) &&
						(robotTeam == opponent ||
						robotTeam == NEUTRAL && roundNum < STRATEGY_CHANGE_ROUND ||
						robotTeam == myTeam && robot.getType() == MINER &&
								(isOnWall(robot.location) || Math.random() < 0.3 || isNextTo(robot.location, hqLoc)
										&& roundNum > STRATEGY_CHANGE_ROUND) ||
						robotTeam == myTeam && robot.getType() == LANDSCAPER && isNextTo(robot.location, hqLoc))) {
					pickUp(robot, robotTeam);
				}
			}
		}
	}

	private void pickUp(RobotInfo robot, Team robotTeam) throws GameActionException {
		rc.pickUpUnit(robot.getID());
		directionBundle = logistics.getRandomDirectionBundle();
		carriedUnitTeam = robotTeam;
		carriedUnitType = robot.getType();
	}

	private void dropInWater() throws GameActionException {
		for (Direction dir : directions) {
			MapLocation locationForDrop = myLocation.add(dir);
			if (rc.canDropUnit(dir) && rc.canSenseLocation(locationForDrop) &&
					rc.senseFlooding(locationForDrop)) {
				drop(dir);
				return;
			}
		}
		MapLocation closestWater = null;
		for (MapLocation water : waterLocations) {
			if (closestWater == null || logistics.isCloser(water, closestWater)) {
				closestWater = water;
			}
		}
		if (closestWater != null) {
			logistics.moveTowards(closestWater);
		}
	}

	private void dropSomewhere() throws GameActionException {
		for (Direction dir : directions) {
			if (rc.canDropUnit(dir)) {
				drop(dir);
				return;
			}
		}
	}

	private void drop(Direction directionForDrop) throws GameActionException {
		rc.dropUnit(directionForDrop);
		directionBundle = logistics.getRandomDirectionBundle();
		carriedUnitTeam = null;
		carriedUnitType = null;
	}

	private void transportTeammate() throws GameActionException {
		if (carriedUnitType == LANDSCAPER) {
			transportOntoWall();
		} else if (carriedUnitType == MINER) {
			transportNearSoup();
		}
	}

	private void transportNearSoup() throws GameActionException {
		if (roundNum > DROP_ANYWHERE) {
			dropSomewhere();
		}
		MapLocation closestSoup = null;
		for (MapLocation soup : soupLocations) {
			if (rc.canSenseLocation(soup) && rc.senseFlooding(soup)) {
				continue;
			}
			if (closestSoup == null || logistics.isCloser(soup, closestSoup)) {
				closestSoup = soup;
			}
		}
		if (closestSoup == null) {
			locateSoup();
			move();
		} else if (closestSoup.isWithinDistanceSquared(myLocation, CLOSE_FOR_DROP) &&
				!rc.senseFlooding(myLocation)) {
			dropSomewhere();
		} else {
			rc.setIndicatorLine(myLocation, closestSoup, 255, 255, 0);
			if (logistics.moveTowards(closestSoup)) {
				timesStuck = 0;
			} else {
				timesStuck++; // Unable to reach the desired soup
			}
		}
	}

	private void transportNearEnemyHQ() throws GameActionException {
		if (hqLocEnemy == null) {
			move();
		} else if (rc.canSenseLocation(hqLocEnemy)) {
			dropSomewhere();
		} else {
			logistics.moveTowards(hqLocEnemy);
		}
	}

	private void transportOntoWall() throws GameActionException {
		if (hqLoc == null) {
			move();
		} else {
			for (MapLocation location : wallLocations) {
				if (isNextTo(location, myLocation)) {
					if (rc.canDropUnit(myLocation.directionTo(location))) {
						drop(myLocation.directionTo(location));
						return;
					}
				}
			}
		}
	}

	private void locateEnemyHQ() throws GameActionException {
		if (hqLocEnemy == null) {
			for (Building building : locatedBuildings.get(HQ)) {
				if (building.getTeam() == opponent) {
					hqLocEnemy = building.getLocation();
				}
			}
			for (RobotInfo robot : senseNearbyRobots()) {
				if (robot.type == HQ && robot.team == opponent) {
					hqLocEnemy = robot.getLocation();
					break;
				}
			}
		}
		if (hqLocEnemy != null && (!hqBroadcasted || roundNum % BROADCAST_ENEMY_HQ_AFTER_ROUNDS == 0)) {
			if (broadcastLocation(hqLocEnemy, HQ, opponent)) {
				hqBroadcasted = true;
			}
		}
	}

	protected void updateDroneInfo() throws GameActionException {
		for (Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
			int[] mess = tx.getMessage();
			if (mess[0] == TEAM_SECRET) {
				int msg = mess[1];
				if (msg == DRONE_ATTACK) {
					attack = true;
					return;
				} else if(msg == DRONE_IN_PERIMETER) {
					dronesInPerimeter++;
				}
			}
		}
	}

	protected boolean broadcastAttack() throws GameActionException {
		return broadcast(DRONE_ATTACK);
	}

	protected boolean broadcastInPerimeter() throws GameActionException {
		return broadcast(DRONE_IN_PERIMETER);
	}

	protected boolean broadcast(int type) throws GameActionException {
		int[] message = new int[7];
		message[0] = TEAM_SECRET;
		message[1] = type;
		if (rc.canSubmitTransaction(message, 1)) {
			rc.submitTransaction(message, 1);
			return true;
		}
		return false;
	}
}
