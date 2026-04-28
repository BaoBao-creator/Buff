package bao.buff.client.mixin;

import bao.buff.client.CullingManager;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Shadow public Camera camera;

    @Inject(method = "shouldRender", at = @At("RETURN"), cancellable = true)
    private void buff$applyEntityCulling(Entity entity, Frustum frustum, double d, double e, double f, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() || this.camera == null) {
            return;
        }

        if (entity instanceof Player player && !CullingManager.shouldRenderPlayer(player, this.camera.position())) {
            cir.setReturnValue(false);
            return;
        }

        if (entity instanceof ItemEntity item && !CullingManager.shouldRenderItem(item, this.camera.position())) {
            cir.setReturnValue(false);
        }
    }
}
