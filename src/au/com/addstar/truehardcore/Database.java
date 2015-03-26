package au.com.addstar.truehardcore;
/*
* TrueHardcore
* Copyright (C) 2013 add5tar <copyright at addstar dot com dot au>
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>
*/

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class Database {
	public TrueHardcore plugin;
	public String DBFilename;
	public Connection Conn;
	public boolean IsConnected = false;
	
	public Database(TrueHardcore instance) {
		plugin = instance;
		OpenDatabase();
	}

	public boolean OpenDatabase() {
		try {
			Class.forName ("com.mysql.jdbc.Driver");
			if (plugin.DBHost == null) { plugin.Log("Host is null"); }
			if (plugin.DBPort == null) { plugin.Log("Port is null"); }
			if (plugin.DBName == null) { plugin.Log("Name is null"); }
			String url = "jdbc:mysql://" + plugin.DBHost + ":" + plugin.DBPort + "/" + plugin.DBName;
			Conn = DriverManager.getConnection(url, plugin.DBUser, plugin.DBPass);
			
			IsConnected = true;
			
			tryConvert();
		} catch (SQLException e) {
			plugin.Warn("Unable to open database!");
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			plugin.Warn("Unable to find a suitable MySQL driver!");
			e.printStackTrace();
		}
		return IsConnected;
	}
	
	private void tryConvert() throws SQLException {
		
		// Schema check for new id column
		Statement st = Conn.createStatement();
		try {
			st.executeQuery("SELECT `id` from `players`");
			return;
		} catch (SQLException e) {
			// Not present, do upgrade
		}
		
		Logger log = TrueHardcore.instance.getLogger();
		
		log.info("A conversion to UUIDs is required.");
		
		// First build a name map
		log.info("- Building name cache");
		HashMap<String, UUID> lookup = new HashMap<String, UUID>();
		for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
			lookup.put(player.getName().toLowerCase(), player.getUniqueId());
		}
		
		// Next update the schema for the table
		log.info("- Updating db schema");
		st.executeUpdate("ALTER TABLE `players` ADD COLUMN (`id` CHAR(36));");
		
		// Now query for all entries for an inplace update
		log.info("- Converting players ...");
		
		PreparedStatement update = Conn.prepareStatement("UPDATE `players` SET `id`= ? WHERE `player`=? AND `world`=?;");
		
		int pending = 0;
		int count = 0;
		int failCount = 0;
		long lastAnnounce = System.currentTimeMillis();
		
		ResultSet rs = st.executeQuery("SELECT `player`,`world` from `players`");
		while (rs.next()) {
			String name = rs.getString("player");
			String world = rs.getString("world");
			UUID id = lookup.get(name.toLowerCase());
			if (id != null) {
				update.setString(1, id.toString());
				update.setString(2, name);
				update.setString(3, world);
				update.addBatch();
				++pending;
			} else {
				update.setString(1, UUID.nameUUIDFromBytes(name.getBytes()).toString());
				update.setString(2, name);
				update.setString(3, world);
				update.addBatch();
				++pending;
				++failCount;
			}
			
			++count;
			
			if (pending > 30) {
				update.executeBatch();
				pending = 0;
			}
			
			if (System.currentTimeMillis() - lastAnnounce > 4000) {
				lastAnnounce = System.currentTimeMillis();
				log.info("  * Processed " + count + " entries. " + failCount + " failed");
			}
		}
		
		if (pending > 0) {
			update.executeBatch();
			pending = 0;
		}
		
		log.info("  Finished converting " + count + " entries. " + failCount + " failed");
		
		rs.close();
		update.close();
		
		// Finish the schema change
		st.executeUpdate("ALTER TABLE `players` DROP PRIMARY KEY;");
		st.executeUpdate("ALTER TABLE `players` ADD PRIMARY KEY (`id`, `world`);");
		
		// Now convert the whitelist
		log.info("- Converting whitelist");
		st.executeUpdate("ALTER TABLE `whitelist` ADD COLUMN (`id` CHAR(36));");
		update = Conn.prepareStatement("UPDATE `whitelist` SET `id`= ? WHERE `player`=?;");
		
		rs = st.executeQuery("SELECT `player` from `whitelist`");
		while (rs.next()) {
			String name = rs.getString("player");
			UUID id = lookup.get(name.toLowerCase());
			if (id != null) {
				update.setString(1, id.toString());
				update.setString(2, name);
				update.addBatch();
				++pending;
			} else {
				++failCount;
				update.setString(1, UUID.nameUUIDFromBytes(name.getBytes()).toString());
				update.setString(2, name);
				update.addBatch();
				++pending;
			}
			
			++count;
			
			if (pending > 30) {
				update.executeBatch();
				pending = 0;
			}
			
			if (System.currentTimeMillis() - lastAnnounce > 4000) {
				lastAnnounce = System.currentTimeMillis();
				log.info("  * Processed " + count + " entries. " + failCount + " failed");
			}
		}
		
		if (pending > 0) {
			update.executeBatch();
			pending = 0;
		}
		
		log.info("  Finished converting " + count + " entries. " + failCount + " failed");
		
		st.executeUpdate("ALTER TABLE `whitelist` DROP PRIMARY KEY;");
		st.executeUpdate("ALTER TABLE `whitelist` ADD PRIMARY KEY (`id`);");
		rs.close();
		
		log.info("- Conversion complete");
	}
	
	public ResultSet ExecuteQuery(String query) {
		Statement st;
		
		if (!IsConnected) { return null; }
		
		try {
			st = Conn.createStatement();
			plugin.Debug("SQL Query: " + query);
			return st.executeQuery(query);
		} catch (SQLException e) {
			plugin.Warn("Query execution failed!");
			plugin.Log("SQL: " + query);
			e.printStackTrace();
			return null;
		}
	}
	
	public ResultSet PreparedQuery(String query, String[] params) {
		PreparedStatement ps;
		
		if (!IsConnected) { return null; }
		
		try {
			ps = Conn.prepareStatement(query);
			// Construct PreparedStatement by adding all supplied params to the query
			plugin.DebugLog("SQL Query: " + query);
			if (params != null) {
				for (int x=0; x < params.length; x++) {
					plugin.DebugLog("Param " + (x+1) + ": "+ params[x]);
					ps.setString(x+1, params[x]);
				}
			}
			return ps.executeQuery();
		} catch (SQLException e) {
			plugin.Warn("Prepared query execution failed!");
			plugin.DebugLog("SQL: " + query);
			e.printStackTrace();
			return null;
		}
	}
	
	public int ExecuteUpdate(String query) {
		Statement st;
		
		if (!IsConnected) { return -1; }
		
		try {
			st = Conn.createStatement();
			plugin.Debug("SQL Update: " + query);
			return st.executeUpdate(query);
		} catch (SQLException e) {
			plugin.Warn("Query execution failed!");
			plugin.Log("SQL: " + query);
			e.printStackTrace();
			return -1;
		}
	}
	
	public int PreparedUpdate(String query, String[] params) {
		return PreparedUpdate(query, params, false);
	}

	public int PreparedUpdate(String query, String[] params, boolean silent) {
		PreparedStatement ps;
		
		if (!IsConnected) { return -1; }
		
		try {
			ps = Conn.prepareStatement(query);
			// Construct PreparedStatement by adding all supplied params to the query
			if (!silent) plugin.DebugLog("SQL Update: " + query);
			for (int x=0; x < params.length; x++) {
				if (!silent) plugin.DebugLog("Param " + (x+1) + ": "+ params[x]);
				ps.setString(x+1, params[x]);
			}
			return ps.executeUpdate();
		} catch (SQLException e) {
			plugin.Warn("Prepared query execution failed!");
			plugin.DebugLog("SQL: " + query);
			e.printStackTrace();
			return -1;
		}
	}
	
	public boolean CloseDatabase() {
		try {
			Conn.close();
		} catch (SQLException e) {
			plugin.Warn("Close database failed!");
			e.printStackTrace();
		}
		return true;
	}
	
	public boolean TableExists(Connection conn, String tname) {
		DatabaseMetaData md;
		ResultSet rs;
		
		try {
			md = conn.getMetaData();
		} catch (SQLException e) {
			// This shouldn't really happen
			plugin.Warn("Unable to read DatabaseMetaData from DB connection!");
			e.printStackTrace();
			return false;
		}

		try {
			plugin.Debug("Getting list of database tables");
			rs = md.getTables(null, null, tname, null);
		} catch (SQLException e) {
			// This shouldn't really happen
			plugin.Warn("Unable to getTables from DatabaseMetaData!");
			e.printStackTrace();
			return false;
		}
		
		try {
			if (rs.next()) {
				// Table exists 
				return true;
			}
		} catch (SQLException e) {
			// This shouldn't really happen
			plugin.Warn("Unable to iterate table resultSet!");
			e.printStackTrace();
		}
		return false;
	}

	public boolean ColumnExists(Connection conn, String tname, String cname) {
		DatabaseMetaData md;
		ResultSet rs;
		
		try {
			md = conn.getMetaData();
		} catch (SQLException e) {
			// This shouldn't really happen
			plugin.Warn("Unable to read DatabaseMetaData from DB connection!");
			e.printStackTrace();
			return false;
		}

		try {
			plugin.Debug("Getting list of table columns");
			rs = md.getColumns(null, null, tname, cname);
		} catch (SQLException e) {
			// This shouldn't really happen
			plugin.Warn("Unable to getColumns from DatabaseMetaData!");
			e.printStackTrace();
			return false;
		}
		
		try {
			if (rs.next()) {
				// Table exists 
				return true;
			}
		} catch (SQLException e) {
			// This shouldn't really happen
			plugin.Warn("Unable to iterate column resultSet!");
			e.printStackTrace();
		}
		return false;
	}
}
