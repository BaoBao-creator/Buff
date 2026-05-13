package bao.buff.client.mixin;

import bao.buff.client.util.BlockAnimationFreezer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
    @Inject(method = "tryExtractRenderState", at = @At("RETURN"))
    private <E extends BlockEntity, S extends BlockEntityRenderState> void buff$freezeBlockAnimations(E blockEntity, float tickProgress,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, CallbackInfoReturnable<S> cir) {
        BlockAnimationFreezer.freeze(cir.getReturnValue());
    }
}
