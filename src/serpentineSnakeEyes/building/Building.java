package serpentineSnakeEyes.building;

import battlecode.common.MapLocation;
import battlecode.common.Team;

/**
 * Entity for keeping team and location within one instance for easy access
 */
public class Building {
	private Team team;
	private MapLocation location;

	public Building(Team team, MapLocation location) {
		this.team = team;
		this.location = location;
	}

	public Team getTeam() {
		return team;
	}

	public void setTeam(Team team) {
		this.team = team;
	}

	public MapLocation getLocation() {
		return location;
	}

	public void setLocation(MapLocation location) {
		this.location = location;
	}
}
