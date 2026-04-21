package bao.buff.client.mixin;

import bao.buff.client.Config;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightTexture.class)
public class LightTextureMixin {

    // Can thiệp vào phương thức getBrightness của LightTexture
    @Inject(
        method = "getBrightness", 
        at = @At("HEAD"), 
        cancellable = true
    )
    private static void onGetBrightness(DimensionType dimensionType, int lightLevel, CallbackInfoReturnable<Float> cir) {
        if (Config.fullBrightEnabled) {
            // Trả về độ sáng tối đa (1.0f)
            cir.setReturnValue(1.0f);
        }
    }
}