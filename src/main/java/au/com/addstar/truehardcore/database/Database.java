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
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class Database {

    private HikariDataSource dataSource;
    private Connection queryConn;

    public Database() {
    }

    /**
     * Check if connected and if not open a Connection.
     * @return connected
     */
    public boolean isConnected() {
        if (dataSource == null || dataSource.isClosed()) {
            return openDatabase();
        }
        return true;
    }

    private boolean openDatabase() {
        String host = TrueHardcore.getCfg().host;
        String port = TrueHardcore.getCfg().port;
        String name = TrueHardcore.getCfg().name;
        String user = TrueHardcore.getCfg().user;
        String password = TrueHardcore.getCfg().password;
        String url = "jdbc:mysql://" + host + ":" + port + "/" + name;

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(5);
        config.setConnectionTimeout(5000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(600000);
        config.setPoolName("TrueHardcore-DB");

        dataSource = new HikariDataSource(config);

        return true;
    }

    /**
     * Get a persistent connection for query operations.
     * Reconnects automatically if the connection has died.
     */
    private Connection getQueryConnection() throws SQLException {
        if (queryConn == null || queryConn.isClosed() || !queryConn.isValid(2)) {
            if (queryConn != null) {
                try { queryConn.close(); } catch (SQLException ignored) {}
            }
            queryConn = dataSource.getConnection();
        }
        return queryConn;
    }

    /**
     * Prepare a query.
     * @param query the query
     * @param params the params
     * @return ResultSet
     */
    public ResultSet preparedQuery(String query, String[] params) {
        return preparedQuery(query, params, false);
    }

    /**
     * Prepare a query.
     * @param query the query
     * @param params the params
     * @param silent suppress debug logging
     * @return ResultSet
     */
    public ResultSet preparedQuery(String query, String[] params, boolean silent) {
        try {
            Connection conn = getQueryConnection();
            PreparedStatement ps = conn.prepareStatement(query);
            if (!silent) {
                TrueHardcore.debugLog("SQL Query: " + query);
            }
            if (params != null) {
                List<String> values = new ArrayList<>();
                for (int x = 0; x < params.length; x++) {
                    values.add((x+1) + ":" + params[x]);
                    ps.setString(x + 1, params[x]);
                }
                if (!silent) {
                    TrueHardcore.debug("Params: " + String.join(", ", values));
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
     * Prepare an update.
     * @param query the query
     * @param params the params
     * @return int -1 on failure
     */
    public int preparedUpdate(String query, String[] params) {
        return preparedUpdate(query, params, false);
    }

    /**
     * Prepare an update.
     * @param query the query
     * @param params the params
     * @param silent suppress debug logging
     * @return int -1 on failure
     */
    public int preparedUpdate(String query, String[] params, boolean silent) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            if (!silent) {
                TrueHardcore.debugLog("SQL Update: " + query);
            }
            List<String> values = new ArrayList<>();
            for (int x = 0; x < params.length; x++) {
                values.add((x+1) + ":" + params[x]);
                ps.setString(x + 1, params[x]);
            }
            if (!silent) {
                TrueHardcore.debug("Params: " + String.join(", ", values));
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            TrueHardcore.warn("Prepared update execution failed!");
            TrueHardcore.debugLog("SQL: " + query);
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Close the Database.
     */
    public void closeDatabase() {
        if (queryConn != null) {
            try { queryConn.close(); } catch (SQLException ignored) {}
            queryConn = null;
        }
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    /**
     * Check if a table exists.
     * @param conn connection
     * @param tableName table
     * @return true if exists.
     */
    @SuppressWarnings("unused")
    public boolean tableExists(Connection conn, String tableName) {
        try {
            DatabaseMetaData md = conn.getMetaData();
            ResultSet rs = md.getTables(null, null, tableName, null);
            if (rs.next()) {
                return true;
            }
        } catch (SQLException e) {
            TrueHardcore.warn("Unable to check table existence!");
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Check if a column exists on a table.
     * @param conn connection
     * @param tableName table
     * @param cname column
     * @return true if exists
     */
    public boolean columnExists(Connection conn, String tableName, String cname) {
        try {
            DatabaseMetaData md = conn.getMetaData();
            ResultSet rs = md.getColumns(null, null, tableName, cname);
            if (rs.next()) {
                return true;
            }
        } catch (SQLException e) {
            TrueHardcore.warn("Unable to check column existence!");
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Ensure a column exists on a table, adding it via ALTER TABLE if it is missing.
     *
     * <p>Lightweight schema migration so new columns deploy automatically across all HC
     * instances without a manual DB step. No-op if the column already exists.</p>
     *
     * @param tableName the table
     * @param column    the column name
     * @param ddlType   the column definition (e.g. "tinyint(1) NOT NULL DEFAULT 0")
     */
    public void ensureColumn(String tableName, String column, String ddlType) {
        try {
            Connection conn = getQueryConnection();
            if (columnExists(conn, tableName, column)) {
                return;
            }
            String sql = "ALTER TABLE `" + tableName + "` ADD COLUMN `" + column + "` " + ddlType;
            try (Statement st = conn.createStatement()) {
                st.executeUpdate(sql);
            }
            TrueHardcore.log("Added missing column `" + column + "` to `" + tableName + "`.");
        } catch (SQLException e) {
            TrueHardcore.warn("Failed to ensure column `" + column + "` on `" + tableName + "`!");
            e.printStackTrace();
        }
    }
}
