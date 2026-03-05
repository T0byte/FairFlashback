package com.moulberry.flashback.mixin.movement;

import com.moulberry.flashback.EnhancedFlight;
import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.cache.PlayerCache;
import com.moulberry.flashback.combo_options.MovementDirection;
import com.moulberry.flashback.configuration.FlashbackConfigV1;
import com.moulberry.flashback.state.EditorState;
import com.moulberry.flashback.state.EditorStateManager;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Player.class, priority = 1100)
public abstract class MixinPlayerEntity extends LivingEntity {
    private String nearestPlayerNameOnFirstTick = null;

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
            PlayerCache.setPlayer(player);
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

                // Store nearest player name on first tick
                if (nearestPlayerNameOnFirstTick == null) {
                    double nearestDistance = Double.MAX_VALUE;
                    for (net.minecraft.world.entity.player.Player otherPlayer : player.level().players()) {
                        if (otherPlayer != player) {
                            double distance = otherPlayer.position().distanceTo(player.position());
                            if (distance < nearestDistance) {
                                nearestDistance = distance;
                                nearestPlayerNameOnFirstTick = otherPlayer.getName().getString();
                            }
                        }
                    }
                    PlayerCache.setNearestPlayerName(nearestPlayerNameOnFirstTick);
                }

                if (nearestPlayerNameOnFirstTick != null) {
                    // Find the player by name and get their current position
                    net.minecraft.world.entity.player.Player targetPlayer = null;
                    for (net.minecraft.world.entity.player.Player otherPlayer : player.level().players()) {
                        if (otherPlayer.getName().getString().equals(nearestPlayerNameOnFirstTick)) {
                            targetPlayer = otherPlayer;
                            break;
                        }
                    }

                    if (targetPlayer != null) {
                        Vec3 targetPos = targetPlayer.position();
                        Vec3 currentPos = player.position();

                        // Calculate direction to target (X and Z only)
                        double dx = targetPos.x - currentPos.x;
                        double dz = targetPos.z - currentPos.z;
                        double distance = Math.sqrt(dx * dx + dz * dz);

                        if (distance > 128) {
                            // Normalize direction and apply momentum
                            double speed = 2; // Adjust this value to control push strength
                            Vec3 currentMotion = player.getDeltaMovement();
                            player.setDeltaMovement(
                                    (dx / distance) * speed,
                                    currentMotion.y,
                                    (dz / distance) * speed
                            );
                        }
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
