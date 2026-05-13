package bao.buff.client.mixin;

import bao.buff.client.util.VisibilityCuller;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ModelBlockRenderer.class)
public abstract class ModelBlockRendererMixin {
    @Inject(method = "shouldRenderFace", at = @At("RETURN"), cancellable = true)
    private static void buff$cullHiddenBlockFaces(BlockAndTintGetter level, BlockState state, boolean cull, Direction direction, BlockPos neighborPos,
            CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && !VisibilityCuller.shouldRenderBlockFace(direction, neighborPos)) {
            cir.setReturnValue(false);
        }
    }
}
