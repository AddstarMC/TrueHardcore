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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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
		} catch (SQLException e) {
			plugin.Warn("Unable to open database!");
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			plugin.Warn("Unable to find a suitable MySQL driver!");
			e.printStackTrace();
		}
		return IsConnected;
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
			plugin.Debug("SQL Query: " + query);
			for (int x=0; x < params.length; x++) {
				plugin.Debug("Param " + (x+1) + ": "+ params[x]);
				ps.setString(x+1, params[x]);
			}
			return ps.executeQuery();
		} catch (SQLException e) {
			plugin.Warn("Prepared query execution failed!");
			plugin.Log("SQL: " + query);
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
		PreparedStatement ps;
		
		if (!IsConnected) { return -1; }
		
		try {
			ps = Conn.prepareStatement(query);
			// Construct PreparedStatement by adding all supplied params to the query
			plugin.Debug("SQL Update: " + query);
			for (int x=0; x < params.length; x++) {
				plugin.Debug("Param " + (x+1) + ": "+ params[x]);
				ps.setString(x+1, params[x]);
			}
			return ps.executeUpdate();
		} catch (SQLException e) {
			plugin.Warn("Prepared query execution failed!");
			plugin.Log("SQL: " + query);
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
