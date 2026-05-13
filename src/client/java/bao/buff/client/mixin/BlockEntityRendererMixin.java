package bao.buff.client.mixin;

import bao.buff.client.util.BlockAnimationFreezer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderer.class)
public interface BlockEntityRendererMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void buff$freezeBlockAnimations(BlockEntity blockEntity, BlockEntityRenderState state, float tickProgress, Vec3 cameraPos,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, CallbackInfo ci) {
        BlockAnimationFreezer.freeze(state);
    }
}
