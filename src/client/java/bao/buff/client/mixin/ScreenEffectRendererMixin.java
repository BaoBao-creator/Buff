package bao.buff.client.mixin;

import bao.buff.client.Config;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {

    @Inject(
        method = "renderFire", 
        at = @At("HEAD"), 
        cancellable = true
    )
    private static void onRenderFire(Minecraft minecraft, PoseStack poseStack, CallbackInfo ci) {
        if (Config.noFireEnabled) {
            // Hủy việc thực hiện hàm renderFire gốc của Minecraft
            ci.cancel();
        }
    }
}