package bao.buff.client.mixin;

import bao.buff.client.AttributeSwapManager;
import bao.buff.client.Config;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class PlayerMixin {
    // Logic cho Auto Sprint
    @Inject(method = "aiStep", at = @At("TAIL"))
    private void onAiStep(CallbackInfo ci) {
        if ((Object)this instanceof LocalPlayer player) {
            if (Config.autoSprintEnabled && 
                player.input.hasForwardImpulse() && 
                !player.isShiftKeyDown() && 
                (player.getFoodData().getFoodLevel() > 6 || player.getAbilities().instabuild)) {
                
                player.setSprinting(true);
            }
        }
        AttributeSwapManager.onTick();
    }
}