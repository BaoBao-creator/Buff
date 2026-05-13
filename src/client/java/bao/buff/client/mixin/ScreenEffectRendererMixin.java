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

    @Inject(
            method = "renderFire(Lnet/minecraft/client/Minecraft;Lcom/mojang/blaze3d/vertex/PoseStack;)V",
            at = @At("HEAD"),
            require = 0
    )
    private static void buff$applyLowFireTransform(Minecraft minecraft, PoseStack poseStack, CallbackInfo ci) {
        poseStack.pushPose();
        poseStack.translate(0.0F, -0.95F, 0.0F);
        poseStack.scale(1.0F, LOW_FIRE_HEIGHT_FACTOR, 1.0F);
    }

    @Inject(
            method = "renderFire(Lnet/minecraft/client/Minecraft;Lcom/mojang/blaze3d/vertex/PoseStack;)V",
            at = @At("TAIL"),
            require = 0
    )
    private static void buff$restoreLowFireTransform(Minecraft minecraft, PoseStack poseStack, CallbackInfo ci) {
        poseStack.popPose();
    }
}
