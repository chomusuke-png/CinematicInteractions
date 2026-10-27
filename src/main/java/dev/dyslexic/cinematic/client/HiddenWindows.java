package dev.dyslexic.cinematic.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.ContainerListener;

/** The windows a scene keeps shut while it stands in for them. */
public final class HiddenWindows {
    private HiddenWindows() {}

    /**
     * A window that is not opened must not hear about its menu either: a mod's screen that listens from its
     * constructor (Scribble's lectern) would otherwise read pages it only builds when it opens.
     */
    public static void keptShut(Screen screen) {
        if (screen instanceof MenuAccess<?> window && screen instanceof ContainerListener listener) {
            window.getMenu().removeSlotListener(listener);
        }
    }
}
