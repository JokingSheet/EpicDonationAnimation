package de.epicdonation.config;

import de.epicdonation.render.DonationHud;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class DonationConfigScreen extends Screen {
    private final Screen parent;
    public DonationConfigScreen(Screen parent) {
        super(Component.literal("Epic Donation Animation"));
        this.parent = parent;
    }
    @Override
    protected void init() {
        DonationConfig c = DonationConfig.get();
        int center = width / 2;
        int y = height / 4;
        addRenderableWidget(Button.builder(toggle("Animationen", c.enabled), b -> {
            c.enabled = !c.enabled; b.setMessage(toggle("Animationen", c.enabled)); DonationConfig.save();
        }).bounds(center-155, y, 150, 20).build());
        addRenderableWidget(Button.builder(toggle("Sounds", c.sounds), b -> {
            c.sounds = !c.sounds; b.setMessage(toggle("Sounds", c.sounds)); DonationConfig.save();
        }).bounds(center+5, y, 150, 20).build());
        addRenderableWidget(Button.builder(toggle("Partikel", c.particles), b -> {
            c.particles = !c.particles; b.setMessage(toggle("Partikel", c.particles)); DonationConfig.save();
        }).bounds(center-155, y+28, 150, 20).build());
        addRenderableWidget(Button.builder(toggle("Bildschirm-Vibration", c.screenShake), b -> {
            c.screenShake = !c.screenShake; b.setMessage(toggle("Bildschirm-Vibration", c.screenShake)); DonationConfig.save();
        }).bounds(center+5, y+28, 150, 20).build());
        for (int i=0; i<c.tiers.size(); i++) {
            final int index = i;
            addRenderableWidget(Button.builder(Component.literal("Vorschau: " + c.tiers.get(i).id),
                    b -> DonationHud.preview(index))
                    .bounds(center-155 + (i%2)*160, y+66+(i/2)*27, 150, 20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Schließen"), b -> onClose())
                .bounds(center-75, height-34, 150, 20).build());
    }
    private static Component toggle(String name, boolean enabled) {
        return Component.literal(name + ": " + (enabled ? "AN" : "AUS"));
    }
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(font, title, width/2, 24, 0xFFFFD54F);
        graphics.drawCenteredString(font, "Taste O öffnet dieses Menü", width/2, 42, 0xFFBBBBBB);
        graphics.drawCenteredString(font, "Betragsgrenzen und Dauer: config/epicdonation.json", width/2, height-52, 0xFFBBBBBB);
        super.render(graphics, mouseX, mouseY, delta);
    }
    @Override
    public void onClose() {
        DonationConfig.save();
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
