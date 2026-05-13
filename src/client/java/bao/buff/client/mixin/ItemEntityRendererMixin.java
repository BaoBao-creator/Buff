package bao.buff.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {
    private static final float BUFF_DROPPED_ITEM_SCALE = 0.5F;

    @Inject(method = "submit", at = @At("HEAD"))
    private void buff$scaleDroppedItems(ItemEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        poseStack.scale(BUFF_DROPPED_ITEM_SCALE, BUFF_DROPPED_ITEM_SCALE, BUFF_DROPPED_ITEM_SCALE);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void buff$freezeDroppedItemAnimation(ItemEntity itemEntity, ItemEntityRenderState state, float tickProgress, CallbackInfo ci) {
        state.ageInTicks = 0.0F;
        state.bobOffset = 0.0F;
    }
}
