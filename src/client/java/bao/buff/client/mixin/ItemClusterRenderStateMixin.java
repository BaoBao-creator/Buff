package bao.buff.client.mixin;

import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ItemClusterRenderState.class)
public abstract class ItemClusterRenderStateMixin {
    @ModifyArg(
            method = "extractItemGroupRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/item/ItemModelResolver;updateForTopItem(Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/ItemOwner;I)V"),
            index = 2,
            require = 0)
    private ItemDisplayContext buff$renderTopGroundItemsAsFlat2d(ItemDisplayContext displayContext) {
        return ItemDisplayContext.GUI;
    }

    @ModifyArg(
            method = "extractItemGroupRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/item/ItemModelResolver;updateForNonLiving(Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/entity/Entity;)V"),
            index = 2,
            require = 0)
    private ItemDisplayContext buff$renderNonLivingGroundItemsAsFlat2d(ItemDisplayContext displayContext) {
        return ItemDisplayContext.GUI;
    }
}
