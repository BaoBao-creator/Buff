package bao.buff.client.gui;

import bao.buff.client.Config;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

public class SmallItemSettingsScreen extends Screen {
    private final Screen lastScreen;

    public SmallItemSettingsScreen(Screen lastScreen) {
        super(Component.literal("Small Items Setup"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        // 1. Nút Bật/Tắt (Toggle)
        this.addRenderableWidget(Button.builder(
            Component.literal("Trạng thái: " + (Config.smallItemEnabled ? "BẬT" : "TẮT")), 
            (button) -> {
                Config.smallItemEnabled = !Config.smallItemEnabled;
                button.setMessage(Component.literal("Trạng thái: " + (Config.smallItemEnabled ? "BẬT" : "TẮT")));
            })
            .bounds(this.width / 2 - 100, 50, 200, 20).build());

        // 2. Thanh trượt (Slider) để chỉnh độ lớn nhỏ (từ 0.1 đến 1.0)
        this.addRenderableWidget(new AbstractSliderButton(this.width / 2 - 100, 80, 200, 20, Component.literal("Độ lớn"), Config.itemScale) {
            { updateMessage(); }
            @Override
            protected void updateMessage() {
                setMessage(Component.literal("Độ lớn: " + (int)(Config.itemScale * 100) + "%"));
            }
            @Override
            protected void applyValue() {
                Config.itemScale = this.value; // value của slider mặc định từ 0.0 -> 1.0
            }
        });

        // 3. Nút Quay lại
        this.addRenderableWidget(Button.builder(Component.literal("Back"), (button) -> {
            this.minecraft.setScreen(lastScreen);
        }).bounds(this.width / 2 - 100, this.height - 40, 200, 20).build());
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}