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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import au.com.addstar.truehardcore.TrueHardcore;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class Util {

    public static class LogFormatter extends Formatter {
        //
        // Create a DateFormat to format the logger timestamp.
        //
        private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

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

    public static String padRight(String s, int n) {
         return String.format("%1$-" + n + "s", s);
    }

    public static String padLeft(String s, int n) {
        return String.format("%1$" + n + "s", s);
    }

    public static String Loc2Str(Location loc) {
        if (loc == null) { return null; }
        String result = loc.getWorld().getName() + "," +
                        loc.getX() + "," +
                        loc.getY() + "," +
                        loc.getZ() + "," +
                        loc.getYaw() + "," +
                        loc.getPitch();

        return result;
    }

    public static Location Str2Loc(String input) {
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

    public static String Long2Time(long time) {
        long dS = (time % 60);
        long dM = ((time / 60) % 60);
        long dH = ((time / (60 * 60)) % 24);
        long dD = (time / (24 * 60 * 60));

        String result = "";
        String d = "", h = "", m = "", s = "";

        if (dS > 0) s = dS + "s";
        if (dM > 0) m = dM + "m";
        if (dH > 0) h = dH + "h";
        if (dD > 0) d = dD + "d";

        if ((m.length() == 0) && (s.length() > 0) && (h.length() > 0)) m = dM + "m";
        if ((h.length() == 0) && (m.length() > 0) && (d.length() > 0)) h = dH + "h";

        if ((m.length() > 0) && (s.length() > 0)) m += " ";
        if ((h.length() > 0) && (m.length() > 0)) h += " ";
        if ((d.length() > 0) && (h.length() > 0)) d += " ";

        result = d + h + m + s;

        return result;
    }

    // TODO: add exception checking
    public static String Date2Mysql(Date date) {
        if (date == null) { return null; }

        SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }

    // TODO: add exception checking
    public static Date Mysql2Date(String mysqldate) {
        if ((Objects.equals(mysqldate, "")) || (mysqldate == null) || (Objects.equals(mysqldate, "0000-00-00 00:00:00"))) {
            return null;
        }

        SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            return sdf.parse(mysqldate);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    public static Material GetMaterial(String name) {
        Material mat = Material.matchMaterial(name);
        if (mat != null) {
            return mat;
        }
        return null;
    }

    public static boolean GiveItemStack(Player player, ItemStack itemstack) {
        PlayerInventory inventory = player.getInventory();
        inventory.addItem(itemstack);
        //TODO: Check "result" to ensure all items were given
        return true;
    }

    /*
     * Check if the player has the specified permission
     */
    public static boolean HasPermission(Player player, String perm) {
        if (player != null) {
            // Real player
            return player.hasPermission(perm);
        } else {
            // Console has permissions for everything
            return true;
        }
    }

    /*
     * Check required permission and send error response to player if not allowed
     */
    public static boolean RequirePermission(Player player, String perm) {
        if (!HasPermission(player, perm)) {
            player.sendMessage(ChatColor.RED + "Sorry, you do not have permission for this command.");
            return false;
        }
        return true;
    }

    /*
     * Check if player is online
     */
    public static boolean IsPlayerOnline(String player) {
        if (player == null) { return false; }
        if (Objects.equals(player, "")) {
            return false;
        }
        // Found player.. they must be online!
        return TrueHardcore.instance.getServer().getPlayer(player) != null;
    }

    public static boolean Teleport(Player player, Location loc) {
        if ((loc == null) || (player == null)) {
            TrueHardcore.DebugLog("Teleport location or player null!");
            return false;
        } else {
            TrueHardcore.DebugLog("Teleport (" + player.getName() + "): " + loc);
            /*Location newloc = new Location(
                    loc.getWorld(),
                    (double) loc.getBlockX() + 0.5,
                    (double) loc.getBlockY(),
                    (double) loc.getBlockZ() + 0.5
            );*/
            return player.teleport(loc);
        }
    }
}
