package de.smpcore.data;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerData {
    public final UUID uuid;
    public String name;
    public double money;
    public long crystals;   // AFK-Währung
    public long gems;       // Store-Währung
    public int kills, deaths;
    public long playtime; // Sekunden
    public long lastDaily;
    public int dailyStreak;
    public boolean scoreboardOff;
    public String rank; // null = Standard-Rang
    public final Map<String, Integer> keys = new HashMap<>();
    public final Map<String, String> homes = new LinkedHashMap<>();

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }
}
