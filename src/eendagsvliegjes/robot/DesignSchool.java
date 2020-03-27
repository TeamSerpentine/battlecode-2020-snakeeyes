package eendagsvliegjes.robot;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class DesignSchool extends AbstractRobot {

    // Constant strategy variables
    final int MAX_LANDSCAPERS = 1;  // Maximum amount of miners produced

    // Robot parameters
    int builtLandscapers = 0; // Amount of miners built by HQ

    public DesignSchool(RobotController rc) {
        super(rc);
    }

    @Override
    protected void run() throws GameActionException {
        buildLandscaper();
    }

    /**
     * Build landscapers provided maximum number has not been reached
     *
     * @return True on successful action execution
     * @throws GameActionException
     */
    private boolean buildLandscaper() throws GameActionException {
        if (builtLandscapers < MAX_LANDSCAPERS) {
            for (Direction dir : directions) {
                if (tryBuild(RobotType.LANDSCAPER, dir)) {
                    builtLandscapers += 1;
                    return true;
                }
            }
        }
        return false;
    }
}
