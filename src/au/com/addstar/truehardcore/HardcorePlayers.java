package au.com.addstar.truehardcore;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class HardcorePlayers {

	private Map<String, HardcorePlayer> Players;
	public HardcorePlayers() {
		Players = new HashMap<String, HardcorePlayer>();
	}
	
	public static enum PlayerState {
		NOT_IN_GAME,
		IN_GAME,
		ALIVE,
		DEAD;
	}
	
	static class HardcorePlayer {
		private String PlayerName;
		private String World;
		private Location SpawnPos;
		private Location LastPos;
		private Date LastJoin;
		private Date LastQuit;
		private Date GameStart;
		private Date GameEnd;
		private Integer GameTime = 0;
		private Integer Level = 0;
		private float Exp = 0;
		private Integer Score = 0;
		private Integer TopScore = 0;
		private PlayerState State = PlayerState.NOT_IN_GAME;
		private String DeathMsg;
		private Location DeathPos;
		private Integer Deaths = 0;
		private Integer CowKills=0, PigKills=0, SheepKills=0, ChickenKills=0, CreeperKills=0;
		private Integer ZombieKills=0, SkeletonKills=0, SpiderKills=0, EnderKills=0, SlimeKills=0, MooshKills=0;
		private Integer OtherKills=0, PlayerKills=0;
		private boolean Modified = false;
		private boolean LoadDataOnly = false;
		private boolean GodMode = false;
		
		public boolean isLoadDataOnly() {
			return LoadDataOnly;
		}
		public void setLoadDataOnly(boolean loadDataOnly) {
			LoadDataOnly = loadDataOnly;
		}
		
		public String getPlayerName() {
			return PlayerName;
		}
		public void setPlayerName(String playerName) {
			PlayerName = playerName;
			if (LoadDataOnly) { return; }
			setModified(true);
		}
		public String getWorld() {
			return World;
		}
		public void setWorld(String world) {
			World = world;
			if (LoadDataOnly) { return; }
			setModified(true);
		}
		public Location getSpawnPos() {
			return SpawnPos;
		}
		public void setSpawnPos(Location spawnPos) {
			SpawnPos = spawnPos;
			if (LoadDataOnly) { return; }
			setModified(true);
		}
		public Location getLastPos() {
			return LastPos;
		}
		public void setLastPos(Location lastPos) {
			LastPos = lastPos;
			if (LoadDataOnly) { return; }
			setModified(true);
		}
		public Date getLastJoin() {
			return LastJoin;
		}
		public void setLastJoin(Date lastJoin) {
			LastJoin = lastJoin;
			if (LoadDataOnly) { return; }
			setModified(true);
		}
		public Date getLastQuit() {
			return LastQuit;
		}
		public void setLastQuit(Date lastQuit) {
			LastQuit = lastQuit;
			if (LoadDataOnly) { return; }
			setModified(true);
		}
		public Date getGameStart() {
			return GameStart;
		}
		public void setGameStart(Date gameStart) {
			GameStart = gameStart;
			if (LoadDataOnly) { return; }
			setModified(true);
		}
		public Date getGameEnd() {
			return GameEnd;
		}
		public void setGameEnd(Date gameEnd) {
			GameEnd = gameEnd;
			if (LoadDataOnly) { return; }
			setModified(true);
		}
		public Integer getGameTime() {
			return GameTime;
		}
		public void setGameTime(Integer gameTime) {
			GameTime = gameTime;
			if (LoadDataOnly) { return; }
			setModified(true);
		}
		public Integer getLevel() {
			return Level;
		}
		public void setLevel(Integer level) {
			Level = level;
			if (LoadDataOnly) { return; }
			setModified(true);
		}
		public float getExp() {
			return Exp;
		}
		public void setExp(float exp) {
			Exp = exp;
			if (LoadDataOnly) { return; }
			setModified(true);
		}
		public Integer getScore() {
			return Score;
		}
		public void setScore(Integer score) {
			Score = score;
			if (LoadDataOnly) { return; }
			setModified(true);
		}
		public Integer getTopScore() {
			return TopScore;
		}
		public void setTopScore(Integer topScore) {
			TopScore = topScore;
			if (LoadDataOnly) { return; }
			setModified(true);
		}
		public PlayerState getState() {
			return State;
		}
		public void setState(PlayerState state) {
			if (!LoadDataOnly) {
				if ((state == PlayerState.DEAD) && (State != PlayerState.DEAD)) {
					// Player has died
					setGameEnd(new Date());
					setLastQuit(new Date());
				}
				else if ((state == PlayerState.IN_GAME) && (State != PlayerState.IN_GAME)) {
					// Joining a game
					if (State != PlayerState.ALIVE) {
						// Starting a new game
						setGameStart(new Date());
					}
					// Always set the join date when transitioning -> IN_GAME
					setLastJoin(new Date());
				}
				else if ((State == PlayerState.IN_GAME) && (state != PlayerState.IN_GAME)) {
					// Leaving a game (for any reason)
					setLastQuit(new Date());
				}
			}
			State = state;
		}
		public String getDeathMsg() {
			return DeathMsg;
		}
		public void setDeathMsg(String deathMsg) {
			DeathMsg = deathMsg;
			if (LoadDataOnly) { return; }
			setModified(true);
		}
		public Location getDeathPos() {
			return DeathPos;
		}
		public void setDeathPos(Location deathPos) {
			DeathPos = deathPos;
			if (LoadDataOnly) { return; }
			setModified(true);
		}
		public Integer getDeaths() {
			return Deaths;
		}
		public void setDeaths(Integer deaths) {
			Deaths = deaths;
			if (LoadDataOnly) { return; }
			setModified(true);
		}

		public Integer getCowKills() {
			return CowKills;
		}
		public void setCowKills(Integer cowKills) {
			CowKills = cowKills;
			if (LoadDataOnly) { return; }
			setModified(true);
		}
		public Integer getPigKills() {
			return PigKills;
		}
		public void setPigKills(Integer pigKills) {
			PigKills = pigKills;
			if (LoadDataOnly) { return; }
			setModified(true);
		}
		public Integer getSheepKills() {
			return SheepKills;
		}
		public void setSheepKills(Integer sheepKills) {
			SheepKills = sheepKills;
			if (LoadDataOnly) { return; }
			setModified(true);
		}
		public Integer getChickenKills() {
			return ChickenKills;
		}
		public void setChickenKills(Integer chickenKills) {
			ChickenKills = chickenKills;
			if (LoadDataOnly) { return; }
			setModified(true);
		}
		public Integer getCreeperKills() {
			return CreeperKills;
		}
		public void setCreeperKills(Integer creeperKills) {
			CreeperKills = creeperKills;
			if (LoadDataOnly) { return; }
			setModified(true);
		}
		public Integer getZombieKills() {
			return ZombieKills;
		}
		public void setZombieKills(Integer zombieKills) {
			ZombieKills = zombieKills;
			if (LoadDataOnly) { return; }
			setModified(true);
		}
		public Integer getSkeletonKills() {
			return SkeletonKills;
		}
		public void setSkeletonKills(Integer skeletonKills) {
			SkeletonKills = skeletonKills;
			if (LoadDataOnly) { return; }
			setModified(true);
		}
		public Integer getSpiderKills() {
			return SpiderKills;
		}
		public void setSpiderKills(Integer spiderKills) {
			SpiderKills = spiderKills;
			if (LoadDataOnly) { return; }
			setModified(true);
		}
		public Integer getEnderKills() {
			return EnderKills;
		}
		public void setEnderKills(Integer enderKills) {
			EnderKills = enderKills;
			if (LoadDataOnly) { return; }
			setModified(true);
		}
		public Integer getSlimeKills() {
			return SlimeKills;
		}
		public void setSlimeKills(Integer slimeKills) {
			SlimeKills = slimeKills;
			if (LoadDataOnly) { return; }
			setModified(true);
		}
		public Integer getMooshKills() {
			return MooshKills;
		}
		public void setMooshKills(Integer mooshKills) {
			MooshKills = mooshKills;
			if (LoadDataOnly) { return; }
			setModified(true);
		}
		public Integer getPlayerKills() {
			return PlayerKills;
		}
		public void setPlayerKills(Integer playerKills) {
			PlayerKills = playerKills;
			if (LoadDataOnly) { return; }
			setModified(true);
		}
		public Integer getOtherKills() {
			return OtherKills;
		}
		public void setOtherKills(Integer otherKills) {
			OtherKills = otherKills;
			if (LoadDataOnly) { return; }
			setModified(true);
		}

		
		public void updatePlayer(Player player) {
			setModified(true);
			setExp(player.getExp());
			setLastPos(player.getLocation());
			setScore(player.getTotalExperience());
			setLevel(player.getLevel());
		}
		
		public Integer TimeDiff(Date d1, Date d2) {
			//Date d1 = getLastJoin();
			//Date d2 = getLastQuit();

			//TrueHardcore.instance.DebugLog("DATES: " + d1 + " / " + d2);
			
			// Only calculate game time if quit is after join
			if (d2.after(d1)) {
				int diff = (int) ((d2.getTime() - d1.getTime()) / 1000);
				//TrueHardcore.instance.DebugLog("DIFF: " + diff);
				//TrueHardcore.instance.DebugLog("NEW : " + (getGameTime() + diff));
				return diff;
			}
			return null;
		}
		public void calcGameTime() {
			Integer diff = TimeDiff(getLastJoin(), getLastQuit());
			if (diff != null) {
				setGameTime(getGameTime() + diff);
			}
		}
		
		public boolean isModified() {
			return Modified;
		}
		public void setModified(boolean modified) {
			Modified = modified;
		}
		public boolean isGodMode() {
			return GodMode;
		}
		public void setGodMode(boolean godMode) {
			GodMode = godMode;
		}
	}
	
	public HardcorePlayer NewPlayer(String world, String name) {
		HardcorePlayer hcp = new HardcorePlayer();
		if (hcp != null) {
			TrueHardcore.instance.Debug("Creating new player record: " + world + "/" + name);
			hcp.LoadDataOnly = true;
			hcp.setPlayerName(name);
			hcp.setWorld(world);
			hcp.LoadDataOnly = false;
			AddPlayer(world, name, hcp);
		}
		return hcp;
	}

	public HardcorePlayer Get(String world, String name) {
		String key = StringUtils.replace(world, "_nether", "") + "/" + name;
		if (Players.containsKey(key)) {
			HardcorePlayer hcp = Players.get(key);
			return hcp;
		}
		return null;
	}
	
	public HardcorePlayer Get(World world, Player player) {
		if ((world == null) || (player == null)) { return null; }
		return Get(world.getName(), player.getName());
	}

	public HardcorePlayer Get(Player player) {
		if (player == null) { return null; }
		return Get(player.getLocation().getWorld().getName(), player.getName());
	}
	
	public HardcorePlayer Get(String key) {
		if (Players.containsKey(key)) { return null; }
		return Players.get(StringUtils.replace(key, "_nether", ""));
	}
	
	public boolean AddPlayer(String world, String name, HardcorePlayer hcp) {
		String key = world + "/" + name;
		Players.put(key, hcp);
		return true;
	}
	
	public boolean IsHardcorePlayer(Player player) {
		return (Get(player) != null);
	}
	
	public void Clear() {
		Players.clear();
	}
	
	public Map<String, HardcorePlayer> AllRecords() {
		return Players; 
	}
}
