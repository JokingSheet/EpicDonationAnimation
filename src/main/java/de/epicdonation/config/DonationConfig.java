package de.epicdonation.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;

public final class DonationConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("epicdonation.json");
    private static DonationConfig INSTANCE = defaults();
    public boolean enabled = true;
    public boolean sounds = true;
    public boolean particles = true;
    public boolean screenShake = true;
    public double minimumAmount = 1_000_000;
    public int durationSeconds = 7;
    public float horizontalPosition = 0.5f;
    public float verticalPosition = 0.42f;
    public List<Tier> tiers = new ArrayList<>();
    public static final class Tier {
        public String id;
        public String title;
        public double threshold;
        public int primaryColor;
        public int secondaryColor;
        public Tier(String id, String title, double threshold, int primaryColor, int secondaryColor) {
            this.id=id; this.title=title; this.threshold=threshold;
            this.primaryColor=primaryColor; this.secondaryColor=secondaryColor;
        }
    }
    private static DonationConfig defaults() {
        DonationConfig c = new DonationConfig();
        c.tiers.add(new Tier("epic", "🔥 BIG PAYMENT!", 1e6, 0xFFFFD54F, 0xFFFF8F00));
        c.tiers.add(new Tier("super", "⚡ MASSIVE PAYMENT!", 5e6, 0xFFFFE082, 0xFFFF6F00));
        c.tiers.add(new Tier("legendary", "👑 LEGENDARY PAYMENT!", 25e6, 0xFFFFF176, 0xFFFFB300));
        c.tiers.add(new Tier("mythic", "🌌 MYTHIC PAYMENT!", 100e6, 0xFFE1BEE7, 0xFF8E24AA));
        c.tiers.add(new Tier("godlike", "⚡ GODLIKE PAYMENT ⚡", 500e6, 0xFFFFF59D, 0xFFFFD700));
        c.tiers.add(new Tier("ultimate", "👑 THE ULTIMATE PAYMENT 👑", 1e9, 0xFFFFF8E1, 0xFF7E57C2));
        return c;
    }
    public static DonationConfig get() { return INSTANCE; }
    public static void load() {
        try {
            if (Files.exists(FILE)) {
                DonationConfig loaded = GSON.fromJson(Files.readString(FILE), DonationConfig.class);
                if (loaded != null) INSTANCE = loaded;
            } else INSTANCE = defaults();
        } catch (Exception e) {
            INSTANCE = defaults();
            System.err.println("[EpicDonation] Could not read config: " + e.getMessage());
        }
        save();
    }
    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(INSTANCE));
        } catch (IOException e) {
            System.err.println("[EpicDonation] Could not save config: " + e.getMessage());
        }
    }
    public Tier tierFor(double amount) {
        Tier result = tiers.getFirst();
        for (Tier tier : tiers) if (amount >= tier.threshold && tier.threshold >= result.threshold) result = tier;
        return result;
    }
}
