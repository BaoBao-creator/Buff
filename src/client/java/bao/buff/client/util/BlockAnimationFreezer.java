package bao.buff.client.util;

import net.minecraft.client.renderer.blockentity.state.BellRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.blockentity.state.BrushableBlockRenderState;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.blockentity.state.CondiutRenderState;
import net.minecraft.client.renderer.blockentity.state.DecoratedPotRenderState;
import net.minecraft.client.renderer.blockentity.state.EnchantTableRenderState;
import net.minecraft.client.renderer.blockentity.state.PistonHeadRenderState;
import net.minecraft.client.renderer.blockentity.state.ShulkerBoxRenderState;
import net.minecraft.client.renderer.blockentity.state.SpawnerRenderState;
import net.minecraft.client.renderer.blockentity.state.VaultRenderState;

public final class BlockAnimationFreezer {
    private BlockAnimationFreezer() {
    }

    public static void freeze(BlockEntityRenderState renderState) {
        if (renderState instanceof BellRenderState state) {
            state.ticks = 0.0F;
        } else if (renderState instanceof BrushableBlockRenderState state) {
            state.dustProgress = 0;
        } else if (renderState instanceof ChestRenderState state) {
            state.open = 0.0F;
        } else if (renderState instanceof CondiutRenderState state) {
            state.animTime = 0.0F;
            state.activeRotation = 0.0F;
            state.animationPhase = 0;
        } else if (renderState instanceof DecoratedPotRenderState state) {
            state.wobbleProgress = 0.0F;
        } else if (renderState instanceof EnchantTableRenderState state) {
            state.time = 0.0F;
            state.yRot = 0.0F;
            state.flip = 0.0F;
            state.open = 0.0F;
        } else if (renderState instanceof PistonHeadRenderState state) {
            state.xOffset = 0.0F;
            state.yOffset = 0.0F;
            state.zOffset = 0.0F;
        } else if (renderState instanceof ShulkerBoxRenderState state) {
            state.progress = 0.0F;
        } else if (renderState instanceof SpawnerRenderState state) {
            state.spin = 0.0F;
        } else if (renderState instanceof VaultRenderState state) {
            state.spin = 0.0F;
        }
    }
}
