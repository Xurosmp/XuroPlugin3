package de.smpcore.listeners;

import de.smpcore.SMPCore;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public final class DimensionListener implements Listener {
    private final SMPCore plugin;

    public DimensionListener(SMPCore plugin) {
        this.plugin = plugin;
    }

    public static boolean enabled(SMPCore plugin, String dim) {
        if (plugin == null) return true;
        return plugin.getConfig().getBoolean("dimensions." + dim, true);
    }

    public static void setEnabled(SMPCore plugin, String dim, boolean on) {
        if (plugin == null) return;
        plugin.getConfig().set("dimensions." + dim, on);
        plugin.saveConfig();
    }

    private boolean disabledFor(Player player) {
        World world = player.getWorld();
        if (world == null) return false;
        if (world.getEnvironment() == World.Environment.NETHER && !enabled(plugin, "nether")) return true;
        if (world.getEnvironment() == World.Environment.THE_END && !enabled(plugin, "end")) return true;
        return false;
    }

    private void sendHome(Player player) {
        if (plugin.data() == null) return;
        Location spawn = plugin.data().spawn();
        if (spawn != null) {
            player.teleportAsync(spawn);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("smpcore.admin")) return;
        if (disabledFor(player)) {
            sendHome(player);
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("smpcore.admin")) return;
        if (disabledFor(player)) {
            sendHome(player);
        }
    }
}
