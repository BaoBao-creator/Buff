package bao.buff.client.mixin;

import bao.buff.client.Config;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {

    @Inject(
        method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At("HEAD")
    )
    private void onRenderItem(LivingEntity entity, ItemStack itemStack, ItemDisplayContext displayContext, boolean leftHanded, PoseStack poseStack, MultiBufferSource buffer, int light, CallbackInfo ci) {
        
        // SỬA TẠI ĐÂY: Đổi displayContext.isFirstPerson() thành displayContext.firstPerson()
        if (displayContext.firstPerson() && Config.smallItemEnabled) {
            float scale = (float) Config.itemScale; // Chỉnh kích thước (0.7 = 70%)
            poseStack.scale(scale, scale, scale);
        }
    }
}