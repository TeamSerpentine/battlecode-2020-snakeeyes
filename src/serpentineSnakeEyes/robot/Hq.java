package serpentineSnakeEyes.robot;

import battlecode.common.GameActionException;

import static serpentineSnakeEyes.RobotPlayer.turnCount;
import static serpentineSnakeEyes.util.Constants.NUM_MINERS;
import static serpentineSnakeEyes.util.Constants.STRATEGY_CHANGE_ROUND;
import static battlecode.common.RobotType.HQ;
import static battlecode.common.RobotType.MINER;

public class Hq extends Robot {

	int minersSpawned = 0;
	boolean spawnSingleMiner;

	@Override
	public void runRobot() throws GameActionException {
		if (turnCount == 1) {
			broadcastLocation(myLocation, HQ);
		}
		shootEnemies();
		if (minersSpawned < NUM_MINERS && turnCount < STRATEGY_CHANGE_ROUND) {
			if (builder.buildAnywhere(MINER)) {
				minersSpawned++;
			}
		}
		if (turnCount == STRATEGY_CHANGE_ROUND) {
			spawnSingleMiner = true;
		}
		if (spawnSingleMiner) {
			if (builder.buildAnywhere(MINER)) {
				spawnSingleMiner = false;
			}
		}
	}
}
