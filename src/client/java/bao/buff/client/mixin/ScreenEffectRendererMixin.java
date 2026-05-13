package bao.buff.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {
    private static final float BUFF_FIRE_OVERLAY_SCALE = 0.2236068F; // sqrt(5%)

    @Inject(
            method = "renderFire(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V",
            at = @At("HEAD")
    )
    private static void buff$scaleFireOverlayAtHead(PoseStack poseStack, MultiBufferSource multiBufferSource, TextureAtlasSprite textureAtlasSprite, CallbackInfo ci) {
        poseStack.pushPose();
        float offset = (1.0F - BUFF_FIRE_OVERLAY_SCALE) * 0.5F;
        poseStack.translate(offset, offset, 0.0F);
        poseStack.scale(BUFF_FIRE_OVERLAY_SCALE, BUFF_FIRE_OVERLAY_SCALE, 1.0F);
    }

    @Inject(
            method = "renderFire(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V",
            at = @At("RETURN")
    )
    private static void buff$restoreFireOverlayTransform(PoseStack poseStack, MultiBufferSource multiBufferSource, TextureAtlasSprite textureAtlasSprite, CallbackInfo ci) {
        poseStack.popPose();
    }
}
