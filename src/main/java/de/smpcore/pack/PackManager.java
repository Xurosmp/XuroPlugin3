package de.smpcore.pack;

import de.smpcore.SMPCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class PackManager {
    private final SMPCore plugin;

    public PackManager(SMPCore plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        plugin.getConfig().addDefault("resourcepack.enabled", false);
        plugin.getConfig().addDefault("resourcepack.url", "");
        plugin.getConfig().addDefault("resourcepack.sha1", "");
        plugin.getConfig().addDefault("resourcepack.force", false);
        plugin.getConfig().addDefault("resourcepack.prompt", "");
    }

    public boolean enabled() {
        return plugin.getConfig().getBoolean("resourcepack.enabled", false);
    }

    public String url() {
        return plugin.getConfig().getString("resourcepack.url", "");
    }

    public String sha1() {
        return plugin.getConfig().getString("resourcepack.sha1", "");
    }

    public boolean force() {
        return plugin.getConfig().getBoolean("resourcepack.force", false);
    }

    public String prompt() {
        return plugin.getConfig().getString("resourcepack.prompt", "");
    }

    public void apply(Player player) {
        if (!enabled() || url() == null || url().isBlank()) return;
        String sha = sha1() == null || sha1().isBlank() ? null : sha1();
        player.setResourcePack(url(), sha, force(), prompt());
    }

    public void applyAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            apply(player);
        }
    }

    public void reload() {
        load();
        applyAll();
    }
}
