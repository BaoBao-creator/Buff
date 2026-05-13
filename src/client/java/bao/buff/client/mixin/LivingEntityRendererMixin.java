package bao.buff.client.mixin;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
    private static final ConcurrentMap<Class<?>, Field[]> BUFF_ANIMATION_STATE_FIELDS = new ConcurrentHashMap<>();

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
        for (Field field : BUFF_ANIMATION_STATE_FIELDS.computeIfAbsent(state.getClass(), LivingEntityRendererMixin::buff$animationStateFields)) {
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

    private static Field[] buff$animationStateFields(Class<?> stateClass) {
        return Arrays.stream(stateClass.getFields())
                .filter(field -> field.getType() == AnimationState.class)
                .toArray(Field[]::new);
    }
}
