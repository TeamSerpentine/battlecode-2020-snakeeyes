package serpentineSnakeEyes.robot;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Stream;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.Transaction;
import serpentineSnakeEyes.builder.RobotBuilder;
import serpentineSnakeEyes.building.Building;
import serpentineSnakeEyes.logistics.RobotLogistics;
import serpentineSnakeEyes.util.CachedSupplier;

import static battlecode.common.RobotType.DELIVERY_DRONE;
import static battlecode.common.RobotType.DESIGN_SCHOOL;
import static battlecode.common.RobotType.FULFILLMENT_CENTER;
import static battlecode.common.RobotType.HQ;
import static battlecode.common.RobotType.NET_GUN;
import static battlecode.common.RobotType.REFINERY;
import static battlecode.common.RobotType.VAPORATOR;
import static serpentineSnakeEyes.RobotPlayer.directions;
import static serpentineSnakeEyes.util.Constants.DISTANCE_OF_WALL_FROM_HQ;
import static serpentineSnakeEyes.util.Constants.PREVIOUS_STORED_LOCATIONS_SIZE;
import static serpentineSnakeEyes.util.Constants.SOUP_TYPE;
import static serpentineSnakeEyes.util.Constants.TEAM_SECRET;
import static serpentineSnakeEyes.util.Constants.WATER_TYPE;

public abstract strictfp class Robot {

	public static MapLocation hqLoc;
	public static MapLocation hqLocEnemy;
	public static RobotController rc;
	public static Set<MapLocation> wallLocations;
	public static RobotType[] buildingTypes = { HQ, REFINERY, VAPORATOR, DESIGN_SCHOOL, FULFILLMENT_CENTER, NET_GUN };
	public static Map<RobotType, Set<Building>> locatedBuildings;
	public static Set<MapLocation> soupLocations = new HashSet<>();
	public static Set<MapLocation> waterLocations = new HashSet<>();
	public static Queue<MapLocation> previousLocations = new LinkedList<>();

	static {
		locatedBuildings = new HashMap<>();
		Arrays.stream(buildingTypes).forEach(type -> locatedBuildings.put(type, new HashSet<>()));
		locatedBuildings.put(DELIVERY_DRONE, new HashSet<>());
	}

	// Caches
	private final HashMap<Integer, Transaction[]> blocksCache = new HashMap<>();
	private final CachedSupplier<RobotInfo[]> senseNearbyRobotsCache =
			new CachedSupplier<>(() -> rc.senseNearbyRobots());
	private final CachedSupplier<MapLocation[]> senseNearbySoupCache =
			new CachedSupplier<>(() -> rc.senseNearbySoup());

	protected Team myTeam;
	protected Team opponent;
	protected RobotType myType;
	protected MapLocation myLocation;
	protected RobotBuilder builder;
	protected RobotLogistics logistics;
	protected Direction[] directionBundle;
	protected int timesStuck;
	protected int roundNum;
	protected int timesBroadcasted;
	protected boolean justSpawned = true;

	protected abstract void runRobot() throws GameActionException;

	public void run() {
		try {
			if (hqLoc == null) {
				hqLoc = locateBuilding(HQ);
			}
			roundNum = rc.getRoundNum();
			myLocation = rc.getLocation();
			if (Arrays.asList(buildingTypes).contains(myType) || myType == DELIVERY_DRONE) {
				broadcastCreation();
			}
			if (wallLocations == null && hqLoc != null) {
				setWallLocations();
			}
			if (previousLocations.size() > PREVIOUS_STORED_LOCATIONS_SIZE) {
				previousLocations.remove();
			}
			previousLocations.add(myLocation);
			runRobot();
		} catch (GameActionException e) {
			System.out.println(rc.getType() + " Exception: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public boolean isOnWall(MapLocation location) {
		return wallLocations.contains(location);
	}

	public boolean isNextTo(MapLocation location1, MapLocation location2) {
		return Math.abs(location1.x - location2.x) <= 1 && Math.abs(location1.y - location2.y) <= 1;
	}

	public void setRc(RobotController rc) {
		this.rc = rc;
		this.myType = rc.getType();
		this.myTeam = rc.getTeam();
		this.opponent = rc.getTeam().opponent();
		this.builder = new RobotBuilder(rc);
		this.logistics = new RobotLogistics(rc);
	}

	private void broadcastCreation() throws GameActionException {
		if (timesBroadcasted < 2) { // Broadcast twice in case some unit reaches bytecode limit on the same round
			if (broadcastLocation(myLocation, myType)) {
				timesBroadcasted++;
			}
		}
	}

	protected boolean broadcastLocation(MapLocation location, RobotType type) throws GameActionException {
		return broadcastLocation(location, type.ordinal());
	}

	protected boolean broadcastLocation(MapLocation location, int type) throws GameActionException {
		return broadcastLocation(location, type, myTeam);
	}

	protected boolean broadcastLocation(MapLocation location, RobotType type, Team team) throws GameActionException {
		return broadcastLocation(location, type.ordinal(), team);
	}

	protected boolean broadcastLocation(MapLocation loc, int type, Team team) throws GameActionException {
		int[] message = new int[7];
		message[0] = TEAM_SECRET;
		message[1] = type;
		message[2] = loc.x; // x coord of robot
		message[3] = loc.y; // y coord of robot
		message[4] = team == myTeam ? 0 : 1; // 0 for my team, 1 fo opponent
		if (rc.canSubmitTransaction(message, 1)) {
			rc.submitTransaction(message, 1);
			return true;
		}
		return false;
	}

	protected void checkNewBuildings(RobotType type) throws GameActionException {
		for (Building building : getMapLocationFromBlockChain(type.ordinal())) {
			addNewBuildingLocation(building, type);
		}
	}

	protected void addNewBuildingLocation(Building building, RobotType type) throws GameActionException {
		Set<Building> buildings = locatedBuildings.get(type);
		buildings.add(building);
		locatedBuildings.replace(type, buildings);
	}

	protected void updateSoupLocations() throws GameActionException {
		updateLocations(SOUP_TYPE, soupLocations);
	}

	protected void updateWaterLocations() throws GameActionException {
		updateLocations(WATER_TYPE, waterLocations);
	}

	protected void updateLocations(int type, Collection<MapLocation> locations) throws GameActionException {
		for (Building soup : getMapLocationFromBlockChain(type)) {
			locations.add(soup.getLocation());
		}
	}

	protected void updateBuildings() throws GameActionException {
		for (RobotType building : buildingTypes) {
			checkNewBuildings(building);
		}
	}

	protected void readAllBuildingLocations() throws GameActionException {
		for (RobotType building : buildingTypes) {
			locateBuilding(building);
		}
	}

	protected Set<Building> getMapLocationFromBlockChain(Integer type) throws GameActionException {
		Set<Building> buildings = new HashSet<>();
		for (Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
			int[] mess = tx.getMessage();
			if (mess[0] == TEAM_SECRET && mess[1] == type) {
				MapLocation newLocation = new MapLocation(mess[2], mess[3]);
				Team team = mess[4] == 0 ? myTeam : opponent;
				buildings.add(new Building(team, newLocation));

			}
		}
		return buildings;
	}

	protected MapLocation locateBuilding(RobotType building) throws GameActionException {
		return locateBuilding(building, myTeam);
	}

	protected MapLocation locateBuilding(RobotType building, Team team) throws GameActionException {
		for (RobotInfo robot : senseNearbyRobots()) {
			if (robot.type == building && robot.team == team) {
				return robot.getLocation();
			}
		}
		return getBuildingFromBlockchain(building, team);
	}

	private MapLocation getBuildingFromBlockchain(RobotType building, Team team) throws GameActionException {
		return getFromBlockchain(building.ordinal(), team);
	}

	private MapLocation getFromBlockchain(int type, Team team) throws GameActionException {
		for (int roundNum = 1; roundNum < rc.getRoundNum(); roundNum++) {
			for (Transaction transaction : getBlock(roundNum)) {
				int[] msg = transaction.getMessage();
				if (msg[0] == TEAM_SECRET && msg[1] == type && msg[4] == (myTeam == team ? 0 : 1)) {
					return new MapLocation(msg[2], msg[3]);
				}
			}
		}
		return null;
	}

	private void setWallLocations() {
		wallLocations = new HashSet<>();
		addAllDirections(hqLoc)
				.flatMap(this::addAllDirections)
//				.flatMap(this::addAllDirections) for radius of tiles 3
				.filter(loc -> hqLoc.distanceSquaredTo(loc) >= DISTANCE_OF_WALL_FROM_HQ)
				.forEach(wallLocations::add);
	}

	private Stream<MapLocation> addAllDirections(MapLocation location) {
		return Arrays.stream(directions).map(dir -> location.add(dir));
	}

	protected void move() throws GameActionException {
		if (directionBundle == null || logistics.onTheEdgeOfTheMap()) {
			directionBundle = logistics.getRandomDirectionBundle();
			timesStuck = 0;
			if (!logistics.moveSomewhere(directionBundle)) {
				timesStuck++;
			}
		} else {
			if (logistics.moveSomewhere(directionBundle)) {
				timesStuck = 0;
			} else {
				timesStuck++;
				if (timesStuck > 10) {
					directionBundle = logistics.getRandomDirectionBundle();
					timesStuck = 0;
				}
			}
		}
		logistics.moveAnywhere();
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
		}
		return false;
	}

	protected boolean soupLeftForRefinery(RobotType building) {
		return !locatedBuildings.get(REFINERY).isEmpty() || rc.getTeamSoup() > REFINERY.cost + building.cost;
	}

	protected boolean soupLeftForDrone(RobotType building) {
		return rc.getTeamSoup() > DELIVERY_DRONE.cost + building.cost;
	}

	protected boolean isSpawnBlocked(RobotType buildingType) throws GameActionException {
		Set<Building> buildings = locatedBuildings.get(buildingType);
		for (Building building : buildings) {
			MapLocation location = building.getLocation();
			for (Direction dir : directions) {
				MapLocation spawnLocation = location.add(dir);
				if (!logistics.isWall(spawnLocation) && !rc.isLocationOccupied(spawnLocation)) {
					return false;
				}
			}
		}
		return true;
	}

	protected void shootEnemies() throws GameActionException {
		for (RobotInfo robot : senseNearbyRobots()) {
			if (rc.canShootUnit(robot.getID()) && robot.getTeam() != myTeam) {
				rc.shootUnit(robot.getID());
			}
		}
	}

	protected long getNumberOfUnitsOfTypeNearby(RobotType robotType) throws GameActionException {
		int count = 0;
		for (RobotInfo robot : senseNearbyRobots()) {
			if (robot.type == robotType) {
				count++;
			}
		}
		return count;
	}

	protected void suicide() {
		throw new RuntimeException("My time has come");
	}

	// ##### Caching #####

	protected Transaction[] getBlock(int roundNumber) throws GameActionException {
		if (! blocksCache.containsKey(roundNumber)) {
			blocksCache.put(roundNumber, rc.getBlock(roundNumber));
		}
		return blocksCache.get(roundNumber);
	}

	protected RobotInfo[] senseNearbyRobots() throws GameActionException {
		return senseNearbyRobotsCache.get();
	}

	public MapLocation[] senseNearbySoup() throws GameActionException {
		return senseNearbySoupCache.get();
	}
}
