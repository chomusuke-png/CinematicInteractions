package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.api.AddonStations;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
//? if forge {
/*import java.util.function.IntSupplier;
*///?}
import java.util.function.Function;
//? if neoforge {
import java.util.function.IntSupplier;
//?}
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
//? if neoforge {
import net.neoforged.neoforge.common.ModConfigSpec;
//?} else if forge {
/*import net.minecraftforge.common.ForgeConfigSpec;
*///?}

/**
 * The mod's settings: a search box over four tabs (general, stations, camera, advanced), every option a row that names
 * what it does, with its value on the right and a reset arrow once it is changed. What the option under the mouse
 * does is spelled out in a box under the list. Changes are kept here until Done (or Esc); Cancel forgets them.
 */
public final class SettingsScreen extends Screen {
    private static final int ROW_WIDTH = 310, CONTROL = 120, NOTE = 0xA8A296, HEADER = 0xE0C080;

    private enum Tab { GENERAL, STATIONS, CAMERA, ADVANCED }

    /**
     * The order of the groups on the Stations tab; each station names its own group ({@link StationKind#group}), and a
     * group not listed here comes after these.
     */
    private static final List<String> GROUP_ORDER = List.of("crafting", "cooking", "smithing", "magic", "other");

    /** Pace, view and effects set at once; any other mix reads as custom. */
    private enum Preset {
        CINEMATIC(80, 80, 100, 100, true, true, true, true),
        BALANCED(100, 100, 100, 100, true, true, true, true),
        FAST(160, 160, 70, 60, false, true, true, true),
        MINIMAL(200, 200, 0, 0, false, false, false, false),
        CUSTOM(0, 0, 0, 0, false, false, false, false);

        final int approach, leave, lean, zoom;
        final boolean letterbox, living, shake, flash;

        Preset(int approach, int leave, int lean, int zoom, boolean letterbox, boolean living, boolean shake, boolean flash) {
            this.approach = approach;
            this.leave = leave;
            this.lean = lean;
            this.zoom = zoom;
            this.letterbox = letterbox;
            this.living = living;
            this.shake = shake;
            this.flash = flash;
        }

        Component title() {
            return Component.translatable("cinematic.settings.preset." + name().toLowerCase(Locale.ROOT));
        }
    }

    private final Screen parent;
    /** Changed values not yet written to the settings file. */
    //? if neoforge {
    private final Map<ModConfigSpec.ConfigValue<?>, Object> draft = new HashMap<>();
    //?} else if fabric {
    /*private final Map<ClientConfig.Value<?>, Object> draft = new HashMap<>();
    *///?} else if forge {
    /*private final Map<ForgeConfigSpec.ConfigValue<?>, Object> draft = new HashMap<>();
    *///?}
    private final Map<StationKind, Integer> profiles = new EnumMap<>(StationKind.class);
    private final boolean inWorld;
    private Tab tab = Tab.GENERAL;
    /** The search as typed, and as matched (trimmed, lower case). */
    private String searchText = "", query = "";
    private EditBox search;
    private Rows rows;
    private final List<Button> tabButtons = new ArrayList<>();
    /** The list is built again on the next tick (after a change that moves other controls). */
    private boolean dirty;
    @Nullable
    private Component described;

    public SettingsScreen(Screen parent) {
        super(Component.translatable("cinematic.settings.title"));
        this.parent = parent;
        this.inWorld = Minecraft.getInstance().level != null;
    }

    // ---- the draft ---------------------------------------------------------------------------------

    //? if neoforge {
    private boolean bool(ModConfigSpec.BooleanValue value) {
    //?} else if fabric {
    /*private boolean bool(ClientConfig.BooleanValue value) {
    *///?} else if forge {
    /*private boolean bool(ForgeConfigSpec.BooleanValue value) {
    *///?}
        return (Boolean) draft.getOrDefault(value, value.get());
    }

    //? if neoforge {
    private int number(ModConfigSpec.IntValue value) {
    //?} else if fabric {
    /*private int number(ClientConfig.IntValue value) {
    *///?} else if forge {
    /*private int number(ForgeConfigSpec.IntValue value) {
    *///?}
        return (Integer) draft.getOrDefault(value, value.get());
    }

    //? if neoforge {
    private void put(ModConfigSpec.ConfigValue<?> value, Object to) {
    //?} else if fabric {
    /*private void put(ClientConfig.Value<?> value, Object to) {
    *///?} else if forge {
    /*private void put(ForgeConfigSpec.ConfigValue<?> value, Object to) {
    *///?}
        draft.put(value, to);
    }

    private int profile(StationKind kind) {
        return profiles.getOrDefault(kind, LayoutProfiles.selected(kind));
    }

    @SuppressWarnings("unchecked")
    private void apply() {
        //? if neoforge {
        for (Map.Entry<ModConfigSpec.ConfigValue<?>, Object> change : draft.entrySet()) {
            ModConfigSpec.ConfigValue<Object> value = (ModConfigSpec.ConfigValue<Object>) change.getKey();
        //?} else if fabric {
        /*for (Map.Entry<ClientConfig.Value<?>, Object> change : draft.entrySet()) {
            ClientConfig.Value<Object> value = (ClientConfig.Value<Object>) change.getKey();
        *///?} else if forge {
        /*for (Map.Entry<ForgeConfigSpec.ConfigValue<?>, Object> change : draft.entrySet()) {
            ForgeConfigSpec.ConfigValue<Object> value = (ForgeConfigSpec.ConfigValue<Object>) change.getKey();
        *///?}
            if (!value.get().equals(change.getValue())) {
                //? if !forge {
                value.set(change.getValue());
                //?} else if forge {
                /*// (through Settings.set: Forge's reload of the file must not put an older value back)
                Settings.set(value, change.getValue());
                *///?}
            }
        }
        //? if !forge {
        Settings.SPEC.save();
        //?}
        profiles.forEach(LayoutProfiles::select);
    }

    private Preset preset() {
        for (Preset preset : Preset.values()) {
            if (preset != Preset.CUSTOM && number(Settings.APPROACH_SPEED) == preset.approach && number(Settings.LEAVE_SPEED) == preset.leave
                    && number(Settings.LEAN_STRENGTH) == preset.lean && number(Settings.SCENE_ZOOM) == preset.zoom
                    && bool(Settings.LETTERBOX) == preset.letterbox && bool(Settings.LIVING_CAMERA) == preset.living
                    && bool(Settings.CAMERA_SHAKE) == preset.shake && bool(Settings.SCREEN_FLASH) == preset.flash) {
                return preset;
            }
        }
        return Preset.CUSTOM;
    }

    private void choose(Preset preset) {
        put(Settings.APPROACH_SPEED, preset.approach);
        put(Settings.LEAVE_SPEED, preset.leave);
        put(Settings.LEAN_STRENGTH, preset.lean);
        put(Settings.SCENE_ZOOM, preset.zoom);
        put(Settings.LETTERBOX, preset.letterbox);
        put(Settings.LIVING_CAMERA, preset.living);
        put(Settings.CAMERA_SHAKE, preset.shake);
        put(Settings.SCREEN_FLASH, preset.flash);
    }

    // ---- the options -------------------------------------------------------------------------------

    /** One row: its name and what it does (searched alike), its control, and how to put it back as made. */
    private abstract class Option {
        final Tab on;
        final Component label;
        final Component description;
        BooleanSupplier enabled = () -> true;
        /** Shown only with developer mode on (the layout profiles). */
        boolean developer;

        Option(Tab on, Component label, Component description) {
            this.on = on;
            this.label = label;
            this.description = description;
        }

        abstract AbstractWidget control();

        abstract boolean changed();

        abstract void reset();

        boolean matches(String q) {
            return (label.getString() + " " + description.getString()).toLowerCase(Locale.ROOT).contains(q);
        }
    }

    //? if neoforge {
    private Option toggle(Tab on, String key, ModConfigSpec.BooleanValue value, Function<Boolean, Component> text) {
    //?} else if fabric {
    /*private Option toggle(Tab on, String key, ClientConfig.BooleanValue value, Function<Boolean, Component> text) {
    *///?} else if forge {
    /*private Option toggle(Tab on, String key, ForgeConfigSpec.BooleanValue value, Function<Boolean, Component> text) {
    *///?}
        return toggle(on, Component.translatable("cinematic.configuration." + key), Component.translatable("cinematic.configuration." + key + ".tooltip"),
                value, text);
    }

    //? if neoforge {
    private Option toggle(Tab on, Component label, Component description, ModConfigSpec.BooleanValue value, Function<Boolean, Component> text) {
    //?} else if fabric {
    /*private Option toggle(Tab on, Component label, Component description, ClientConfig.BooleanValue value, Function<Boolean, Component> text) {
    *///?} else if forge {
    /*private Option toggle(Tab on, Component label, Component description, ForgeConfigSpec.BooleanValue value, Function<Boolean, Component> text) {
    *///?}
        return new Option(on, label, description) {
            @Override
            AbstractWidget control() {
                return CycleButton.<Boolean>builder(text::apply).withValues(true, false).withInitialValue(bool(value)).displayOnlyValue()
                        .create(0, 0, CONTROL, 20, label, (button, to) -> {
                            put(value, to);
                            // (the layout profiles show only with developer mode on)
                            dirty = value == Settings.DEVELOPER_MODE;
                        });
            }

            @Override
            boolean changed() {
                return bool(value) != value.getDefault();
            }

            @Override
            void reset() {
                put(value, value.getDefault());
            }
        };
    }

    //? if neoforge {
    private Option slider(Tab on, String key, ModConfigSpec.IntValue value, int min, int max, Function<Integer, Component> text) {
    //?} else if fabric {
    /*private Option slider(Tab on, String key, ClientConfig.IntValue value, int min, int max, Function<Integer, Component> text) {
    *///?} else if forge {
    /*private Option slider(Tab on, String key, ForgeConfigSpec.IntValue value, int min, int max, Function<Integer, Component> text) {
    *///?}
        return new Option(on, Component.translatable("cinematic.configuration." + key), Component.translatable("cinematic.configuration." + key + ".tooltip")) {
            @Override
            AbstractWidget control() {
                return new IntSlider(value, min, max, text);
            }

            @Override
            boolean changed() {
                return number(value) != value.getDefault();
            }

            @Override
            void reset() {
                put(value, value.getDefault());
            }
        };
    }

    //? if neoforge {
    /** The way in as it plays: the three-way setting, or (still -1 from an older file) the old quick return switch. */
    private int quickMode() {
        int mode = number(Settings.QUICK_MODE);
        return mode < 0 ? (bool(Settings.QUICK_RETURN) ? 1 : 0) : mode;
    }

    /**
     * One of a few numbered choices (the way in, the arms), each named by its translation key and number; as made it
     * is {@code made}.
     */
    private Option choice(Tab on, String key, ModConfigSpec.IntValue value, int count, IntSupplier current, int made) {
    //?} else if fabric {
    /*/^* One of a few numbered choices (the way in, the arms), each named by its translation key and number. ^/
    private Option choice(Tab on, String key, ClientConfig.IntValue value, int count) {
    *///?} else if forge {
    /*/^* The way in as it plays: the three-way setting, or (still -1 from an older file) the old quick return switch. ^/
    private int quickMode() {
        int mode = number(Settings.QUICK_MODE);
        return mode < 0 ? (bool(Settings.QUICK_RETURN) ? 1 : 0) : mode;
    }

    /^*
     * One of a few numbered choices (the way in, the arms), each named by its translation key and number; as made it
     * is {@code made}.
     ^/
    private Option choice(Tab on, String key, ForgeConfigSpec.IntValue value, int count, IntSupplier current, int made) {
    *///?}
        return new Option(on, Component.translatable("cinematic.configuration." + key), Component.translatable("cinematic.configuration." + key + ".tooltip")) {
            @Override
            AbstractWidget control() {
                Integer[] values = new Integer[count];
                for (int i = 0; i < count; i++) values[i] = i;
                return CycleButton.<Integer>builder(v -> Component.translatable("cinematic.settings." + key + "." + v).withStyle(ChatFormatting.WHITE))
                        //? if neoforge {
                        .withValues(values).withInitialValue(current.getAsInt()).displayOnlyValue()
                        //?} else if fabric {
                        /*.withValues(values).withInitialValue(number(value)).displayOnlyValue()
                        *///?} else if forge {
                        /*.withValues(values).withInitialValue(current.getAsInt()).displayOnlyValue()
                        *///?}
                        .create(0, 0, CONTROL, 20, label, (button, to) -> {
                            put(value, to);
                            // (the way in switches the approach speed and the quick return window on or off)
                            dirty = true;
                        });
            }

            @Override
            boolean changed() {
                //? if neoforge {
                return current.getAsInt() != made;
                //?} else if fabric {
                /*return number(value) != value.getDefault();
                *///?} else if forge {
                /*return current.getAsInt() != made;
                *///?}
            }

            @Override
            void reset() {
                //? if neoforge {
                put(value, made);
                //?} else if fabric {
                /*put(value, value.getDefault());
                *///?} else if forge {
                /*put(value, made);
                *///?}
            }
        };
    }

    private Option presetOption() {
        return new Option(Tab.CAMERA, Component.translatable("cinematic.settings.preset"), Component.translatable("cinematic.settings.preset.tooltip")) {
            @Override
            AbstractWidget control() {
                return new Button(0, 0, CONTROL, 20, Component.empty(), button -> {
                    Preset now = preset();
                    choose(Preset.values()[now == Preset.CUSTOM ? Preset.BALANCED.ordinal() : (now.ordinal() + 1) % Preset.CUSTOM.ordinal()]);
                    dirty = true;
                }, narration -> narration.get()) {
                    @Override
                    public Component getMessage() {
                        Preset now = preset();
                        return now.title().copy().withStyle(now == Preset.CUSTOM ? ChatFormatting.GOLD : ChatFormatting.WHITE);
                    }
                };
            }

            @Override
            boolean changed() {
                return preset() != Preset.BALANCED;
            }

            @Override
            void reset() {
                choose(Preset.BALANCED);
            }
        };
    }

    private Option profileOption(StationKind kind) {
        Option option = new Option(Tab.ADVANCED, LayoutProfiles.name(kind), Component.translatable("cinematic.settings.profiles.tooltip")) {
            @Override
            AbstractWidget control() {
                return CycleButton.<Integer>builder(LayoutProfiles::value).withValues(0, 1, 2).withInitialValue(profile(kind)).displayOnlyValue()
                        .create(0, 0, CONTROL, 20, label, (button, to) -> profiles.put(kind, to));
            }

            @Override
            boolean changed() {
                return profile(kind) != 0;
            }

            @Override
            void reset() {
                profiles.put(kind, 0);
            }
        };
        option.developer = true;
        return option;
    }

    /** Every option in the order it is shown, each tab's under its headers (a header is a null option and its title). */
    private List<Object> options() {
        Function<Boolean, Component> onOff = on -> value(on, "cinematic.settings.on", "cinematic.settings.off");
        Function<Boolean, Component> showHide = on -> value(on, "cinematic.settings.show", "cinematic.settings.hide");
        List<Object> all = new ArrayList<>();
        // general
        all.add(toggle(Tab.GENERAL, "heightLogic", Settings.HEIGHT_LOGIC,
                on -> on ? Component.translatable("cinematic.settings.reach.window").withStyle(ChatFormatting.WHITE)
                        : Component.translatable("cinematic.settings.reach.notice").withStyle(ChatFormatting.WHITE)));
        all.add(toggle(Tab.GENERAL, "controlHints", Settings.CONTROL_HINTS, showHide));
        Option invite = toggle(Tab.GENERAL, "campfireInvite", Settings.CAMPFIRE_INVITE, showHide);
        // there is nothing to invite to with the campfire scene off
        invite.enabled = () -> bool(Settings.SCENES.get(StationKind.CAMPFIRE));
        all.add(invite);
        all.add(toggle(Tab.GENERAL, "lookToggles", Settings.LOOK_TOGGLES,
                on -> Component.translatable(on ? "cinematic.settings.look.toggle" : "cinematic.settings.look.hold").withStyle(ChatFormatting.WHITE)));
        //? if neoforge {
        all.add(choice(Tab.GENERAL, "armStyle", Settings.ARM_STYLE, 3, () -> number(Settings.ARM_STYLE), 0));
        //?} else if fabric {
        /*all.add(choice(Tab.GENERAL, "armStyle", Settings.ARM_STYLE, 3));
        *///?} else if forge {
        /*all.add(choice(Tab.GENERAL, "armStyle", Settings.ARM_STYLE, 3, () -> number(Settings.ARM_STYLE), 0));
        *///?}
        // stations: each in its own group, then those of add-ons under the mod that adds them
        Map<String, List<StationKind>> groups = new LinkedHashMap<>();
        for (String group : GROUP_ORDER) groups.put(group, new ArrayList<>());
        for (StationKind kind : StationKind.values()) {
            if (kind != StationKind.ADDON) groups.computeIfAbsent(kind.group, group -> new ArrayList<>()).add(kind);
        }
        groups.forEach((group, kinds) -> {
            if (kinds.isEmpty()) return;
            all.add(new Header(Tab.STATIONS, Component.translatable("cinematic.settings.group." + group)));
            for (StationKind kind : kinds) all.add(new StationOption(kind));
        });
        Map<String, List<AddonStations.Definition>> addons = new LinkedHashMap<>();
        for (AddonStations.Definition addon : AddonStations.all()) {
            addons.computeIfAbsent(addon.blockId().getNamespace(), mod -> new ArrayList<>()).add(addon);
        }
        addons.forEach((mod, definitions) -> {
            all.add(new Header(Tab.STATIONS, Component.literal(modName(mod))));
            for (AddonStations.Definition addon : definitions) all.add(new AddonOption(addon, modName(mod)));
        });
        // camera
        all.add(presetOption());
        all.add(toggle(Tab.CAMERA, "reduceMotion", Settings.REDUCE_MOTION, onOff));
        all.add(new Header(Tab.CAMERA, Component.translatable("cinematic.settings.pace")));
        //? if neoforge {
        all.add(choice(Tab.CAMERA, "quickMode", Settings.QUICK_MODE, 3, this::quickMode, 1));
        //?} else if fabric {
        /*all.add(choice(Tab.CAMERA, "quickMode", Settings.QUICK_MODE, 3));
        *///?} else if forge {
        /*all.add(choice(Tab.CAMERA, "quickMode", Settings.QUICK_MODE, 3, this::quickMode, 1));
        *///?}
        Option approach = slider(Tab.CAMERA, "approachSpeed", Settings.APPROACH_SPEED, 50, 200, SettingsScreen::percent);
        // every way in quick: the lean in always has the quick pace
        //? if neoforge {
        approach.enabled = () -> quickMode() != 2;
        //?} else if fabric {
        /*approach.enabled = () -> number(Settings.QUICK_MODE) != 2;
        *///?} else if forge {
        /*approach.enabled = () -> quickMode() != 2;
        *///?}
        all.add(approach);
        all.add(slider(Tab.CAMERA, "leaveSpeed", Settings.LEAVE_SPEED, 50, 200, SettingsScreen::percent));
        Option window = slider(Tab.CAMERA, "quickWindow", Settings.QUICK_WINDOW, 5, 30, v -> Component.translatable("cinematic.settings.seconds", v));
        //? if neoforge {
        window.enabled = () -> quickMode() == 1;
        //?} else if fabric {
        /*window.enabled = () -> number(Settings.QUICK_MODE) == 1;
        *///?} else if forge {
        /*window.enabled = () -> quickMode() == 1;
        *///?}
        all.add(window);
        all.add(new Header(Tab.CAMERA, Component.translatable("cinematic.settings.view")));
        all.add(slider(Tab.CAMERA, "leanStrength", Settings.LEAN_STRENGTH, 0, 100, SettingsScreen::percent));
        all.add(slider(Tab.CAMERA, "sceneZoom", Settings.SCENE_ZOOM, 0, 100, SettingsScreen::percent));
        all.add(new Header(Tab.CAMERA, Component.translatable("cinematic.settings.effects")));
        for (String key : new String[]{"letterbox", "livingCamera", "cameraShake", "screenFlash"}) {
            //? if neoforge {
            ModConfigSpec.BooleanValue value = switch (key) {
            //?} else if fabric {
            /*ClientConfig.BooleanValue value = switch (key) {
            *///?} else if forge {
            /*ForgeConfigSpec.BooleanValue value = switch (key) {
            *///?}
                case "letterbox" -> Settings.LETTERBOX;
                case "livingCamera" -> Settings.LIVING_CAMERA;
                case "cameraShake" -> Settings.CAMERA_SHAKE;
                default -> Settings.SCREEN_FLASH;
            };
            Option effect = toggle(Tab.CAMERA, key, value, onOff);
            // reduce motion has them all off, whatever they are set to
            effect.enabled = () -> !bool(Settings.REDUCE_MOTION);
            all.add(effect);
        }
        // advanced
        Option clientOnly = toggle(Tab.ADVANCED, Component.translatable("cinematic.configuration.clientOnlyMode"),
                inWorld ? Component.translatable("cinematic.settings.clientOnlyMode.locked")
                        : Component.translatable("cinematic.configuration.clientOnlyMode.tooltip"),
                Settings.CLIENT_ONLY_MODE, onOff);
        // it takes effect on the next connection: changed from the main menu only
        clientOnly.enabled = () -> !inWorld;
        all.add(clientOnly);
        all.add(toggle(Tab.ADVANCED, "developerMode", Settings.DEVELOPER_MODE, onOff));
        all.add(new Header(Tab.ADVANCED, Component.translatable("cinematic.settings.profiles"), true));
        for (StationKind kind : StationKind.values()) {
            //? if !forge {
            if (kind != StationKind.ADDON) {
            //?} else if forge {
            /*if (LayoutProfiles.supports(kind)) {
            *///?}
                all.add(profileOption(kind));
            }
        }
        return all;
    }

    /** A station's scene on or off, shown as its block with its name in a grid of two. */
    private final class StationOption extends Option {
        final StationKind kind;

        StationOption(StationKind kind) {
            super(Tab.STATIONS, LayoutProfiles.name(kind), Component.translatable("cinematic.configuration." + Settings.key(kind) + ".tooltip"));
            this.kind = kind;
        }

        @Override
        AbstractWidget control() {
            return new StationButton(this);
        }

        @Override
        boolean changed() {
            return !bool(Settings.SCENES.get(kind));
        }

        @Override
        void reset() {
            put(Settings.SCENES.get(kind), true);
        }
    }

    /**
     * An add-on's station, listed from the add-ons' registry: on or off, kept in the list of those turned off (add-ons
     * register theirs after the settings file is built, so they cannot have a setting each).
     */
    private final class AddonOption extends Option {
        final AddonStations.Definition addon;

        AddonOption(AddonStations.Definition addon, String mod) {
            super(Tab.STATIONS, block(addon).getName(), Component.translatable("cinematic.settings.addon.tooltip", mod));
            this.addon = addon;
        }

        boolean on() {
            return !disabledAddons().contains(addon.id().toString());
        }

        void set(boolean on) {
            Set<String> ids = new java.util.LinkedHashSet<>(disabledAddons());
            if (on) ids.remove(addon.id().toString());
            else ids.add(addon.id().toString());
            putDisabledAddons(ids);
        }

        @Override
        AbstractWidget control() {
            return new AddonButton(this);
        }

        @Override
        boolean changed() {
            return !on();
        }

        @Override
        void reset() {
            set(true);
        }
    }

    private static net.minecraft.world.level.block.Block block(AddonStations.Definition addon) {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(addon.blockId());
    }

    /** A title over the options after it (shown only with developer mode on, for the profiles). */
    private record Header(Tab on, Component title, boolean developer) {
        Header(Tab on, Component title) {
            this(on, title, false);
        }
    }

    // ---- layout ------------------------------------------------------------------------------------

    @Override
    protected void init() {
        tabButtons.clear();
        search = new EditBox(font, width / 2 - 100, 20, 200, 18, Component.translatable("cinematic.settings.search"));
        search.setHint(Component.translatable("cinematic.settings.search").withStyle(ChatFormatting.DARK_GRAY));
        // (set before the responder: the list is not there yet)
        search.setValue(searchText);
        search.setResponder(text -> {
            searchText = text;
            query = text.trim().toLowerCase(Locale.ROOT);
            fill(true);
        });
        addRenderableWidget(search);
        int tabWidth = Math.min(90, (width - 20) / Tab.values().length);
        int tabsLeft = width / 2 - tabWidth * Tab.values().length / 2;
        for (Tab t : Tab.values()) {
            Button button = Button.builder(Component.translatable("cinematic.settings.tab." + t.name().toLowerCase(Locale.ROOT)), b -> {
                tab = t;
                search.setValue("");
                fill(true);
            }).bounds(tabsLeft + t.ordinal() * tabWidth, 42, tabWidth - 2, 20).build();
            tabButtons.add(button);
            addRenderableWidget(button);
        }
        rows = new Rows(minecraft, width, Math.max(40, height - 66 - 94), 66);
        addRenderableWidget(rows);
        int footer = height - 26;
        addRenderableWidget(Button.builder(Component.translatable("cinematic.settings.reset_all"), b -> {
            for (Object o : options()) {
                // (not what is locked or out of sight: client-only mode in a world, the profiles without developer mode)
                if (o instanceof Option option && option.enabled.getAsBoolean() && (!option.developer || bool(Settings.DEVELOPER_MODE))) {
                    option.reset();
                }
            }
            dirty = true;
        }).bounds(width / 2 - ROW_WIDTH / 2, footer, 100, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> {
            draft.clear();
            profiles.clear();
            minecraft.setScreen(parent);
        }).bounds(width / 2 + ROW_WIDTH / 2 - 204, footer, 100, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(width / 2 + ROW_WIDTH / 2 - 100, footer, 100, 20).build());
        fill(false);
    }

    /** The rows of the open tab, or of every tab that has a match for the search. */
    private void fill(boolean keepScroll) {
        double scroll = keepScroll ? rows.getScrollAmount() : 0;
        boolean searching = !query.isEmpty();
        for (Button button : tabButtons) {
            button.active = searching || tabButtons.indexOf(button) != tab.ordinal();
        }
        List<Row> list = new ArrayList<>();
        Tab heading = null;
        List<AbstractWidget> pair = new ArrayList<>();
        boolean developer = bool(Settings.DEVELOPER_MODE);
        int found = 0;
        if (!searching && tab == Tab.STATIONS) {
            list.add(new Row(Row.Style.BAR, Component.translatable("cinematic.settings.stations.note"), null,
                    Button.builder(Component.translatable("cinematic.settings.all_on"), b -> stations(true)).size(78, 20).build(),
                    Button.builder(Component.translatable("cinematic.settings.all_off"), b -> stations(false)).size(78, 20).build()));
        }
        for (Object o : options()) {
            if (o instanceof Header header) {
                if (!searching && header.on() == tab && (developer || !header.developer())) {
                    flush(pair, list);
                    list.add(new Row(Row.Style.HEADER, header.title(), null, null, null));
                }
                continue;
            }
            Option option = (Option) o;
            if (searching ? !option.matches(query) : option.on != tab || option.developer && !developer) continue;
            found++;
            if (searching && heading != option.on) {
                flush(pair, list);
                heading = option.on;
                list.add(new Row(Row.Style.HEADER, tabTitle(option.on), null, null, null));
            }
            AbstractWidget control = option.control();
            if (control instanceof StationButton) {
                pair.add(control);
                if (pair.size() == 2) flush(pair, list);
                continue;
            }
            flush(pair, list);
            Button reset = Button.builder(Component.literal("↺"), b -> {
                option.reset();
                dirty = true;
            }).size(20, 20).build();
            reset.setTooltip(Tooltip.create(Component.translatable("cinematic.settings.reset")));
            list.add(new Row(Row.Style.OPTION, option.label, option, control, reset));
        }
        flush(pair, list);
        if (!searching && tab == Tab.ADVANCED && !developer) {
            list.add(new Row(Row.Style.NOTE, Component.translatable("cinematic.settings.profiles.hidden"), null, null, null));
        }
        if (searching) {
            //? if >=1.21 {
            list.addFirst(new Row(Row.Style.NOTE, found == 0 ? Component.translatable("cinematic.settings.no_results", searchText.trim())
            //?} else if <1.21 {
            /*list.add(0, new Row(Row.Style.NOTE, found == 0 ? Component.translatable("cinematic.settings.no_results", searchText.trim())
            *///?}
                    : Component.translatable("cinematic.settings.results", found), null, null, null));
        }
        rows.set(list);
        rows.setScrollAmount(scroll);
    }

    private void flush(List<AbstractWidget> pair, List<Row> list) {
        if (!pair.isEmpty()) {
            list.add(new Row(Row.Style.PAIR, null, null, pair.get(0), pair.size() > 1 ? pair.get(1) : null));
            pair.clear();
        }
    }

    private void stations(boolean on) {
        for (StationKind kind : Settings.SCENES.keySet()) {
            put(Settings.SCENES.get(kind), on);
        }
        Set<String> off = new java.util.LinkedHashSet<>();
        if (!on) for (AddonStations.Definition addon : AddonStations.all()) off.add(addon.id().toString());
        putDisabledAddons(off);
        dirty = true;
    }

    /** The add-on stations turned off in the draft. */
    private Set<String> disabledAddons() {
        return Settings.parseIds((String) draft.getOrDefault(Settings.DISABLED_ADDONS, Settings.DISABLED_ADDONS.get()));
    }

    private void putDisabledAddons(Set<String> ids) {
        put(Settings.DISABLED_ADDONS, String.join(",", ids));
    }

    /** The name a mod shows in the mod list, or its id. */
    private static String modName(String mod) {
        //? if neoforge {
        return net.neoforged.fml.ModList.get().getModContainerById(mod).map(c -> c.getModInfo().getDisplayName()).orElse(mod);
        //?} else if fabric {
        /*return net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer(mod).map(c -> c.getMetadata().getName()).orElse(mod);
        *///?} else if forge {
        /*return net.minecraftforge.fml.ModList.get().getModContainerById(mod).map(c -> c.getModInfo().getDisplayName()).orElse(mod);
        *///?}
    }

    private static Component tabTitle(Tab t) {
        return Component.translatable("cinematic.settings.tab." + t.name().toLowerCase(Locale.ROOT));
    }

    /** Developer mode on in the draft, so the layout profiles show (for the automated walkthrough). */
    void showDeveloper() {
        put(Settings.DEVELOPER_MODE, true);
        fill(true);
    }

    @Override
    public void tick() {
        if (dirty) {
            dirty = false;
            fill(true);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        described = null;
        //? if fabric && <1.21 {
        /*// (1.20.1's screens draw no background of their own)
        renderBackground(graphics);
        *///?} else if forge {
        /*// (Forge 1.20.1's screens draw no background of their own)
        renderBackground(graphics);
        *///?}
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 7, 0xFFFFFF);
        // what the option under the mouse does
        int top = height - 88, box = 56, left = width / 2 - ROW_WIDTH / 2;
        graphics.fill(left, top, left + ROW_WIDTH, top + box, 0x90000000);
        graphics.renderOutline(left, top, ROW_WIDTH, box, 0x40FFFFFF);
        Component text = described != null ? described : Component.translatable("cinematic.settings.describe");
        int y = top + 5;
        for (FormattedCharSequence line : font.split(text, ROW_WIDTH - 12)) {
            if (y > top + box - 10) break;
            graphics.drawString(font, line, left + 6, y, described != null ? 0xE0E0E0 : NOTE, false);
            y += 10;
        }
    }

    @Override
    public void onClose() {
        apply();
        minecraft.setScreen(parent);
    }

    /** What a switch does in its current position: green when on, muted when off. */
    private static Component value(boolean on, String onKey, String offKey) {
        return Component.translatable(on ? onKey : offKey).withStyle(on ? ChatFormatting.GREEN : ChatFormatting.GRAY);
    }

    private static Component percent(int v) {
        return Component.literal(v + "%");
    }

    // ---- widgets -----------------------------------------------------------------------------------

    /** A station's block, its name and On or Off; a click switches its scene. */
    private final class StationButton extends Button {
        final StationOption option;
        private final ItemStack stack;

        StationButton(StationOption option) {
            super(0, 0, 150, 20, Component.empty(), button -> {
                //? if neoforge {
                ModConfigSpec.BooleanValue value = Settings.SCENES.get(option.kind);
                //?} else if fabric {
                /*ClientConfig.BooleanValue value = Settings.SCENES.get(option.kind);
                *///?} else if forge {
                /*ForgeConfigSpec.BooleanValue value = Settings.SCENES.get(option.kind);
                *///?}
                put(value, !bool(value));
            }, narration -> narration.get());
            this.option = option;
            this.stack = new ItemStack(option.kind.icon);
        }

        @Override
        public Component getMessage() {
            boolean on = bool(Settings.SCENES.get(option.kind));
            return Component.empty().append(option.label).append(" · ").append(value(on, "cinematic.settings.on", "cinematic.settings.off"));
        }

        @Override
        public void renderString(GuiGraphics graphics, Font font, int color) {
            graphics.renderItem(stack, getX() + 3, getY() + 2);
            renderScrollingString(graphics, font, getMessage(), getX() + 22, getY(), getX() + getWidth() - 3, getY() + getHeight(), color);
        }
    }

    /** An add-on station's button: its block's item, its name, and on or off. */
    private final class AddonButton extends Button {
        final AddonOption option;
        private final ItemStack stack;

        AddonButton(AddonOption option) {
            super(0, 0, 150, 20, Component.empty(), button -> option.set(!option.on()), narration -> narration.get());
            this.option = option;
            this.stack = new ItemStack(block(option.addon));
        }

        @Override
        public Component getMessage() {
            return Component.empty().append(option.label).append(" · ").append(value(option.on(), "cinematic.settings.on", "cinematic.settings.off"));
        }

        @Override
        public void renderString(GuiGraphics graphics, Font font, int color) {
            graphics.renderItem(stack, getX() + 3, getY() + 2);
            renderScrollingString(graphics, font, getMessage(), getX() + 22, getY(), getX() + getWidth() - 3, getY() + getHeight(), color);
        }
    }

    /** A whole number on a slider, kept in the draft as it moves. */
    private final class IntSlider extends AbstractSliderButton {
        //? if neoforge {
        private final ModConfigSpec.IntValue setting;
        //?} else if fabric {
        /*private final ClientConfig.IntValue setting;
        *///?} else if forge {
        /*private final ForgeConfigSpec.IntValue setting;
        *///?}
        private final int min, max;
        private final Function<Integer, Component> text;

        //? if neoforge {
        IntSlider(ModConfigSpec.IntValue setting, int min, int max, Function<Integer, Component> text) {
        //?} else if fabric {
        /*IntSlider(ClientConfig.IntValue setting, int min, int max, Function<Integer, Component> text) {
        *///?} else if forge {
        /*IntSlider(ForgeConfigSpec.IntValue setting, int min, int max, Function<Integer, Component> text) {
        *///?}
            super(0, 0, CONTROL, 20, Component.empty(), (Mth.clamp(number(setting), min, max) - min) / (double) (max - min));
            this.setting = setting;
            this.min = min;
            this.max = max;
            this.text = text;
            updateMessage();
        }

        private int current() {
            return min + (int) Math.round(value * (max - min));
        }

        @Override
        protected void updateMessage() {
            int v = current();
            setMessage(text.apply(v).copy().withStyle(v == setting.getDefault() ? ChatFormatting.WHITE : ChatFormatting.GOLD));
        }

        @Override
        protected void applyValue() {
            put(setting, current());
        }
    }

    /** The scrolling list of rows, as wide as the row and with its scroll bar just beside it. */
    private static final class Rows extends ContainerObjectSelectionList<Row> {
        Rows(Minecraft minecraft, int width, int height, int y) {
            //? if >=1.21 {
            super(minecraft, width, height, y, 24);
            //?} else if <1.21 {
            /*super(minecraft, width, height, y, y + height, 24);
            setRenderBackground(false);
            setRenderTopAndBottom(false);
            *///?}
        }

        void set(List<Row> list) {
            replaceEntries(list);
        }

        @Override
        public int getRowWidth() {
            return ROW_WIDTH;
        }

        @Override
        protected int getScrollbarPosition() {
            return width / 2 + ROW_WIDTH / 2 + 6;
        }
    }

    /** One row of the list. */
    private final class Row extends ContainerObjectSelectionList.Entry<Row> {
        enum Style {
            /** A title over the rows after it. */
            HEADER,
            /** A line of explanation. */
            NOTE,
            /** An option: its name, its control and its reset arrow. */
            OPTION,
            /** Two stations side by side. */
            PAIR,
            /** A line of explanation with two buttons after it (the stations' all on / all off). */
            BAR
        }

        private final Style style;
        @Nullable
        private final Component label;
        @Nullable
        private final Option option;
        @Nullable
        private final AbstractWidget first, second;
        private final List<AbstractWidget> widgets = new ArrayList<>();

        Row(Style style, @Nullable Component label, @Nullable Option option, @Nullable AbstractWidget first, @Nullable AbstractWidget second) {
            this.style = style;
            this.label = label;
            this.option = option;
            this.first = first;
            this.second = second;
            if (first != null) widgets.add(first);
            if (second != null) widgets.add(second);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY,
                boolean hovering, float partialTick) {
            switch (style) {
                case HEADER -> graphics.drawString(font, label, left + 4, top + 10, HEADER, false);
                case NOTE -> graphics.drawString(font, font.plainSubstrByWidth(label.getString(), width - 8), left + 4, top + 8, NOTE, false);
                case OPTION -> {
                    boolean enabled = option.enabled.getAsBoolean();
                    graphics.drawString(font, font.plainSubstrByWidth(label.getString(), width - CONTROL - 32), left + 4, top + 6,
                            enabled ? 0xFFFFFF : 0x808080, false);
                    // (another option may switch this one on or off: the campfire its invitation, reduce motion the effects)
                    first.active = enabled;
                    first.setPosition(left + width - CONTROL - 24, top);
                    first.render(graphics, mouseX, mouseY, partialTick);
                    second.setPosition(left + width - 20, top);
                    second.visible = enabled && option.changed();
                    second.render(graphics, mouseX, mouseY, partialTick);
                    if (hovering) described = option.description;
                }
                case BAR -> {
                    graphics.drawString(font, font.plainSubstrByWidth(label.getString(), width - 170), left + 4, top + 6, NOTE, false);
                    first.setPosition(left + width - 160, top);
                    second.setPosition(left + width - 78, top);
                    first.render(graphics, mouseX, mouseY, partialTick);
                    second.render(graphics, mouseX, mouseY, partialTick);
                }
                case PAIR -> {
                    first.setPosition(left, top);
                    first.render(graphics, mouseX, mouseY, partialTick);
                    if (first.isHovered()) described = ((StationButton) first).option.description;
                    if (second != null) {
                        second.setPosition(left + width - second.getWidth(), top);
                        second.render(graphics, mouseX, mouseY, partialTick);
                        if (second.isHovered()) described = ((StationButton) second).option.description;
                    }
                }
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return widgets;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return widgets;
        }
    }
}
