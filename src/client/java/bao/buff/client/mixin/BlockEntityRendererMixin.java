package bao.buff.client.mixin;

import bao.buff.client.util.BlockAnimationFreezer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderer.class)
public abstract class BlockEntityRendererMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void buff$freezeBlockAnimations(Object blockEntity, Object state, float tickProgress, Object cameraPos,
            Object crumblingOverlay, CallbackInfo ci) {
        BlockAnimationFreezer.freeze(state);
    }
}
