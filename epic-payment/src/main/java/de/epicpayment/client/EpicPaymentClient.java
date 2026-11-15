package de.epicpayment.client;

import de.epicpayment.config.ModConfig;
import de.epicpayment.detect.PaymentParser;
import de.epicpayment.render.PaymentOverlay;
import de.epicpayment.ui.PaymentConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import java.util.LinkedHashMap;
import java.util.Map;

public class EpicPaymentClient implements ClientModInitializer {
    public static ModConfig CONFIG;
    private static KeyMapping openSettings;
    private static final Map<String, Long> RECENT = new LinkedHashMap<>();

    @Override public void onInitializeClient() {
        CONFIG = ModConfig.load();
        PaymentOverlay.initialize(CONFIG);
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> handle(message.getString()));
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, timestamp) -> handle(message.getString()));
        openSettings = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.epicpayment.settings", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, KeyMapping.Category.MISC));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openSettings.consumeClick()) client.setScreen(new PaymentConfigScreen(client.screen, CONFIG));
            long now = System.currentTimeMillis();
            RECENT.entrySet().removeIf(e -> now - e.getValue() > 5000);
        });
    }
    private static void handle(String message) {
        if (!CONFIG.enabled) return;
        PaymentParser.parse(message, CONFIG.customPatterns).ifPresent(payment -> {
            if (payment.amount() < CONFIG.minimumAmount) return;
            long now = System.currentTimeMillis();
            String key = payment.amount() + "|" + payment.player() + "|" + message;
            synchronized (RECENT) {
                if (RECENT.containsKey(key)) return;
                RECENT.put(key, now);
            }
            var tier = CONFIG.tierFor(payment.amount());
            PaymentOverlay.show(payment.amount(), payment.player(), tier);
            Minecraft mc = Minecraft.getInstance();
            if (CONFIG.sounds && mc.getSoundManager() != null) {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.85f, 1.0f));
            }
        });
    }
}
