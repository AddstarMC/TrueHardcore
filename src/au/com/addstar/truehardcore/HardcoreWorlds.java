package au.com.addstar.truehardcore;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Location;
import org.bukkit.World;

public class HardcoreWorlds {
	private Map<String, HardcoreWorld> Worlds;

	public HardcoreWorlds() {
		Worlds = new HashMap<String, HardcoreWorld>();
	}
	
	static class HardcoreWorld {
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

	public HardcoreWorld NewWorld(String world) {
		HardcoreWorld hcw = new HardcoreWorld();
		if (hcw != null) {
			AddWorld(world, hcw);
		}
		return hcw;
	}

	public boolean AddWorld(String world, HardcoreWorld hcw) {
		Worlds.put(world, hcw);
		return true;
	}

	public HardcoreWorld Get(String world) {
		String key = StringUtils.replace(world, "_nether", "");
		if (Worlds.containsKey(key)) {
			HardcoreWorld hcw = Worlds.get(key);
			return hcw;
		}
		return null;
	}

	public boolean Contains(String world) {
		String key = StringUtils.replace(world, "_nether", "");
		return Worlds.containsKey(key);
	}
	
	public Map<String, HardcoreWorld> AllRecords() {
		return Worlds;
	}
}