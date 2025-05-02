package com.moulberry.flashback.mixin.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.moulberry.flashback.Flashback;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CommandDispatcher.class, remap = false)
public class MixinCommand {
    @Inject(method = "execute(Lcom/mojang/brigadier/ParseResults;)I", at = @At("HEAD"), cancellable = true, remap = false)
    private <S> void onExecute(ParseResults<S> parse, CallbackInfoReturnable<Integer> cir) {
        if (Flashback.isInReplay()) {
            cir.setReturnValue(0);
            cir.cancel();
        }
    }
}
