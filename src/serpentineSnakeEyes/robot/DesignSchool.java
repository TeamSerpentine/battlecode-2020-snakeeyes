package serpentineSnakeEyes.robot;

import battlecode.common.GameActionException;

import static serpentineSnakeEyes.util.Constants.CREATE_MIN_LANDSCAPERS_PER_DESIGN_SCHOOL;
import static serpentineSnakeEyes.util.Constants.STRATEGY_CHANGE_ROUND;
import static battlecode.common.RobotType.FULFILLMENT_CENTER;
import static battlecode.common.RobotType.LANDSCAPER;

public class DesignSchool extends Robot {

	private int landscapersBuilt = 0;

	@Override
	public void runRobot() throws GameActionException {
		updateBuildings();
		if (fulfillmentCenterBuilt() && enoughSoup() && roomForSpawning() && spawnSuccessful()) {
			landscapersBuilt++;
			System.out.println("Spawned new " + LANDSCAPER.name());
		}
	}

	private boolean spawnSuccessful() throws GameActionException {
		return builder.buildAnywhere(LANDSCAPER);
	}

	private boolean enoughSoup() {
		return (landscapersBuilt < CREATE_MIN_LANDSCAPERS_PER_DESIGN_SCHOOL || roundNum < STRATEGY_CHANGE_ROUND)
				&& soupLeftForRefinery(LANDSCAPER);
	}

	private boolean fulfillmentCenterBuilt() {
		return roundNum > 150 || !locatedBuildings.get(FULFILLMENT_CENTER).isEmpty();
	}

	private boolean roomForSpawning() throws GameActionException {
		return roundNum < STRATEGY_CHANGE_ROUND || !isSpawnBlocked(FULFILLMENT_CENTER);
	}
}
