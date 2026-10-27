package de.smpcore.scoreboard;

import de.smpcore.SMPCore;
import de.smpcore.data.PlayerData;
import de.smpcore.economy.EconomyManager;
import de.smpcore.util.Msg;
import de.smpcore.util.Num;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ScoreboardManager {
    private record Board(Scoreboard sb, int lines) {}

    private final SMPCore plugin;
    private final Map<UUID, Board> boards = new HashMap<>();
    private BukkitTask task;

    public ScoreboardManager(SMPCore plugin) { this.plugin = plugin; }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(), 20L, 20L);
    }

    public void shutdown() {
        if (task != null) task.cancel();
        for (Player p : Bukkit.getOnlinePlayers()) remove(p);
    }

    public void remove(Player p) {
        if (boards.remove(p.getUniqueId()) != null && p.isOnline()) {
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    private Board create(Player p, int lines) {
        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective o = sb.registerNewObjective("smp", Criteria.DUMMY, Msg.c("SMP"));
        o.setDisplaySlot(DisplaySlot.SIDEBAR);
        for (int i = 0; i < lines; i++) {
            String entry = "\u00a7" + "0123456789abcdef".charAt(i) + "\u00a7r";
            Team t = sb.registerNewTeam("l" + i);
            t.addEntry(entry);
            o.getScore(entry).setScore(lines - i);
        }
        p.setScoreboard(sb);
        return new Board(sb, lines);
    }

    private void tick() {
        boolean on = plugin.feature("scoreboard");
        List<String> lines = plugin.getConfig().getStringList("scoreboard.lines");
        int n = Math.min(lines.size(), 15);
        String title = plugin.getConfig().getString("scoreboard.title", "SMP");
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerData d = plugin.data().get(p.getUniqueId());
            if (!on || d == null || d.scoreboardOff || n == 0) { remove(p); continue; }
            Board b = boards.get(p.getUniqueId());
            if (b == null || b.lines() != n) {
                b = create(p, n);
                boards.put(p.getUniqueId(), b);
            } else if (p.getScoreboard() != b.sb()) {
                p.setScoreboard(b.sb());
            }
            Objective o = b.sb().getObjective("smp");
            if (o != null) o.displayName(Msg.c(title));
            for (int i = 0; i < n; i++) {
                Team t = b.sb().getTeam("l" + i);
                if (t != null) t.prefix(Msg.c(apply(lines.get(i), p, d)));
            }
        }
    }

    private String clanName(Player p) {
        de.smpcore.clan.ClanManager.Clan c = plugin.clans().of(p.getUniqueId());
        return c == null ? "???" : Msg.esc(c.name);
    }

    private String apply(String line, Player p, PlayerData d) {
        double bounty = plugin.bounties().get(p.getUniqueId());
        return line
                .replace("%player%", Msg.esc(p.getName()))
                .replace("%money%", EconomyManager.fmt(d.money))
                .replace("%crystals%", Num.format(d.crystals))
                .replace("%kills%", String.valueOf(d.kills))
                .replace("%deaths%", String.valueOf(d.deaths))
                .replace("%playtime%", Num.duration(d.playtime * 1000))
                .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%bounty%", bounty > 0 ? EconomyManager.fmt(bounty) : "-")
                .replace("%ping%", String.valueOf(p.getPing()))
                .replace("%max%", String.valueOf(Bukkit.getMaxPlayers()))
                .replace("%rank%", plugin.ranks().rankOf(p.getUniqueId()).display)
                .replace("%keyall%", plugin.keyall().enabled() ? Num.mmss(plugin.keyall().secondsLeft()) : "aus")
                .replace("%gems%", Num.format(d.gems))
                .replace("%clan%", clanName(p))
                .replace("%server%", plugin.getConfig().getString("server-name", "SERVER"));
    }
}
