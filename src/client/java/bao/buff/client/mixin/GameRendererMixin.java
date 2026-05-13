package bao.buff.client.mixin;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow @Final Minecraft minecraft;

    @Inject(method = "getDarkenWorldAmount", at = @At("HEAD"), cancellable = true)
    private void buff$disableWorldDarkening(float tickProgress, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(0.0F);
    }

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void buff$disableHurtCam(PoseStack poseStack, float tickProgress, CallbackInfo ci) {
        ci.cancel();
    }


    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void buff$lockFov(Camera camera, float tickProgress, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(this.minecraft.options.fov().get().floatValue());
    }
}
