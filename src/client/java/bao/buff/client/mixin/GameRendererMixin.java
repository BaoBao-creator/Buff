package bao.buff.client.mixin;

import net.minecraft.client.renderer.GameRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow private float spinningEffectTime;
    @Shadow private float spinningEffectSpeed;

    @Inject(method = "getDarkenWorldAmount", at = @At("HEAD"), cancellable = true)
    private void buff$disableWorldDarkening(float tickProgress, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(0.0F);
    }

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void buff$disableHurtCam(PoseStack poseStack, float tickProgress, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "getFov", at = @At("HEAD"), cancellable = true)
    private void buff$disableDynamicFov(net.minecraft.client.Camera camera, float tickProgress, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue((float) this.minecraft.options.fov().get().intValue());
    }

    @Inject(method = "checkEntityPostEffect", at = @At("HEAD"), cancellable = true)
    private void buff$disablePostEffects(net.minecraft.world.entity.Entity entity, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void buff$disableSpinningAndPortalEffects(CallbackInfo ci) {
        this.spinningEffectTime = 0.0F;
        this.spinningEffectSpeed = 0.0F;
    }
}
