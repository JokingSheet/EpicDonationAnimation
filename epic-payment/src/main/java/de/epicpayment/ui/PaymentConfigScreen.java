package de.epicpayment.ui;

import de.epicpayment.config.ModConfig;
import de.epicpayment.config.PaymentTier;
import de.epicpayment.render.PaymentOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PaymentConfigScreen extends Screen {
    private final Screen parent;
    private final ModConfig config;
    public PaymentConfigScreen(Screen parent, ModConfig config) {
        super(Component.literal("Epic Payment Settings")); this.parent = parent; this.config = config;
    }
    @Override protected void init() {
        int center = width / 2, y = height / 4;
        addRenderableWidget(Button.builder(toggle("Animations", config.enabled), b -> { config.enabled = !config.enabled; rebuild(); }).bounds(center-155, y, 150, 20).build());
        addRenderableWidget(Button.builder(toggle("Sounds", config.sounds), b -> { config.sounds = !config.sounds; rebuild(); }).bounds(center+5, y, 150, 20).build());
        addRenderableWidget(Button.builder(toggle("Particles", config.particles), b -> { config.particles = !config.particles; rebuild(); }).bounds(center-155, y+26, 150, 20).build());
        addRenderableWidget(Button.builder(toggle("Screen shake", config.screenShake), b -> { config.screenShake = !config.screenShake; rebuild(); }).bounds(center+5, y+26, 150, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Duration: " + config.durationSeconds + "s  (-)"), b -> { config.durationSeconds = Math.max(2, config.durationSeconds-1); rebuild(); }).bounds(center-155, y+56, 150, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Duration: " + config.durationSeconds + "s  (+)"), b -> { config.durationSeconds = Math.min(20, config.durationSeconds+1); rebuild(); }).bounds(center+5, y+56, 150, 20).build());
        int tierY = y + 88;
        for (PaymentTier tier : config.tiers) {
            addRenderableWidget(Button.builder(Component.literal("Preview: " + tier.title), b -> PaymentOverlay.show(tier.threshold, "PreviewPlayer", tier)).bounds(center-155, tierY, 190, 20).build());
            long step = Math.max(100_000L, tier.threshold / 10L);
            addRenderableWidget(Button.builder(Component.literal("−"), b -> { tier.threshold = Math.max(1L, tier.threshold-step); config.save(); rebuild(); }).bounds(center+39, tierY, 38, 20).build());
            addRenderableWidget(Button.builder(Component.literal("+"), b -> { tier.threshold = Math.min(Long.MAX_VALUE/2, tier.threshold+step); config.save(); rebuild(); }).bounds(center+81, tierY, 38, 20).build());
            tierY += 23;
        }
        addRenderableWidget(Button.builder(Component.literal("Save & Close"), b -> { config.save(); minecraft.setScreen(parent); }).bounds(center-100, height-34, 200, 20).build());
    }
    private Component toggle(String name, boolean enabled) { return Component.literal(name + ": " + (enabled ? "ON" : "OFF")); }
    private void rebuild() { clearWidgets(); init(); }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(font, title, width/2, 24, 0xFFFFD34E);
        graphics.drawCenteredString(font, Component.literal("Minimum amount: " + config.minimumAmount + " (edit config/epicpayment.json)"), width/2, 47, 0xFFE0E0E0);
        super.render(graphics, mouseX, mouseY, delta);
    }
    @Override public void onClose() { config.save(); minecraft.setScreen(parent); }
}
