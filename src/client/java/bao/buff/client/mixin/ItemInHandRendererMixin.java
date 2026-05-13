package bao.buff.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    private static final float BUFF_FIRST_PERSON_ITEM_SCALE = 0.4F;

    @Inject(method = "renderItem", at = @At("HEAD"))
    private void buff$scaleFirstPersonItems(LivingEntity entity, ItemStack itemStack, ItemDisplayContext displayContext,
            PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        if (displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            poseStack.scale(BUFF_FIRST_PERSON_ITEM_SCALE, BUFF_FIRST_PERSON_ITEM_SCALE, BUFF_FIRST_PERSON_ITEM_SCALE);
        }
    }
}
