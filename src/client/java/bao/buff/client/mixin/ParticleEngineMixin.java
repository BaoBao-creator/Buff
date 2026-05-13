package bao.buff.client.mixin;

import bao.buff.client.util.VisibilityCuller;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.ParticlesRenderState;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
    @Inject(method = "createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;)V", at = @At("HEAD"), cancellable = true)
    private void buff$skipTrackingEmitter(Entity entity, ParticleOptions options, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;I)V", at = @At("HEAD"), cancellable = true)
    private void buff$skipTrackingEmitterWithAge(Entity entity, ParticleOptions options, int maxAge, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void buff$skipCreateParticle(ParticleOptions options, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfoReturnable<Particle> cir) {
        cir.setReturnValue(null);
    }

    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void buff$skipAddParticle(Particle particle, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void buff$skipParticleTick(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "extract", at = @At("HEAD"), cancellable = true)
    private void buff$skipParticleRenderExtraction(ParticlesRenderState renderState, Frustum frustum, Camera camera, float tickProgress, CallbackInfo ci) {
        renderState.reset();
        ci.cancel();
    }

    @Inject(method = "setLevel", at = @At("TAIL"))
    private void buff$clearParticlesOnWorldChange(ClientLevel level, CallbackInfo ci) {
        ((ParticleEngine) (Object) this).clearParticles();
        VisibilityCuller.clearCaches();
    }
}
