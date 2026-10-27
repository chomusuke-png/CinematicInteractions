package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.api.AddonStations;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Set;
//? if forge {
/*import java.util.LinkedHashMap;
*///?}
import java.util.Locale;
import java.util.Map;
import net.minecraft.world.level.block.state.BlockState;
//? if neoforge {
import net.neoforged.neoforge.common.ModConfigSpec;
//?} else if forge {
/*import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;
*///?}

/**
 * The player's own settings (config/cinematic-client.toml), edited on {@link SettingsScreen}. The client-only choice
 * is captured on connection; other values are read where they are used.
 */
public final class Settings {
    private Settings() {}

    //? if neoforge {
    public static final ModConfigSpec SPEC;
    //?} else if fabric {
    /*public static final ClientConfig SPEC;
    *///?} else if forge {
    /*public static final ForgeConfigSpec SPEC;
    *///?}

    //? if neoforge {
    static final ModConfigSpec.BooleanValue HEIGHT_LOGIC;
    static final Map<StationKind, ModConfigSpec.BooleanValue> SCENES = new EnumMap<>(StationKind.class);
    /** The add-on stations turned off, by id, comma-separated: add-ons register theirs after this file is built. */
    static final ModConfigSpec.ConfigValue<String> DISABLED_ADDONS;
    static final ModConfigSpec.BooleanValue CONTROL_HINTS;
    static final ModConfigSpec.BooleanValue CAMPFIRE_INVITE;
    static final ModConfigSpec.IntValue APPROACH_SPEED;
    static final ModConfigSpec.IntValue LEAVE_SPEED;
    static final ModConfigSpec.IntValue LEAN_STRENGTH;
    static final ModConfigSpec.IntValue SCENE_ZOOM;
    static final ModConfigSpec.BooleanValue QUICK_RETURN;
    /** 0 = cinematic, 1 = quick only on return, 2 = quick on every entry; -1 = still the old quickReturn switch. */
    static final ModConfigSpec.IntValue QUICK_MODE;
    static final ModConfigSpec.IntValue QUICK_WINDOW;
    /** 0 = original stretched arms, 1 = natural reach, 2 = floating forearms. */
    static final ModConfigSpec.IntValue ARM_STYLE;
    static final ModConfigSpec.BooleanValue LOOK_TOGGLES;
    static final ModConfigSpec.BooleanValue LETTERBOX;
    static final ModConfigSpec.BooleanValue LIVING_CAMERA;
    static final ModConfigSpec.BooleanValue CAMERA_SHAKE;
    static final ModConfigSpec.BooleanValue SCREEN_FLASH;
    static final ModConfigSpec.BooleanValue REDUCE_MOTION;
    static final ModConfigSpec.BooleanValue CLIENT_ONLY_MODE;
    /** Not a setting: remembers that the server list has shown its note about the client-only mode ({@link ServerListHint}). */
    static final ModConfigSpec.BooleanValue SERVER_HINT_SEEN;
    static final ModConfigSpec.BooleanValue DEVELOPER_MODE;
    //?} else if fabric {
    /*static final ClientConfig.BooleanValue HEIGHT_LOGIC;
    static final Map<StationKind, ClientConfig.BooleanValue> SCENES = new EnumMap<>(StationKind.class);
    static final ClientConfig.StringValue DISABLED_ADDONS;
    static final ClientConfig.BooleanValue CONTROL_HINTS;
    static final ClientConfig.BooleanValue CAMPFIRE_INVITE;
    static final ClientConfig.IntValue APPROACH_SPEED;
    static final ClientConfig.IntValue LEAVE_SPEED;
    static final ClientConfig.IntValue LEAN_STRENGTH;
    static final ClientConfig.IntValue SCENE_ZOOM;
    /^* 0 = cinematic, 1 = quick only on return, 2 = quick on every entry. ^/
    static final ClientConfig.IntValue QUICK_MODE;
    static final ClientConfig.IntValue QUICK_WINDOW;
    /^* 0 = original stretched arms, 1 = natural reach, 2 = floating forearms. ^/
    static final ClientConfig.IntValue ARM_STYLE;
    static final ClientConfig.BooleanValue LOOK_TOGGLES;
    static final ClientConfig.BooleanValue LETTERBOX;
    static final ClientConfig.BooleanValue LIVING_CAMERA;
    static final ClientConfig.BooleanValue CAMERA_SHAKE;
    static final ClientConfig.BooleanValue SCREEN_FLASH;
    static final ClientConfig.BooleanValue REDUCE_MOTION;
    static final ClientConfig.BooleanValue CLIENT_ONLY_MODE;
    /^* Not a setting: remembers that the server list has shown its note about the client-only mode ({@link ServerListHint}). ^/
    static final ClientConfig.BooleanValue SERVER_HINT_SEEN;
    static final ClientConfig.BooleanValue DEVELOPER_MODE;
    *///?} else if forge {
    /*static final ForgeConfigSpec.BooleanValue HEIGHT_LOGIC;
    static final Map<StationKind, ForgeConfigSpec.BooleanValue> SCENES = new EnumMap<>(StationKind.class);
    static final ForgeConfigSpec.ConfigValue<String> DISABLED_ADDONS;
    static final ForgeConfigSpec.BooleanValue CONTROL_HINTS;
    static final ForgeConfigSpec.BooleanValue CAMPFIRE_INVITE;
    static final ForgeConfigSpec.IntValue APPROACH_SPEED;
    static final ForgeConfigSpec.IntValue LEAVE_SPEED;
    static final ForgeConfigSpec.IntValue LEAN_STRENGTH;
    static final ForgeConfigSpec.IntValue SCENE_ZOOM;
    static final ForgeConfigSpec.BooleanValue QUICK_RETURN;
    static final ForgeConfigSpec.IntValue QUICK_MODE;
    static final ForgeConfigSpec.IntValue QUICK_WINDOW;
    static final ForgeConfigSpec.IntValue ARM_STYLE;
    static final ForgeConfigSpec.BooleanValue LOOK_TOGGLES;
    static final ForgeConfigSpec.BooleanValue LETTERBOX;
    static final ForgeConfigSpec.BooleanValue LIVING_CAMERA;
    static final ForgeConfigSpec.BooleanValue CAMERA_SHAKE;
    static final ForgeConfigSpec.BooleanValue SCREEN_FLASH;
    static final ForgeConfigSpec.BooleanValue REDUCE_MOTION;
    static final ForgeConfigSpec.BooleanValue CLIENT_ONLY_MODE;
    static final ForgeConfigSpec.BooleanValue SERVER_HINT_SEEN;
    static final ForgeConfigSpec.BooleanValue DEVELOPER_MODE;
    *///?}

    static {
        //? if neoforge {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        //?} else if fabric {
        /*ClientConfig.Builder b = new ClientConfig.Builder();
        *///?} else if forge {
        /*ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        *///?}
        b.translation("cinematic.configuration.scenes").push("scenes");
        HEIGHT_LOGIC = b.comment("When a block's work cannot be reached from where the player stands (too high, too far, covered),",
                        "true opens the block's own window instead of the scene; false only says why, over the crosshair.")
                .translation("cinematic.configuration.heightLogic").define("heightLogic", false);
        b.translation("cinematic.configuration.blocks").push("blocks");
        for (StationKind kind : StationKind.values()) {
            // (the stations of add-ons are listed apart: theirs are only known once the add-ons are loaded)
            if (kind == StationKind.ADDON) continue;
            String key = key(kind);
            SCENES.put(kind, b.comment("Whether " + key + " plays its scene (false: the block keeps its usual window).")
                    .translation("cinematic.configuration." + key).define(key, true));
        }
        DISABLED_ADDONS = b.comment("The add-on stations that keep their usual window: their ids, separated by commas.")
                .translation("cinematic.configuration.disabledAddons").define("disabledAddons", "");
        b.pop();
        b.pop();
        b.translation("cinematic.configuration.interface").push("interface");
        CONTROL_HINTS = b.comment("The line of controls at the bottom of the screen during a scene.")
                .translation("cinematic.configuration.controlHints").define("controlHints", true);
        CAMPFIRE_INVITE = b.comment("The \"[H] Sit by the fire\" invitation under the crosshair when looking at a campfire (the key works either way).")
                .translation("cinematic.configuration.campfireInvite").define("campfireInvite", true);
        b.pop();
        b.translation("cinematic.configuration.camera").push("camera");
        APPROACH_SPEED = b.comment("How fast the camera leans in to the block once the player has stepped up to it, in percent (100: 1.2 s).")
                .translation("cinematic.configuration.approachSpeed").defineInRange("approachSpeed", 100, 50, 200);
        LEAVE_SPEED = b.comment("How fast the player steps away and the camera comes back to the eyes, in percent (100: about a second).")
                .translation("cinematic.configuration.leaveSpeed").defineInRange("leaveSpeed", 100, 50, 200);
        LEAN_STRENGTH = b.comment("How far the camera leans in from the eyes towards the block, in percent (0: it stays at the eyes, looking at the work).")
                .translation("cinematic.configuration.leanStrength").defineInRange("leanStrength", 100, 0, 100);
        SCENE_ZOOM = b.comment("How much the view narrows on the work in a scene, in percent (0: the field of view stays your own).")
                .translation("cinematic.configuration.sceneZoom").defineInRange("sceneZoom", 100, 0, 100);
        //? if neoforge {
        QUICK_RETURN = b.comment("Coming back to the same station soon after stepping away, the way in is short: a quick lean, no bars.")
                .translation("cinematic.configuration.quickReturn").define("quickReturn", true);
        QUICK_MODE = b.comment("0: cinematic entry; 1: quick on a recent return; 2: quick on every entry. -1 keeps the old quickReturn setting.")
                .translation("cinematic.configuration.quickMode").defineInRange("quickMode", -1, -1, 2);
        //?} else if fabric {
        /*QUICK_MODE = b.comment("0: cinematic entry; 1: quick on a recent return; 2: quick on every entry.")
                .translation("cinematic.configuration.quickMode").defineInRange("quickMode", 1, 0, 2);
        *///?} else if forge {
        /*QUICK_RETURN = b.comment("Coming back to the same station soon after stepping away, the way in is short: a quick lean, no bars.")
                .translation("cinematic.configuration.quickReturn").define("quickReturn", true);
        QUICK_MODE = b.comment("0: cinematic entry; 1: quick on a recent return; 2: quick on every entry. -1 keeps the old quickReturn setting.")
                .translation("cinematic.configuration.quickMode").defineInRange("quickMode", -1, -1, 2);
        *///?}
        QUICK_WINDOW = b.comment("For how many seconds after stepping away coming back to the same station is quick.")
                .translation("cinematic.configuration.quickWindow").defineInRange("quickWindow", 12, 5, 30);
        //? if !forge {
        ARM_STYLE = b.comment("0: original stretching arms; 1: short natural reach with a gentle camera lean; 2: floating forearms.")
        //?} else if forge {
        /*ARM_STYLE = b.comment("0: original stretching arms; 1: natural reach; 2: floating forearms.")
        *///?}
                .translation("cinematic.configuration.armStyle").defineInRange("armStyle", 0, 0, 2);
        LOOK_TOGGLES = b.comment("Looking round from behind while at work (Left Alt, rebindable): false while the key is held, true pressed on and off.")
                .translation("cinematic.configuration.lookToggles").define("lookToggles", false);
        LETTERBOX = b.comment("Black bars at the top and bottom while the camera moves in and out.")
                .translation("cinematic.configuration.letterbox").define("letterbox", true);
        LIVING_CAMERA = b.comment("The camera breathes slightly and drifts a little towards the cursor while working.")
                .translation("cinematic.configuration.livingCamera").define("livingCamera", true);
        CAMERA_SHAKE = b.comment("The camera trembles at big moments (a hammer blow, a spell, the beacon lighting up).")
                .translation("cinematic.configuration.cameraShake").define("cameraShake", true);
        SCREEN_FLASH = b.comment("The screen flashes at big moments (the light of a spell, white-hot metal).")
                .translation("cinematic.configuration.screenFlash").define("screenFlash", true);
        REDUCE_MOTION = b.comment("Turns the letterbox bars, the breathing camera, the shake and the flashes off at once, whatever they are set to.")
                .translation("cinematic.configuration.reduceMotion").define("reduceMotion", false);
        b.pop();
        b.translation("cinematic.configuration.multiplayer").push("multiplayer");
        CLIENT_ONLY_MODE = b.comment("On a server without Cinematic Interactions, show local scenes over supported vanilla menus. "
                        + "Set this from the main menu; it takes effect on the next connection. Other stations keep their vanilla behavior.")
                .translation("cinematic.configuration.clientOnlyMode").define("clientOnlyMode", false);
        //? if !forge {
        SERVER_HINT_SEEN = b.comment("Whether the server list has already shown its note about the client-only mode (it shows once).")
        //?} else if forge {
        /*SERVER_HINT_SEEN = b.comment("Whether the one-time server-list note has already been answered.")
        *///?}
                .define("serverHintSeen", false);
        b.pop();
        b.translation("cinematic.configuration.developer").push("developer");
        DEVELOPER_MODE = b.comment("Show the local station layout editors and enable their G shortcut. Saved layouts apply even when this is false.")
                .translation("cinematic.configuration.developerMode").define("developerMode", false);
        b.pop();
        SPEC = b.build();
    }

    /** The key of a station in the settings file and the translations: "table", "blast_furnace", ... */
    static String key(StationKind kind) {
        return kind.name().toLowerCase(Locale.ROOT);
    }

    //? if neoforge {
    private static boolean get(ModConfigSpec.BooleanValue value) {
    //?} else if fabric && >=1.21 {
    /*private static boolean get(ClientConfig.BooleanValue value) {
    *///?} else if fabric && <1.21 {
    /*/^* Changes a setting in memory (the same call as on Forge, where the walkthrough sets them; nothing is saved). ^/
    public static <T> void set(ClientConfig.Value<T> value, T to) {
        value.set(to);
    }

    private static boolean get(ClientConfig.BooleanValue value) {
    *///?} else if forge {
    /*private static boolean get(ForgeConfigSpec.BooleanValue value) {
    *///?}
        // before the file is read (very early in loading) the defaults apply
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    //? if neoforge {
    private static double percent(ModConfigSpec.IntValue value) {
    //?} else if fabric {
    /*private static double percent(ClientConfig.IntValue value) {
    *///?} else if forge {
    /*private static double percent(ForgeConfigSpec.IntValue value) {
    *///?}
        return (SPEC.isLoaded() ? value.get() : value.getDefault()) / 100.0;
    }

    /** How fast the camera leans in once the player has arrived: 1 as made, 2 twice as fast. */
    public static double approachSpeed() {
        return quickMode() == 2 ? 1.0 : percent(APPROACH_SPEED);
    }

    /** How fast the player steps away: 1 as made, 2 twice as fast. */
    public static double leaveSpeed() {
        return percent(LEAVE_SPEED);
    }

    /** How far the camera goes from the eyes towards the station's pose: 1 all the way, 0 not at all. */
    public static double leanStrength() {
        return percent(LEAN_STRENGTH);
    }

    /** How far the field of view goes to the station's: 1 all the way, 0 not at all. */
    public static double sceneZoom() {
        return percent(SCENE_ZOOM);
    }

    /** Out of reach: the block's own window (true) or just a notice saying why (false). */
    public static boolean heightLogic() {
        return get(HEIGHT_LOGIC);
    }

    /** Whether this block plays its scene; otherwise it keeps its usual window. */
    public static boolean scene(StationKind kind) {
        if (kind == StationKind.ADDON) return true;
        return get(SCENES.get(kind));
    }

    /** Whether the block plays its scene: an add-on's block unless the player turned that add-on station off. */
    public static boolean scene(StationKind kind, BlockState state) {
        if (kind != StationKind.ADDON) return scene(kind);
        AddonStations.Definition definition = AddonStations.forBlock(state);
        return definition == null || !disabledAddons().contains(definition.id().toString());
    }

    /** The ids of the add-on stations the player turned off. */
    public static Set<String> disabledAddons() {
        return parseIds(SPEC.isLoaded() ? DISABLED_ADDONS.get() : "");
    }

    static Set<String> parseIds(String ids) {
        Set<String> set = new LinkedHashSet<>();
        for (String id : ids.split(",")) if (!id.isBlank()) set.add(id.strip());
        return set;
    }

    public static boolean controlHints() {
        return get(CONTROL_HINTS);
    }

    public static boolean clientOnlyMode() { return get(CLIENT_ONLY_MODE); }

    public static boolean developerMode() { return get(DEVELOPER_MODE); }

    public static boolean campfireInvite() {
        return get(CAMPFIRE_INVITE);
    }

    /** Cinematic, quick on recent return, or always quick. */
    //? if neoforge {
    /** Coming back to the same station soon: a quick way in. */
    public static boolean quickReturn() {
        return quickMode() != 0;
    }

    /** The way in: 0 cinematic, 1 quick on a recent return, 2 quick every time (an older file's quickReturn until changed). */
    //?} else if forge {
    /*/^* Coming back to the same station soon: a quick way in. ^/
    public static boolean quickReturn() {
        return quickMode() != 0;
    }

    *///?}
    public static int quickMode() {
        //? if neoforge {
        int mode = SPEC.isLoaded() ? QUICK_MODE.get() : QUICK_MODE.getDefault();
        return mode < 0 ? (get(QUICK_RETURN) ? 1 : 0) : mode;
        //?} else if fabric {
        /*return SPEC.isLoaded() ? QUICK_MODE.get() : QUICK_MODE.getDefault();
        *///?} else if forge {
        /*int mode = SPEC.isLoaded() ? QUICK_MODE.get() : QUICK_MODE.getDefault();
        return mode < 0 ? (get(QUICK_RETURN) ? 1 : 0) : mode;
        *///?}
    }

    /** 0 original stretched arms, 1 natural reach, 2 floating forearms. */
    public static int armStyle() {
        return SPEC.isLoaded() ? ARM_STYLE.get() : ARM_STYLE.getDefault();
    }

    /** Seconds after stepping away that coming back is quick. */
    public static int quickWindow() {
        return SPEC.isLoaded() ? QUICK_WINDOW.get() : QUICK_WINDOW.getDefault();
    }

    /** Looking round from behind: pressed on and off (true) or while the key is held (false). */
    public static boolean lookToggles() {
        return get(LOOK_TOGGLES);
    }

    // (reduce motion turns every effect off, whatever each is set to)
    public static boolean letterbox() {
        return get(LETTERBOX) && !get(REDUCE_MOTION);
    }

    public static boolean livingCamera() {
        return get(LIVING_CAMERA) && !get(REDUCE_MOTION);
    }

    public static boolean cameraShake() {
        return get(CAMERA_SHAKE) && !get(REDUCE_MOTION);
    }

    public static boolean screenFlash() {
        return get(SCREEN_FLASH) && !get(REDUCE_MOTION);
    //? if forge {
    /*}

    // Forge 1.20.1 writes the file on every set() and reloads it from a watcher thread. A reload that reads the file
    // while a later write is under way puts an older value back in memory, or "corrects" the half-read file to the
    // defaults and saves those over it (Done on the settings screen sets many values at once). Values set in the last few seconds
    // are therefore set again after any reload, on the game thread.
    private static final Map<ForgeConfigSpec.ConfigValue<?>, Object> RECENT = new LinkedHashMap<>();
    private static long recentUntil;
    private static volatile boolean reloaded;

    public static <T> void set(ForgeConfigSpec.ConfigValue<T> value, T v) {
        synchronized (RECENT) {
            RECENT.put(value, v);
            recentUntil = System.currentTimeMillis() + 3000;
        }
        value.set(v);
    }

    static void onReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) reloaded = true;
    }

    @SuppressWarnings("unchecked")
    static void tick() {
        if (!reloaded) return;
        reloaded = false;
        synchronized (RECENT) {
            if (System.currentTimeMillis() > recentUntil) {
                RECENT.clear();
                return;
            }
            RECENT.forEach((value, v) -> {
                if (!v.equals(value.get())) ((ForgeConfigSpec.ConfigValue<Object>) value).set(v);
            });
        }
    *///?}
    }
}
