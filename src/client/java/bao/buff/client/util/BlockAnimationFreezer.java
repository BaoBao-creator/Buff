package bao.buff.client.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Set;

import net.minecraft.world.entity.AnimationState;

public final class BlockAnimationFreezer {
    private static final Set<String> ANIMATED_FIELDS = Set.of(
            // BrushableBlockRenderState
            "BrushableBlockRenderState#dustProgress", "BrushableBlockEntityRenderState#dusted", "class_11957#field_62689",
            // BellRenderState
            "BellRenderState#ticks", "BellBlockEntityRenderState#ringTicks", "class_11953#field_62672",
            // ChestRenderState
            "ChestRenderState#open", "ChestBlockEntityRenderState#lidAnimationProgress", "class_11959#field_62694",
            // CondiutRenderState / ConduitBlockEntityRenderState
            "CondiutRenderState#animTime", "CondiutRenderState#activeRotation", "CondiutRenderState#animationPhase",
            "ConduitBlockEntityRenderState#ticks", "ConduitBlockEntityRenderState#rotation", "ConduitBlockEntityRenderState#rotationPhase",
            "class_11961#field_62706", "class_11961#field_62708", "class_11961#field_62709",
            // DecoratedPotRenderState
            "DecoratedPotRenderState#wobbleProgress", "DecoratedPotBlockEntityRenderState#wobbleAnimationProgress", "class_11963#field_62715",
            // EnchantTableRenderState
            "EnchantTableRenderState#time", "EnchantTableRenderState#yRot", "EnchantTableRenderState#flip", "EnchantTableRenderState#open",
            "EnchantingTableBlockEntityRenderState#ticks", "EnchantingTableBlockEntityRenderState#bookRotationDegrees",
            "EnchantingTableBlockEntityRenderState#pageAngle", "EnchantingTableBlockEntityRenderState#pageTurningSpeed",
            "class_11964#field_62718", "class_11964#field_62719", "class_11964#field_62720", "class_11964#field_62721",
            // PistonHeadRenderState
            "PistonHeadRenderState#xOffset", "PistonHeadRenderState#yOffset", "PistonHeadRenderState#zOffset",
            "PistonBlockEntityRenderState#offsetX", "PistonBlockEntityRenderState#offsetY", "PistonBlockEntityRenderState#offsetZ",
            "class_11968#field_62731", "class_11968#field_62732", "class_11968#field_62733",
            // ShulkerBoxRenderState
            "ShulkerBoxRenderState#progress", "ShulkerBoxBlockEntityRenderState#animationProgress", "class_11970#field_62738",
            // SpawnerRenderState
            "SpawnerRenderState#spin", "MobSpawnerBlockEntityRenderState#displayEntityRotation", "class_11973#field_62751",
            // VaultRenderState
            "VaultRenderState#spin", "VaultBlockEntityRenderState#displayRotationDegrees", "class_11975#field_62756"
    );

    private static final Set<String> ANIMATED_FIELD_NAMES = Set.of(
            "animTime", "activeRotation", "animationPhase", "bookRotationDegrees", "displayEntityRotation",
            "displayRotationDegrees", "dustProgress", "dusted", "flip", "lidAnimationProgress", "offsetX", "offsetY", "offsetZ", "open",
            "pageAngle", "pageTurningSpeed", "progress", "ringTicks", "spin", "ticks", "wobbleAnimationProgress",
            "wobbleProgress", "xOffset", "yOffset", "zOffset"
    );

    private BlockAnimationFreezer() {
    }

    public static void freeze(Object renderState) {
        if (renderState == null) {
            return;
        }

        for (Class<?> stateClass = renderState.getClass(); stateClass != null; stateClass = stateClass.getSuperclass()) {
            freezeFields(renderState, stateClass);
        }
    }

    private static void freezeFields(Object renderState, Class<?> stateClass) {
        String simpleClassName = stateClass.getSimpleName();
        for (Field field : stateClass.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            Class<?> fieldType = field.getType();
            if (fieldType == AnimationState.class) {
                stopAnimationState(renderState, field);
                continue;
            }

            if (!shouldFreezeField(simpleClassName, field.getName())) {
                continue;
            }

            try {
                field.setAccessible(true);
                if (fieldType == float.class) {
                    field.setFloat(renderState, 0.0F);
                } else if (fieldType == double.class) {
                    field.setDouble(renderState, 0.0D);
                } else if (fieldType == int.class) {
                    field.setInt(renderState, 0);
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Some modded render states may block reflective writes; leave those fields unchanged.
            }
        }
    }

    private static boolean shouldFreezeField(String simpleClassName, String fieldName) {
        return ANIMATED_FIELD_NAMES.contains(fieldName) || ANIMATED_FIELDS.contains(simpleClassName + "#" + fieldName);
    }

    private static void stopAnimationState(Object renderState, Field field) {
        try {
            field.setAccessible(true);
            AnimationState animationState = (AnimationState) field.get(renderState);
            if (animationState != null) {
                animationState.stop();
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Some modded render states may block reflective access; leave those fields unchanged.
        }
    }
}
