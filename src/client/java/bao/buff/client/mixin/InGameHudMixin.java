package bao.buff.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.class_329")
public abstract class InGameHudMixin {
    @Inject(method = {"renderNausea", "method_55440(Lnet/minecraft/class_332;F)V"}, at = @At("HEAD"), cancellable = true)
    private void buff$disableConfusionOverlay(Object context, float tickProgress, CallbackInfo ci) {
        ci.cancel();
    }
}
