/*
 * TrueHardcore
 * Copyright (C) 2013 - 2020  AddstarMC <copyright at addstar dot com dot au>
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
 *
 */

package au.com.addstar.truehardcore.database;

import au.com.addstar.truehardcore.TrueHardcore;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

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

public class Database {
    /**
     * Check if connected and if not open a Connection.
     * @return connected
     */
    public boolean isConnected() {
        if (!connected) {
            connected = openDatabase();
        }
        return connected;
    }

    private boolean connected = false;
    private Connection conn;

    public Database() {
    }

    private boolean openDatabase() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            String host = TrueHardcore.getCfg().host;
            String port = TrueHardcore.getCfg().port;
            String name = TrueHardcore.getCfg().name;
            if (host == null) {
                TrueHardcore.log("Host is null");
            }
            if (port == null) {
                TrueHardcore.log("Port is null");
            }
            if (name == null) {
                TrueHardcore.log("Name is null");
            }
            String user = TrueHardcore.getCfg().user;
            String password = TrueHardcore.getCfg().password;
            String url = "jdbc:mysql://" + host + ":" + port + "/" + name;
            conn = DriverManager.getConnection(url, user, password);

            connected = true;

            tryConvert();
        } catch (SQLException e) {
            TrueHardcore.warn("Unable to open database!");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            TrueHardcore.warn("Unable to find a suitable MySQL driver!");
            e.printStackTrace();
        }
        return connected;
    }

    private void tryConvert() throws SQLException {

        // Schema check for new id column
        Statement st = conn.createStatement();
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
        HashMap<String, UUID> lookup = new HashMap<>();
        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            if (player.getName() != null) {
                lookup.put(player.getName().toLowerCase(), player.getUniqueId());
            }
        }

        // Next update the schema for the table
        log.info("- Updating db schema");
        st.executeUpdate("ALTER TABLE `players` ADD COLUMN (`id` CHAR(36));");

        // Now query for all entries for an in place update
        log.info("- Converting players ...");

        PreparedStatement update = conn.prepareStatement(
              "UPDATE `players` SET `id`= ? WHERE `player`=? AND `world`=?;");

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
                update.setString(1,
                      UUID.nameUUIDFromBytes(name.getBytes()).toString());
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
                log.info("  * Processed " + count + " entries. "
                      + failCount + " failed");
            }
        }

        if (pending > 0) {
            update.executeBatch();
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
        //noinspection SqlResolve
        update = conn.prepareStatement(
              "UPDATE `whitelist` SET `id`= ? WHERE `player`=?;");
        //noinspection SqlResolve
        rs = st.executeQuery("SELECT `player` from `whitelist`");
        while (rs.next()) {
            String name = rs.getString("player");
            UUID id = lookup.get(name.toLowerCase());
            if (id != null) {
                update.setString(1, id.toString());
            } else {
                ++failCount;
                update.setString(1, UUID.nameUUIDFromBytes(name.getBytes()).toString());
            }
            update.setString(2, name);
            update.addBatch();
            ++pending;
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
        }
        log.info("  Finished converting " + count + " entries. "
              + failCount + " failed");
        st.executeUpdate("ALTER TABLE `whitelist` DROP PRIMARY KEY;");
        //noinspection SqlResolve
        st.executeUpdate("ALTER TABLE `whitelist` DROP `player`;");
        st.executeUpdate("ALTER TABLE `whitelist` ADD PRIMARY KEY (`id`);");
        rs.close();

        log.info("- Conversion complete");
    }

    /**
     * execute a query.
     * @param query the query
     * @return ResultSet
     */
    @SuppressWarnings("unused")
    public ResultSet executeQuery(String query) {
        Statement st;

        if (!connected) {
            return null;
        }

        try {
            st = conn.createStatement();
            TrueHardcore.debug("SQL Query: " + query);
            return st.executeQuery(query);
        } catch (SQLException e) {
            TrueHardcore.warn("Query execution failed!");
            TrueHardcore.log("SQL: " + query);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Prepare a query.
     * @param query the query
     * @param params the params
     * @return ResultSet
     */
    public ResultSet preparedQuery(String query, String[] params) {
        PreparedStatement ps;

        if (!connected) {
            return null;
        }

        try {
            ps = conn.prepareStatement(query);
            // Construct PreparedStatement by adding all supplied params to the query
            TrueHardcore.debugLog("SQL Query: " + query);
            if (params != null) {
                for (int x = 0; x < params.length; x++) {
                    TrueHardcore.debugLog("Param " + (x + 1) + ": " + params[x]);
                    ps.setString(x + 1, params[x]);
                }
            }
            return ps.executeQuery();
        } catch (SQLException e) {
            TrueHardcore.warn("Prepared query execution failed!");
            TrueHardcore.debugLog("SQL: " + query);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Execute an update.
     * @param query query
     * @return int -1 on failure.
     */
    @SuppressWarnings("unused")
    public int executeUpdate(String query) {
        Statement st;

        if (!connected) {
            return -1;
        }

        try {
            st = conn.createStatement();
            TrueHardcore.debug("SQL Update: " + query);
            return st.executeUpdate(query);
        } catch (SQLException e) {
            TrueHardcore.warn("Query execution failed!");
            TrueHardcore.log("SQL: " + query);
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Prepare a update.
     * @param query the query
     * @param params the params
     * @return int -1 on failure
     */
    public int preparedUpdate(String query, String[] params) {
        return preparedUpdate(query, params, false);
    }

    /**
     * Prepare a update.
     * @param query the query
     * @param params the params
     * @return int -1 on failure
     */
    public int preparedUpdate(String query, String[] params, boolean silent) {
        PreparedStatement ps;

        if (!connected) {
            return -1;
        }

        try {
            ps = conn.prepareStatement(query);
            // Construct PreparedStatement by adding all supplied params to the query
            if (!silent) {
                TrueHardcore.debugLog("SQL Update: " + query);
            }
            for (int x = 0; x < params.length; x++) {
                if (!silent) {
                    TrueHardcore.debugLog("Param " + (x + 1) + ": "
                          + params[x]);
                }
                ps.setString(x + 1, params[x]);
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            TrueHardcore.warn("Prepared query execution failed!");
            TrueHardcore.debugLog("SQL: " + query);
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Close the Database.
     * @return true on success.
     */
    @SuppressWarnings("unused")
    public boolean closeDatabase() {
        try {
            conn.close();
        } catch (SQLException e) {
            TrueHardcore.warn("Close database failed!");
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Check and table exists.
     * @param conn connection
     * @param tableName table
     * @return true if exists.
     */
    @SuppressWarnings("unused")
    public boolean tableExists(Connection conn, String tableName) {
        DatabaseMetaData md;
        ResultSet rs;

        try {
            md = conn.getMetaData();
        } catch (SQLException e) {
            // This shouldn't really happen
            TrueHardcore.warn("Unable to read DatabaseMetaData from DB connection!");
            e.printStackTrace();
            return false;
        }

        try {
            TrueHardcore.debug("Getting list of database tables");
            rs = md.getTables(null, null, tableName, null);
        } catch (SQLException e) {
            // This shouldn't really happen
            TrueHardcore.warn("Unable to getTables from DatabaseMetaData!");
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
            TrueHardcore.warn("Unable to iterate table resultSet!");
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Check a column exists on a table.
     * @param conn connection
     * @param tableName table
     * @param cname column
     * @return true if exists
     */
    @SuppressWarnings("unused")
    public boolean columnExists(Connection conn, String tableName, String cname) {
        DatabaseMetaData md;
        ResultSet rs;

        try {
            md = conn.getMetaData();
        } catch (SQLException e) {
            // This shouldn't really happen
            TrueHardcore.warn("Unable to read DatabaseMetaData from DB connection!");
            e.printStackTrace();
            return false;
        }

        try {
            TrueHardcore.debug("Getting list of table columns");
            rs = md.getColumns(null, null, tableName, cname);
        } catch (SQLException e) {
            // This shouldn't really happen
            TrueHardcore.warn("Unable to getColumns from DatabaseMetaData!");
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
            TrueHardcore.warn("Unable to iterate column resultSet!");
            e.printStackTrace();
        }
        return false;
    }
}
