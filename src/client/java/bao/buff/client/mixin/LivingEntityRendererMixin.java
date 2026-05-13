package bao.buff.client.mixin;

import java.lang.reflect.Field;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void buff$freezeMobAnimations(LivingEntity entity, LivingEntityRenderState state, float tickProgress, CallbackInfo ci) {
        if (!(entity instanceof Mob)) {
            return;
        }

        state.ageInTicks = 0.0F;
        state.walkAnimationPos = 0.0F;
        state.walkAnimationSpeed = 0.0F;
        state.xRot = 0.0F;
        state.yRot = 0.0F;
        state.deathTime = 0.0F;
        state.ticksSinceKineticHitFeedback = 0.0F;
        state.wornHeadAnimationPos = 0.0F;
        state.hasRedOverlay = false;
        state.isAutoSpinAttack = false;
        state.isFullyFrozen = false;

        buff$stopAnimationStates(state);
    }

    private static void buff$stopAnimationStates(LivingEntityRenderState state) {
        for (Field field : state.getClass().getFields()) {
            if (field.getType() != AnimationState.class) {
                continue;
            }

            try {
                AnimationState animationState = (AnimationState) field.get(state);
                if (animationState != null) {
                    animationState.stop();
                }
            } catch (IllegalAccessException ignored) {
                // Public render-state fields should be accessible; ignore any modded edge cases.
            }
        }
    }
}
