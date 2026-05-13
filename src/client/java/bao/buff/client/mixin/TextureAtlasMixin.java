package bao.buff.client.mixin;

import net.minecraft.client.renderer.texture.TextureAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureAtlas.class)
public abstract class TextureAtlasMixin {
    @Inject(method = "cycleAnimationFrames", at = @At("HEAD"), cancellable = true)
    private void buff$freezeAnimationFrameUploads(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void buff$freezeTextureAnimations(CallbackInfo ci) {
        ci.cancel();
    }
}
