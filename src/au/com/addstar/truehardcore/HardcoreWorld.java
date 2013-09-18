package au.com.addstar.truehardcore;

import org.bukkit.Location;
import org.bukkit.World;

public class HardcoreWorld {
	public HardcoreWorld() {
		// Nothing needed here yet
	}
	
	private World world;
	private String greeting;
	private Integer bantime;
	private Integer spawndistance;
	private Integer spawnprotection;
	private Location exitpos;

	public World getWorld() {
		return world;
	}
	public void setWorld(World world) {
		this.world = world;
	}

	public String getGreeting() {
		return greeting;
	}

	public void setGreeting(String greeting) {
		this.greeting = greeting;
	}

	public Integer getBantime() {
		return bantime;
	}

	public void setBantime(Integer bantime) {
		this.bantime = bantime;
	}

	public Integer getSpawnDistance() {
		return spawndistance;
	}

	public void setSpawnDistance(Integer spawndistance) {
		this.spawndistance = spawndistance;
	}

	public Integer getSpawnProtection() {
		return spawnprotection;
	}

	public void setSpawnProtection(Integer spawnprotection) {
		this.spawnprotection = spawnprotection;
	}
	public Location getExitPos() {
		return exitpos;
	}
	public void setExitPos(Location exitpos) {
		this.exitpos = exitpos;
	}
}
