package bao.buff.client;

import bao.buff.client.gui.BuffMainMenu;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public class BuffClient implements ClientModInitializer {
    private static KeyMapping openMenuKey;

    @Override
    public void onInitializeClient() {
        // 1. Tạo và đăng ký phím Right Shift để mở Menu
        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.buff.open_menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "category.buff.main"
        ));

        // 2. Lắng nghe sự kiện Tick của Client
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Xử lý mở Menu khi nhấn phím
            while (openMenuKey.consumeClick()) {
                client.setScreen(new BuffMainMenu());
            }
        });
    }
}