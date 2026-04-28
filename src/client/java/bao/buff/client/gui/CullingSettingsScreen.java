package bao.buff.client.gui;

import bao.buff.client.Config;
import bao.buff.client.CullingManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CullingSettingsScreen extends Screen {
    private final Screen lastScreen;

    public CullingSettingsScreen(Screen lastScreen) {
        super(Component.literal("Culling"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2 - 100;

        this.addRenderableWidget(Button.builder(getPlayerCullingLabel(), button -> {
            Config.playerCulling = !Config.playerCulling;
            CullingManager.resetCache();
            Config.save();
            button.setMessage(getPlayerCullingLabel());
        }).bounds(centerX, 60, 200, 20).build());

        this.addRenderableWidget(Button.builder(getItemCullingLabel(), button -> {
            Config.itemCulling = !Config.itemCulling;
            CullingManager.resetCache();
            Config.save();
            button.setMessage(getItemCullingLabel());
        }).bounds(centerX, 85, 200, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Back"), button -> {
            this.minecraft.setScreen(lastScreen);
        }).bounds(centerX, this.height - 40, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
    }

    private Component getPlayerCullingLabel() {
        return Component.literal("Player Culling: " + Config.getPlayerCullingStatusName());
    }

    private Component getItemCullingLabel() {
        return Component.literal("Item Culling: " + Config.getItemCullingStatusName());
    }
}
