package bao.buff.client.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import net.minecraft.client.Camera;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "addSkyPass", at = @At("HEAD"), cancellable = true)
    private void buff$skipSkyPass(FrameGraphBuilder frameGraphBuilder, Camera camera, GpuBufferSlice fogBuffer, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "addCloudsPass", at = @At("HEAD"), cancellable = true)
    private void buff$skipCloudsPass(FrameGraphBuilder frameGraphBuilder, CloudStatus mode, Vec3 cameraPos, long ticks, float tickProgress,
            int color, float cloudHeight, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "addWeatherPass", at = @At("HEAD"), cancellable = true)
    private void buff$skipWeatherPass(FrameGraphBuilder frameGraphBuilder, GpuBufferSlice fogBuffer, CallbackInfo ci) {
        ci.cancel();
    }
}
