// Đường dẫn: src/client/java/bao/buff/client/gui/TriggerBotSettingsScreen.java

package bao.buff.client.gui;

import bao.buff.client.Config;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class TriggerBotSettingsScreen extends Screen {
    private final Screen lastScreen;

    public TriggerBotSettingsScreen(Screen lastScreen) {
        super(Component.literal("Trigger Bot Settings"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2 - 100;
        int centerY = 60;

        // Nút 1: Bật/Tắt Trigger Bot
        this.addRenderableWidget(Button.builder(getToggleLabel(), b -> {
            Config.triggerBot = !Config.triggerBot;
            b.setMessage(getToggleLabel());
        }).bounds(centerX, centerY, 200, 20).build());

        // Nút 2: Chế độ đánh (Check Cooldown hoặc Spam Click)
        this.addRenderableWidget(Button.builder(getModeLabel(), b -> {
            Config.ignoreCooldown = !Config.ignoreCooldown;
            b.setMessage(getModeLabel());
        }).bounds(centerX, centerY + 30, 200, 20).build());

        // Nút Quay lại
        this.addRenderableWidget(Button.builder(Component.literal("Back"), b -> {
            this.minecraft.setScreen(lastScreen);
        }).bounds(centerX, this.height - 40, 200, 20).build());
    }

    private Component getToggleLabel() {
        return Component.literal("Trigger Bot: " + (Config.triggerBot ? "§aON" : "§cOFF"));
    }

    private Component getModeLabel() {
        if (Config.ignoreCooldown) {
            return Component.literal("Mode: §eSpam Click (1 tick)");
        } else {
            return Component.literal("Mode: §bCheck Cooldown");
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}