package bao.buff.client.gui;

import bao.buff.client.Config;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

public class BuffMainMenu extends Screen {
    private EditBox searchBox;
    private final List<Feature> allFeatures = new ArrayList<>();
    private final List<Button> featureButtons = new ArrayList<>();

    public BuffMainMenu() {
        super(Component.literal("Buff Mod Menu"));

        // --- ĐĂNG KÝ CÁC CHỨC NĂNG TẠI ĐÂY ---
        
        // 1. Small Items (Vẫn giữ Menu riêng vì có Slider)
        allFeatures.add(new Feature(
            "Small Items", 
            () -> Component.literal("Small Items >"), 
            () -> this.minecraft.setScreen(new SmallItemSettingsScreen(this))
        ));

        // 2. Auto Sprint (Toggle trực tiếp)
        allFeatures.add(new Feature(
            "Auto Sprint", 
            () -> Component.literal("Auto Sprint: " + (Config.autoSprintEnabled ? "BẬT" : "TẮT")), 
            () -> { Config.autoSprintEnabled = !Config.autoSprintEnabled; }
        ));

        // 3. Full Bright (Toggle trực tiếp)
        allFeatures.add(new Feature(
            "Full Bright", 
            () -> Component.literal("Full Bright: " + (Config.fullBrightEnabled ? "BẬT" : "TẮT")), 
            () -> { Config.fullBrightEnabled = !Config.fullBrightEnabled; }
        ));

        // 4. No Fire Overlay (Toggle trực tiếp)
        allFeatures.add(new Feature(
            "No Fire Overlay", 
            () -> Component.literal("No Fire Overlay: " + (Config.noFireEnabled ? "BẬT" : "TẮT")), 
            () -> { Config.noFireEnabled = !Config.noFireEnabled; }
        ));
        allFeatures.add(new Feature(
            "Attribute Swap", 
            () -> Component.literal("Attribute Swap >"), 
            () -> this.minecraft.setScreen(new AttributeSwapSettingsScreen(this))
        ));
        allFeatures.add(new Feature(
            "Trigger Bot", 
            () -> Component.literal("Trigger Bot >"), 
            () -> this.minecraft.setScreen(new TriggerBotSettingsScreen(this))
        ));
        // Sắp xếp theo bảng chữ cái theo baseName để tìm kiếm dễ dàng
        allFeatures.sort(Comparator.comparing(f -> f.baseName));
    }

    @Override
    protected void init() {
        this.searchBox = new EditBox(this.font, this.width / 2 - 100, 40, 200, 20, Component.literal("Search..."));
        this.searchBox.setHint(Component.literal("Tìm kiếm chức năng..."));
        this.searchBox.setResponder(this::updateSearch);
        
        this.addRenderableWidget(this.searchBox);
        this.setInitialFocus(this.searchBox);

        // Hiển thị danh sách nút bấm lần đầu
        updateSearch(this.searchBox.getValue());
    }

    private void updateSearch(String query) {
        // Xóa các nút cũ đi khi tìm kiếm
        for (Button btn : featureButtons) {
            this.removeWidget(btn);
        }
        featureButtons.clear();

        int yOffset = 70;
        for (Feature feature : allFeatures) {
            if (feature.baseName.toLowerCase().contains(query.toLowerCase())) {
                
                // Lấy tên nút linh hoạt từ Supplier
                Button btn = Button.builder(feature.labelSupplier.get(), (button) -> {
                    feature.action.run(); // Chạy lệnh bật/tắt (hoặc mở screen)
                    button.setMessage(feature.labelSupplier.get()); // Cập nhật lại text ngay lập tức
                })
                .bounds(this.width / 2 - 100, yOffset, 200, 20)
                .build();
                
                this.addRenderableWidget(btn);
                featureButtons.add(btn);
                yOffset += 25;
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFF00);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    // Cấu trúc mới: 
    // baseName (String cố định để Search)
    // labelSupplier (Chức năng gọi text động theo Config)
    // action (Hành động chạy khi click)
    private record Feature(String baseName, Supplier<Component> labelSupplier, Runnable action) {}
}