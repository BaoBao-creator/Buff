package bao.buff.client.mixin;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class BlockMixin {
    @Inject(method = "shouldRenderFace", at = @At("HEAD"), cancellable = true)
    private static void buff$skipCoveredStaticFaces(BlockState state, BlockState adjacentState, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (!state.canOcclude() || !adjacentState.canOcclude()) {
            return;
        }

        VoxelShape faceShape = state.getFaceOcclusionShape(direction);
        VoxelShape adjacentFaceShape = adjacentState.getFaceOcclusionShape(direction.getOpposite());
        if (Block.isFaceFull(faceShape, direction) && Block.isFaceFull(adjacentFaceShape, direction.getOpposite())) {
            cir.setReturnValue(false);
        }
    }
}
