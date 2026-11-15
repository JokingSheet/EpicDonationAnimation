package de.epicpayment.render;

import de.epicpayment.config.ModConfig;
import de.epicpayment.config.PaymentTier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class PaymentOverlay {
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));
    private static volatile Active active;
    private static ModConfig config;
    private record Active(long amount, String player, PaymentTier tier, long startedAt, long durationMs) {}
    private PaymentOverlay() {}

    public static void initialize(ModConfig cfg) {
        config = cfg;
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath("epicpayment", "payment_overlay"), PaymentOverlay::render);
    }
    public static void show(long amount, String player, PaymentTier tier) {
        active = new Active(amount, player == null || player.isBlank() ? "A player" : player,
                tier, Util.getMillis(), config.durationSeconds * 1000L);
    }
    public static boolean isActive() { return active != null; }

    private static void render(GuiGraphics g, net.minecraft.client.DeltaTracker tickCounter) {
        Active a = active;
        if (a == null || config == null || !config.enabled) return;
        long elapsed = Util.getMillis() - a.startedAt;
        float t = elapsed / (float) a.durationMs;
        if (t >= 1f) { active = null; return; }
        Minecraft mc = Minecraft.getInstance();
        int w = g.guiWidth(), h = g.guiHeight();
        float fadeIn = Mth.clamp(elapsed / 350f, 0f, 1f);
        float fadeOut = Mth.clamp((a.durationMs - elapsed) / 650f, 0f, 1f);
        float alpha = Math.min(fadeIn, fadeOut);
        float pulse = 1f + Mth.sin(elapsed / 170f) * 0.025f;
        int shake = config.screenShake && a.tier.threshold >= 5_000_000L ? (int)(Math.sin(elapsed * 0.075) * 3.0 * fadeOut) : 0;
        int cx = w / 2 + shake, cy = (int)(h * config.verticalPosition);
        int main = withAlpha(a.tier.color, (int)(255 * alpha));
        int accent = withAlpha(a.tier.accent, (int)(230 * alpha));
        boolean cosmic = a.tier.id.equals("overlord") || a.tier.id.equals("mythic");
        boolean grand = a.tier.id.equals("legendary") || a.tier.id.equals("godlike") || cosmic;

        // Layered translucent cinematic backdrop.
        int panelW = grand ? 390 : 330;
        int panelH = grand ? 118 : 100;
        int top = cy - panelH / 2;
        if (grand) {
            g.fill(0, 0, w, h, withAlpha(cosmic ? 0xFF160D2C : 0xFF2A1A08, (int)((cosmic ? 72 : 54) * alpha)));
            for (int i = 0; i < 7; i++) {
                float phase = elapsed / (260f + i * 24) + i * 0.9f;
                int x = (int)(cx + Mth.sin(phase) * (70 + i * 35));
                int y = (int)(cy + Mth.cos(phase * 0.83f) * (40 + i * 20));
                g.fill(x - 1, 0, x + 2, h, withAlpha(a.tier.accent, (int)(18 * alpha)));
                g.fill(0, y, w, y + 1, withAlpha(a.tier.color, (int)(12 * alpha)));
            }
        }
        // Animated frame and soft glow.
        for (int i = 4; i >= 1; i--) {
            int glow = withAlpha(a.tier.color, (int)((12 - i) * alpha));
            g.fill(cx - panelW/2 - i*2, top - i*2, cx + panelW/2 + i*2, top + panelH + i*2, glow);
        }
        g.fill(cx - panelW/2, top, cx + panelW/2, top + panelH, withAlpha(0xFF090B13, (int)(222 * alpha)));
        g.fill(cx - panelW/2, top, cx + panelW/2, top + 2, main);
        g.fill(cx - panelW/2, top + panelH - 2, cx + panelW/2, top + panelH, accent);
        g.fill(cx - panelW/2, top, cx - panelW/2 + 2, top + panelH, accent);
        g.fill(cx + panelW/2 - 2, top, cx + panelW/2, top + panelH, accent);

        // Coins and sparks are deterministic per frame and tier-specific in density.
        if (config.particles) {
            int count = cosmic ? 44 : grand ? 28 : a.tier.id.equals("super") ? 18 : 10;
            for (int i = 0; i < count; i++) {
                double phase = elapsed * (0.0012 + (i % 5) * 0.00017) + i * 2.399;
                double radius = 35 + (i * 19 % (grand ? 190 : 130));
                int x = cx + (int)(Math.cos(phase) * radius);
                int y = cy + (int)(Math.sin(phase * 1.23) * radius * 0.58);
                int size = i % 6 == 0 ? 4 : 2;
                g.fill(x-size, y-size, x+size, y+size, withAlpha(i % 3 == 0 ? a.tier.accent : a.tier.color, (int)((100 + (i%4)*35) * alpha)));
            }
            int coinCount = grand ? 9 : a.tier.id.equals("super") ? 5 : 2;
            for (int i = 0; i < coinCount; i++) {
                int x = cx - panelW/2 + 18 + (int)((elapsed / (5f + i) + i*57) % (panelW - 36));
                int y = top - 12 - (int)((elapsed / (8f + i) + i*23) % 75);
                g.fill(x-5, y-5, x+6, y+6, withAlpha(0xFFFFD34E, (int)(210*alpha)));
                g.fill(x-2, y-3, x+3, y+4, withAlpha(0xFF9B5B14, (int)(220*alpha)));
            }
        }
        // Tier 5/6 lightning accents.
        if (grand) {
            for (int bolt = 0; bolt < (cosmic ? 5 : 3); bolt++) {
                int bx = cx - panelW/2 + 20 + (bolt * 97 + (int)(elapsed/7)) % (panelW - 40);
                int by = top - 28;
                int col = withAlpha(a.tier.accent, (int)(190 * alpha));
                for (int s = 0; s < 5; s++) {
                    int x1 = bx + (int)(Math.sin(elapsed/90.0 + bolt+s) * 8);
                    int y1 = by + s*10;
                    g.fill(x1, y1, x1+2, y1+12, col);
                }
            }
        }
        g.pose().pushMatrix();
        g.pose().translate(cx, cy);
        float scale = pulse * (0.88f + 0.12f * fadeIn);
        g.pose().scale(scale, scale);
        g.drawCenteredString(mc.font, Component.literal(a.tier.title), 0, -panelH/2 + 14, main);
        g.drawCenteredString(mc.font, Component.literal(MONEY.format(a.amount) + " $"), 0, -5, 0xFFFFFFFF);
        g.drawCenteredString(mc.font, Component.literal("Received from " + a.player), 0, panelH/2 - 22, withAlpha(0xFFE8E8F0, (int)(235*alpha)));
        g.pose().popMatrix();
    }
    private static int withAlpha(int color, int alpha) { return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF); }
}
