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

package au.com.addstar.truehardcore.functions;

import au.com.addstar.truehardcore.TrueHardcore;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;


public class Util {

    public static String padRight(String s, int n) {
        return String.format("%1$-" + n + "s", s);
    }

    public static String padLeft(String s, int n) {
        return String.format("%1$" + n + "s", s);
    }

    /**
     * String from Location.
     * @param loc Location
     * @return String
     */
    public static String loc2Str(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return null;
        }

        return loc.getWorld().getName() + ","
              + loc.getX() + ","
              + loc.getY() + ","
              + loc.getZ() + ","
              + loc.getYaw() + ","
              + loc.getPitch();
    }

    /**
     * String to Bukkit Location.
     * @param input String
     * @return Bukkit Location
     */
    public static Location str2Loc(String input) {
        if (input == null) {
            return null;
        }

        Location loc;
        String[] parts = input.split(",");

        World world = TrueHardcore.instance.getServer().getWorld(parts[0]);
        if (world == null) {
            throw new IllegalArgumentException("World name not found. " + input);
        }
        try {
            loc = new Location(world,
                  Double.parseDouble(parts[1]),
                  Double.parseDouble(parts[2]),
                  Double.parseDouble(parts[3]),
                  Float.parseFloat(parts[4]),
                  Float.parseFloat(parts[5]));
            return loc;
        } catch (NumberFormatException | NullPointerException e) {
            IllegalArgumentException ex = new IllegalArgumentException("Location could not be "
                  + "determined: " + input);
            ex.addSuppressed(e);
            throw ex;
        }
    }

    /**
     * A time in seconds.
     * @param time the Time variable
     * @return String
     */
    public static String long2Time(long time) {
        long daySecond = (time % 60);
        long dayMinute = ((time / 60) % 60);
        long dayHours = (time / (60 * 60)) % 24;

        String day = "";
        String hours = "";
        String minutes = "";
        String seconds = "";

        if (daySecond > 0) {
            seconds = daySecond + "s";
        }
        if (dayMinute > 0) {
            minutes = dayMinute + "m";
        }
        if (dayHours > 0) {
            hours = dayHours + "h";
        }
        long dayDay = time / 24 * 60 * 60;
        if (dayDay > 0) {
            day = dayDay + "d";
        }

        if ((minutes.length() == 0) && (seconds.length() > 0) && (hours.length() > 0)) {
            minutes = dayMinute + "m";
        }
        if ((hours.length() == 0) && (minutes.length() > 0) && (day.length() > 0)) {
            hours = dayHours + "h";
        }
        if ((minutes.length() > 0) && (seconds.length() > 0)) {
            minutes += " ";
        }
        if ((hours.length() > 0) && (minutes.length() > 0)) {
            hours += " ";
        }
        if ((day.length() > 0) && (hours.length() > 0)) {
            day += " ";
        }

        return day + hours + minutes + seconds;
    }

    /**
     * convert date to mysql string timestamp.
     * @param date the Date
     * @return String
     */
    // TODO: add exception checking
    public static String date2Mysql(Date date) {
        if (date == null) {
            return null;
        }

        SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }

    /**
     * Convert a mysql date to a String.
     * @param mysqlDate the string
     * @return Date
     */
    // TODO: add exception checking
    public static Date mysql2Date(String mysqlDate) {
        if ((Objects.equals(mysqlDate, "")) || (mysqlDate == null) || (Objects.equals(mysqlDate,
              "0000-00-00 00:00:00"))) {
            return null;
        }

        SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            return sdf.parse(mysqlDate);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Check if the player has the specified permission.
     **/
    public static boolean noPermission(Player player, String perm) {
        if (player != null) {
            // Real player
            return !player.hasPermission(perm);
        } else {
            // Console has permissions for everything
            return false;
        }
    }

    /**
     * Check required permission and send error response to player if not allowed.
     **/
    public static boolean requirePermission(Player player, String perm) {
        if (noPermission(player, perm)) {
            player.sendMessage(ChatColor.RED
                  + "Sorry, you do not have permission for this command.");
            return false;
        }
        return true;
    }


    /**
     * Teleport a player.
     * @param player the player
     * @param loc location
     * @return true on success
     */
    public static boolean teleport(Player player, Location loc) {
        if ((loc == null) || (player == null)) {
            TrueHardcore.debugLog("Teleport location or player null!");
            return false;
        } else {
            TrueHardcore.debugLog("Teleport (" + player.getName() + "): " + loc);
            return player.teleport(loc);
        }
    }

    public static class LogFormatter extends Formatter {
        //
        // Create a DateFormat to format the logger timestamp.
        //
        private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        /**
         * Create a formatted log record.
         * @param record LogRecord
         * @return String
         */
        public String format(LogRecord record) {
            StringBuilder builder = new StringBuilder(1000);
            builder.append(df.format(new Date(record.getMillis()))).append(" ");
            //builder.append("[").append(record.getSourceClassName()).append(".");
            //builder.append(record.getSourceMethodName()).append("] - ");
            builder.append("[").append(record.getLevel()).append("]: ");
            builder.append(formatMessage(record));
            builder.append("\n");
            return builder.toString();
        }

        public String getHead(Handler h) {
            return super.getHead(h);
        }

        public String getTail(Handler h) {
            return super.getTail(h);
        }
    }
}
