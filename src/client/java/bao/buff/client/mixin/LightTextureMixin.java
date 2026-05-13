package bao.buff.client.mixin;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightTexture.class)
public abstract class LightTextureMixin {
    @Inject(method = "getBrightness(Lnet/minecraft/world/level/dimension/DimensionType;I)F", at = @At("HEAD"), cancellable = true)
    private static void buff$fullBrightDimension(DimensionType type, int lightLevel, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(1.0F);
    }

    @Inject(method = "getBrightness(FI)F", at = @At("HEAD"), cancellable = true)
    private static void buff$fullBrightAmbient(float ambientLight, int lightLevel, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(1.0F);
    }

    @Inject(method = "pack", at = @At("HEAD"), cancellable = true)
    private static void buff$packFullBright(int block, int sky, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(LightTexture.FULL_BRIGHT);
    }

    @Inject(method = "lightCoordsWithEmission", at = @At("HEAD"), cancellable = true)
    private static void buff$emissionFullBright(int light, int lightEmission, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(LightTexture.FULL_BRIGHT);
    }
}
