package de.epicdonation;

import de.epicdonation.config.DonationConfig;
import de.epicdonation.detection.ChatDetector;
import de.epicdonation.render.DonationHud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class EpicDonationClient implements ClientModInitializer {
    public static final String MOD_ID = "epicdonation";
    private static KeyMapping configKey;

    @Override
    public void onInitializeClient() {
        DonationConfig.load();
        ChatDetector.init();
        ClientReceiveMessageEvents.GAME.register((message, overlay) ->
                ChatDetector.inspect(message.getString()));
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) ->
                ChatDetector.inspect(message.getString()));
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(MOD_ID, "donation_hud"),
                DonationHud::render
        );
        configKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.epicdonation.config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                KeyMapping.Category.MISC
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (configKey.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new de.epicdonation.config.DonationConfigScreen(null));
                }
            }
            DonationHud.tick();
        });
    }

    public static Minecraft client() {
        return Minecraft.getInstance();
    }
}
