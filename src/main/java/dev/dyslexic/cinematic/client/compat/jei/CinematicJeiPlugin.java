package dev.dyslexic.cinematic.client.compat.jei;

import dev.dyslexic.cinematic.Cinematic;
import mezz.jei.api.IModPlugin;
import dev.dyslexic.cinematic.client.SceneScreen;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

/** Found by JEI only when JEI is installed (on Fabric through the "jei_mod_plugin" entrypoint); it hands the runtime to the field guide and keeps the ingredient list off the scene. */
@JeiPlugin
public final class CinematicJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return Cinematic.id("field_guide");
    }

    /**
     * The scene is a container screen only for its drawer: no ingredient list, search or bookmarks over it. A handler
     * answering null only hands the screen on to JEI's own one for container screens, so the scene claims the whole
     * screen as its window instead, and JEI finds no room beside it.
     */
    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiScreenHandler(SceneScreen.class, WholeScreen::new);
    }

    private record WholeScreen(SceneScreen screen) implements IGuiProperties {
        //? if >=1.21 {
        @Override public Class<? extends Screen> screenClass() { return SceneScreen.class; }
        @Override public int guiLeft() { return 0; }
        @Override public int guiTop() { return 0; }
        @Override public int guiXSize() { return screen.width; }
        @Override public int guiYSize() { return screen.height; }
        @Override public int screenWidth() { return screen.width; }
        @Override public int screenHeight() { return screen.height; }
        //?} else {
        /*@Override public Class<? extends Screen> getScreenClass() { return SceneScreen.class; }
        @Override public int getGuiLeft() { return 0; }
        @Override public int getGuiTop() { return 0; }
        @Override public int getGuiXSize() { return screen.width; }
        @Override public int getGuiYSize() { return screen.height; }
        @Override public int getScreenWidth() { return screen.width; }
        @Override public int getScreenHeight() { return screen.height; }
        *///?}
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        JeiRecipeViewer.available(runtime);
    }

    @Override
    public void onRuntimeUnavailable() {
        JeiRecipeViewer.available(null);
    }
}
