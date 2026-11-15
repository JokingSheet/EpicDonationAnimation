package de.epicdonation.render;

import de.epicdonation.EpicDonationClient;
import de.epicdonation.config.DonationConfig;
import de.epicdonation.config.DonationConfig.Tier;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Random;

public final class DonationHud {
    private static String player = "";
    private static double amount;
    private static long started;
    private static boolean active;
    private static final Random RANDOM = new Random(41);
    private static final DecimalFormat FORMAT = new DecimalFormat("#,##0.##",
            DecimalFormatSymbols.getInstance(Locale.US));
    private DonationHud() {}
    public static void showDonation(String name, double value) {
        if (!DonationConfig.get().enabled) return;
        player = name;
        amount = value;
        started = System.currentTimeMillis();
        active = true;
        if (DonationConfig.get().sounds && EpicDonationClient.client().player != null) {
            EpicDonationClient.client().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F));
        }
    }
    public static void tick() {
        if (active && System.currentTimeMillis() - started > DonationConfig.get().durationSeconds * 1000L) active = false;
    }
    public static void preview(int tierIndex) {
        var tiers = DonationConfig.get().tiers;
        if (tiers.isEmpty()) return;
        Tier tier = tiers.get(Math.max(0, Math.min(tierIndex, tiers.size()-1)));
        showDonation("PreviewPlayer", Math.max(tier.threshold, 1));
    }
    public static void render(GuiGraphics g, DeltaTracker delta) {
        if (!active || !DonationConfig.get().enabled) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        DonationConfig cfg = DonationConfig.get();
        Tier tier = cfg.tierFor(amount);
        long elapsed = System.currentTimeMillis() - started;
        float fadeIn = Math.min(1f, elapsed / 350f);
        float fadeOut = Math.min(1f, (cfg.durationSeconds * 1000f - elapsed) / 650f);
        float alpha = Math.max(0, Math.min(fadeIn, fadeOut));
        float pulse = 1f + 0.035f * (float)Math.sin(elapsed / 170.0);
        int w = g.guiWidth(), h = g.guiHeight();
        int cx = (int)(w * cfg.horizontalPosition), cy = (int)(h * cfg.verticalPosition);
        int panelW = tier.threshold >= 1e9 ? 330 : tier.threshold >= 1e8 ? 310 : 285;
        int panelH = tier.threshold >= 1e8 ? 112 : 96;
        int a = (int)(alpha * 210) << 24;
        int border = (int)(alpha * 255) << 24;
        int primary = (tier.primaryColor & 0x00FFFFFF) | ((int)(alpha*255) << 24);
        int secondary = (tier.secondaryColor & 0x00FFFFFF) | ((int)(alpha*255) << 24);
        g.fill(cx-panelW/2-3, cy-panelH/2-3, cx+panelW/2+3, cy+panelH/2+3, border | 0x003A1B08);
        g.fill(cx-panelW/2, cy-panelH/2, cx+panelW/2, cy+panelH/2, a | 0x00110B16);
        g.fill(cx-panelW/2, cy-panelH/2, cx+panelW/2, cy-panelH/2+2, primary);
        g.fill(cx-panelW/2, cy+panelH/2-2, cx+panelW/2, cy+panelH/2, secondary);
        g.pose().pushMatrix();
        g.pose().translate(cx, cy);
        g.pose().scale(pulse, pulse);
        g.drawCenteredString(mc.font, tier.title, 0, -panelH/2+13, primary);
        g.drawCenteredString(mc.font, FORMAT.format(amount) + " Coins", 0, -4, primary);
        g.drawCenteredString(mc.font, "Von: " + player, 0, 18, 0xFFFFFFFF & ((int)(alpha*255) << 24 | 0x00FFFFFF));
        g.pose().popMatrix();
        if (cfg.particles) {
            int count = tier.threshold >= 1e9 ? 34 : tier.threshold >= 1e8 ? 24 : 12;
            for (int i=0; i<count; i++) {
                double phase = elapsed / (tier.threshold >= 1e8 ? 115.0 : 180.0) + i * 2.399;
                double radius = 35 + (i * 17 % 85);
                int px = cx + (int)(Math.cos(phase) * radius);
                int py = cy + (int)(Math.sin(phase * 1.2) * radius * .55);
                int color = (i % 3 == 0 ? primary : secondary);
                g.fill(px, py, px+2, py+2, color);
            }
        }
        if (tier.threshold >= 25e6) {
            int flash = (int)(Math.max(0, Math.sin(elapsed / 95.0)) * alpha * 38);
            g.fill(0, 0, w, h, (flash << 24) | 0x00FFD54F);
        }
    }
    public static String format(double value) { return FORMAT.format(value); }
}
