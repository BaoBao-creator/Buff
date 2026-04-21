package bao.buff.client.gui;

import bao.buff.client.Config;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AttributeSwapSettingsScreen extends Screen {
    private final Screen lastScreen;

    private Button breachBtn;
    private Button windBtn;
    private Button smartBtn;

    public AttributeSwapSettingsScreen(Screen lastScreen) {
        super(Component.literal("Attribute Swap Setup"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2 - 100;

        breachBtn = Button.builder(getLabel("Swap Breach Mace", Config.swapBreachMace), b -> {
            Config.swapBreachMace = !Config.swapBreachMace;
            if (Config.swapBreachMace) { Config.swapWindMace = false; Config.smartMaceSwap = false; }
            updateButtons();
        }).bounds(centerX, 40, 200, 20).build();

        windBtn = Button.builder(getLabel("Swap Wind Mace", Config.swapWindMace), b -> {
            Config.swapWindMace = !Config.swapWindMace;
            if (Config.swapWindMace) { Config.swapBreachMace = false; Config.smartMaceSwap = false; }
            updateButtons();
        }).bounds(centerX, 70, 200, 20).build();

        smartBtn = Button.builder(getLabel("Smart Mace Swap", Config.smartMaceSwap), b -> {
            Config.smartMaceSwap = !Config.smartMaceSwap;
            if (Config.smartMaceSwap) { Config.swapBreachMace = false; Config.swapWindMace = false; }
            updateButtons();
        }).bounds(centerX, 100, 200, 20).build();

        Button breakShieldBtn = Button.builder(getLabel("Break Shield (Axe)", Config.breakShield), b -> {
            Config.breakShield = !Config.breakShield;
            b.setMessage(getLabel("Break Shield (Axe)", Config.breakShield));
        }).bounds(centerX, 140, 200, 20).build(); // Cách ra 1 khoảng

        Button backBtn = Button.builder(Component.literal("Back"), b -> {
            this.minecraft.setScreen(lastScreen);
        }).bounds(centerX, this.height - 40, 200, 20).build();

        this.addRenderableWidget(breachBtn);
        this.addRenderableWidget(windBtn);
        this.addRenderableWidget(smartBtn);
        this.addRenderableWidget(breakShieldBtn);
        this.addRenderableWidget(backBtn);
    }

    private Component getLabel(String name, boolean state) {
        return Component.literal(name + ": " + (state ? "BẬT" : "TẮT"));
    }

    private void updateButtons() {
        breachBtn.setMessage(getLabel("Swap Breach Mace", Config.swapBreachMace));
        windBtn.setMessage(getLabel("Swap Wind Mace", Config.swapWindMace));
        smartBtn.setMessage(getLabel("Smart Mace Swap", Config.smartMaceSwap));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFF00);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}