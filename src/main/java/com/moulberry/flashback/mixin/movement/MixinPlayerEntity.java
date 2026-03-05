package com.moulberry.flashback.mixin.movement;

import com.moulberry.flashback.EnhancedFlight;
import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.combo_options.MovementDirection;
import com.moulberry.flashback.configuration.FlashbackConfigV1;
import com.moulberry.flashback.state.EditorState;
import com.moulberry.flashback.state.EditorStateManager;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Player.class, priority = 1100)
public abstract class MixinPlayerEntity extends LivingEntity {
    private Player nearestPlayerOnFirstTick = null;


    protected MixinPlayerEntity() {
        super(null, null);
    }

    @Shadow public abstract void travel(Vec3 movementInput);

    @Shadow
    protected abstract float getFlyingSpeed();

    @Inject(method="getFlyingSpeed", at=@At("HEAD"), cancellable = true)
    public void getFlyingSpeed(CallbackInfoReturnable<Float> cir) {
        if ((Object)this instanceof LocalPlayer player && Flashback.isInReplay()) {
            FlashbackConfigV1 config = Flashback.getConfig();

            boolean doAirplaneFlight = config.editorMovement.flightDirection == MovementDirection.CAMERA || config.editorMovement.flightMomentum < 0.98;
            if (doAirplaneFlight && player.getAbilities().flying && !player.isPassenger() && !player.isFallFlying()) {
                cir.setReturnValue(EnhancedFlight.getFlightSpeed(player, config));
            }
        }
    }

    @Inject(method="travel", at=@At(value = "HEAD"), cancellable = true)
    public void travel(Vec3 movementInput, CallbackInfo ci) {
        if ((Object) this instanceof LocalPlayer player && Flashback.isInReplay()) {
            FlashbackConfigV1 config = Flashback.getConfig();

            if (player.isSpectator()) {
                boolean noClip = player.noPhysics;
                player.noPhysics = false;
                player.setNoGravity(true);

                super.travel(movementInput);
                if (!(player.input.keyPresses.jump() || player.input.keyPresses.shift())) {
                    Vec3 motion = player.getDeltaMovement();
                    player.setDeltaMovement(motion.x, 0, motion.z);
                }

                // Store nearest player on first tick, then check distance to that player
                if (nearestPlayerOnFirstTick == null) {
                    double nearestDistance = Double.MAX_VALUE;
                    for (net.minecraft.world.entity.player.Player otherPlayer : player.level().players()) {
                        if (otherPlayer != player) {
                            double distance = otherPlayer.position().distanceTo(player.position());
                            if (distance < nearestDistance) {
                                nearestDistance = distance;
                                nearestPlayerOnFirstTick = otherPlayer;
                            }
                        }
                    }
                }

                if (nearestPlayerOnFirstTick != null) {
                    Vec3 targetPos = player.position().add(player.getDeltaMovement());
                    if (nearestPlayerOnFirstTick.position().distanceTo(targetPos) > 128) {
                        Vec3 currentMotion = player.getDeltaMovement();
                        player.setDeltaMovement(currentMotion.multiply(-1, 1, -1));
                    }
                }

                player.noPhysics = noClip;
                player.setNoGravity(false);

                ci.cancel();
                return;
            }
        }
    }

    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    public void getName(CallbackInfoReturnable<Component> cir) {
        if ((Object)this instanceof RemotePlayer) {
            EditorState editorState = EditorStateManager.getCurrent();
            if (editorState != null) {
                String nameOverride = editorState.nameOverride.get(this.uuid);
                if (nameOverride != null) {
                    cir.setReturnValue(Component.literal(nameOverride));
                }
            }
        }
    }

}
