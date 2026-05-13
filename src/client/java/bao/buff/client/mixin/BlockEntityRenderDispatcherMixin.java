package bao.buff.client.mixin;

import bao.buff.client.util.BlockAnimationFreezer;
import bao.buff.client.util.VisibilityCuller;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
    @Shadow
    private Vec3 cameraPos;

    @Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true)
    private <E extends BlockEntity, S extends BlockEntityRenderState> void buff$cullHiddenBlockEntities(E blockEntity, float tickProgress,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, CallbackInfoReturnable<S> cir) {
        Level level = blockEntity.getLevel();
        if (level != null && !VisibilityCuller.shouldRenderBlockEntity(level, blockEntity.getBlockPos(), this.cameraPos)) {
            cir.setReturnValue(null);
        }
    }
    @Inject(method = "tryExtractRenderState", at = @At("RETURN"))
    private <E extends BlockEntity, S extends BlockEntityRenderState> void buff$freezeBlockAnimations(E blockEntity, float tickProgress,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, CallbackInfoReturnable<S> cir) {
        BlockAnimationFreezer.freeze(cir.getReturnValue());
    }
}
