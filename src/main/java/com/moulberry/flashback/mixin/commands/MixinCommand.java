package com.moulberry.flashback.mixin.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.cache.PlayerCache;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CommandDispatcher.class, remap = false)
public class MixinCommand {
    @Inject(method = "execute(Lcom/mojang/brigadier/ParseResults;)I", at = @At("HEAD"), cancellable = true, remap = false)
    private <S> void onExecute(ParseResults<S> parse, CallbackInfoReturnable<Integer> cir) {
        if (Flashback.isInReplay()) {
            String input = parse.getReader().getString();

            // Allow /selftp command
            if (input.startsWith("/selftp") || input.startsWith("selftp")) {
                LocalPlayer player = PlayerCache.getPlayer();
                if (player != null) {
                    String nearestPlayerName = PlayerCache.getNearestPlayerName();
                    if (nearestPlayerName != null) {
                        for (Player otherPlayer : player.level().players()) {
                            if (otherPlayer.getName().getString().equals(nearestPlayerName)) {
                                player.setPos(otherPlayer.getX(), otherPlayer.getY(), otherPlayer.getZ());
                                player.displayClientMessage(Component.literal("Teleported to " + nearestPlayerName), false);
                                cir.setReturnValue(1);
                                cir.cancel();
                                return;
                            }
                        }
                    }
                }
                cir.setReturnValue(0);
                cir.cancel();
                return;
            }

            cir.setReturnValue(0);
            cir.cancel();
        }
    }
}
