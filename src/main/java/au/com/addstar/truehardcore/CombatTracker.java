package au.com.addstar.truehardcore;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import au.com.addstar.truehardcore.HardcorePlayers.HardcorePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.ExecutionException;

/**
 * This class will track players in combat and remove the status when adequate time has passed...
 * Created for the AddstarMC Project. Created by Narimm on 15/10/2018.
 */
public class CombatTracker implements Listener {
    
    private TrueHardcore plugin;
    private BukkitTask task;
    
    public CombatTracker(TrueHardcore plugin) {
        this.plugin = plugin;
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, combatMonitor(),40,
                100);

        
    }
    
    public void onDisable(){
        for (Player player : Bukkit.getOnlinePlayers()) {
            HardcorePlayer hcPlayer = plugin.HCPlayers.Get(player);
            hcPlayer.setCombat(false);
            hcPlayer.setCombatTime(0);
        }
        EntityDamageByEntityEvent.getHandlerList().unregister(this);
        task.cancel();
    }

    /**
     * Runs Aysnc
     * @return Runnable combat monitor
     */
    public Runnable combatMonitor(){
        return () -> {
            long currTime = System.currentTimeMillis();
            try {
                for (Player player : Bukkit.getScheduler().callSyncMethod(TrueHardcore.instance, Bukkit::getOnlinePlayers).get()) {
                    if (!plugin.IsHardcoreWorld(player.getWorld())) {
                        return;
                    }
                    HardcorePlayer hcPlayer = plugin.HCPlayers.Get(player);
                    if (hcPlayer == null) continue;
                    if (hcPlayer.isCombat()) {
                        if (hcPlayer.getCombatTime() <= currTime) {
                            hcPlayer.setCombatTime(0);
                            hcPlayer.setCombat(false);
                            Bukkit.getScheduler().runTask(TrueHardcore.instance,()->
                                    player.sendMessage(" You have exited combat mode..."));
                        }
                    }
                }
            } catch (InterruptedException | ExecutionException e){
                e.printStackTrace();
            }
        };
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamagebyEntity(EntityDamageByEntityEvent event) {
        if (!plugin.IsHardcoreWorld(event.getDamager().getWorld())) {
            return;
        }
        if (event.getDamager() instanceof Player) {
            if (event.getEntity() instanceof Player) {
                combatLog((Player) event.getDamager());
                combatLog((Player) event.getEntity());
                plugin.getLogger().info("[Combat Log] " + ((Player) event.getEntity()).getDisplayName() + "v" + ((Player) event.getDamager()).getDisplayName());
            }
        }

    }

    private void combatLog(Player player) {
        HardcorePlayer hcp = plugin.HCPlayers.Get(player);
        if (hcp.isCombat()) return;
        hcp.setCombat(true);
        hcp.setCombatTime(System.currentTimeMillis() + plugin.combatTime);
        player.sendMessage(" You have entered combat logging out will incur a penalty. Until:" + Util.Long2Time(hcp.getCombatTime() - System.currentTimeMillis()));
    }
}
