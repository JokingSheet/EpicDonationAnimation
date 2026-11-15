package de.epicpayment.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = Path.of("config", "epicpayment.json");
    public boolean enabled = true;
    public boolean sounds = true;
    public boolean particles = true;
    public boolean screenShake = true;
    public long minimumAmount = 1_000_000L;
    public int durationSeconds = 6;
    public float verticalPosition = 0.48f;
    public List<PaymentTier> tiers = defaults();
    /** Optional Java regexes; group 1 = amount, group 2 = sender (optional). */
    public List<String> customPatterns = new ArrayList<>();

    public static List<PaymentTier> defaults() {
        return new ArrayList<>(List.of(
            new PaymentTier("epic", "BIG PAYMENT!", 1_000_000L, 0xFFFFD34E, 0xFFFF8C24),
            new PaymentTier("super", "MASSIVE PAYMENT!", 5_000_000L, 0xFFFFE46B, 0xFFFFA52E),
            new PaymentTier("legendary", "LEGENDARY PAYMENT!", 25_000_000L, 0xFFFFD34E, 0xFFB96BFF),
            new PaymentTier("mythic", "MYTHIC PAYMENT!", 100_000_000L, 0xFFDBB6FF, 0xFF8A43FF),
            new PaymentTier("godlike", "GODLIKE PAYMENT", 500_000_000L, 0xFFFFF0A6, 0xFFFFB52E),
            new PaymentTier("overlord", "THE ULTIMATE PAYMENT", 1_000_000_000L, 0xFFFFE7A0, 0xFF9D65FF)
        ));
    }
    public void normalize() {
        if (tiers == null || tiers.isEmpty()) tiers = defaults();
        tiers.sort(Comparator.comparingLong(t -> t.threshold));
        minimumAmount = Math.max(1, minimumAmount);
        durationSeconds = Math.max(2, Math.min(20, durationSeconds));
        verticalPosition = Math.max(0.15f, Math.min(0.8f, verticalPosition));
    }
    public PaymentTier tierFor(long amount) {
        PaymentTier selected = tiers.get(0);
        for (PaymentTier tier : tiers) if (amount >= tier.threshold) selected = tier;
        return selected;
    }
    public static ModConfig load() {
        try {
            Files.createDirectories(FILE.getParent());
            if (Files.exists(FILE)) {
                ModConfig config = GSON.fromJson(Files.readString(FILE), ModConfig.class);
                if (config != null) { config.normalize(); return config; }
            }
        } catch (Exception e) { System.err.println("[EpicPayment] Could not read config: " + e.getMessage()); }
        ModConfig config = new ModConfig(); config.save(); return config;
    }
    public void save() {
        normalize();
        try { Files.createDirectories(FILE.getParent()); Files.writeString(FILE, GSON.toJson(this)); }
        catch (IOException e) { System.err.println("[EpicPayment] Could not save config: " + e.getMessage()); }
    }
}
