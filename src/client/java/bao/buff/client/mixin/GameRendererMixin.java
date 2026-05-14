package bao.buff.client.mixin;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "getDarkenWorldAmount", at = @At("HEAD"), cancellable = true)
    private void buff$disableWorldDarkening(float tickProgress, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(0.0F);
    }
}
