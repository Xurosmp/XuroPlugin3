package de.smpcore.teleport;

import de.smpcore.SMPCore;
import de.smpcore.util.Msg;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class TeleportManager {
    public record Request(UUID from, long expires) {}

    private final SMPCore plugin;
    private final Map<UUID, BukkitTask> pending = new HashMap<>();
    private final Map<UUID, Long> rtpCooldown = new HashMap<>();
    private final Map<UUID, Request> requests = new HashMap<>(); // Schlüssel = Ziel

    public TeleportManager(SMPCore plugin) { this.plugin = plugin; }

    public void cancel(Player p) {
        BukkitTask t = pending.remove(p.getUniqueId());
        if (t != null) t.cancel();
    }

    public void teleport(Player p, Location dest, String label) {
        int delay = plugin.getConfig().getInt("teleport.delay-seconds", 3);
        cancel(p);
        if (delay <= 0 || p.hasPermission("smpcore.tp.bypass")) {
            p.teleportAsync(dest);
            p.sendActionBar(Msg.c("<green>Teleportiert: <white>" + Msg.esc(label)));
            return;
        }
        Location start = p.getLocation().clone();
        BukkitTask task = new BukkitRunnable() {
            int left = delay;

            @Override
            public void run() {
                if (!p.isOnline()) { pending.remove(p.getUniqueId()); cancel(); return; }
                Location now = p.getLocation();
                if (now.getWorld() != start.getWorld() || now.distanceSquared(start) > 0.5) {
                    pending.remove(p.getUniqueId());
                    cancel();
                    p.sendActionBar(Msg.c("<red>Teleport abgebrochen – du hast dich bewegt!"));
                    return;
                }
                if (left <= 0) {
                    pending.remove(p.getUniqueId());
                    cancel();
                    p.teleportAsync(dest);
                    p.sendActionBar(Msg.c("<green>Teleportiert: <white>" + Msg.esc(label)));
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1f);
                    return;
                }
                p.sendActionBar(Msg.c("<yellow>Teleport in <white>" + left + "s <gray>– nicht bewegen!"));
                left--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
        pending.put(p.getUniqueId(), task);
    }

    // ---- TPA ----
    public void request(Player from, Player to) {
        requests.put(to.getUniqueId(), new Request(from.getUniqueId(), System.currentTimeMillis() + 60_000L));
    }

    public Request takeRequest(UUID target) {
        Request r = requests.remove(target);
        if (r == null || r.expires() < System.currentTimeMillis()) return null;
        return r;
    }

    // ---- RTP ----
    public void rtp(Player p) {
        long cd = plugin.getConfig().getLong("rtp.cooldown-seconds", 60) * 1000L;
        Long last = rtpCooldown.get(p.getUniqueId());
        if (last != null && System.currentTimeMillis() - last < cd && !p.hasPermission("smpcore.tp.bypass")) {
            Msg.error(p, "Bitte warte noch " + ((cd - (System.currentTimeMillis() - last)) / 1000 + 1) + "s.");
            return;
        }
        World w = Bukkit.getWorld(plugin.getConfig().getString("rtp.world", "world"));
        if (w == null) w = p.getWorld();
        int min = plugin.getConfig().getInt("rtp.min-radius", 500);
        int max = Math.max(min + 1, plugin.getConfig().getInt("rtp.max-radius", 5000));
        Msg.send(p, "<yellow>Suche einen sicheren Ort ...");
        attempt(p, w, min, max, 0);
    }

    private void attempt(Player p, World w, int min, int max, int tries) {
        if (!p.isOnline()) return;
        if (tries >= 10) { Msg.error(p, "Kein sicherer Ort gefunden, versuche es nochmal."); return; }
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        double ang = rnd.nextDouble() * Math.PI * 2;
        int r = min + rnd.nextInt(max - min + 1);
        final int x = (int) (Math.cos(ang) * r);
        final int z = (int) (Math.sin(ang) * r);
        w.getChunkAtAsync(x >> 4, z >> 4).thenAccept(chunk -> {
            int y = w.getHighestBlockYAt(x, z);
            Block b = w.getBlockAt(x, y, z);
            Material m = b.getType();
            if (!m.isSolid() || Tag.LEAVES.isTagged(m) || m == Material.MAGMA_BLOCK || m == Material.CACTUS) {
                attempt(p, w, min, max, tries + 1);
                return;
            }
            Location dest = new Location(w, x + 0.5, y + 1, z + 0.5, p.getLocation().getYaw(), p.getLocation().getPitch());
            rtpCooldown.put(p.getUniqueId(), System.currentTimeMillis());
            teleport(p, dest, "Zufälliger Ort");
        });
    }
}
