package bao.buff.client.mixin;

import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void buff$freezeDroppedItemAnimation(ItemEntity itemEntity, ItemEntityRenderState state, float tickProgress, CallbackInfo ci) {
        state.ageInTicks = 0.0F;
        state.bobOffset = 0.0F;
    }
}
