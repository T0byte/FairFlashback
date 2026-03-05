package com.moulberry.flashback.mixin.commands;

import com.moulberry.flashback.Flashback;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyboardHandler.class)
public class MixinGameModeSwitcher {

    @Inject(method = "handleDebugKeys", at = @At("HEAD"), cancellable = true)
    private void blockGameModeSwitcher(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
        if (Flashback.isInReplay() && keyEvent.key() == GLFW.GLFW_KEY_F4) {
            cir.setReturnValue(false);
        }
        if (Flashback.isInReplay() && keyEvent.key() == GLFW.GLFW_KEY_N) {
            cir.setReturnValue(false);
        }
    }
}
