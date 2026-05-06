package bao.buff.client.gui;

import bao.buff.client.Config;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PearlUpSettingsScreen extends Screen {
    private final Screen lastScreen;

    public PearlUpSettingsScreen(Screen lastScreen) {
        super(Component.literal("Pearl Up"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2 - 100;

        this.addRenderableWidget(Button.builder(getToggleLabel(), button -> {
            Config.pearlUpEnabled = !Config.pearlUpEnabled;
            Config.save();
            button.setMessage(getToggleLabel());
        }).bounds(centerX, 60, 200, 20).build());

        this.addRenderableWidget(new AbstractSliderButton(
            centerX,
            90,
            200,
            20,
            getPitchSpeedLabel(),
            speedToSliderValue(Config.pearlUpPitchSpeed)
        ) {
            {
                updateMessage();
            }

            @Override
            protected void updateMessage() {
                setMessage(getPitchSpeedLabel());
            }

            @Override
            protected void applyValue() {
                Config.pearlUpPitchSpeed = sliderValueToSpeed(this.value);
                Config.save();
            }
        });

        this.addRenderableWidget(new AbstractSliderButton(
            centerX,
            120,
            200,
            20,
            getWindChargeDelayLabel(),
            delayToSliderValue(Config.pearlUpWindChargeDelayTicks)
        ) {
            {
                updateMessage();
            }

            @Override
            protected void updateMessage() {
                setMessage(getWindChargeDelayLabel());
            }

            @Override
            protected void applyValue() {
                Config.pearlUpWindChargeDelayTicks = sliderValueToDelay(this.value);
                Config.save();
            }
        });

        this.addRenderableWidget(Button.builder(Component.literal("Back"), button -> {
            this.minecraft.setScreen(lastScreen);
        }).bounds(centerX, this.height - 40, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
    }

    private Component getToggleLabel() {
        return Component.literal("Pearl Up: " + Config.getPearlUpStatusName());
    }

    private static Component getPitchSpeedLabel() {
        return Component.literal("Tốc độ ngước lên: " + Math.round(Config.pearlUpPitchSpeed) + "°/tick");
    }

    private static Component getWindChargeDelayLabel() {
        return Component.literal("Đợi ném Wind Charge: " + Config.pearlUpWindChargeDelayTicks + " tick");
    }

    private static double speedToSliderValue(double speed) {
        return (clamp(speed, 1.0, 90.0) - 1.0) / 89.0;
    }

    private static double sliderValueToSpeed(double value) {
        return Math.round(1.0 + clamp(value, 0.0, 1.0) * 89.0);
    }

    private static double delayToSliderValue(int delay) {
        return (clamp(delay, 1, 10) - 1) / 9.0;
    }

    private static int sliderValueToDelay(double value) {
        return (int) Math.round(1 + clamp(value, 0.0, 1.0) * 9.0);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
