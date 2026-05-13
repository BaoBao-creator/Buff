package bao.buff.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {
    private static final float LOW_FIRE_HEIGHT_FACTOR = 0.05F;

    @Inject(method = "renderFire", at = @At("HEAD"))
    private static void buff$applyLowFireTransform(Minecraft minecraft, PoseStack poseStack, CallbackInfo ci) {
        poseStack.pushPose();
        poseStack.translate(0.0F, -0.95F, 0.0F);
        poseStack.scale(1.0F, LOW_FIRE_HEIGHT_FACTOR, 1.0F);
    }

    @Inject(method = "renderFire", at = @At("RETURN"))
    private static void buff$restoreLowFireTransform(Minecraft minecraft, PoseStack poseStack, CallbackInfo ci) {
        poseStack.popPose();
    }

    @Inject(method = "renderScreenEffect", at = @At("HEAD"), cancellable = true)
    private static void buff$disableBlockingScreenEffects(Minecraft minecraft, PoseStack poseStack, CallbackInfo ci) {
        ci.cancel();
    }
}
