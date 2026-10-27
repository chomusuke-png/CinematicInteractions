//? if fabric && >=1.21 {
/*package dev.dyslexic.cinematic.mixin;

import dev.dyslexic.cinematic.ClientBridge;
import dev.dyslexic.cinematic.client.CinematicClient;
import dev.dyslexic.cinematic.client.Scene;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/^* The places in the game where NeoForge fires the client events the mod listens to and Fabric API has none. ^/
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow
    public ClientLevel level;

    /^* RenderFrameEvent.Pre: after this frame's ticks, right before the frame is drawn. ^/
    @Inject(method = "runTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;render(Lnet/minecraft/client/DeltaTracker;Z)V"))
    private void cinematic$frame(boolean renderLevel, CallbackInfo ci) {
        Scene.frame();
    }

    /^* ScreenEvent.Opening, which NeoForge fires for a screen opening (not for closing one). ^/
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void cinematic$screen(Screen next, CallbackInfo ci) {
        if (next != null && CinematicClient.onScreenOpening(next)) {
            dev.dyslexic.cinematic.client.HiddenWindows.keptShut(next);
            ci.cancel();
        }
    }

    /^* LevelEvent.Unload: another level (a dimension, a respawn) replaces this one ... ^/
    @Inject(method = "setLevel", at = @At("HEAD"))
    private void cinematic$replaceLevel(ClientLevel next, ReceivingLevelScreen.Reason reason, CallbackInfo ci) {
        if (level != null) CinematicClient.onLevelUnload();
    }

    /^* ... or the player leaves it. ^/
    @Inject(method = {"disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V", "clearClientLevel"}, at = @At("HEAD"))
    private void cinematic$leaveLevel(CallbackInfo ci) {
        if (level != null) CinematicClient.onLevelUnload();
    }

    // InputEvent.InteractionKeyMappingTriggered: the mouse buttons do nothing in the world while the scene drives the view

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void cinematic$use(CallbackInfo ci) {
        if (Scene.active()) ci.cancel();
    }

    @Inject(method = "startUseItem", at = @At("RETURN"))
    private void cinematic$used(CallbackInfo ci) {
        ClientBridge.swallowSwing = false;
    }

    /^* A use made outside startUseItem (another mod, the showcase) leaves no swing to skip past its own tick. ^/
    @Inject(method = "tick", at = @At("HEAD"))
    private void cinematic$tick(CallbackInfo ci) {
        ClientBridge.swallowSwing = false;
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void cinematic$attack(CallbackInfoReturnable<Boolean> cir) {
        if (Scene.active()) cir.setReturnValue(false);
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void cinematic$break(boolean leftClick, CallbackInfo ci) {
        if (Scene.active()) ci.cancel();
    }

    @Inject(method = "pickBlock", at = @At("HEAD"), cancellable = true)
    private void cinematic$pick(CallbackInfo ci) {
        if (Scene.active()) ci.cancel();
    }
}
*///?} else if fabric && <1.21 {
/*package dev.dyslexic.cinematic.mixin;

import dev.dyslexic.cinematic.ClientBridge;
import dev.dyslexic.cinematic.client.CinematicClient;
import dev.dyslexic.cinematic.client.Scene;
import net.minecraft.client.Minecraft;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/^* The places in the game where NeoForge fires the client events the mod listens to and Fabric API has none. ^/
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow
    public ClientLevel level;

    /^* RenderFrameEvent.Pre: after this frame's ticks, right before the frame is drawn. ^/
    @Inject(method = "runTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;render(FJZ)V"))
    private void cinematic$frame(boolean renderLevel, CallbackInfo ci) {
        Scene.frame();
    }

    /^* ScreenEvent.Opening, which NeoForge fires for a screen opening (not for closing one). ^/
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void cinematic$screen(Screen next, CallbackInfo ci) {
        if (next != null && CinematicClient.onScreenOpening(next)) {
            dev.dyslexic.cinematic.client.HiddenWindows.keptShut(next);
            ci.cancel();
        }
    }

    /^* LevelEvent.Unload: another level (a dimension, a respawn) replaces this one ... ^/
    @Inject(method = "setLevel", at = @At("HEAD"))
    private void cinematic$replaceLevel(ClientLevel next, CallbackInfo ci) {
        if (level != null) CinematicClient.onLevelUnload();
    }

    /^* ... or the player leaves it. ^/
    @Inject(method = "clearLevel(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("HEAD"))
    private void cinematic$leaveLevel(CallbackInfo ci) {
        if (level != null) CinematicClient.onLevelUnload();
    }

    // InputEvent.InteractionKeyMappingTriggered: the mouse buttons do nothing in the world while the scene drives the view

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void cinematic$use(CallbackInfo ci) {
        if (Scene.active()) ci.cancel();
    }

    @Inject(method = "startUseItem", at = @At("RETURN"))
    private void cinematic$used(CallbackInfo ci) {
        ClientBridge.swallowSwing = false;
    }

    /^* A use made outside startUseItem (another mod, the showcase) leaves no swing to skip past its own tick. ^/
    @Inject(method = "tick", at = @At("HEAD"))
    private void cinematic$tick(CallbackInfo ci) {
        ClientBridge.swallowSwing = false;
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void cinematic$attack(CallbackInfoReturnable<Boolean> cir) {
        if (Scene.active()) cir.setReturnValue(false);
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void cinematic$break(boolean leftClick, CallbackInfo ci) {
        if (Scene.active()) ci.cancel();
    }

    @Inject(method = "pickBlock", at = @At("HEAD"), cancellable = true)
    private void cinematic$pick(CallbackInfo ci) {
        if (Scene.active()) ci.cancel();
    }
}
*///?}
