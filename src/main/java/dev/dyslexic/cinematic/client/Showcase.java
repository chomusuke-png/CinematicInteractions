package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.Cinematic;
import dev.dyslexic.cinematic.furnace.FurnaceLayout;
import dev.dyslexic.cinematic.furnace.FurnaceZones;
import dev.dyslexic.cinematic.station.StationKind;
import dev.dyslexic.cinematic.table.TableGrid;
import dev.dyslexic.cinematic.table.TableStore;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
//? if >=1.21 {
import net.minecraft.core.component.DataComponents;
//?}
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
//? if neoforge {
import net.neoforged.neoforge.client.model.data.ModelData;
//?} else if forge {
/*import net.minecraftforge.client.model.data.ModelData;
*///?}
import org.lwjgl.glfw.GLFW;

/**
 * Development-only scripted walkthrough, enabled with -Dcinematic.showcase=true: builds a little workshop in a
 * flat world and goes through every station with simulated cursor moves (craft, smelt, smoke, blast, enchant,
 * forge, grind, smith, cut), saving screenshots along the way and logging what ended up where. Inert in normal
 * play.
 */
public final class Showcase {
    private Showcase() {}

    public static final boolean ENABLED = Boolean.getBoolean("cinematic.showcase");
    /**
     * Which act to start from, to film one station quickly: 0 table, 3 furnace, 4 smoker, 5 blast furnace,
     * 6 enchanting, a anvil, g grindstone, s smithing table, c stonecutter, l loom, m cartography table, b brewing stand,
     * r crafter, f campfire, u soul campfire, k lectern, n beacon, h heights, o settings, t moving over between stations,
     * y laying out a recipe, v looking round from behind, j jukebox, q the quick way back, e chiseled bookshelf,
     * w what others see (another player, a stand-in, forges, grinds, cuts and reads).
     */
    private static final String FROM = System.getProperty("cinematic.showcase.from", "0");
    /** Just the act started from: the walkthrough ends where it would hand over to the next act. */
    private static final boolean ONLY = Boolean.getBoolean("cinematic.showcase.only");
    private static final boolean EDITOR_BATCH = Boolean.getBoolean("cinematic.showcase.editorBatch");
    private static final String WORLD = System.getProperty("cinematic.showcase.world", "cinematic-showcase");
    private static final BlockPos TABLE = new BlockPos(0, -60, 2);
    private static final BlockPos FURNACE = TABLE.west();
    private static final BlockPos SMOKER = new BlockPos(2, -60, 2);
    private static final BlockPos BLAST = new BlockPos(4, -60, 2);
    private static final BlockPos ENCHANT = new BlockPos(-5, -60, 8);
    // the smithy: a row along a back wall, each worked from the south
    private static final BlockPos ANVIL = new BlockPos(8, -60, 5);
    private static final BlockPos GRINDSTONE = new BlockPos(10, -60, 5);
    private static final BlockPos SMITHING = new BlockPos(12, -60, 5);
    private static final BlockPos STONECUTTER = new BlockPos(14, -60, 5);
    // the second workshop, along another wall, and a campfire out in the open
    private static final BlockPos LOOM = new BlockPos(8, -60, 11);
    private static final BlockPos CARTOGRAPHY = new BlockPos(10, -60, 11);
    private static final BlockPos BREWING = new BlockPos(12, -60, 11);
    //? if >=1.21 {
    private static final BlockPos CRAFTER = new BlockPos(14, -60, 11);
    //?}
    private static final BlockPos CAMPFIRE = new BlockPos(11, -60, 17);
    private static final BlockPos SOUL_CAMPFIRE = new BlockPos(15, -60, 17);
    private static final BlockPos LECTERN = new BlockPos(6, -60, 21);
    /** On top of a full pyramid: iron, gold, emerald and diamond, nine to three wide. */
    private static final BlockPos BEACON = new BlockPos(22, -56, 22);
    /** A chiseled bookshelf in the reading corner, worked from the south, another one on top of it. */
    private static final BlockPos SHELF = new BlockPos(3, -60, 21);
    /** Where on the shelf's south face each slot's book is (x from the west, y up). */
    private static final Vec3[] SHELF_SLOTS = {new Vec3(0.19, 0.72, 1), new Vec3(0.5, 0.75, 1), new Vec3(0.81, 0.75, 1), new Vec3(0.19, 0.25, 1),
            new Vec3(0.5, 0.22, 1), new Vec3(0.81, 0.25, 1)};
    /** A jukebox out by the campfires, worked from the west. */
    private static final BlockPos JUKEBOX = new BlockPos(19, -60, 17);
    /** The height cases stand in a row along z = 22, four blocks apart, going west from here. */
    private static final int HEIGHTS_X = -10;


    private record Step(int delay, Runnable action) {}

    private static boolean requested;
    private static int t = -1;
    private static int wait;
    private static final Deque<Step> STEPS = new ArrayDeque<>();
    private static final List<String> REPORT = new ArrayList<>();
    private static final double[] INTRO_SHOTS = {0.15, 0.35, 0.55, 0.75, 0.92};
    private static final double[] OUTRO_SHOTS = {0.1, 0.25, 0.45, 0.65, 0.85};
    private static int introShot, outroShot;
    /** Frames of a jukebox move, by its progress. */
    private static final double[] MOVE_SHOTS = {0.15, 0.35, 0.55, 0.75, 0.95};
    private static int moveShot;
    /** Screenshot name prefix for the scene being filmed (the act, see {@link #FROM}). */
    private static String act = "0";
    private static boolean waitingForActive;
    private static int waitingTicks;
    private static boolean done;

    public static void onScreen(Screen screen) {
        if (!ENABLED || requested || !(screen instanceof TitleScreen)) {
            return;
        }
        requested = true;
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            try {
                if (mc.getLevelSource().levelExists(WORLD)) {
                    //? if >=1.21 {
                    mc.createWorldOpenFlows().openWorld(WORLD, () -> mc.setScreen(new TitleScreen()));
                    //?} else if <1.21 {
                    /*mc.createWorldOpenFlows().loadLevel(new TitleScreen(), WORLD);
                    *///?}
                    return;
                }
            } catch (Exception e) {
                Cinematic.LOGGER.warn("[Showcase] could not reopen world", e);
            }
            LevelSettings settings = new LevelSettings(WORLD, GameType.SURVIVAL, false, Difficulty.PEACEFUL, true, new GameRules(),
                    WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(20261010L, false, false),
                    registry -> registry.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value()
                            //? if >=1.21 {
                            .createWorldDimensions(), screen);
                            //?} else if <1.21 {
                            /*.createWorldDimensions());
                            *///?}
        });
    }

    public static void tick() {
        if (!ENABLED || done) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) {
            return;
        }
        if (mc.screen != null && t < 0) {
            return;
        }
        t++;
        if (t == 1) {
            setup();
        } else if (t == 70) {
            shot("00_before");
        } else if (t == 75) {
            switch (FROM) {
                case "ip" -> nextAct("ip", new Vec3(0.7, -60, 5.6), 172f, TABLE, new Vec3(0.5, 1, 0.8), Direction.UP);
                //? if <1.21 {
                /*case "tw" -> nextAct("tw", new Vec3(0.7, -60, 5.6), 172f, TABLE, new Vec3(0.5, 1, 0.8), Direction.UP);
                case "pm", "pf", "ps" -> polymorphAct(FROM);
                *///?}
                case "ji" -> nextAct("ji", new Vec3(0.7, -60, 5.6), 172f, TABLE, new Vec3(0.5, 1, 0.8), Direction.UP);
                case "3" -> nextAct("3", new Vec3(-3.2, -60, 4.6), 200f, FURNACE, new Vec3(0.5, 0.6, 1.0), Direction.SOUTH);
                //? if <1.21 {
                /*case "fb" -> nextAct("fb", new Vec3(-3.2, -60, 4.6), 200f, FURNACE, new Vec3(0.5, 0.6, 1.0), Direction.SOUTH);
                *///?}
                case "4" -> nextAct("4", new Vec3(2.5, -60, 5.4), 180f, SMOKER, new Vec3(0.5, 0.6, 1.0), Direction.SOUTH);
                case "5" -> nextAct("5", new Vec3(4.5, -60, 5.4), 180f, BLAST, new Vec3(0.5, 0.6, 1.0), Direction.SOUTH);
                //? if >=1.21 {
                case "i" -> {
                    step(2, () -> server(p -> p.serverLevel().setBlockAndUpdate(CRAFTER, Blocks.CRAFTER.defaultBlockState()
                            .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.ORIENTATION, net.minecraft.core.FrontAndTop.UP_SOUTH))));
                    nextAct("i", new Vec3(14.5, -60, 14.4), 180f, CRAFTER, new Vec3(0.5, 0.5, 1), Direction.SOUTH);
                //?} else if <1.21 {
                /*case "i", "r" -> {
                    // Minecraft 1.20.1 has no crafter: its acts are not part of this port
                    REPORT.add("act " + FROM + ": the crafter is not in Minecraft 1.20.1");
                    step(2, () -> finish(Minecraft.getInstance()));
                *///?}
                }
                case "6" -> nextAct("6", new Vec3(-4.5, -60, 11.6), 180f, ENCHANT, new Vec3(0.5, 0.75, 0.6), Direction.UP);
                case "a" -> smithyAct("a", ANVIL);
                case "g" -> smithyAct("g", GRINDSTONE);
                case "s" -> smithyAct("s", SMITHING);
                case "c" -> smithyAct("c", STONECUTTER);
                case "l" -> {
                    restock();
                    smithyAct("l", LOOM);
                }
                case "m" -> {
                    restock();
                    smithyAct("m", CARTOGRAPHY);
                }
                case "b" -> {
                    restock();
                    smithyAct("b", BREWING);
                }
                //? if >=1.21 {
                case "r" -> {
                    restock();
                    smithyAct("r", CRAFTER);
                }
                //?}
                case "f" -> {
                    restock();
                    campfireAct();
                }
                case "u" -> {
                    restock();
                    soulCampfireAct();
                }
                case "k" -> {
                    restock();
                    lecternAct();
                }
                case "n" -> {
                    restock();
                    beaconAct();
                }
                case "h" -> heightAct();
                case "o" -> optionsAct();
                case "t" -> transitionAct();
                case "y" -> layoutAct();
                case "v" -> lookAct();
                case "j" -> jukeboxAct();
                case "q" -> quickAct();
                case "e" -> {
                    restock();
                    shelfAct();
                }
                case "w" -> watchAct();
                case "pn" -> penchantAct();
                case "av" -> anvilRulesAct();
                case "sq" -> quickMoveAct();
                //? if fabric && >=1.21 {
                /*case "ri" -> reopenAfterInventoryAct();
                case "jp" -> jumpUseAct();
                case "tb" -> tableKeepsAct();
                case "pm" -> polymorphAct();
                case "vx" -> lookAtFixesAct();
                case "vy" -> visualFixesAct();
                *///?} else if fabric && <1.21 {
                /*case "ri" -> reopenAfterInventoryAct();
                case "jp" -> jumpUseAct();
                case "tb" -> tableKeepsAct();
                case "vx" -> lookAtFixesAct();
                case "vy" -> visualFixesAct();
                case "cr" -> customRecipesAct();
                case "fm" -> foreignMenuAct();
                *///?} else if forge {
                /*case "cr" -> customRecipesAct();
                case "fm" -> foreignMenuAct();
                *///?}
                case "d" -> nextAct("d", new Vec3(0.7, -60, 5.6), 172f, TABLE, new Vec3(0.5, 1, 0.8), Direction.UP);
                case "p" -> nextAct("p", new Vec3(-3.2, -60, 4.6), 200f, FURNACE, new Vec3(0.5, 0.6, 1), Direction.SOUTH);
                case "z" -> { developerOptionsScript(); step(3, () -> finish(Minecraft.getInstance())); }
                default -> use(TABLE, new Vec3(0.5, 1.0, 0.8), Direction.UP);
            }
        }
        if (Scene.phase == Scene.Phase.INTRO && introShot < INTRO_SHOTS.length && Scene.introProgress() >= INTRO_SHOTS[introShot]) {
            shot(String.format("%s1_intro_%02d", act, Math.round(INTRO_SHOTS[introShot++] * 100)));
        }
        if (Scene.phase == Scene.Phase.OUTRO && outroShot < OUTRO_SHOTS.length && Scene.time / Scene.OUTRO_LENGTH >= OUTRO_SHOTS[outroShot]) {
            shot(String.format("%s8_outro_%02d", act, Math.round(OUTRO_SHOTS[outroShot++] * 100)));
        }
        if (Scene.station instanceof JukeboxStation jukebox && Scene.phase == Scene.Phase.ACTIVE) {
            String[] move = jukebox.moveName().split(" ");
            double progress = Double.parseDouble(move[1].replace(',', '.'));
            if (!move[0].equals("none") && moveShot < MOVE_SHOTS.length && progress >= MOVE_SHOTS[moveShot]) {
                shot(String.format("%s5_%s_%02d", act, move[0], Math.round(MOVE_SHOTS[moveShot++] * 100)));
            }
        }
        if (waitingForActive && Scene.phase == Scene.Phase.NONE && ++waitingTicks > 300) {
            // the scene never started: say so and go on, so one broken act does not stall the rest
            check("scene of act " + act + " started", false, "refused=" + Scene.refused + " screen=" + Minecraft.getInstance().screen);
            closeWindow();
            waitingTicks = 0;
            waitingForActive = false;
            // its script runs anyway (its steps fail one by one and say so) and leads on to the next act
            runScript();
        }
        if (!waitingForActive) {
            waitingTicks = 0;
        }
        if (waitingForActive && Scene.phase == Scene.Phase.ACTIVE && Scene.time > 0.9) {
            waitingForActive = false;
            runScript();
        }
        if (wait > 0) {
            wait--;
        } else if (!STEPS.isEmpty()) {
            Step step = STEPS.poll();
            try {
                step.action().run();
            } catch (RuntimeException e) {
                check("step in act " + act + " ran", false, e.toString());
                Cinematic.LOGGER.warn("[Showcase] a step failed", e);
            }
            wait = STEPS.isEmpty() ? 0 : STEPS.peek().delay();
        }
        if (t > 40000) {
            REPORT.add("timed out");
            finish(mc);
        }
    }

    /** The script of the act whose scene has just begun. */
    private static void runScript() {
        switch (act) {
            case "ip" -> inventoryEffectsScript();
            //? if <1.21 {
            /*case "tw" -> tableWindowScript();
            case "pm" -> polymorphScript();
            case "pf" -> polymorphFurnaceScript();
            case "ps" -> polymorphSmithingScript();
            *///?}
            case "ji" -> fieldGuideScript();
            case "0" -> tableScript();
            case "d" -> {
                tableGridEditorScript();
                step(30, () -> finish(Minecraft.getInstance()));
            }
            case "p" -> {
                StationKind kind = Scene.station.kind();
                furnaceEditorScript();
                step(3, Scene::leave);
                step(45, () -> {});
                if (kind == StationKind.FURNACE) nextAct("p", new Vec3(2.5, -60, 5.4), 180f, SMOKER, new Vec3(0.5, 0.6, 1), Direction.SOUTH);
                else if (kind == StationKind.SMOKER) nextAct("p", new Vec3(4.5, -60, 5.4), 180f, BLAST, new Vec3(0.5, 0.6, 1), Direction.SOUTH);
                else { developerOptionsScript(); step(3, () -> finish(Minecraft.getInstance())); }
            }
            case "3" -> furnaceScript();
            //? if <1.21 {
            /*case "fb" -> furnaceBreakScript();
            *///?}
            case "4" -> smokerScript();
            case "5" -> blastScript();
            case "6" -> enchantScript();
            case "a" -> anvilScript();
            case "g" -> grindstoneScript();
            case "s" -> smithingScript();
            case "c" -> stonecutterScript();
            case "l" -> loomScript();
            case "m" -> cartographyScript();
            case "b" -> brewingScript();
            //? if >=1.21 {
            case "r", "i" -> crafterScript();
            //?}
            case "u" -> soulCampfireScript();
            case "k" -> lecternScript();
            case "e" -> shelfScript();
            case "n" -> beaconScript();
            //? if <1.21 {
            /*case "cr" -> customRecipesScript();
            *///?}
            default -> campfireScript();
        }
    }

    private static void setup() {
        Minecraft mc = Minecraft.getInstance();
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        mc.options.hideGui = false;
        for (String c : new String[]{"gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false",
                "weather clear", "time set 4000", "gamerule announceAdvancements false"}) {
            cmd(c);
        }
        server(p -> {
            ServerLevel level = p.serverLevel();
            for (int x = -4; x <= 6; x++) {
                for (int y = -60; y <= -57; y++) {
                    level.setBlockAndUpdate(new BlockPos(x, y, 0), y == -57 ? Blocks.SPRUCE_PLANKS.defaultBlockState()
                            : Blocks.STONE_BRICKS.defaultBlockState());
                }
            }
            level.setBlockAndUpdate(TABLE, Blocks.CRAFTING_TABLE.defaultBlockState());
            level.setBlockAndUpdate(FURNACE, Blocks.FURNACE.defaultBlockState().setValue(AbstractFurnaceBlock.FACING, Direction.SOUTH));
            level.setBlockAndUpdate(TABLE.east(), Blocks.BARREL.defaultBlockState());
            level.setBlockAndUpdate(SMOKER, Blocks.SMOKER.defaultBlockState().setValue(AbstractFurnaceBlock.FACING, Direction.SOUTH));
            level.setBlockAndUpdate(BLAST, Blocks.BLAST_FURNACE.defaultBlockState().setValue(AbstractFurnaceBlock.FACING, Direction.SOUTH));
            level.setBlockAndUpdate(SMOKER.east(), Blocks.SPRUCE_PLANKS.defaultBlockState());
            level.setBlockAndUpdate(FURNACE.west(), Blocks.SPRUCE_LOG.defaultBlockState());
            level.setBlockAndUpdate(FURNACE.west().above(), Blocks.LANTERN.defaultBlockState());
            // a reading corner: the enchanting table with a horseshoe of bookshelves, open to the south
            level.setBlockAndUpdate(ENCHANT, Blocks.ENCHANTING_TABLE.defaultBlockState());
            for (int y = 0; y <= 1; y++) {
                for (int dx = -2; dx <= 2; dx++) {
                    level.setBlockAndUpdate(ENCHANT.offset(dx, y, -2), Blocks.BOOKSHELF.defaultBlockState());
                }
                for (int dz = -1; dz <= 0; dz++) {
                    level.setBlockAndUpdate(ENCHANT.offset(-2, y, dz), Blocks.BOOKSHELF.defaultBlockState());
                    level.setBlockAndUpdate(ENCHANT.offset(2, y, dz), Blocks.BOOKSHELF.defaultBlockState());
                }
            }
            level.setBlockAndUpdate(ENCHANT.offset(-2, 2, -2), Blocks.CANDLE.defaultBlockState().setValue(net.minecraft.world.level.block.CandleBlock.LIT, true));
            level.setBlockAndUpdate(ENCHANT.offset(2, 2, -2), Blocks.CANDLE.defaultBlockState().setValue(net.minecraft.world.level.block.CandleBlock.LIT, true));
            // the smithy
            for (int x = 7; x <= 15; x++) {
                for (int y = -60; y <= -58; y++) {
                    level.setBlockAndUpdate(new BlockPos(x, y, 3), y == -58 ? Blocks.SPRUCE_PLANKS.defaultBlockState() : Blocks.DEEPSLATE_BRICKS.defaultBlockState());
                }
            }
            level.setBlockAndUpdate(ANVIL, Blocks.ANVIL.defaultBlockState().setValue(AnvilBlock.FACING, Direction.EAST));
            level.setBlockAndUpdate(GRINDSTONE, Blocks.GRINDSTONE.defaultBlockState().setValue(GrindstoneBlock.FACING, Direction.SOUTH));
            level.setBlockAndUpdate(SMITHING, Blocks.SMITHING_TABLE.defaultBlockState());
            level.setBlockAndUpdate(STONECUTTER, Blocks.STONECUTTER.defaultBlockState().setValue(StonecutterBlock.FACING, Direction.SOUTH));
            level.setBlockAndUpdate(new BlockPos(9, -60, 4), Blocks.BLAST_FURNACE.defaultBlockState().setValue(AbstractFurnaceBlock.FACING, Direction.SOUTH));
            level.setBlockAndUpdate(new BlockPos(11, -57, 3), Blocks.LANTERN.defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(13, -57, 3), Blocks.LANTERN.defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(15, -60, 4), Blocks.STONE.defaultBlockState());
            // the second workshop and the campfire
            for (int x = 7; x <= 15; x++) {
                for (int y = -60; y <= -58; y++) {
                    level.setBlockAndUpdate(new BlockPos(x, y, 10), y == -58 ? Blocks.SPRUCE_PLANKS.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState());
                }
            }
            level.setBlockAndUpdate(LOOM, Blocks.LOOM.defaultBlockState().setValue(net.minecraft.world.level.block.LoomBlock.FACING, Direction.SOUTH));
            level.setBlockAndUpdate(CARTOGRAPHY, Blocks.CARTOGRAPHY_TABLE.defaultBlockState());
            level.setBlockAndUpdate(BREWING, Blocks.BREWING_STAND.defaultBlockState());
            // (later versions put a crafter here; 1.20.1 has none)
            //? if >=1.21 {
            level.setBlockAndUpdate(CRAFTER, Blocks.CRAFTER.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.ORIENTATION, net.minecraft.core.FrontAndTop.SOUTH_UP));
            //?}
            level.setBlockAndUpdate(new BlockPos(11, -57, 10), Blocks.LANTERN.defaultBlockState());
            level.setBlockAndUpdate(CAMPFIRE, Blocks.CAMPFIRE.defaultBlockState());
            level.setBlockAndUpdate(SOUL_CAMPFIRE, Blocks.SOUL_CAMPFIRE.defaultBlockState());
            // a reading corner: a lectern with a book, a shelf and a lantern
            level.setBlockAndUpdate(LECTERN, Blocks.LECTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LecternBlock.FACING, Direction.SOUTH));
            net.minecraft.world.level.block.LecternBlock.tryPlaceBook(null, level, LECTERN, level.getBlockState(LECTERN), storyBook());
            level.setBlockAndUpdate(LECTERN.west(), Blocks.BOOKSHELF.defaultBlockState());
            level.setBlockAndUpdate(LECTERN.west().above(), Blocks.LANTERN.defaultBlockState());
            level.setBlockAndUpdate(LECTERN.east(), Blocks.BOOKSHELF.defaultBlockState());
            // the beacon on its pyramid, open to the sky
            net.minecraft.world.level.block.Block[] layers = {Blocks.IRON_BLOCK, Blocks.GOLD_BLOCK, Blocks.EMERALD_BLOCK, Blocks.DIAMOND_BLOCK};
            for (int layer = 0; layer < 4; layer++) {
                int r = 4 - layer;
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        level.setBlockAndUpdate(BEACON.offset(dx, -4 + layer, dz), layers[layer].defaultBlockState());
                    }
                }
            }
            level.setBlockAndUpdate(BEACON, Blocks.BEACON.defaultBlockState());
            level.setBlockAndUpdate(JUKEBOX, Blocks.JUKEBOX.defaultBlockState());
            buildHeights(level);
            p.getInventory().clearContent();
            p.getInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 24));
            p.getInventory().setItem(1, new ItemStack(Items.STICK, 12));
            p.getInventory().setItem(2, new ItemStack(Items.COBBLESTONE, 40));
            p.getInventory().setItem(3, new ItemStack(Items.REDSTONE, 9));
            p.getInventory().setItem(4, new ItemStack(Items.IRON_INGOT, 5));
            p.getInventory().setItem(5, new ItemStack(Items.COAL, 8));
            p.getInventory().setItem(6, new ItemStack(Items.RAW_IRON, 6));
            p.getInventory().setItem(9, new ItemStack(Items.COAL, 16));
            p.getInventory().setItem(10, new ItemStack(Items.TORCH, 16));
            p.getInventory().setItem(11, new ItemStack(Items.BEEF, 4));
            p.getInventory().setItem(12, new ItemStack(Items.APPLE, 3));
            p.getInventory().setItem(13, new ItemStack(Items.DIAMOND_SWORD));
            p.getInventory().setItem(14, new ItemStack(Items.LAPIS_LAZULI, 12));
            p.getInventory().setItem(15, new ItemStack(Items.RAW_IRON, 4));
            p.getInventory().setItem(16, new ItemStack(Items.COAL, 16));
            ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
            pickaxe.setDamageValue(140);
            p.getInventory().setItem(17, pickaxe);
            //? if >=1.21 {
            var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            p.getInventory().setItem(19, EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantments.getOrThrow(Enchantments.EFFICIENCY), 3)));
            //?} else if <1.21 {
            /*p.getInventory().setItem(19, EnchantedBookItem.createForEnchantment(new EnchantmentInstance(Enchantments.BLOCK_EFFICIENCY, 3)));
            *///?}
            ItemStack golden = new ItemStack(Items.GOLDEN_SWORD);
            //? if >=1.21 {
            golden.enchant(enchantments.getOrThrow(Enchantments.SHARPNESS), 3);
            golden.enchant(enchantments.getOrThrow(Enchantments.LOOTING), 2);
            //?} else if <1.21 {
            /*golden.enchant(Enchantments.SHARPNESS, 3);
            golden.enchant(Enchantments.MOB_LOOTING, 2);
            *///?}
            p.getInventory().setItem(20, golden);
            // the chiseled bookshelves: books of every kind, with gaps
            net.minecraft.world.level.block.state.BlockState shelfState = Blocks.CHISELED_BOOKSHELF.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.SOUTH);
            level.setBlockAndUpdate(SHELF, shelfState);
            level.setBlockAndUpdate(SHELF.above(), shelfState);
            if (level.getBlockEntity(SHELF) instanceof net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity shelf) {
                //? if >=1.21 {
                shelf.setItem(0, EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantments.getOrThrow(Enchantments.SHARPNESS), 4)));
                //?} else if <1.21 {
                /*shelf.setItem(0, EnchantedBookItem.createForEnchantment(new EnchantmentInstance(Enchantments.SHARPNESS, 4)));
                *///?}
                shelf.setItem(1, notesBook());
                shelf.setItem(3, storyBook());
                shelf.setItem(4, new ItemStack(Items.BOOK));
                //? if >=1.21 {
                shelf.setItem(5, EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantments.getOrThrow(Enchantments.MENDING), 1)));
                //?} else if <1.21 {
                /*shelf.setItem(5, EnchantedBookItem.createForEnchantment(new EnchantmentInstance(Enchantments.MENDING, 1)));
                *///?}
            }
            if (level.getBlockEntity(SHELF.above()) instanceof net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity shelf) {
                shelf.setItem(0, new ItemStack(Items.BOOK));
                shelf.setItem(2, new ItemStack(Items.BOOK));
                shelf.setItem(3, new ItemStack(Items.WRITABLE_BOOK));
                shelf.setItem(4, new ItemStack(Items.BOOK));
            }
            p.getInventory().setItem(21, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
            p.getInventory().setItem(22, new ItemStack(Items.DIAMOND_CHESTPLATE));
            p.getInventory().setItem(23, new ItemStack(Items.NETHERITE_INGOT));
            p.getInventory().setItem(24, new ItemStack(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE));
            p.getInventory().setItem(25, new ItemStack(Items.AMETHYST_SHARD, 3));
            p.getInventory().setItem(26, new ItemStack(Items.STONE, 24));
            p.getInventory().setItem(27, new ItemStack(Items.COBBLED_DEEPSLATE, 16));
            p.getInventory().selected = 0;
            p.giveExperienceLevels(30);
            p.teleportTo(level, 0.7, -60, 5.6, 172f, 18f);
            p.setItemInHand(InteractionHand.MAIN_HAND, p.getInventory().getItem(0));
        });
    }

    private static void use(BlockPos pos, Vec3 local, Direction face) {
        Minecraft mc = Minecraft.getInstance();
        BlockHitResult hit = new BlockHitResult(Vec3.atLowerCornerOf(pos).add(local), face, pos, false);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        REPORT.add("used " + pos.toShortString() + ", phase=" + Scene.phase + " intro=" + String.format("%.2f", Scene.introLength)
                + " station=" + (Scene.station == null ? "-" : Scene.station.kind()));
        introShot = 0;
        outroShot = 0;
        waitingForActive = true;
    }

    /** Moves on to the next station: walk to a spot, then use it. */
    /** Running just one act ({@link #ONLY}): the walkthrough ends where it would go on to another. */
    private static boolean stopHere(String code) {
        if (ONLY && !code.equals(FROM)) {
            step(2, () -> finish(Minecraft.getInstance()));
            return true;
        }
        return false;
    }

    private static void nextAct(String next, Vec3 from, float yaw, BlockPos station, Vec3 local, Direction face) {
        if (ONLY && !next.equals(FROM)) {
            step(2, () -> finish(Minecraft.getInstance()));
            return;
        }
        step(2, () -> {
            act = next;
            server(p -> p.teleportTo(p.serverLevel(), from.x, from.y, from.z, yaw, 20f));
        });
        step(30, () -> shot(next + "0_before"));
        step(4, () -> use(station, local, face));
    }





    // ---- shift-click from the drawer into every station -------------------------------------------------------

    /** One shift-click: optional preparation on the server, the inventory slot clicked, what must be true after. */
    private record QuickCase(String name, Consumer<ServerPlayer> before, int slot, java.util.function.Predicate<ServerPlayer> expect, int settle) {
        QuickCase(String name, int slot, java.util.function.Predicate<ServerPlayer> expect) {
            this(name, p -> {}, slot, expect, 0);
        }
    }

    private static ItemStack inv(ServerPlayer p, int slot) {
        return p.getInventory().getItem(slot);
    }

    private static boolean holds(ItemStack stack, net.minecraft.world.item.Item item, int count) {
        return count == 0 ? stack.isEmpty() : stack.is(item) && stack.getCount() == count;
    }

    /** The station's own slots in the open menu (not the player's), for menu stations. */
    private static List<ItemStack> menuItems(ServerPlayer p) {
        List<ItemStack> items = new ArrayList<>();
        for (var slot : p.containerMenu.slots) if (slot.container != p.getInventory()) items.add(slot.getItem());
        return items;
    }

    private static String describeAll(List<ItemStack> items) {
        StringBuilder b = new StringBuilder("[");
        for (ItemStack stack : items) b.append(describe(stack)).append(' ');
        return b.append(']').toString();
    }

    /**
     * Sets the inventory up, enters the station's scene ({@code enter}), runs the cases one after another and steps away.
     * {@code state} tells what the station holds, for the report.
     */
    private static void quickStation(String name, Consumer<ServerPlayer> setup, Runnable enter, int settle,
                                     java.util.function.Function<ServerPlayer, String> state, List<QuickCase> cases) {
        step(4, () -> server(p -> {
            for (int i = 0; i < 36; i++) p.getInventory().setItem(i, ItemStack.EMPTY);
            p.getInventory().selected = 8;
            setup.accept(p);
            p.inventoryMenu.broadcastChanges();
        }));
        step(4, () -> {
            Minecraft.getInstance().player.getInventory().selected = 8;
            Minecraft.getInstance().getConnection().send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(8));
        });
        enter.run();
        step(settle, () -> check(name + ": the scene is at work", Scene.phase == Scene.Phase.ACTIVE && Minecraft.getInstance().screen instanceof SceneScreen,
                "phase=" + Scene.phase + " station=" + (Scene.station == null ? "-" : Scene.station.kind())));
        for (QuickCase c : cases) {
            step(4, () -> server(c.before()));
            step(4, () -> {
                if (Minecraft.getInstance().screen instanceof SceneScreen screen) {
                    // the drawer numbers the hotbar 36..44 and the pack 9..35, as the inventory menu does
                    screen.panelClick(c.slot() < 9 ? 36 + c.slot() : c.slot(), 0, true);
                }
            });
            step(Math.max(8, c.settle()), () -> server(p -> check(name + ": " + c.name(), c.expect().test(p),
                    "slot " + c.slot() + "=" + describe(inv(p, c.slot())) + " station=" + state.apply(p))));
        }
        step(2, () -> shot("sq_" + name.replace(' ', '_')));
        step(2, () -> { if (Scene.phase == Scene.Phase.ACTIVE) Scene.leave(); });
        step(50, () -> server(p -> check(name + ": no session after leaving", dev.dyslexic.cinematic.station.Sessions.get(p) == null, "session")));
    }

    //? if fabric && >=1.21 {
    /*// ---- the inventory opened while stepping away ---------------------------------------------------------

    /^*
     * The player steps away (E, as from the work) and presses E again almost at once: the inventory opens while the scene
     * is still stepping away. Then the same station is used again: its scene must play again, not its own window, and a
     * plain way in and out without the inventory must still work. Several pauses, on menu stations and on the table.
     ^/
    /^*
     * A station used in the air: at every stage of a jump (rising, at the top, falling, the landing tick), standing and
     * running at it. The use must end in the scene or in the block's own window, never in a scene that plays and lets go.
     ^/
    private static void jumpUseAct() {
        step(2, () -> act = "jp");
        record Case(String name, BlockPos pos, Vec3 feet, Vec3 local, Class<?> vanillaScreen) {}
        List<Case> stations = List.of(
                new Case("table", TABLE, new Vec3(TABLE.getX() + 0.5, -60, TABLE.getZ() + 3.4), new Vec3(0.5, 1.0, 0.7),
                        net.minecraft.client.gui.screens.inventory.CraftingScreen.class),
                new Case("smithing", SMITHING, new Vec3(SMITHING.getX() + 0.5, -60, SMITHING.getZ() + 3.4), new Vec3(0.5, 1.0, 0.5),
                        net.minecraft.client.gui.screens.inventory.SmithingScreen.class),
                new Case("anvil", ANVIL, new Vec3(ANVIL.getX() + 0.5, -60, ANVIL.getZ() + 3.4), new Vec3(0.5, 1.0, 0.5),
                        net.minecraft.client.gui.screens.inventory.AnvilScreen.class));
        // ticks from the jump to the use: rising (1-4), the top (5-6), falling (7-10), landing (11-12), just landed (13)
        int[] delays = {1, 3, 5, 7, 9, 10, 11, 12, 13};
        for (Case c : stations) {
            for (boolean running : new boolean[]{false, true}) {
                for (int delay : delays) {
                    String label = c.name() + (running ? " running" : "") + " jump +" + delay;
                    step(2, () -> standLooking(running ? c.feet().add(0, 0, 1.2) : c.feet(), Vec3.atCenterOf(c.pos())));
                    step(20, () -> {
                        LocalPlayer player = Minecraft.getInstance().player;
                        player.jumpFromGround();
                        if (running) player.setDeltaMovement(player.getDeltaMovement().add(0, 0, -0.28));
                    });
                    step(delay, () -> {
                        LocalPlayer player = Minecraft.getInstance().player;
                        REPORT.add(label + ": used with onGround=" + player.onGround() + " vy=" + String.format("%.3f", player.getDeltaMovement().y)
                                + " fall=" + String.format("%.2f", player.fallDistance));
                        useAt(c.pos(), c.local(), Direction.UP);
                    });
                    step(110, () -> {
                        Minecraft mc = Minecraft.getInstance();
                        boolean scene = Scene.phase == Scene.Phase.ACTIVE && Scene.station != null && Scene.station.pos.equals(c.pos());
                        boolean window = c.vanillaScreen().isInstance(mc.screen);
                        REPORT.add(label + ": phase=" + Scene.phase + " screen=" + screenName() + " why=" + Scene.refused);
                        check(label + ": the scene or the block's window, not an empty scene", scene || window,
                                "phase=" + Scene.phase + " screen=" + screenName());
                        if (scene) server(p -> check(label + ": the server has the session", dev.dyslexic.cinematic.station.Sessions.get(p) != null
                                && dev.dyslexic.cinematic.station.Sessions.get(p).pos().equals(c.pos()), "session"));
                        if (delay == 7 && !running) shot("jp_" + c.name() + "_" + delay);
                        if (scene) Scene.leave();
                        else if (mc.player.containerMenu != mc.player.inventoryMenu) mc.player.closeContainer();
                        else mc.setScreen(null);
                    });
                    step(40, () -> server(p -> check(label + ": nothing left open", dev.dyslexic.cinematic.station.Sessions.get(p) == null
                            && p.containerMenu == p.inventoryMenu, "session")));
                }
            }
        }
        // kept in the air longer than the use waits for the ground (levitation): the block's own window instead
        step(2, () -> standLooking(new Vec3(SMITHING.getX() + 0.5, -60, SMITHING.getZ() + 3.4), Vec3.atCenterOf(SMITHING)));
        step(20, () -> server(p -> p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.LEVITATION, 60, 0))));
        step(8, () -> useAt(SMITHING, new Vec3(0.5, 1.0, 0.5), Direction.UP));
        step(45, () -> {
            check("smithing in the air too long: the block's own window", Minecraft.getInstance().screen
                    instanceof net.minecraft.client.gui.screens.inventory.SmithingScreen && Scene.phase == Scene.Phase.NONE, "phase=" + Scene.phase + " screen=" + screenName());
            shot("jp_levitating_window");
            Minecraft.getInstance().player.closeContainer();
        });
        step(80, () -> server(p -> p.removeAllEffects()));
        step(2, () -> finish(Minecraft.getInstance()));
    }

    private static int onTableCount(ServerPlayer p, net.minecraft.world.item.Item item) {
        TableStore.Table stored = TableStore.get(p.serverLevel()).get(TABLE);
        int n = 0;
        for (int i = 0; stored != null && i < TableGrid.CELLS; i++) if (stored.items.get(i).is(item)) n += stored.items.get(i).getCount();
        return n;
    }

    private static void selectRecipe(String id) {
        TableRecipeGuide guide = table().guide;
        guide.tick();
        guide.select(guide.entry(net.minecraft.resources.ResourceLocation.withDefaultNamespace(id)));
        guide.tick();
    }

    /^*
     * 2.1: another recipe laid out over what the last one left (by hand or from the book), with Shift and with no room
     * in the inventory; 2.2: what lies on the table goes into the crafting window whenever that opens instead of the scene.
     ^/
    private static void tableKeepsAct() {
        Vec3 feet = new Vec3(0.5, -60, 3.36);
        step(4, () -> {
            act = "tb";
            closeWindow();
            server(Showcase::wipeTable);
            testInventory(new ItemStack(Items.LAPIS_LAZULI, 9));
            standLooking(feet, Vec3.atCenterOf(TABLE));
        });
        step(30, () -> useAt(TABLE, new Vec3(0.5, 1.0, 0.8), Direction.UP));
        step(80, () -> check("table: at the table", Scene.station instanceof TableStation && Scene.phase == Scene.Phase.ACTIVE, "phase=" + Scene.phase));
        // 1. the last recipe left an iron ingot where the lapis block wants lapis
        step(4, () -> server(p -> putOnTable(p, 1, 1, new ItemStack(Items.IRON_INGOT))));
        step(10, () -> {
            selectRecipe("lapis_block");
            TableRecipeGuide guide = table().guide;
            check("2.1 lapis over an iron ingot: something to lay out", guide.layableAfterTakingBack() == 9 && guide.takeBackMoves().size() == 1,
                    "after=" + guide.layableAfterTakingBack() + " back=" + guide.takeBackMoves().size());
            table().guide.layOut(false);
        });
        step(8, () -> shot("tb1_taking_back"));
        step(70, () -> {
            server(p -> check("2.1 lapis: the ingot back in the inventory, nine lapis on the table", onTableCount(p, Items.LAPIS_LAZULI) == 9
                    && onTableCount(p, Items.IRON_INGOT) == 0 && heldEverywhere(p, Items.IRON_INGOT) == 1 && heldEverywhere(p, Items.LAPIS_LAZULI) == 9,
                    "lapis on table=" + onTableCount(p, Items.LAPIS_LAZULI) + " iron=" + heldEverywhere(p, Items.IRON_INGOT)));
            check("2.1 lapis: the table shows the block", table().result.is(Items.LAPIS_BLOCK), "result=" + describe(table().result));
            pointAside();
            shot("tb2_lapis_laid");
        });
        // 2. a crafting table laid by hand, then a chest: the planks in place stay, the middle one comes back and goes out again
        step(4, () -> {
            testInventory(new ItemStack(Items.OAK_PLANKS, 4));
            server(p -> {
                wipeTable(p);
                for (int[] at : new int[][]{{0, 0}, {0, 1}, {1, 0}, {1, 1}}) putOnTable(p, at[0], at[1], new ItemStack(Items.OAK_PLANKS));
            });
        });
        step(10, () -> {
            selectRecipe("chest");
            table().guide.layOut(false);
        });
        step(100, () -> {
            server(p -> {
                boolean ring = true;
                for (int i = 0; i < 9; i++) ring &= i == 4 ? onTable(p, 1, 1).isEmpty() : onTable(p, i / 3, i % 3).is(Items.OAK_PLANKS);
                check("2.1 chest after a crafting table: eight planks round the middle, none lost", ring
                        && heldEverywhere(p, Items.OAK_PLANKS) == 8 && onTableCount(p, Items.OAK_PLANKS) == 8,
                        "planks=" + heldEverywhere(p, Items.OAK_PLANKS) + " on table=" + onTableCount(p, Items.OAK_PLANKS));
            });
            check("2.1 chest: the table shows the chest", table().result.is(Items.CHEST), "result=" + describe(table().result));
            shot("tb3_chest");
        });
        // 3. Shift: every set of sticks, a cobblestone in the way
        step(4, () -> {
            testInventory(new ItemStack(Items.OAK_PLANKS, 20));
            server(p -> {
                wipeTable(p);
                putOnTable(p, 0, 0, new ItemStack(Items.COBBLESTONE));
            });
        });
        step(10, () -> {
            selectRecipe("stick");
            table().guide.layOut(true);
        });
        step(100, () -> {
            server(p -> check("2.1 Shift sticks: the cobblestone back, all twenty planks laid in two cells", onTableCount(p, Items.COBBLESTONE) == 0
                    && heldEverywhere(p, Items.COBBLESTONE) == 1 && onTableCount(p, Items.OAK_PLANKS) == 20 && heldEverywhere(p, Items.OAK_PLANKS) == 20,
                    "planks on table=" + onTableCount(p, Items.OAK_PLANKS) + " cobble=" + heldEverywhere(p, Items.COBBLESTONE)));
            check("2.1 Shift sticks: the table shows sticks", table().result.is(Items.STICK), "result=" + describe(table().result));
        });
        // 4. no room to take the ingot back: it stays where it is, nothing is lost, the rest is laid out
        step(4, () -> {
            ItemStack[] full = new ItemStack[36];
            for (int i = 0; i < 35; i++) full[i] = new ItemStack(Items.DIRT, 64);
            full[35] = new ItemStack(Items.LAPIS_LAZULI, 9);
            testInventory(full);
            server(p -> {
                wipeTable(p);
                putOnTable(p, 1, 1, new ItemStack(Items.IRON_INGOT));
            });
        });
        step(10, () -> {
            selectRecipe("lapis_block");
            check("2.1 no room: the ingot cannot go back", table().guide.stuck() == 1 && table().guide.takeBackMoves().isEmpty(),
                    "stuck=" + table().guide.stuck());
            table().guide.layOut(false);
        });
        step(90, () -> server(p -> check("2.1 no room: the ingot stays on the table, the lapis round it, nothing lost",
                onTableCount(p, Items.IRON_INGOT) == 1 && onTableCount(p, Items.LAPIS_LAZULI) == 8 && heldEverywhere(p, Items.LAPIS_LAZULI) == 9,
                "iron=" + onTableCount(p, Items.IRON_INGOT) + " lapis=" + onTableCount(p, Items.LAPIS_LAZULI))));
        // 2.2: eight planks left on the table, then the scene turned off: the crafting window has them, the table gives them up
        step(4, () -> {
            testInventory();
            server(p -> {
                wipeTable(p);
                for (int[] at : new int[][]{{0, 0}, {0, 1}, {1, 0}, {1, 1}}) putOnTable(p, at[0], at[1], new ItemStack(Items.OAK_PLANKS, 2));
            });
        });
        step(10, Scene::leave);
        step(40, () -> {
            check("2.2 left the table with the planks on it", Scene.phase == Scene.Phase.NONE, "phase=" + Scene.phase);
            server(p -> check("2.2 the planks stay on the table after leaving (as before)", onTableCount(p, Items.OAK_PLANKS) == 8,
                    "on table=" + onTableCount(p, Items.OAK_PLANKS)));
            Settings.SCENES.get(dev.dyslexic.cinematic.station.StationKind.TABLE).set(false);
            useAt(TABLE, new Vec3(0.5, 1.0, 0.8), Direction.UP);
        });
        step(20, () -> {
            check("2.2 scene off: the crafting window opens", Minecraft.getInstance().screen instanceof net.minecraft.client.gui.screens.inventory.CraftingScreen,
                    "screen=" + screenName());
            server(p -> {
                boolean inGrid = p.containerMenu instanceof net.minecraft.world.inventory.CraftingMenu menu
                        && menu.getSlot(1).getItem().is(Items.OAK_PLANKS) && menu.getSlot(2).getItem().is(Items.OAK_PLANKS)
                        && menu.getSlot(4).getItem().is(Items.OAK_PLANKS) && menu.getSlot(5).getItem().is(Items.OAK_PLANKS)
                        && menu.getSlot(0).getItem().is(Items.CRAFTING_TABLE);
                check("2.2 the planks are in the window's grid as on the table, the result a crafting table", inGrid, "menu=" + p.containerMenu
                        + " result=" + describe(p.containerMenu.getSlot(0).getItem()));
                check("2.2 the table top is empty while the window has them", onTableCount(p, Items.OAK_PLANKS) == 0,
                        "on table=" + onTableCount(p, Items.OAK_PLANKS));
            });
            shot("tb4_window");
        });
        step(4, () -> Minecraft.getInstance().player.closeContainer());
        step(20, () -> server(p -> check("2.2 closing the window: all eight planks in the inventory, no copies", heldEverywhere(p, Items.OAK_PLANKS) == 8
                && onTableCount(p, Items.OAK_PLANKS) == 0, "planks=" + heldEverywhere(p, Items.OAK_PLANKS))));
        // any other way the window opens (a sneaking use, another mod): the same
        step(4, () -> server(p -> {
            TableStore store = TableStore.get(p.serverLevel());
            store.getOrCreate(TABLE).items.set(southCell(0, 0), new ItemStack(Items.STICK, 3));
            store.setDirty();
            p.openMenu(p.serverLevel().getBlockState(TABLE).getMenuProvider(p.serverLevel(), TABLE));
        }));
        step(20, () -> server(p -> {
            check("2.2 a window opened any other way takes the sticks", p.containerMenu instanceof net.minecraft.world.inventory.CraftingMenu menu
                    && menu.getSlot(1).getItem().is(Items.STICK) && menu.getSlot(1).getItem().getCount() == 3 && onTableCount(p, Items.STICK) == 0,
                    "menu=" + p.containerMenu);
            p.closeContainer();
        }));
        step(20, () -> {
            server(p -> check("2.2 the sticks back in the inventory", heldEverywhere(p, Items.STICK) == 3 && onTableCount(p, Items.STICK) == 0,
                    "sticks=" + heldEverywhere(p, Items.STICK)));
            Settings.SCENES.get(dev.dyslexic.cinematic.station.StationKind.TABLE).set(true);
            restoreInventory();
        });
        step(2, () -> finish(Minecraft.getInstance()));
    }

    private static int southCell(int row, int col) {
        return TableGrid.worldIndex(Direction.SOUTH, row, col);
    }

    /^* A data pack of recipes that compete with vanilla ones, for the Polymorph act (written into the test world). ^/
    private static void conflictPack(ServerPlayer p) {
        try {
            java.nio.file.Path dir = p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.DATAPACK_DIR).resolve("cinematic_conflicts");
            java.nio.file.Path recipes = dir.resolve("data/cinematic_test/recipe");
            java.nio.file.Files.createDirectories(recipes);
            java.nio.file.Files.writeString(dir.resolve("pack.mcmeta"), "{\"pack\":{\"pack_format\":48,\"description\":\"Cinematic test conflicts\"}}");
            java.nio.file.Files.writeString(recipes.resolve("planks_barrel.json"), "{\"type\":\"minecraft:crafting_shaped\",\"pattern\":[\"##\",\"##\"],"
                    + "\"key\":{\"#\":{\"tag\":\"minecraft:planks\"}},\"result\":{\"id\":\"minecraft:barrel\",\"count\":1}}");
            java.nio.file.Files.writeString(recipes.resolve("raw_iron_nugget.json"), "{\"type\":\"minecraft:smelting\",\"ingredient\":{\"item\":\"minecraft:raw_iron\"},"
                    + "\"result\":{\"id\":\"minecraft:gold_nugget\"},\"experience\":0.1,\"cookingtime\":40}");
            java.nio.file.Files.writeString(recipes.resolve("sword_gold.json"), "{\"type\":\"minecraft:smithing_transform\","
                    + "\"template\":{\"item\":\"minecraft:netherite_upgrade_smithing_template\"},\"base\":{\"item\":\"minecraft:diamond_sword\"},"
                    + "\"addition\":{\"item\":\"minecraft:netherite_ingot\"},\"result\":{\"id\":\"minecraft:golden_sword\",\"count\":1}}");
            p.server.getPackRepository().reload();
            List<String> selected = new ArrayList<>(p.server.getPackRepository().getSelectedIds());
            if (!selected.contains("file/cinematic_conflicts")) selected.add("file/cinematic_conflicts");
            p.server.reloadResources(selected);
        } catch (java.io.IOException e) {
            check("polymorph: the conflict pack is written", false, e.toString());
        }
    }

    /^*
     * Polymorph: recipes competing for the table's cells, a furnace's input and the smithing table; the scene shows the
     * results, the player picks one, the server makes exactly that one. Without Polymorph: no strip, vanilla's first.
     ^/
    private static void polymorphAct() {
        boolean polymorph = dev.dyslexic.cinematic.station.RecipeChoices.available();
        step(4, () -> {
            act = "pm";
            closeWindow();
            server(Showcase::conflictPack);
            REPORT.add("polymorph loaded=" + polymorph);
        });
        step(80, () -> check("polymorph: the conflicting recipes reached the client", Minecraft.getInstance().level.getRecipeManager()
                .byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("cinematic_test", "planks_barrel")).isPresent(), ""));
        // the table: four planks spell a crafting table and (the pack) a barrel
        step(4, () -> {
            server(Showcase::wipeTable);
            testInventory(new ItemStack(Items.OAK_PLANKS, 8));
            standLooking(new Vec3(0.5, -60, 3.36), Vec3.atCenterOf(TABLE));
        });
        step(30, () -> useAt(TABLE, new Vec3(0.5, 1.0, 0.8), Direction.UP));
        step(80, () -> server(p -> {
            for (int[] at : new int[][]{{0, 0}, {0, 1}, {1, 0}, {1, 1}}) putOnTable(p, at[0], at[1], new ItemStack(Items.OAK_PLANKS, 2));
        }));
        step(20, () -> {
            RecipeChoiceStrip strip = table().choices();
            if (!polymorph) {
                check("no polymorph: no choice strip at the table", !strip.active(), "active=" + strip.active());
                return;
            }
            check("polymorph table: two results offered, the first in use", strip.active() && strip.selected() == 0
                    && table().result.is(Items.BARREL), "selected=" + strip.selected() + " result=" + describe(table().result));
            pointAside();
            shot("pm1_table_choices");
        });
        step(4, () -> table().craft(false));
        step(20, () -> server(p -> {
            ItemStack made = p.inventoryMenu.getCarried();
            check(polymorph ? "polymorph table: the server made the barrel shown" : "no polymorph: the table still crafts",
                    polymorph ? made.is(Items.BARREL) : !made.isEmpty(), "made=" + describe(made));
            p.getInventory().placeItemBackInInventory(made.copy());
            p.inventoryMenu.setCarried(ItemStack.EMPTY);
            p.inventoryMenu.broadcastChanges();
        }));
        if (polymorph) {
            step(4, () -> table().chose(1));
            step(10, () -> {
                check("polymorph table: picking the second shows the crafting table", table().choices().selected() == 1
                        && table().result.is(Items.CRAFTING_TABLE), "result=" + describe(table().result));
                shot("pm2_table_second");
                table().craft(false);
            });
            step(20, () -> server(p -> check("polymorph table: the server made the crafting table picked", p.inventoryMenu.getCarried().is(Items.CRAFTING_TABLE),
                    "made=" + describe(p.inventoryMenu.getCarried()))));
            step(4, () -> server(p -> check("polymorph table: Polymorph keeps the player's choice", net.minecraft.resources.ResourceLocation
                    .withDefaultNamespace("crafting_table").equals(dev.dyslexic.cinematic.station.RecipeChoices.chosen(p)), "chosen=" + dev.dyslexic.cinematic.station.RecipeChoices.chosen(p))));
        }
        step(4, Scene::leave);
        step(40, () -> {});
        // the furnace: raw iron makes an ingot and (the pack) a gold nugget
        step(4, () -> {
            testInventory();
            // no fuel yet: the choice is made before anything cooks
            server(p -> setFurnace(p, FURNACE, new ItemStack(Items.RAW_IRON, 4), ItemStack.EMPTY));
        });
        enterAt(new Vec3(-3.2, -60, 4.6), 200f, FURNACE, new Vec3(0.5, 0.6, 1), Direction.SOUTH).run();
        step(90, () -> {
            check("furnace: at the furnace", Scene.station instanceof FurnaceStation && Scene.phase == Scene.Phase.ACTIVE, "phase=" + Scene.phase);
            RecipeChoiceStrip strip = Scene.station instanceof FurnaceStation f ? f.choices() : null;
            check(polymorph ? "polymorph furnace: two results offered for raw iron" : "no polymorph: no choice strip at the furnace",
                    strip != null && strip.active() == polymorph, "active=" + (strip != null && strip.active()));
            if (polymorph) Scene.station.chose(0);
        });
        if (polymorph) {
            step(10, () -> {
                check("polymorph furnace: the server reports the nugget chosen", Scene.station.choices().selected() == 0, "selected=" + Scene.station.choices().selected());
                pointAside();
                shot("pm3_furnace_choices");
                server(p -> {
                    if (p.serverLevel().getBlockEntity(FURNACE) instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity f) f.setItem(1, new ItemStack(Items.COAL, 4));
                });
            });
            step(60, () -> server(p -> check("polymorph furnace: the furnace made a gold nugget", furnaceItems(p, FURNACE).get(2).is(Items.GOLD_NUGGET),
                    "out=" + describe(furnaceItems(p, FURNACE).get(2)))));
            step(4, () -> server(p -> {
                if (p.serverLevel().getBlockEntity(FURNACE) instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity f) f.setItem(2, ItemStack.EMPTY);
            }));
            step(4, () -> Scene.station.chose(1));
            step(260, () -> server(p -> check("polymorph furnace: after picking the ingot the furnace makes an iron ingot",
                    furnaceItems(p, FURNACE).get(2).is(Items.IRON_INGOT), "out=" + describe(furnaceItems(p, FURNACE).get(2)))));
        }
        step(4, Scene::leave);
        step(40, () -> server(p -> setFurnace(p, FURNACE, ItemStack.EMPTY, ItemStack.EMPTY)));
        // the smithing table: the upgrade template makes a netherite sword and (the pack) a golden one
        step(4, () -> testInventory());
        enterSmithy(SMITHING).run();
        step(90, () -> server(p -> {
            check("smithing: the menu is open", p.containerMenu instanceof net.minecraft.world.inventory.SmithingMenu, "menu=" + p.containerMenu);
            if (p.containerMenu instanceof net.minecraft.world.inventory.SmithingMenu menu) {
                menu.getSlot(0).set(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
                menu.getSlot(1).set(new ItemStack(Items.DIAMOND_SWORD));
                menu.getSlot(2).set(new ItemStack(Items.NETHERITE_INGOT));
                menu.broadcastChanges();
            }
        }));
        step(20, () -> {
            RecipeChoiceStrip strip = Scene.station.choices();
            check(polymorph ? "polymorph smithing: two results offered" : "no polymorph: no choice strip at the smithing table",
                    strip != null && strip.active() == polymorph, "active=" + (strip != null && strip.active()) + " selected=" + (strip == null ? -9 : strip.selected()));
            if (polymorph) Scene.station.chose(0);
        });
        if (polymorph) {
            step(10, () -> {
                server(p -> check("polymorph smithing: the server's result is the golden sword picked", p.containerMenu.getSlot(3).getItem().is(Items.GOLDEN_SWORD),
                        "result=" + describe(p.containerMenu.getSlot(3).getItem())));
                check("polymorph smithing: the strip shows the golden sword in use", Scene.station.choices().selected() == 0, "selected=" + Scene.station.choices().selected());
                pointAside();
                shot("pm4_smithing_choices");
            });
            step(4, () -> Scene.station.chose(1));
            step(10, () -> server(p -> check("polymorph smithing: picking again gives the netherite sword", p.containerMenu.getSlot(3).getItem().is(Items.NETHERITE_SWORD),
                    "result=" + describe(p.containerMenu.getSlot(3).getItem()))));
            step(4, () -> server(p -> p.containerMenu.clicked(3, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE, p)));
            step(10, () -> server(p -> {
                int swords = 0, diamond = 0;
                for (int i = 0; i < 36; i++) {
                    if (p.getInventory().getItem(i).is(Items.NETHERITE_SWORD)) swords++;
                    if (p.getInventory().getItem(i).is(Items.DIAMOND_SWORD)) diamond++;
                }
                check("polymorph smithing: taking the result gives one netherite sword, the inputs are used", swords == 1 && diamond == 0
                        && p.containerMenu.getSlot(1).getItem().isEmpty(), "swords=" + swords + " base=" + describe(p.containerMenu.getSlot(1).getItem()));
            }));
        }
        step(4, Scene::leave);
        step(40, Showcase::restoreInventory);
        step(2, () -> finish(Minecraft.getInstance()));
    }

    /^* A stand-in player, its skin chosen by the UUID (the default skins come wide and slim), with all or none of the skin layers. ^/
    private static net.minecraft.client.player.RemotePlayer standIn(int id, byte seed, boolean layers) {
        Minecraft mc = Minecraft.getInstance();
        var stand = new net.minecraft.client.player.RemotePlayer(mc.level, new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes(new byte[]{seed}), "Stand" + seed));
        stand.setId(id);
        stand.getEntityData().set(net.minecraft.world.entity.player.Player.DATA_PLAYER_MODE_CUSTOMISATION, (byte) (layers ? 0x7F : 0));
        mc.level.addEntity(stand);
        return stand;
    }

    private static void placeStandIn(net.minecraft.client.player.RemotePlayer stand, Vec3 feet, float headYaw, float bodyYaw, StationKind kind, BlockPos pos) {
        stand.moveTo(feet.x, feet.y, feet.z, headYaw, 30f);
        stand.setYHeadRot(headYaw);
        stand.yHeadRotO = headYaw;
        stand.yBodyRot = bodyYaw;
        stand.yBodyRotO = bodyYaw;
        ClientPayloads.AT_STATION.put(stand.getId(), new ClientPayloads.AtStation(kind, pos));
    }

    private static float yawTo(Vec3 from, BlockPos to) {
        return (float) Math.toDegrees(Math.atan2(to.getZ() + 0.5 - from.z, to.getX() + 0.5 - from.x)) - 90f;
    }

    /^* The shapes of the stonecutter's dial that lie inside a block next to it (what a wall would hide). ^/
    private static int shapesInBlocks(StonecutterStation cutter) {
        var level = Minecraft.getInstance().level;
        int inside = 0;
        for (int k = 0; k < cutter.shapes().size(); k++) {
            Vec3 at = cutter.shapePoint(k);
            BlockPos block = BlockPos.containing(at);
            if (block.equals(cutter.pos)) continue;
            for (net.minecraft.world.phys.AABB box : level.getBlockState(block).getShape(level, block).toAabbs()) {
                if (box.move(block).contains(at)) inside++;
            }
        }
        return inside;
    }

    /^*
     * 1.1 the key on the campfire's hint, 1.2 the arms of someone at a station seen by others, 4.1/4.2 the knees of
     * someone sitting at a campfire, 3.1 the stonecutter's shapes between two blocks. Each with a before shot (the
     * fix switched off where it can be) and an after shot.
     ^/
    private static void lookAtFixesAct() {
        step(4, () -> {
            act = "vx";
            closeWindow();
        });
        // 1.1: the bound key by its Latin letter (a Russian layout would name the H key "Р")
        step(4, () -> {
            var key = net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.getBoundKeyOf(CampfireHint.SIT);
            REPORT.add("1.1 the layout here names the H key " + org.lwjgl.glfw.GLFW.glfwGetKeyName(org.lwjgl.glfw.GLFW.GLFW_KEY_H, 0)
                    + ", vanilla shows " + CampfireHint.SIT.getTranslatedKeyMessage().getString());
            check("1.1 the hint names the H key H", KeyLabels.of(CampfireHint.SIT).getString().equals("H"), KeyLabels.of(CampfireHint.SIT).getString());
            CampfireHint.SIT.setKey(com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_J));
            net.minecraft.client.KeyMapping.resetMapping();
            check("1.1 rebound to J, the hint says J", KeyLabels.of(CampfireHint.SIT).getString().equals("J"), KeyLabels.of(CampfireHint.SIT).getString());
            CampfireHint.SIT.setKey(com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_ALT));
            net.minecraft.client.KeyMapping.resetMapping();
            check("1.1 rebound to a key without a letter, its own name", KeyLabels.of(CampfireHint.SIT).getString()
                    .equals(CampfireHint.SIT.getTranslatedKeyMessage().getString()), KeyLabels.of(CampfireHint.SIT).getString());
            CampfireHint.SIT.setKey(key);
            net.minecraft.client.KeyMapping.resetMapping();
            standLooking(new Vec3(CAMPFIRE.getX() + 0.5, -60, CAMPFIRE.getZ() + 2.6), new Vec3(CAMPFIRE.getX() + 0.5, -59.7, CAMPFIRE.getZ() + 0.5));
        });
        step(30, () -> shot("vx1_campfire_hint"));
        // 1.2: three people at three stations, each having looked away before stepping up (the body 70 degrees off)
        record At(StationKind kind, BlockPos pos, Vec3 feet, float off) {}
        List<At> stations = List.of(
                new At(StationKind.TABLE, TABLE, new Vec3(TABLE.getX() + 0.5, -60, TABLE.getZ() + 1.3), 70f),
                new At(StationKind.ANVIL, ANVIL, new Vec3(ANVIL.getX() + 0.5, -60, ANVIL.getZ() + 1.36), -70f),
                new At(StationKind.LOOM, LOOM, new Vec3(LOOM.getX() + 1.3, -60, LOOM.getZ() + 0.5), 65f));
        for (boolean fixed : new boolean[]{false, true}) {
            for (At at : stations) {
                String name = "vx2_" + at.kind().name().toLowerCase() + (fixed ? "_after" : "_before");
                step(4, () -> {
                    ClientPayloads.facesStations = fixed;
                    if (smith == null || smith.isRemoved()) smith = standIn(SMITH_ID, (byte) 7, true);
                    float head = yawTo(at.feet(), at.pos());
                    placeStandIn(smith, at.feet(), head, head + at.off(), at.kind(), at.pos());
                    Vec3 side = at.feet().subtract(Vec3.atBottomCenterOf(at.pos())).normalize();
                    Vec3 across = new Vec3(-side.z, 0, side.x);
                    standLooking(at.feet().add(across.scale(2.4)).add(side.scale(1.2)), at.feet().add(0, 1.1, 0).add(side.scale(-0.4)));
                });
                step(30, () -> {
                    float body = net.minecraft.util.Mth.wrapDegrees(smith.yBodyRot - yawTo(smith.position(), at.pos()));
                    if (fixed) check("1.2 " + at.kind() + ": the body turns to the station whatever the player looked at", Math.abs(body) < 8,
                            "body off by " + body);
                    else REPORT.add("1.2 " + at.kind() + " before: the body stays off by " + body);
                    shot(name);
                });
            }
        }
        step(2, () -> ClientPayloads.facesStations = true);
        // 4.2: sitting at the campfire and the soul campfire, seen by others: a wide skin with every layer, a slim one with none
        for (boolean bent : new boolean[]{false, true}) {
            for (BlockPos fire : List.of(CAMPFIRE, SOUL_CAMPFIRE)) {
                String name = "vx3_" + (fire == CAMPFIRE ? "campfire" : "soul") + (bent ? "_after" : "_before");
                step(4, () -> {
                    KneeLayer.bends = bent;
                    if (smith == null || smith.isRemoved()) smith = standIn(SMITH_ID, (byte) 7, true);
                    net.minecraft.client.player.RemotePlayer second = Minecraft.getInstance().level.getEntity(SMITH_ID + 1)
                            instanceof net.minecraft.client.player.RemotePlayer p ? p : standIn(SMITH_ID + 1, (byte) 9, false);
                    Vec3 a = new Vec3(fire.getX() + 0.5, -60, fire.getZ() + 2.1), b = new Vec3(fire.getX() + 2.1, -60, fire.getZ() + 0.5);
                    placeStandIn(smith, a, yawTo(a, fire), yawTo(a, fire), StationKind.CAMPFIRE, fire);
                    placeStandIn(second, b, yawTo(b, fire), yawTo(b, fire), StationKind.CAMPFIRE, fire);
                    standLooking(new Vec3(fire.getX() + 3.4, -60, fire.getZ() + 3.6), new Vec3(fire.getX() + 1.3, -59.6, fire.getZ() + 1.3));
                });
                step(30, () -> {
                    if (bent) check("4.2 " + name + ": the raised leg is drawn in two, the straight one hidden", KneeLayer.sitting(smith), "");
                    shot(name);
                });
                // side on, at the sitters' height (raised a block on stone): the raised knee in profile
                step(4, () -> {
                    BlockPos a = new BlockPos(fire.getX(), -60, fire.getZ() + 2), b = new BlockPos(fire.getX() + 2, -60, fire.getZ());
                    server(p -> {
                        p.serverLevel().setBlockAndUpdate(a, Blocks.STONE.defaultBlockState());
                        p.serverLevel().setBlockAndUpdate(b, Blocks.STONE.defaultBlockState());
                    });
                    Vec3 seatA = new Vec3(fire.getX() + 0.5, -59, fire.getZ() + 2.5), seatB = new Vec3(fire.getX() + 2.5, -59, fire.getZ() + 0.5);
                    placeStandIn(smith, seatA, yawTo(seatA, fire), yawTo(seatA, fire), StationKind.CAMPFIRE, fire);
                    var second = (net.minecraft.client.player.RemotePlayer) Minecraft.getInstance().level.getEntity(SMITH_ID + 1);
                    placeStandIn(second, seatB, yawTo(seatB, fire), yawTo(seatB, fire), StationKind.CAMPFIRE, fire);
                    standLooking(new Vec3(fire.getX() + 3.6, -60, fire.getZ() + 2.6), new Vec3(fire.getX() + 0.5, -58.6, fire.getZ() + 2.5));
                });
                step(20, () -> shot(name + "_side7"));
                step(4, () -> standLooking(new Vec3(fire.getX() + 2.6, -60, fire.getZ() - 2.6), new Vec3(fire.getX() + 2.5, -58.6, fire.getZ() + 0.5)));
                step(20, () -> {
                    shot(name + "_side9");
                    server(p -> {
                        p.serverLevel().setBlockAndUpdate(new BlockPos(fire.getX(), -60, fire.getZ() + 2), Blocks.AIR.defaultBlockState());
                        p.serverLevel().setBlockAndUpdate(new BlockPos(fire.getX() + 2, -60, fire.getZ()), Blocks.AIR.defaultBlockState());
                    });
                });
            }
        }
        // Armor on the raised knee: both pieces must follow the split leg, including the outer boot shell.
        step(4, () -> {
            KneeLayer.bends = true;
            Vec3 seat = new Vec3(CAMPFIRE.getX() + 0.5, -59, CAMPFIRE.getZ() + 2.5);
            server(p -> p.serverLevel().setBlockAndUpdate(new BlockPos(CAMPFIRE.getX(), -60, CAMPFIRE.getZ() + 2), Blocks.STONE.defaultBlockState()));
            placeStandIn(smith, seat, yawTo(seat, CAMPFIRE), yawTo(seat, CAMPFIRE), StationKind.CAMPFIRE, CAMPFIRE);
            smith.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new net.minecraft.world.item.ItemStack(Items.IRON_LEGGINGS));
            smith.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new net.minecraft.world.item.ItemStack(Items.IRON_BOOTS));
            standLooking(new Vec3(CAMPFIRE.getX() + 3.6, -60, CAMPFIRE.getZ() + 2.6),
                    new Vec3(CAMPFIRE.getX() + 0.5, -58.6, CAMPFIRE.getZ() + 2.5));
        });
        step(24, () -> {
            check("4.2 armor: leggings and boots on the bent campfire leg", KneeLayer.sitting(smith)
                    && smith.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS).is(Items.IRON_LEGGINGS)
                    && smith.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).is(Items.IRON_BOOTS), "");
            shot("vx3_campfire_armor_side");
            server(p -> p.serverLevel().setBlockAndUpdate(new BlockPos(CAMPFIRE.getX(), -60, CAMPFIRE.getZ() + 2), Blocks.AIR.defaultBlockState()));
        });
        step(4, () -> {
            for (int id : new int[]{SMITH_ID, SMITH_ID + 1}) {
                ClientPayloads.AT_STATION.remove(id);
                if (Minecraft.getInstance().level.getEntity(id) != null) Minecraft.getInstance().level.removeEntity(id, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            }
        });
        // 4.1: sitting at the campfire oneself, the knees from the eyes (and from behind, looking round)
        step(4, () -> standLooking(new Vec3(CAMPFIRE.getX() + 0.5, -60, CAMPFIRE.getZ() + 2.6), new Vec3(CAMPFIRE.getX() + 0.5, -59.7, CAMPFIRE.getZ() + 0.5)));
        step(30, () -> {
            REPORT.add("4.1 before H: onGround=" + Minecraft.getInstance().player.onGround() + " screen=" + screenName() + " hint target=" + CampfireHint.target());
            net.minecraft.client.KeyMapping.click(net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.getBoundKeyOf(CampfireHint.SIT));
        });
        step(90, () -> {
            check("4.1 sitting at the campfire", Scene.station instanceof CampfireStation && Scene.phase == Scene.Phase.ACTIVE, "phase=" + Scene.phase
                    + " refused=" + Scene.refused);
            pointAside();
            shot("vx4_knees");
        });
        step(4, () -> server(p -> {
            p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new net.minecraft.world.item.ItemStack(Items.IRON_LEGGINGS));
            p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new net.minecraft.world.item.ItemStack(Items.IRON_BOOTS));
        }));
        step(20, () -> shot("vx4_armored_knees"));
        step(4, () -> server(p -> {
            p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.item.ItemStack.EMPTY);
            p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, net.minecraft.world.item.ItemStack.EMPTY);
        }));
        step(4, Scene::leave);
        step(40, () -> {});
        // 3.1: the stonecutter between two stone blocks, then turned to face east with the blocks beside it again
        for (Direction facing : new Direction[]{Direction.SOUTH, Direction.EAST}) {
            Direction across = facing.getClockWise();
            Vec3 feet = Vec3.atBottomCenterOf(STONECUTTER).add(Vec3.atLowerCornerOf(facing.getNormal()).scale(2.9));
            step(4, () -> server(p -> {
                var level = p.serverLevel();
                level.setBlockAndUpdate(STONECUTTER.relative(Direction.NORTH), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(STONECUTTER.relative(Direction.SOUTH), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(STONECUTTER.relative(Direction.EAST), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(STONECUTTER.relative(Direction.WEST), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(STONECUTTER, Blocks.STONECUTTER.defaultBlockState().setValue(StonecutterBlock.FACING, facing));
                level.setBlockAndUpdate(STONECUTTER.relative(across), Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(STONECUTTER.relative(across.getOpposite()), Blocks.STONE.defaultBlockState());
                p.teleportTo(level, feet.x, -60, feet.z, facing.getOpposite().toYRot(), 30f);
            }));
            step(20, () -> useAt(STONECUTTER, new Vec3(0.5, 0.56, 0.5), Direction.UP));
            step(90, () -> server(p -> {
                check("3.1 " + facing + ": at the stonecutter", p.containerMenu instanceof net.minecraft.world.inventory.StonecutterMenu, "menu=" + p.containerMenu);
                if (p.containerMenu instanceof net.minecraft.world.inventory.StonecutterMenu menu) {
                    menu.getSlot(0).set(new ItemStack(Items.STONE, 16));
                    menu.slotsChanged(menu.getSlot(0).container);
                    menu.broadcastChanges();
                }
            }));
            step(20, () -> {
                StonecutterStation.fitsDial = false;
            });
            step(30, () -> {
                StonecutterStation cutter = (StonecutterStation) Scene.station;
                REPORT.add("3.1 " + facing + " before: " + shapesInBlocks(cutter) + " of " + cutter.shapes().size() + " shapes inside the blocks beside");
                pointAside();
                shot("vx5_cutter_" + facing.getName() + "_before");
                StonecutterStation.fitsDial = true;
            });
            step(30, () -> {
                StonecutterStation cutter = (StonecutterStation) Scene.station;
                check("3.1 " + facing + ": no shape inside the blocks beside, the dial made room", shapesInBlocks(cutter) == 0 && cutter.dialFit() > 0,
                        "inside=" + shapesInBlocks(cutter) + " fit=" + cutter.dialFit() + " shapes=" + cutter.shapes().size());
                pointAside();
                shot("vx5_cutter_" + facing.getName() + "_after");
            });
            // the first and the last shape on the dial, picked by the cursor
            for (boolean last : new boolean[]{false, true}) {
                // a pick starts the cut (about a second): the next pick waits for it
                // the piece cut first is in the hand, and another shape would not stack with it: into the inventory first
                if (last) step(46, () -> server(p -> {
                    p.getInventory().placeItemBackInInventory(p.containerMenu.getCarried().copy());
                    p.containerMenu.setCarried(ItemStack.EMPTY);
                    p.containerMenu.broadcastChanges();
                }));
                step(last ? 8 : 4, () -> {
                    StonecutterStation cutter = (StonecutterStation) Scene.station;
                    int k = last ? Math.min(cutter.shapes().size(), StonecutterStation.DIAL_SLOTS) - 1 : 0;
                    double[] at = Picking.toGui(cutter.shapePoint(k));
                    pointGui(at[0], at[1]);
                });
                step(6, () -> screenClick(0));
                step(10, () -> server(p -> check("3.1 " + facing + ": the " + (last ? "last" : "first") + " shape on the dial can be picked",
                        p.containerMenu instanceof net.minecraft.world.inventory.StonecutterMenu menu && menu.getSelectedRecipeIndex() >= 0
                                && menu.getSelectedRecipeIndex() == (last ? Math.min(menu.getNumRecipes(), StonecutterStation.DIAL_SLOTS) - 1 : 0), "selected=" + (p.containerMenu instanceof
                                net.minecraft.world.inventory.StonecutterMenu menu ? menu.getSelectedRecipeIndex() : -9))));
            }
            step(4, Scene::leave);
            step(40, () -> server(p -> {
                p.getInventory().clearOrCountMatchingItems(s -> s.is(Items.STONE) || s.is(Items.STONE_BRICKS) || s.is(Items.STONE_SLAB), -1, p.inventoryMenu.getCraftSlots());
                var level = p.serverLevel();
                level.setBlockAndUpdate(STONECUTTER.relative(across), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(STONECUTTER.relative(across.getOpposite()), Blocks.AIR.defaultBlockState());
            }));
        }
        step(4, () -> server(p -> p.serverLevel().setBlockAndUpdate(STONECUTTER, Blocks.STONECUTTER.defaultBlockState().setValue(StonecutterBlock.FACING, Direction.SOUTH))));
        step(2, () -> finish(Minecraft.getInstance()));
    }

    private static int occlusionFlips;
    private static boolean[] lastInTheWay;
    private static double railJump;
    private static double[][] lastRails;

    /^*
     * 2.3 the table's faded ingredients while the cursor wanders inside one cell, 4.4 the campfire's rails while the hand
     * reaches over the food, 6.2 a hand and blocks in front of the beacon's icons. Numbers here; the frames are compared
     * act by act (0, f, 6, n) against the last build.
     ^/
    private static void visualFixesAct() {
        step(4, () -> {
            act = "vy";
            closeWindow();
            server(Showcase::wipeTable);
            standLooking(new Vec3(0.5, -60, 3.36), Vec3.atCenterOf(TABLE));
        });
        step(30, () -> useAt(TABLE, new Vec3(0.5, 1.0, 0.8), Direction.UP));
        step(80, () -> server(p -> {
            ItemStack[] items = {new ItemStack(Items.CHEST), new ItemStack(Items.RAW_IRON, 40), new ItemStack(Items.FURNACE),
                    new ItemStack(Items.RAW_IRON, 20), new ItemStack(Items.STONE), new ItemStack(Items.RAW_IRON, 40),
                    new ItemStack(Items.CHEST), new ItemStack(Items.RAW_IRON, 40), new ItemStack(Items.FURNACE)};
            for (int i = 0; i < 9; i++) putOnTable(p, i / 3, i % 3, items[i]);
        }));
        // the far middle cell, under the cursor: the cursor wanders inside it for a second and a half
        step(20, () -> {
            occlusionFlips = 0;
            lastInTheWay = null;
        });
        for (int k = 0; k < 30; k++) {
            int n = k;
            step(1, () -> {
                double[] at = Picking.toGui(table().cellPoint(tableWorld(1)));
                pointGui(at[0] + 3 * Math.sin(n * 0.9), at[1] + 2 * Math.cos(n * 1.3));
                boolean[] now = table().inTheWayCopy();
                if (lastInTheWay != null && n > 4) for (int i = 0; i < 9; i++) if (now[i] != lastInTheWay[i]) occlusionFlips++;
                lastInTheWay = now;
                if (n == 20) shot("vy1_table_hover");
            });
        }
        step(2, () -> {
            boolean any = false;
            for (boolean b : lastInTheWay) any |= b;
            check("2.3 the items in front of the hovered cell fade, and stay so while the cursor wanders in it", any && occlusionFlips == 0,
                    "flips=" + occlusionFlips + " faded=" + java.util.Arrays.toString(lastInTheWay));
            check("2.3 the hovered cell's own item stays solid", !lastInTheWay[tableWorld(1)], "");
        });
        step(4, () -> {
            Scene.leave();
            server(Showcase::wipeTable);
        });
        step(40, () -> {});
        // the campfire: food on all four spots, the hand reaching from the near ones to the far ones
        step(4, () -> standLooking(new Vec3(CAMPFIRE.getX() + 0.5, -60, CAMPFIRE.getZ() + 2.6), new Vec3(CAMPFIRE.getX() + 0.5, -59.7, CAMPFIRE.getZ() + 0.5)));
        step(30, () -> net.minecraft.client.KeyMapping.click(net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.getBoundKeyOf(CampfireHint.SIT)));
        step(90, () -> server(p -> {
            if (p.serverLevel().getBlockEntity(CAMPFIRE) instanceof net.minecraft.world.level.block.entity.CampfireBlockEntity fire) {
                for (int i = 0; i < 4; i++) {
                    fire.getItems().set(i, new ItemStack(Items.BEEF));
                    fire.cookingTime[i] = 600;
                    fire.cookingProgress[i] = 100 * i;
                }
                fire.setChanged();
                p.serverLevel().sendBlockUpdated(CAMPFIRE, p.serverLevel().getBlockState(CAMPFIRE), p.serverLevel().getBlockState(CAMPFIRE), 3);
            }
        }));
        step(30, () -> {
            railJump = 0;
            lastRails = null;
        });
        int[] order = {0, 1, 2, 3, 2, 1, 0};
        for (int k = 0; k < order.length * 12; k++) {
            int n = k;
            step(1, () -> {
                if (!(Scene.station instanceof CampfireStation fire)) return;
                if (n % 12 == 0) pointAt(fire.spot(order[n / 12]));
                double[][] now = new double[4][];
                for (int i = 0; i < 4; i++) now[i] = fire.railShown[i] == null ? null : fire.railShown[i].clone();
                // where each rail is drawn against the point of its food this frame: any lift or snap shows as a gap
                for (int i = 0; i < 4; i++) {
                    if (now[i] != null) railJump = Math.max(railJump, Math.hypot(now[i][0] - now[i][2], now[i][1] - now[i][3]));
                }
                lastRails = now;
                if (n == 3 * 12 + 8) shot("vy2_campfire_far");
                if (n == 1 * 12 + 8) shot("vy2_campfire_near");
            });
        }
        step(2, () -> check("4.4 the rails stay on their food while hovered and while the hand reaches round", railJump < 0.01,
                "largest gap=" + String.format("%.2f", railJump)));
        step(4, Scene::leave);
        step(40, () -> server(p -> {
            if (p.serverLevel().getBlockEntity(CAMPFIRE) instanceof net.minecraft.world.level.block.entity.CampfireBlockEntity fire) {
                for (int i = 0; i < 4; i++) fire.getItems().set(i, ItemStack.EMPTY);
                fire.setChanged();
            }
        }));
        // the beacon: a chest and a bell behind its powers, then the hand reaching to the first one
        step(4, () -> server(p -> {
            p.serverLevel().setBlockAndUpdate(BEACON.offset(0, 1, -2), Blocks.CHEST.defaultBlockState());
            p.serverLevel().setBlockAndUpdate(BEACON.offset(-1, 1, -2), Blocks.BELL.defaultBlockState());
            p.serverLevel().setBlockAndUpdate(BEACON.offset(1, 1, -2), Blocks.SHULKER_BOX.defaultBlockState());
            p.teleportTo(p.serverLevel(), BEACON.getX() + 0.5, BEACON.getY(), BEACON.getZ() + 1.75, 180f, 20f);
        }));
        step(20, () -> useAt(BEACON, new Vec3(0.5, 0.9, 0.5), Direction.UP));
        step(90, () -> {
            check("6.2 at the beacon", beacon() != null && Scene.phase == Scene.Phase.ACTIVE, "phase=" + Scene.phase);
            pointAside();
            shot("vy3_beacon_behind");
        });
        // reaching up to the highest power, the arm passes in front of the lower ones
        step(2, () -> {
            railJump = 1;
            pointAt(beacon().powerPos(4));
        });
        for (int k = 0; k < 14; k++) {
            int n = k;
            step(1, () -> {
                railJump = Math.min(railJump, Math.min(beacon().iconSeen(0), beacon().iconSeen(2)));
                if (n == 5) shot("vy3_beacon_hand");
            });
        }
        step(2, () -> check("6.2 the arm reaching past the lower powers hides their icons as it passes", railJump < 0.9,
                "least seen=" + String.format("%.2f", railJump)));
        step(4, Scene::leave);
        step(40, () -> server(p -> {
            for (int dx = -1; dx <= 1; dx++) p.serverLevel().setBlockAndUpdate(BEACON.offset(dx, 1, -2), Blocks.AIR.defaultBlockState());
        }));
        step(2, () -> finish(Minecraft.getInstance()));
    }

    private static void reopenAfterInventoryAct() {
        step(2, () -> act = "ri");
        record Case(String name, BlockPos pos, Class<?> vanillaScreen) {}
        List<Case> stations = List.of(
                new Case("smithing", SMITHING, net.minecraft.client.gui.screens.inventory.SmithingScreen.class),
                new Case("loom", LOOM, net.minecraft.client.gui.screens.inventory.LoomScreen.class),
                new Case("anvil", ANVIL, net.minecraft.client.gui.screens.inventory.AnvilScreen.class),
                new Case("stonecutter", STONECUTTER, net.minecraft.client.gui.screens.inventory.StonecutterScreen.class));
        // ticks between leaving and pressing E: once the work's screen has gone (0.22 s), early and late in the step away
        int[] pauses = {6, 10, 16};
        for (Case c : stations) {
            // a plain way in and out first, no inventory
            enterSmithy(c.pos()).run();
            step(80, () -> check(c.name() + ": scene plays", Scene.phase == Scene.Phase.ACTIVE && Scene.station != null
                    && Scene.station.pos.equals(c.pos()), "phase=" + Scene.phase + " screen=" + screenName()));
            step(2, () -> pressInScene(GLFW.GLFW_KEY_E));
            step(40, () -> check(c.name() + ": plain step away ends", Scene.phase == Scene.Phase.NONE && Minecraft.getInstance().screen == null,
                    "phase=" + Scene.phase + " screen=" + screenName()));
            for (int pause : pauses) {
                String label = c.name() + " E after " + pause + " ticks";
                enterSmithy(c.pos()).run();
                step(80, () -> check(label + ": scene plays", Scene.phase == Scene.Phase.ACTIVE && Scene.station != null
                        && Scene.station.pos.equals(c.pos()), "phase=" + Scene.phase + " screen=" + screenName()));
                step(2, () -> pressInScene(GLFW.GLFW_KEY_E));
                // the game's own key: the inventory opens on the next tick there is no screen
                step(pause, () -> net.minecraft.client.KeyMapping.click(
                        net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.getBoundKeyOf(Minecraft.getInstance().options.keyInventory)));
                step(6, () -> {
                    REPORT.add(label + ": after E phase=" + Scene.phase + " screen=" + screenName());
                    check(label + ": the inventory opens", Minecraft.getInstance().screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen,
                            "phase=" + Scene.phase + " screen=" + screenName());
                });
                step(30, () -> {
                    check(label + ": the step away is over", Scene.phase == Scene.Phase.NONE, "phase=" + Scene.phase);
                    shot("ri_" + c.name() + "_" + pause + "_inventory");
                    Minecraft.getInstance().setScreen(null);
                });
                step(10, () -> server(p -> check(label + ": no session left", dev.dyslexic.cinematic.station.Sessions.get(p) == null, "session")));
                enterSmithy(c.pos()).run();
                step(80, () -> {
                    check(label + ": the scene plays again, not the window", Scene.phase == Scene.Phase.ACTIVE && Scene.station != null
                            && Scene.station.pos.equals(c.pos()) && !c.vanillaScreen().isInstance(Minecraft.getInstance().screen),
                            "phase=" + Scene.phase + " screen=" + screenName());
                    server(p -> check(label + ": the server has the new session", dev.dyslexic.cinematic.station.Sessions.get(p) != null
                            && dev.dyslexic.cinematic.station.Sessions.get(p).pos().equals(c.pos()), "session"));
                    shot("ri_" + c.name() + "_" + pause + "_again");
                });
                step(2, () -> {
                    if (Scene.phase == Scene.Phase.ACTIVE) pressInScene(GLFW.GLFW_KEY_E);
                    else Minecraft.getInstance().setScreen(null);
                });
                step(40, () -> {});
            }
        }
        // another menu while working (a server's chest, a command): the scene gives way, the station is not marked
        enterSmithy(SMITHING).run();
        step(80, () -> check("unrelated menu: scene plays", Scene.phase == Scene.Phase.ACTIVE, "phase=" + Scene.phase));
        step(2, () -> server(p -> p.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (id, inventory, who) -> net.minecraft.world.inventory.ChestMenu.threeRows(id, inventory), Component.literal("Chest")))));
        step(20, () -> check("unrelated menu: its window opens, the scene ends", Minecraft.getInstance().screen
                instanceof net.minecraft.client.gui.screens.inventory.ContainerScreen && Scene.phase == Scene.Phase.NONE,
                "phase=" + Scene.phase + " screen=" + screenName()));
        step(2, () -> Minecraft.getInstance().player.closeContainer());
        enterSmithy(SMITHING).run();
        step(80, () -> check("unrelated menu: the station still plays its scene", Scene.phase == Scene.Phase.ACTIVE
                && Scene.station != null && Scene.station.pos.equals(SMITHING), "phase=" + Scene.phase + " screen=" + screenName()));
        step(2, () -> pressInScene(GLFW.GLFW_KEY_E));
        step(40, () -> {});
        // a mod's own menu in answer to this very attempt (as Penchant's enchanting table on NeoForge): the block keeps
        // its own window from then on. The answer is made here, as MenuScreens makes one, right after BEGIN goes out.
        step(2, () -> server(p -> p.teleportTo(p.serverLevel(), ENCHANT.getX() + 0.5, -60, ENCHANT.getZ() + 3.6, 180f, 20f)));
        step(20, () -> {
            useAt(ENCHANT, new Vec3(0.5, 0.75, 0.6), Direction.UP);
            Minecraft mc = Minecraft.getInstance();
            var foreign = net.minecraft.world.inventory.ChestMenu.threeRows(99, mc.player.getInventory());
            mc.player.containerMenu = foreign;
            mc.setScreen(new net.minecraft.client.gui.screens.inventory.ContainerScreen(foreign, mc.player.getInventory(), Component.literal("Foreign")));
            check("foreign menu of this attempt: its window, not the scene", Scene.phase == Scene.Phase.NONE
                    && mc.screen instanceof net.minecraft.client.gui.screens.inventory.ContainerScreen, "phase=" + Scene.phase + " screen=" + screenName());
        });
        step(20, () -> Minecraft.getInstance().player.closeContainer());
        step(10, () -> useAt(ENCHANT, new Vec3(0.5, 0.75, 0.6), Direction.UP));
        step(20, () -> check("foreign menu: the block keeps its own window", Scene.phase == Scene.Phase.NONE && Minecraft.getInstance().screen
                instanceof net.minecraft.client.gui.screens.inventory.EnchantmentScreen, "phase=" + Scene.phase + " screen=" + screenName()));
        step(4, () -> {
            Minecraft.getInstance().player.closeContainer();
            Scene.forgetOwnWindows();
        });
        step(10, () -> useAt(ENCHANT, new Vec3(0.5, 0.75, 0.6), Direction.UP));
        step(80, () -> check("foreign menu: on a new connection the scene plays again", Scene.phase == Scene.Phase.ACTIVE,
                "phase=" + Scene.phase + " screen=" + screenName()));
        step(2, () -> pressInScene(GLFW.GLFW_KEY_E));
        step(40, () -> {});
        step(2, () -> finish(Minecraft.getInstance()));
    }

    private static String screenName() {
        Screen screen = Minecraft.getInstance().screen;
        return screen == null ? "none" : screen.getClass().getSimpleName();
    }

    /^* A key pressed over the work, as the player presses it (E and Esc step away). ^/
    private static void pressInScene(int key) {
        if (Minecraft.getInstance().screen instanceof SceneScreen screen) {
            int scan = GLFW.glfwGetKeyScancode(key);
            screen.keyPressed(key, scan, 0);
            screen.keyReleased(key, scan, 0);
        }
    }

    *///?} else if fabric && <1.21 {
    /*// ---- the inventory opened while stepping away ---------------------------------------------------------

    /^*
     * The player steps away (E, as from the work) and presses E again almost at once: the inventory opens while the scene
     * is still stepping away. Then the same station is used again: its scene must play again, not its own window, and a
     * plain way in and out without the inventory must still work. Several pauses, on menu stations and on the table.
     ^/
    /^*
     * A station used in the air: at every stage of a jump (rising, at the top, falling, the landing tick), standing and
     * running at it. The use must end in the scene or in the block's own window, never in a scene that plays and lets go.
     ^/
    private static void jumpUseAct() {
        step(2, () -> act = "jp");
        record Case(String name, BlockPos pos, Vec3 feet, Vec3 local, Class<?> vanillaScreen) {}
        List<Case> stations = List.of(
                new Case("table", TABLE, new Vec3(TABLE.getX() + 0.5, -60, TABLE.getZ() + 3.4), new Vec3(0.5, 1.0, 0.7),
                        net.minecraft.client.gui.screens.inventory.CraftingScreen.class),
                new Case("smithing", SMITHING, new Vec3(SMITHING.getX() + 0.5, -60, SMITHING.getZ() + 3.4), new Vec3(0.5, 1.0, 0.5),
                        net.minecraft.client.gui.screens.inventory.SmithingScreen.class),
                new Case("anvil", ANVIL, new Vec3(ANVIL.getX() + 0.5, -60, ANVIL.getZ() + 3.4), new Vec3(0.5, 1.0, 0.5),
                        net.minecraft.client.gui.screens.inventory.AnvilScreen.class));
        // ticks from the jump to the use: rising (1-4), the top (5-6), falling (7-10), landing (11-12), just landed (13)
        int[] delays = {1, 3, 5, 7, 9, 10, 11, 12, 13};
        for (Case c : stations) {
            for (boolean running : new boolean[]{false, true}) {
                for (int delay : delays) {
                    String label = c.name() + (running ? " running" : "") + " jump +" + delay;
                    step(2, () -> standLooking(running ? c.feet().add(0, 0, 1.2) : c.feet(), Vec3.atCenterOf(c.pos())));
                    step(20, () -> {
                        LocalPlayer player = Minecraft.getInstance().player;
                        player.jumpFromGround();
                        if (running) player.setDeltaMovement(player.getDeltaMovement().add(0, 0, -0.28));
                    });
                    step(delay, () -> {
                        LocalPlayer player = Minecraft.getInstance().player;
                        REPORT.add(label + ": used with onGround=" + player.onGround() + " vy=" + String.format("%.3f", player.getDeltaMovement().y)
                                + " fall=" + String.format("%.2f", player.fallDistance));
                        useAt(c.pos(), c.local(), Direction.UP);
                    });
                    step(110, () -> {
                        Minecraft mc = Minecraft.getInstance();
                        boolean scene = Scene.phase == Scene.Phase.ACTIVE && Scene.station != null && Scene.station.pos.equals(c.pos());
                        boolean window = c.vanillaScreen().isInstance(mc.screen);
                        REPORT.add(label + ": phase=" + Scene.phase + " screen=" + screenName() + " why=" + Scene.refused);
                        check(label + ": the scene or the block's window, not an empty scene", scene || window,
                                "phase=" + Scene.phase + " screen=" + screenName());
                        if (scene) server(p -> check(label + ": the server has the session", dev.dyslexic.cinematic.station.Sessions.get(p) != null
                                && dev.dyslexic.cinematic.station.Sessions.get(p).pos().equals(c.pos()), "session"));
                        if (delay == 7 && !running) shot("jp_" + c.name() + "_" + delay);
                        if (scene) Scene.leave();
                        else if (mc.player.containerMenu != mc.player.inventoryMenu) mc.player.closeContainer();
                        else mc.setScreen(null);
                    });
                    step(40, () -> server(p -> check(label + ": nothing left open", dev.dyslexic.cinematic.station.Sessions.get(p) == null
                            && p.containerMenu == p.inventoryMenu, "session")));
                }
            }
        }
        // kept in the air longer than the use waits for the ground (levitation): the block's own window instead
        step(2, () -> standLooking(new Vec3(SMITHING.getX() + 0.5, -60, SMITHING.getZ() + 3.4), Vec3.atCenterOf(SMITHING)));
        step(20, () -> server(p -> p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.LEVITATION, 60, 0))));
        step(8, () -> useAt(SMITHING, new Vec3(0.5, 1.0, 0.5), Direction.UP));
        step(45, () -> {
            check("smithing in the air too long: the block's own window", Minecraft.getInstance().screen
                    instanceof net.minecraft.client.gui.screens.inventory.SmithingScreen && Scene.phase == Scene.Phase.NONE, "phase=" + Scene.phase + " screen=" + screenName());
            shot("jp_levitating_window");
            Minecraft.getInstance().player.closeContainer();
        });
        step(80, () -> server(p -> p.removeAllEffects()));
        step(2, () -> finish(Minecraft.getInstance()));
    }

    private static int onTableCount(ServerPlayer p, net.minecraft.world.item.Item item) {
        TableStore.Table stored = TableStore.get(p.serverLevel()).get(TABLE);
        int n = 0;
        for (int i = 0; stored != null && i < TableGrid.CELLS; i++) if (stored.items.get(i).is(item)) n += stored.items.get(i).getCount();
        return n;
    }

    private static void selectRecipe(String id) {
        TableRecipeGuide guide = table().guide;
        guide.tick();
        guide.select(guide.entry(new net.minecraft.resources.ResourceLocation(id)));
        guide.tick();
    }

    /^*
     * 2.1: another recipe laid out over what the last one left (by hand or from the book), with Shift and with no room
     * in the inventory; 2.2: what lies on the table goes into the crafting window whenever that opens instead of the scene.
     ^/
    private static void tableKeepsAct() {
        Vec3 feet = new Vec3(0.5, -60, 3.36);
        step(4, () -> {
            act = "tb";
            closeWindow();
            server(Showcase::wipeTable);
            testInventory(new ItemStack(Items.LAPIS_LAZULI, 9));
            standLooking(feet, Vec3.atCenterOf(TABLE));
        });
        step(30, () -> useAt(TABLE, new Vec3(0.5, 1.0, 0.8), Direction.UP));
        step(80, () -> check("table: at the table", Scene.station instanceof TableStation && Scene.phase == Scene.Phase.ACTIVE, "phase=" + Scene.phase));
        // 1. the last recipe left an iron ingot where the lapis block wants lapis
        step(4, () -> server(p -> putOnTable(p, 1, 1, new ItemStack(Items.IRON_INGOT))));
        step(10, () -> {
            selectRecipe("lapis_block");
            TableRecipeGuide guide = table().guide;
            check("2.1 lapis over an iron ingot: something to lay out", guide.layableAfterTakingBack() == 9 && guide.takeBackMoves().size() == 1,
                    "after=" + guide.layableAfterTakingBack() + " back=" + guide.takeBackMoves().size());
            table().guide.layOut(false);
        });
        step(8, () -> shot("tb1_taking_back"));
        step(70, () -> {
            server(p -> check("2.1 lapis: the ingot back in the inventory, nine lapis on the table", onTableCount(p, Items.LAPIS_LAZULI) == 9
                    && onTableCount(p, Items.IRON_INGOT) == 0 && heldEverywhere(p, Items.IRON_INGOT) == 1 && heldEverywhere(p, Items.LAPIS_LAZULI) == 9,
                    "lapis on table=" + onTableCount(p, Items.LAPIS_LAZULI) + " iron=" + heldEverywhere(p, Items.IRON_INGOT)));
            check("2.1 lapis: the table shows the block", table().result.is(Items.LAPIS_BLOCK), "result=" + describe(table().result));
            pointAside();
            shot("tb2_lapis_laid");
        });
        // 2. a crafting table laid by hand, then a chest: the planks in place stay, the middle one comes back and goes out again
        step(4, () -> {
            testInventory(new ItemStack(Items.OAK_PLANKS, 4));
            server(p -> {
                wipeTable(p);
                for (int[] at : new int[][]{{0, 0}, {0, 1}, {1, 0}, {1, 1}}) putOnTable(p, at[0], at[1], new ItemStack(Items.OAK_PLANKS));
            });
        });
        step(10, () -> {
            selectRecipe("chest");
            table().guide.layOut(false);
        });
        step(100, () -> {
            server(p -> {
                boolean ring = true;
                for (int i = 0; i < 9; i++) ring &= i == 4 ? onTable(p, 1, 1).isEmpty() : onTable(p, i / 3, i % 3).is(Items.OAK_PLANKS);
                check("2.1 chest after a crafting table: eight planks round the middle, none lost", ring
                        && heldEverywhere(p, Items.OAK_PLANKS) == 8 && onTableCount(p, Items.OAK_PLANKS) == 8,
                        "planks=" + heldEverywhere(p, Items.OAK_PLANKS) + " on table=" + onTableCount(p, Items.OAK_PLANKS));
            });
            check("2.1 chest: the table shows the chest", table().result.is(Items.CHEST), "result=" + describe(table().result));
            shot("tb3_chest");
        });
        // 3. Shift: every set of sticks, a cobblestone in the way
        step(4, () -> {
            testInventory(new ItemStack(Items.OAK_PLANKS, 20));
            server(p -> {
                wipeTable(p);
                putOnTable(p, 0, 0, new ItemStack(Items.COBBLESTONE));
            });
        });
        step(10, () -> {
            selectRecipe("stick");
            table().guide.layOut(true);
        });
        step(100, () -> {
            server(p -> check("2.1 Shift sticks: the cobblestone back, all twenty planks laid in two cells", onTableCount(p, Items.COBBLESTONE) == 0
                    && heldEverywhere(p, Items.COBBLESTONE) == 1 && onTableCount(p, Items.OAK_PLANKS) == 20 && heldEverywhere(p, Items.OAK_PLANKS) == 20,
                    "planks on table=" + onTableCount(p, Items.OAK_PLANKS) + " cobble=" + heldEverywhere(p, Items.COBBLESTONE)));
            check("2.1 Shift sticks: the table shows sticks", table().result.is(Items.STICK), "result=" + describe(table().result));
        });
        // 4. no room to take the ingot back: it stays where it is, nothing is lost, the rest is laid out
        step(4, () -> {
            ItemStack[] full = new ItemStack[36];
            for (int i = 0; i < 35; i++) full[i] = new ItemStack(Items.DIRT, 64);
            full[35] = new ItemStack(Items.LAPIS_LAZULI, 9);
            testInventory(full);
            server(p -> {
                wipeTable(p);
                putOnTable(p, 1, 1, new ItemStack(Items.IRON_INGOT));
            });
        });
        step(10, () -> {
            selectRecipe("lapis_block");
            check("2.1 no room: the ingot cannot go back", table().guide.stuck() == 1 && table().guide.takeBackMoves().isEmpty(),
                    "stuck=" + table().guide.stuck());
            table().guide.layOut(false);
        });
        step(90, () -> server(p -> check("2.1 no room: the ingot stays on the table, the lapis round it, nothing lost",
                onTableCount(p, Items.IRON_INGOT) == 1 && onTableCount(p, Items.LAPIS_LAZULI) == 8 && heldEverywhere(p, Items.LAPIS_LAZULI) == 9,
                "iron=" + onTableCount(p, Items.IRON_INGOT) + " lapis=" + onTableCount(p, Items.LAPIS_LAZULI))));
        // 2.2: eight planks left on the table, then the scene turned off: the crafting window has them, the table gives them up
        step(4, () -> {
            testInventory();
            server(p -> {
                wipeTable(p);
                for (int[] at : new int[][]{{0, 0}, {0, 1}, {1, 0}, {1, 1}}) putOnTable(p, at[0], at[1], new ItemStack(Items.OAK_PLANKS, 2));
            });
        });
        step(10, Scene::leave);
        step(40, () -> {
            check("2.2 left the table with the planks on it", Scene.phase == Scene.Phase.NONE, "phase=" + Scene.phase);
            server(p -> check("2.2 the planks stay on the table after leaving (as before)", onTableCount(p, Items.OAK_PLANKS) == 8,
                    "on table=" + onTableCount(p, Items.OAK_PLANKS)));
            Settings.SCENES.get(dev.dyslexic.cinematic.station.StationKind.TABLE).set(false);
            useAt(TABLE, new Vec3(0.5, 1.0, 0.8), Direction.UP);
        });
        step(20, () -> {
            check("2.2 scene off: the crafting window opens", Minecraft.getInstance().screen instanceof net.minecraft.client.gui.screens.inventory.CraftingScreen,
                    "screen=" + screenName());
            server(p -> {
                boolean inGrid = p.containerMenu instanceof net.minecraft.world.inventory.CraftingMenu menu
                        && menu.getSlot(1).getItem().is(Items.OAK_PLANKS) && menu.getSlot(2).getItem().is(Items.OAK_PLANKS)
                        && menu.getSlot(4).getItem().is(Items.OAK_PLANKS) && menu.getSlot(5).getItem().is(Items.OAK_PLANKS)
                        && menu.getSlot(0).getItem().is(Items.CRAFTING_TABLE);
                check("2.2 the planks are in the window's grid as on the table, the result a crafting table", inGrid, "menu=" + p.containerMenu
                        + " result=" + describe(p.containerMenu.getSlot(0).getItem()));
                check("2.2 the table top is empty while the window has them", onTableCount(p, Items.OAK_PLANKS) == 0,
                        "on table=" + onTableCount(p, Items.OAK_PLANKS));
            });
            shot("tb4_window");
        });
        step(4, () -> Minecraft.getInstance().player.closeContainer());
        step(20, () -> server(p -> check("2.2 closing the window: all eight planks in the inventory, no copies", heldEverywhere(p, Items.OAK_PLANKS) == 8
                && onTableCount(p, Items.OAK_PLANKS) == 0, "planks=" + heldEverywhere(p, Items.OAK_PLANKS))));
        // any other way the window opens (a sneaking use, another mod): the same
        step(4, () -> server(p -> {
            TableStore store = TableStore.get(p.serverLevel());
            store.getOrCreate(TABLE).items.set(southCell(0, 0), new ItemStack(Items.STICK, 3));
            store.setDirty();
            p.openMenu(p.serverLevel().getBlockState(TABLE).getMenuProvider(p.serverLevel(), TABLE));
        }));
        step(20, () -> server(p -> {
            check("2.2 a window opened any other way takes the sticks", p.containerMenu instanceof net.minecraft.world.inventory.CraftingMenu menu
                    && menu.getSlot(1).getItem().is(Items.STICK) && menu.getSlot(1).getItem().getCount() == 3 && onTableCount(p, Items.STICK) == 0,
                    "menu=" + p.containerMenu);
            p.closeContainer();
        }));
        step(20, () -> {
            server(p -> check("2.2 the sticks back in the inventory", heldEverywhere(p, Items.STICK) == 3 && onTableCount(p, Items.STICK) == 0,
                    "sticks=" + heldEverywhere(p, Items.STICK)));
            Settings.SCENES.get(dev.dyslexic.cinematic.station.StationKind.TABLE).set(true);
            restoreInventory();
        });
        step(2, () -> finish(Minecraft.getInstance()));
    }

    private static int southCell(int row, int col) {
        return TableGrid.worldIndex(Direction.SOUTH, row, col);
    }

    /^* A data pack of recipes that compete with vanilla ones, for the Polymorph act (written into the test world). ^/
    private static void conflictPack(ServerPlayer p) {
        try {
            java.nio.file.Path dir = p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.DATAPACK_DIR).resolve("cinematic_conflicts");
            java.nio.file.Path recipes = dir.resolve("data/cinematic_test/recipe");
            java.nio.file.Files.createDirectories(recipes);
            java.nio.file.Files.writeString(dir.resolve("pack.mcmeta"), "{\"pack\":{\"pack_format\":48,\"description\":\"Cinematic test conflicts\"}}");
            java.nio.file.Files.writeString(recipes.resolve("planks_barrel.json"), "{\"type\":\"minecraft:crafting_shaped\",\"pattern\":[\"##\",\"##\"],"
                    + "\"key\":{\"#\":{\"tag\":\"minecraft:planks\"}},\"result\":{\"id\":\"minecraft:barrel\",\"count\":1}}");
            java.nio.file.Files.writeString(recipes.resolve("raw_iron_nugget.json"), "{\"type\":\"minecraft:smelting\",\"ingredient\":{\"item\":\"minecraft:raw_iron\"},"
                    + "\"result\":{\"id\":\"minecraft:gold_nugget\"},\"experience\":0.1,\"cookingtime\":40}");
            java.nio.file.Files.writeString(recipes.resolve("sword_gold.json"), "{\"type\":\"minecraft:smithing_transform\","
                    + "\"template\":{\"item\":\"minecraft:netherite_upgrade_smithing_template\"},\"base\":{\"item\":\"minecraft:diamond_sword\"},"
                    + "\"addition\":{\"item\":\"minecraft:netherite_ingot\"},\"result\":{\"id\":\"minecraft:golden_sword\",\"count\":1}}");
            p.server.getPackRepository().reload();
            List<String> selected = new ArrayList<>(p.server.getPackRepository().getSelectedIds());
            if (!selected.contains("file/cinematic_conflicts")) selected.add("file/cinematic_conflicts");
            p.server.reloadResources(selected);
        } catch (java.io.IOException e) {
            check("polymorph: the conflict pack is written", false, e.toString());
        }
    }

    /^*
     * Polymorph: recipes competing for the table's cells, a furnace's input and the smithing table; the scene shows the
     * results, the player picks one, the server makes exactly that one. Without Polymorph: no strip, vanilla's first.
     ^/
    private static void polymorphAct() {
        boolean polymorph = dev.dyslexic.cinematic.station.RecipeChoices.available();
        step(4, () -> {
            act = "pm";
            closeWindow();
            server(Showcase::conflictPack);
            REPORT.add("polymorph loaded=" + polymorph);
        });
        step(80, () -> check("polymorph: the conflicting recipes reached the client", Minecraft.getInstance().level.getRecipeManager()
                .byKey(new net.minecraft.resources.ResourceLocation("cinematic_test", "planks_barrel")).isPresent(), ""));
        // the table: four planks spell a crafting table and (the pack) a barrel
        step(4, () -> {
            server(Showcase::wipeTable);
            testInventory(new ItemStack(Items.OAK_PLANKS, 8));
            standLooking(new Vec3(0.5, -60, 3.36), Vec3.atCenterOf(TABLE));
        });
        step(30, () -> useAt(TABLE, new Vec3(0.5, 1.0, 0.8), Direction.UP));
        step(80, () -> server(p -> {
            for (int[] at : new int[][]{{0, 0}, {0, 1}, {1, 0}, {1, 1}}) putOnTable(p, at[0], at[1], new ItemStack(Items.OAK_PLANKS, 2));
        }));
        step(20, () -> {
            RecipeChoiceStrip strip = table().choices();
            if (!polymorph) {
                check("no polymorph: no choice strip at the table", !strip.active(), "active=" + strip.active());
                return;
            }
            check("polymorph table: two results offered, the first in use", strip.active() && strip.selected() == 0
                    && table().result.is(Items.BARREL), "selected=" + strip.selected() + " result=" + describe(table().result));
            pointAside();
            shot("pm1_table_choices");
        });
        step(4, () -> table().craft(false));
        step(20, () -> server(p -> {
            ItemStack made = p.inventoryMenu.getCarried();
            check(polymorph ? "polymorph table: the server made the barrel shown" : "no polymorph: the table still crafts",
                    polymorph ? made.is(Items.BARREL) : !made.isEmpty(), "made=" + describe(made));
            p.getInventory().placeItemBackInInventory(made.copy());
            p.inventoryMenu.setCarried(ItemStack.EMPTY);
            p.inventoryMenu.broadcastChanges();
        }));
        if (polymorph) {
            step(4, () -> table().chose(1));
            step(10, () -> {
                check("polymorph table: picking the second shows the crafting table", table().choices().selected() == 1
                        && table().result.is(Items.CRAFTING_TABLE), "result=" + describe(table().result));
                shot("pm2_table_second");
                table().craft(false);
            });
            step(20, () -> server(p -> check("polymorph table: the server made the crafting table picked", p.inventoryMenu.getCarried().is(Items.CRAFTING_TABLE),
                    "made=" + describe(p.inventoryMenu.getCarried()))));
            step(4, () -> server(p -> check("polymorph table: Polymorph keeps the player's choice", new net.minecraft.resources.ResourceLocation("crafting_table").equals(dev.dyslexic.cinematic.station.RecipeChoices.chosen(p)), "chosen=" + dev.dyslexic.cinematic.station.RecipeChoices.chosen(p))));
        }
        step(4, Scene::leave);
        step(40, () -> {});
        // the furnace: raw iron makes an ingot and (the pack) a gold nugget
        step(4, () -> {
            testInventory();
            // no fuel yet: the choice is made before anything cooks
            server(p -> setFurnace(p, FURNACE, new ItemStack(Items.RAW_IRON, 4), ItemStack.EMPTY));
        });
        enterAt(new Vec3(-3.2, -60, 4.6), 200f, FURNACE, new Vec3(0.5, 0.6, 1), Direction.SOUTH).run();
        step(90, () -> {
            check("furnace: at the furnace", Scene.station instanceof FurnaceStation && Scene.phase == Scene.Phase.ACTIVE, "phase=" + Scene.phase);
            RecipeChoiceStrip strip = Scene.station instanceof FurnaceStation f ? f.choices() : null;
            check(polymorph ? "polymorph furnace: two results offered for raw iron" : "no polymorph: no choice strip at the furnace",
                    strip != null && strip.active() == polymorph, "active=" + (strip != null && strip.active()));
            if (polymorph) Scene.station.chose(0);
        });
        if (polymorph) {
            step(10, () -> {
                check("polymorph furnace: the server reports the nugget chosen", Scene.station.choices().selected() == 0, "selected=" + Scene.station.choices().selected());
                pointAside();
                shot("pm3_furnace_choices");
                server(p -> {
                    if (p.serverLevel().getBlockEntity(FURNACE) instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity f) f.setItem(1, new ItemStack(Items.COAL, 4));
                });
            });
            step(60, () -> server(p -> check("polymorph furnace: the furnace made a gold nugget", furnaceItems(p, FURNACE).get(2).is(Items.GOLD_NUGGET),
                    "out=" + describe(furnaceItems(p, FURNACE).get(2)))));
            step(4, () -> server(p -> {
                if (p.serverLevel().getBlockEntity(FURNACE) instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity f) f.setItem(2, ItemStack.EMPTY);
            }));
            step(4, () -> Scene.station.chose(1));
            step(260, () -> server(p -> check("polymorph furnace: after picking the ingot the furnace makes an iron ingot",
                    furnaceItems(p, FURNACE).get(2).is(Items.IRON_INGOT), "out=" + describe(furnaceItems(p, FURNACE).get(2)))));
        }
        step(4, Scene::leave);
        step(40, () -> server(p -> setFurnace(p, FURNACE, ItemStack.EMPTY, ItemStack.EMPTY)));
        // the smithing table: the upgrade template makes a netherite sword and (the pack) a golden one
        step(4, () -> testInventory());
        enterSmithy(SMITHING).run();
        step(90, () -> server(p -> {
            check("smithing: the menu is open", p.containerMenu instanceof net.minecraft.world.inventory.SmithingMenu, "menu=" + p.containerMenu);
            if (p.containerMenu instanceof net.minecraft.world.inventory.SmithingMenu menu) {
                menu.getSlot(0).set(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
                menu.getSlot(1).set(new ItemStack(Items.DIAMOND_SWORD));
                menu.getSlot(2).set(new ItemStack(Items.NETHERITE_INGOT));
                menu.broadcastChanges();
            }
        }));
        step(20, () -> {
            RecipeChoiceStrip strip = Scene.station.choices();
            check(polymorph ? "polymorph smithing: two results offered" : "no polymorph: no choice strip at the smithing table",
                    strip != null && strip.active() == polymorph, "active=" + (strip != null && strip.active()) + " selected=" + (strip == null ? -9 : strip.selected()));
            if (polymorph) Scene.station.chose(0);
        });
        if (polymorph) {
            step(10, () -> {
                server(p -> check("polymorph smithing: the server's result is the golden sword picked", p.containerMenu.getSlot(3).getItem().is(Items.GOLDEN_SWORD),
                        "result=" + describe(p.containerMenu.getSlot(3).getItem())));
                check("polymorph smithing: the strip shows the golden sword in use", Scene.station.choices().selected() == 0, "selected=" + Scene.station.choices().selected());
                pointAside();
                shot("pm4_smithing_choices");
            });
            step(4, () -> Scene.station.chose(1));
            step(10, () -> server(p -> check("polymorph smithing: picking again gives the netherite sword", p.containerMenu.getSlot(3).getItem().is(Items.NETHERITE_SWORD),
                    "result=" + describe(p.containerMenu.getSlot(3).getItem()))));
            step(4, () -> server(p -> p.containerMenu.clicked(3, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE, p)));
            step(10, () -> server(p -> {
                int swords = 0, diamond = 0;
                for (int i = 0; i < 36; i++) {
                    if (p.getInventory().getItem(i).is(Items.NETHERITE_SWORD)) swords++;
                    if (p.getInventory().getItem(i).is(Items.DIAMOND_SWORD)) diamond++;
                }
                check("polymorph smithing: taking the result gives one netherite sword, the inputs are used", swords == 1 && diamond == 0
                        && p.containerMenu.getSlot(1).getItem().isEmpty(), "swords=" + swords + " base=" + describe(p.containerMenu.getSlot(1).getItem()));
            }));
        }
        step(4, Scene::leave);
        step(40, Showcase::restoreInventory);
        step(2, () -> finish(Minecraft.getInstance()));
    }

    /^* A stand-in player, its skin chosen by the UUID (the default skins come wide and slim), with all or none of the skin layers. ^/
    private static net.minecraft.client.player.RemotePlayer standIn(int id, byte seed, boolean layers) {
        Minecraft mc = Minecraft.getInstance();
        var stand = new net.minecraft.client.player.RemotePlayer(mc.level, new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes(new byte[]{seed}), "Stand" + seed));
        stand.setId(id);
        stand.getEntityData().set(net.minecraft.world.entity.player.Player.DATA_PLAYER_MODE_CUSTOMISATION, (byte) (layers ? 0x7F : 0));
        mc.level.putNonPlayerEntity(stand.getId(), stand);
        return stand;
    }

    private static void placeStandIn(net.minecraft.client.player.RemotePlayer stand, Vec3 feet, float headYaw, float bodyYaw, StationKind kind, BlockPos pos) {
        stand.moveTo(feet.x, feet.y, feet.z, headYaw, 30f);
        stand.setYHeadRot(headYaw);
        stand.yHeadRotO = headYaw;
        stand.yBodyRot = bodyYaw;
        stand.yBodyRotO = bodyYaw;
        ClientPayloads.AT_STATION.put(stand.getId(), new ClientPayloads.AtStation(kind, pos));
    }

    private static float yawTo(Vec3 from, BlockPos to) {
        return (float) Math.toDegrees(Math.atan2(to.getZ() + 0.5 - from.z, to.getX() + 0.5 - from.x)) - 90f;
    }

    /^* The shapes of the stonecutter's dial that lie inside a block next to it (what a wall would hide). ^/
    private static int shapesInBlocks(StonecutterStation cutter) {
        var level = Minecraft.getInstance().level;
        int inside = 0;
        for (int k = 0; k < cutter.shapes().size(); k++) {
            Vec3 at = cutter.shapePoint(k);
            BlockPos block = BlockPos.containing(at);
            if (block.equals(cutter.pos)) continue;
            for (net.minecraft.world.phys.AABB box : level.getBlockState(block).getShape(level, block).toAabbs()) {
                if (box.move(block).contains(at)) inside++;
            }
        }
        return inside;
    }

    /^*
     * 1.1 the key on the campfire's hint, 1.2 the arms of someone at a station seen by others, 4.1/4.2 the knees of
     * someone sitting at a campfire, 3.1 the stonecutter's shapes between two blocks. Each with a before shot (the
     * fix switched off where it can be) and an after shot.
     ^/
    private static void lookAtFixesAct() {
        step(4, () -> {
            act = "vx";
            closeWindow();
        });
        // 1.1: the bound key by its Latin letter (a Russian layout would name the H key "Р")
        step(4, () -> {
            var key = net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.getBoundKeyOf(CampfireHint.SIT);
            REPORT.add("1.1 the layout here names the H key " + org.lwjgl.glfw.GLFW.glfwGetKeyName(org.lwjgl.glfw.GLFW.GLFW_KEY_H, 0)
                    + ", vanilla shows " + CampfireHint.SIT.getTranslatedKeyMessage().getString());
            check("1.1 the hint names the H key H", KeyLabels.of(CampfireHint.SIT).getString().equals("H"), KeyLabels.of(CampfireHint.SIT).getString());
            CampfireHint.SIT.setKey(com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_J));
            net.minecraft.client.KeyMapping.resetMapping();
            check("1.1 rebound to J, the hint says J", KeyLabels.of(CampfireHint.SIT).getString().equals("J"), KeyLabels.of(CampfireHint.SIT).getString());
            CampfireHint.SIT.setKey(com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_ALT));
            net.minecraft.client.KeyMapping.resetMapping();
            check("1.1 rebound to a key without a letter, its own name", KeyLabels.of(CampfireHint.SIT).getString()
                    .equals(CampfireHint.SIT.getTranslatedKeyMessage().getString()), KeyLabels.of(CampfireHint.SIT).getString());
            CampfireHint.SIT.setKey(key);
            net.minecraft.client.KeyMapping.resetMapping();
            standLooking(new Vec3(CAMPFIRE.getX() + 0.5, -60, CAMPFIRE.getZ() + 2.6), new Vec3(CAMPFIRE.getX() + 0.5, -59.7, CAMPFIRE.getZ() + 0.5));
        });
        step(30, () -> shot("vx1_campfire_hint"));
        // 1.2: three people at three stations, each having looked away before stepping up (the body 70 degrees off)
        record At(StationKind kind, BlockPos pos, Vec3 feet, float off) {}
        List<At> stations = List.of(
                new At(StationKind.TABLE, TABLE, new Vec3(TABLE.getX() + 0.5, -60, TABLE.getZ() + 1.3), 70f),
                new At(StationKind.ANVIL, ANVIL, new Vec3(ANVIL.getX() + 0.5, -60, ANVIL.getZ() + 1.36), -70f),
                new At(StationKind.LOOM, LOOM, new Vec3(LOOM.getX() + 1.3, -60, LOOM.getZ() + 0.5), 65f));
        for (boolean fixed : new boolean[]{false, true}) {
            for (At at : stations) {
                String name = "vx2_" + at.kind().name().toLowerCase() + (fixed ? "_after" : "_before");
                step(4, () -> {
                    ClientPayloads.facesStations = fixed;
                    if (smith == null || smith.isRemoved()) smith = standIn(SMITH_ID, (byte) 7, true);
                    float head = yawTo(at.feet(), at.pos());
                    placeStandIn(smith, at.feet(), head, head + at.off(), at.kind(), at.pos());
                    Vec3 side = at.feet().subtract(Vec3.atBottomCenterOf(at.pos())).normalize();
                    Vec3 across = new Vec3(-side.z, 0, side.x);
                    standLooking(at.feet().add(across.scale(2.4)).add(side.scale(1.2)), at.feet().add(0, 1.1, 0).add(side.scale(-0.4)));
                });
                step(30, () -> {
                    float body = net.minecraft.util.Mth.wrapDegrees(smith.yBodyRot - yawTo(smith.position(), at.pos()));
                    if (fixed) check("1.2 " + at.kind() + ": the body turns to the station whatever the player looked at", Math.abs(body) < 8,
                            "body off by " + body);
                    else REPORT.add("1.2 " + at.kind() + " before: the body stays off by " + body);
                    shot(name);
                });
            }
        }
        step(2, () -> ClientPayloads.facesStations = true);
        // 4.2: sitting at the campfire and the soul campfire, seen by others: a wide skin with every layer, a slim one with none
        for (boolean bent : new boolean[]{false, true}) {
            for (BlockPos fire : List.of(CAMPFIRE, SOUL_CAMPFIRE)) {
                String name = "vx3_" + (fire == CAMPFIRE ? "campfire" : "soul") + (bent ? "_after" : "_before");
                step(4, () -> {
                    KneeLayer.bends = bent;
                    if (smith == null || smith.isRemoved()) smith = standIn(SMITH_ID, (byte) 7, true);
                    net.minecraft.client.player.RemotePlayer second = Minecraft.getInstance().level.getEntity(SMITH_ID + 1)
                            instanceof net.minecraft.client.player.RemotePlayer p ? p : standIn(SMITH_ID + 1, (byte) 9, false);
                    Vec3 a = new Vec3(fire.getX() + 0.5, -60, fire.getZ() + 2.1), b = new Vec3(fire.getX() + 2.1, -60, fire.getZ() + 0.5);
                    placeStandIn(smith, a, yawTo(a, fire), yawTo(a, fire), StationKind.CAMPFIRE, fire);
                    placeStandIn(second, b, yawTo(b, fire), yawTo(b, fire), StationKind.CAMPFIRE, fire);
                    standLooking(new Vec3(fire.getX() + 3.4, -60, fire.getZ() + 3.6), new Vec3(fire.getX() + 1.3, -59.6, fire.getZ() + 1.3));
                });
                step(30, () -> {
                    if (bent) check("4.2 " + name + ": the raised leg is drawn in two, the straight one hidden", KneeLayer.sitting(smith), "");
                    shot(name);
                });
                // side on, at the sitters' height (raised a block on stone): the raised knee in profile
                step(4, () -> {
                    BlockPos a = new BlockPos(fire.getX(), -60, fire.getZ() + 2), b = new BlockPos(fire.getX() + 2, -60, fire.getZ());
                    server(p -> {
                        p.serverLevel().setBlockAndUpdate(a, Blocks.STONE.defaultBlockState());
                        p.serverLevel().setBlockAndUpdate(b, Blocks.STONE.defaultBlockState());
                    });
                    Vec3 seatA = new Vec3(fire.getX() + 0.5, -59, fire.getZ() + 2.5), seatB = new Vec3(fire.getX() + 2.5, -59, fire.getZ() + 0.5);
                    placeStandIn(smith, seatA, yawTo(seatA, fire), yawTo(seatA, fire), StationKind.CAMPFIRE, fire);
                    var second = (net.minecraft.client.player.RemotePlayer) Minecraft.getInstance().level.getEntity(SMITH_ID + 1);
                    placeStandIn(second, seatB, yawTo(seatB, fire), yawTo(seatB, fire), StationKind.CAMPFIRE, fire);
                    standLooking(new Vec3(fire.getX() + 3.6, -60, fire.getZ() + 2.6), new Vec3(fire.getX() + 0.5, -58.6, fire.getZ() + 2.5));
                });
                step(20, () -> shot(name + "_side7"));
                step(4, () -> standLooking(new Vec3(fire.getX() + 2.6, -60, fire.getZ() - 2.6), new Vec3(fire.getX() + 2.5, -58.6, fire.getZ() + 0.5)));
                step(20, () -> {
                    shot(name + "_side9");
                    server(p -> {
                        p.serverLevel().setBlockAndUpdate(new BlockPos(fire.getX(), -60, fire.getZ() + 2), Blocks.AIR.defaultBlockState());
                        p.serverLevel().setBlockAndUpdate(new BlockPos(fire.getX() + 2, -60, fire.getZ()), Blocks.AIR.defaultBlockState());
                    });
                });
            }
        }
        // Armor on the raised knee: both pieces must follow the split leg, including the outer boot shell.
        step(4, () -> {
            KneeLayer.bends = true;
            Vec3 seat = new Vec3(CAMPFIRE.getX() + 0.5, -59, CAMPFIRE.getZ() + 2.5);
            server(p -> p.serverLevel().setBlockAndUpdate(new BlockPos(CAMPFIRE.getX(), -60, CAMPFIRE.getZ() + 2), Blocks.STONE.defaultBlockState()));
            placeStandIn(smith, seat, yawTo(seat, CAMPFIRE), yawTo(seat, CAMPFIRE), StationKind.CAMPFIRE, CAMPFIRE);
            smith.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new net.minecraft.world.item.ItemStack(Items.IRON_LEGGINGS));
            smith.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new net.minecraft.world.item.ItemStack(Items.IRON_BOOTS));
            standLooking(new Vec3(CAMPFIRE.getX() + 3.6, -60, CAMPFIRE.getZ() + 2.6),
                    new Vec3(CAMPFIRE.getX() + 0.5, -58.6, CAMPFIRE.getZ() + 2.5));
        });
        step(24, () -> {
            check("4.2 armor: leggings and boots on the bent campfire leg", KneeLayer.sitting(smith)
                    && smith.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS).is(Items.IRON_LEGGINGS)
                    && smith.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).is(Items.IRON_BOOTS), "");
            shot("vx3_campfire_armor_side");
            server(p -> p.serverLevel().setBlockAndUpdate(new BlockPos(CAMPFIRE.getX(), -60, CAMPFIRE.getZ() + 2), Blocks.AIR.defaultBlockState()));
        });
        step(4, () -> {
            for (int id : new int[]{SMITH_ID, SMITH_ID + 1}) {
                ClientPayloads.AT_STATION.remove(id);
                if (Minecraft.getInstance().level.getEntity(id) != null) Minecraft.getInstance().level.removeEntity(id, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            }
        });
        // 4.1: sitting at the campfire oneself, the knees from the eyes (and from behind, looking round)
        step(4, () -> standLooking(new Vec3(CAMPFIRE.getX() + 0.5, -60, CAMPFIRE.getZ() + 2.6), new Vec3(CAMPFIRE.getX() + 0.5, -59.7, CAMPFIRE.getZ() + 0.5)));
        step(30, () -> {
            REPORT.add("4.1 before H: onGround=" + Minecraft.getInstance().player.onGround() + " screen=" + screenName() + " hint target=" + CampfireHint.target());
            net.minecraft.client.KeyMapping.click(net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.getBoundKeyOf(CampfireHint.SIT));
        });
        step(90, () -> {
            check("4.1 sitting at the campfire", Scene.station instanceof CampfireStation && Scene.phase == Scene.Phase.ACTIVE, "phase=" + Scene.phase
                    + " refused=" + Scene.refused);
            pointAside();
            shot("vx4_knees");
        });
        step(4, () -> server(p -> {
            p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new net.minecraft.world.item.ItemStack(Items.IRON_LEGGINGS));
            p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new net.minecraft.world.item.ItemStack(Items.IRON_BOOTS));
        }));
        step(20, () -> shot("vx4_armored_knees"));
        step(4, () -> server(p -> {
            p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.item.ItemStack.EMPTY);
            p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, net.minecraft.world.item.ItemStack.EMPTY);
        }));
        step(4, Scene::leave);
        step(40, () -> {});
        // 3.1: the stonecutter between two stone blocks, then turned to face east with the blocks beside it again
        for (Direction facing : new Direction[]{Direction.SOUTH, Direction.EAST}) {
            Direction across = facing.getClockWise();
            Vec3 feet = Vec3.atBottomCenterOf(STONECUTTER).add(Vec3.atLowerCornerOf(facing.getNormal()).scale(2.9));
            step(4, () -> server(p -> {
                var level = p.serverLevel();
                level.setBlockAndUpdate(STONECUTTER.relative(Direction.NORTH), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(STONECUTTER.relative(Direction.SOUTH), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(STONECUTTER.relative(Direction.EAST), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(STONECUTTER.relative(Direction.WEST), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(STONECUTTER, Blocks.STONECUTTER.defaultBlockState().setValue(StonecutterBlock.FACING, facing));
                level.setBlockAndUpdate(STONECUTTER.relative(across), Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(STONECUTTER.relative(across.getOpposite()), Blocks.STONE.defaultBlockState());
                p.teleportTo(level, feet.x, -60, feet.z, facing.getOpposite().toYRot(), 30f);
            }));
            step(20, () -> useAt(STONECUTTER, new Vec3(0.5, 0.56, 0.5), Direction.UP));
            step(90, () -> server(p -> {
                check("3.1 " + facing + ": at the stonecutter", p.containerMenu instanceof net.minecraft.world.inventory.StonecutterMenu, "menu=" + p.containerMenu);
                if (p.containerMenu instanceof net.minecraft.world.inventory.StonecutterMenu menu) {
                    menu.getSlot(0).set(new ItemStack(Items.STONE, 16));
                    menu.slotsChanged(menu.getSlot(0).container);
                    menu.broadcastChanges();
                }
            }));
            step(20, () -> {
                StonecutterStation.fitsDial = false;
            });
            step(30, () -> {
                StonecutterStation cutter = (StonecutterStation) Scene.station;
                REPORT.add("3.1 " + facing + " before: " + shapesInBlocks(cutter) + " of " + cutter.shapes().size() + " shapes inside the blocks beside");
                pointAside();
                shot("vx5_cutter_" + facing.getName() + "_before");
                StonecutterStation.fitsDial = true;
            });
            step(30, () -> {
                StonecutterStation cutter = (StonecutterStation) Scene.station;
                check("3.1 " + facing + ": no shape inside the blocks beside, the dial made room", shapesInBlocks(cutter) == 0 && cutter.dialFit() > 0,
                        "inside=" + shapesInBlocks(cutter) + " fit=" + cutter.dialFit() + " shapes=" + cutter.shapes().size());
                pointAside();
                shot("vx5_cutter_" + facing.getName() + "_after");
            });
            // the first and the last shape on the dial, picked by the cursor
            for (boolean last : new boolean[]{false, true}) {
                // a pick starts the cut (about a second): the next pick waits for it
                // the piece cut first is in the hand, and another shape would not stack with it: into the inventory first
                if (last) step(46, () -> server(p -> {
                    p.getInventory().placeItemBackInInventory(p.containerMenu.getCarried().copy());
                    p.containerMenu.setCarried(ItemStack.EMPTY);
                    p.containerMenu.broadcastChanges();
                }));
                step(last ? 8 : 4, () -> {
                    StonecutterStation cutter = (StonecutterStation) Scene.station;
                    int k = last ? Math.min(cutter.shapes().size(), StonecutterStation.DIAL_SLOTS) - 1 : 0;
                    double[] at = Picking.toGui(cutter.shapePoint(k));
                    pointGui(at[0], at[1]);
                });
                step(6, () -> screenClick(0));
                step(10, () -> server(p -> check("3.1 " + facing + ": the " + (last ? "last" : "first") + " shape on the dial can be picked",
                        p.containerMenu instanceof net.minecraft.world.inventory.StonecutterMenu menu && menu.getSelectedRecipeIndex() >= 0
                                && menu.getSelectedRecipeIndex() == (last ? Math.min(menu.getNumRecipes(), StonecutterStation.DIAL_SLOTS) - 1 : 0), "selected=" + (p.containerMenu instanceof
                                net.minecraft.world.inventory.StonecutterMenu menu ? menu.getSelectedRecipeIndex() : -9))));
            }
            step(4, Scene::leave);
            step(40, () -> server(p -> {
                p.getInventory().clearOrCountMatchingItems(s -> s.is(Items.STONE) || s.is(Items.STONE_BRICKS) || s.is(Items.STONE_SLAB), -1, p.inventoryMenu.getCraftSlots());
                var level = p.serverLevel();
                level.setBlockAndUpdate(STONECUTTER.relative(across), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(STONECUTTER.relative(across.getOpposite()), Blocks.AIR.defaultBlockState());
            }));
        }
        step(4, () -> server(p -> p.serverLevel().setBlockAndUpdate(STONECUTTER, Blocks.STONECUTTER.defaultBlockState().setValue(StonecutterBlock.FACING, Direction.SOUTH))));
        step(2, () -> finish(Minecraft.getInstance()));
    }

    private static int occlusionFlips;
    private static boolean[] lastInTheWay;
    private static double railJump;
    private static double[][] lastRails;

    /^*
     * 2.3 the table's faded ingredients while the cursor wanders inside one cell, 4.4 the campfire's rails while the hand
     * reaches over the food, 6.2 a hand and blocks in front of the beacon's icons. Numbers here; the frames are compared
     * act by act (0, f, 6, n) against the last build.
     ^/
    private static void visualFixesAct() {
        step(4, () -> {
            act = "vy";
            closeWindow();
            server(Showcase::wipeTable);
            standLooking(new Vec3(0.5, -60, 3.36), Vec3.atCenterOf(TABLE));
        });
        step(30, () -> useAt(TABLE, new Vec3(0.5, 1.0, 0.8), Direction.UP));
        step(80, () -> server(p -> {
            ItemStack[] items = {new ItemStack(Items.CHEST), new ItemStack(Items.RAW_IRON, 40), new ItemStack(Items.FURNACE),
                    new ItemStack(Items.RAW_IRON, 20), new ItemStack(Items.STONE), new ItemStack(Items.RAW_IRON, 40),
                    new ItemStack(Items.CHEST), new ItemStack(Items.RAW_IRON, 40), new ItemStack(Items.FURNACE)};
            for (int i = 0; i < 9; i++) putOnTable(p, i / 3, i % 3, items[i]);
        }));
        // the far middle cell, under the cursor: the cursor wanders inside it for a second and a half
        step(20, () -> {
            occlusionFlips = 0;
            lastInTheWay = null;
        });
        for (int k = 0; k < 30; k++) {
            int n = k;
            step(1, () -> {
                double[] at = Picking.toGui(table().cellPoint(tableWorld(1)));
                pointGui(at[0] + 3 * Math.sin(n * 0.9), at[1] + 2 * Math.cos(n * 1.3));
                boolean[] now = table().inTheWayCopy();
                if (lastInTheWay != null && n > 4) for (int i = 0; i < 9; i++) if (now[i] != lastInTheWay[i]) occlusionFlips++;
                lastInTheWay = now;
                if (n == 20) shot("vy1_table_hover");
            });
        }
        step(2, () -> {
            boolean any = false;
            for (boolean b : lastInTheWay) any |= b;
            check("2.3 the items in front of the hovered cell fade, and stay so while the cursor wanders in it", any && occlusionFlips == 0,
                    "flips=" + occlusionFlips + " faded=" + java.util.Arrays.toString(lastInTheWay));
            check("2.3 the hovered cell's own item stays solid", !lastInTheWay[tableWorld(1)], "");
        });
        step(4, () -> {
            Scene.leave();
            server(Showcase::wipeTable);
        });
        step(40, () -> {});
        // the campfire: food on all four spots, the hand reaching from the near ones to the far ones
        step(4, () -> standLooking(new Vec3(CAMPFIRE.getX() + 0.5, -60, CAMPFIRE.getZ() + 2.6), new Vec3(CAMPFIRE.getX() + 0.5, -59.7, CAMPFIRE.getZ() + 0.5)));
        step(30, () -> net.minecraft.client.KeyMapping.click(net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.getBoundKeyOf(CampfireHint.SIT)));
        step(90, () -> server(p -> {
            if (p.serverLevel().getBlockEntity(CAMPFIRE) instanceof net.minecraft.world.level.block.entity.CampfireBlockEntity fire) {
                for (int i = 0; i < 4; i++) {
                    fire.getItems().set(i, new ItemStack(Items.BEEF));
                    fire.cookingTime[i] = 600;
                    fire.cookingProgress[i] = 100 * i;
                }
                fire.setChanged();
                p.serverLevel().sendBlockUpdated(CAMPFIRE, p.serverLevel().getBlockState(CAMPFIRE), p.serverLevel().getBlockState(CAMPFIRE), 3);
            }
        }));
        step(30, () -> {
            railJump = 0;
            lastRails = null;
        });
        int[] order = {0, 1, 2, 3, 2, 1, 0};
        for (int k = 0; k < order.length * 12; k++) {
            int n = k;
            step(1, () -> {
                if (!(Scene.station instanceof CampfireStation fire)) return;
                if (n % 12 == 0) pointAt(fire.spot(order[n / 12]));
                double[][] now = new double[4][];
                for (int i = 0; i < 4; i++) now[i] = fire.railShown[i] == null ? null : fire.railShown[i].clone();
                // where each rail is drawn against the point of its food this frame: any lift or snap shows as a gap
                for (int i = 0; i < 4; i++) {
                    if (now[i] != null) railJump = Math.max(railJump, Math.hypot(now[i][0] - now[i][2], now[i][1] - now[i][3]));
                }
                lastRails = now;
                if (n == 3 * 12 + 8) shot("vy2_campfire_far");
                if (n == 1 * 12 + 8) shot("vy2_campfire_near");
            });
        }
        step(2, () -> check("4.4 the rails stay on their food while hovered and while the hand reaches round", railJump < 0.01,
                "largest gap=" + String.format("%.2f", railJump)));
        step(4, Scene::leave);
        step(40, () -> server(p -> {
            if (p.serverLevel().getBlockEntity(CAMPFIRE) instanceof net.minecraft.world.level.block.entity.CampfireBlockEntity fire) {
                for (int i = 0; i < 4; i++) fire.getItems().set(i, ItemStack.EMPTY);
                fire.setChanged();
            }
        }));
        // the beacon: a chest and a bell behind its powers, then the hand reaching to the first one
        step(4, () -> server(p -> {
            p.serverLevel().setBlockAndUpdate(BEACON.offset(0, 1, -2), Blocks.CHEST.defaultBlockState());
            p.serverLevel().setBlockAndUpdate(BEACON.offset(-1, 1, -2), Blocks.BELL.defaultBlockState());
            p.serverLevel().setBlockAndUpdate(BEACON.offset(1, 1, -2), Blocks.SHULKER_BOX.defaultBlockState());
            p.teleportTo(p.serverLevel(), BEACON.getX() + 0.5, BEACON.getY(), BEACON.getZ() + 1.75, 180f, 20f);
        }));
        step(20, () -> useAt(BEACON, new Vec3(0.5, 0.9, 0.5), Direction.UP));
        step(90, () -> {
            check("6.2 at the beacon", beacon() != null && Scene.phase == Scene.Phase.ACTIVE, "phase=" + Scene.phase);
            pointAside();
            shot("vy3_beacon_behind");
        });
        // reaching up to the highest power, the arm passes in front of the lower ones
        step(2, () -> {
            railJump = 1;
            pointAt(beacon().powerPos(4));
        });
        for (int k = 0; k < 14; k++) {
            int n = k;
            step(1, () -> {
                railJump = Math.min(railJump, Math.min(beacon().iconSeen(0), beacon().iconSeen(2)));
                if (n == 5) shot("vy3_beacon_hand");
            });
        }
        step(2, () -> check("6.2 the arm reaching past the lower powers hides their icons as it passes", railJump < 0.9,
                "least seen=" + String.format("%.2f", railJump)));
        step(4, Scene::leave);
        step(40, () -> server(p -> {
            for (int dx = -1; dx <= 1; dx++) p.serverLevel().setBlockAndUpdate(BEACON.offset(dx, 1, -2), Blocks.AIR.defaultBlockState());
        }));
        step(2, () -> finish(Minecraft.getInstance()));
    }

    private static void reopenAfterInventoryAct() {
        step(2, () -> act = "ri");
        record Case(String name, BlockPos pos, Class<?> vanillaScreen) {}
        List<Case> stations = List.of(
                new Case("smithing", SMITHING, net.minecraft.client.gui.screens.inventory.SmithingScreen.class),
                new Case("loom", LOOM, net.minecraft.client.gui.screens.inventory.LoomScreen.class),
                new Case("anvil", ANVIL, net.minecraft.client.gui.screens.inventory.AnvilScreen.class),
                new Case("stonecutter", STONECUTTER, net.minecraft.client.gui.screens.inventory.StonecutterScreen.class));
        // ticks between leaving and pressing E: once the work's screen has gone (0.22 s), early and late in the step away
        int[] pauses = {6, 10, 16};
        for (Case c : stations) {
            // a plain way in and out first, no inventory
            enterSmithy(c.pos()).run();
            step(80, () -> check(c.name() + ": scene plays", Scene.phase == Scene.Phase.ACTIVE && Scene.station != null
                    && Scene.station.pos.equals(c.pos()), "phase=" + Scene.phase + " screen=" + screenName()));
            step(2, () -> pressInScene(GLFW.GLFW_KEY_E));
            step(40, () -> check(c.name() + ": plain step away ends", Scene.phase == Scene.Phase.NONE && Minecraft.getInstance().screen == null,
                    "phase=" + Scene.phase + " screen=" + screenName()));
            for (int pause : pauses) {
                String label = c.name() + " E after " + pause + " ticks";
                enterSmithy(c.pos()).run();
                step(80, () -> check(label + ": scene plays", Scene.phase == Scene.Phase.ACTIVE && Scene.station != null
                        && Scene.station.pos.equals(c.pos()), "phase=" + Scene.phase + " screen=" + screenName()));
                step(2, () -> pressInScene(GLFW.GLFW_KEY_E));
                // the game's own key: the inventory opens on the next tick there is no screen
                step(pause, () -> net.minecraft.client.KeyMapping.click(
                        net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.getBoundKeyOf(Minecraft.getInstance().options.keyInventory)));
                step(6, () -> {
                    REPORT.add(label + ": after E phase=" + Scene.phase + " screen=" + screenName());
                    check(label + ": the inventory opens", Minecraft.getInstance().screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen,
                            "phase=" + Scene.phase + " screen=" + screenName());
                });
                step(30, () -> {
                    check(label + ": the step away is over", Scene.phase == Scene.Phase.NONE, "phase=" + Scene.phase);
                    shot("ri_" + c.name() + "_" + pause + "_inventory");
                    Minecraft.getInstance().setScreen(null);
                });
                step(10, () -> server(p -> check(label + ": no session left", dev.dyslexic.cinematic.station.Sessions.get(p) == null, "session")));
                enterSmithy(c.pos()).run();
                step(80, () -> {
                    check(label + ": the scene plays again, not the window", Scene.phase == Scene.Phase.ACTIVE && Scene.station != null
                            && Scene.station.pos.equals(c.pos()) && !c.vanillaScreen().isInstance(Minecraft.getInstance().screen),
                            "phase=" + Scene.phase + " screen=" + screenName());
                    server(p -> check(label + ": the server has the new session", dev.dyslexic.cinematic.station.Sessions.get(p) != null
                            && dev.dyslexic.cinematic.station.Sessions.get(p).pos().equals(c.pos()), "session"));
                    shot("ri_" + c.name() + "_" + pause + "_again");
                });
                step(2, () -> {
                    if (Scene.phase == Scene.Phase.ACTIVE) pressInScene(GLFW.GLFW_KEY_E);
                    else Minecraft.getInstance().setScreen(null);
                });
                step(40, () -> {});
            }
        }
        // another menu while working (a server's chest, a command): the scene gives way, the station is not marked
        enterSmithy(SMITHING).run();
        step(80, () -> check("unrelated menu: scene plays", Scene.phase == Scene.Phase.ACTIVE, "phase=" + Scene.phase));
        step(2, () -> server(p -> p.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (id, inventory, who) -> net.minecraft.world.inventory.ChestMenu.threeRows(id, inventory), Component.literal("Chest")))));
        step(20, () -> check("unrelated menu: its window opens, the scene ends", Minecraft.getInstance().screen
                instanceof net.minecraft.client.gui.screens.inventory.ContainerScreen && Scene.phase == Scene.Phase.NONE,
                "phase=" + Scene.phase + " screen=" + screenName()));
        step(2, () -> Minecraft.getInstance().player.closeContainer());
        enterSmithy(SMITHING).run();
        step(80, () -> check("unrelated menu: the station still plays its scene", Scene.phase == Scene.Phase.ACTIVE
                && Scene.station != null && Scene.station.pos.equals(SMITHING), "phase=" + Scene.phase + " screen=" + screenName()));
        step(2, () -> pressInScene(GLFW.GLFW_KEY_E));
        step(40, () -> {});
        // a mod's own menu in answer to this very attempt (as Penchant's enchanting table on NeoForge): the block keeps
        // its own window from then on. The answer is made here, as MenuScreens makes one, right after BEGIN goes out.
        step(2, () -> server(p -> p.teleportTo(p.serverLevel(), ENCHANT.getX() + 0.5, -60, ENCHANT.getZ() + 3.6, 180f, 20f)));
        step(20, () -> {
            useAt(ENCHANT, new Vec3(0.5, 0.75, 0.6), Direction.UP);
            Minecraft mc = Minecraft.getInstance();
            var foreign = net.minecraft.world.inventory.ChestMenu.threeRows(99, mc.player.getInventory());
            mc.player.containerMenu = foreign;
            mc.setScreen(new net.minecraft.client.gui.screens.inventory.ContainerScreen(foreign, mc.player.getInventory(), Component.literal("Foreign")));
            check("foreign menu of this attempt: its window, not the scene", Scene.phase == Scene.Phase.NONE
                    && mc.screen instanceof net.minecraft.client.gui.screens.inventory.ContainerScreen, "phase=" + Scene.phase + " screen=" + screenName());
        });
        step(20, () -> Minecraft.getInstance().player.closeContainer());
        step(10, () -> useAt(ENCHANT, new Vec3(0.5, 0.75, 0.6), Direction.UP));
        step(20, () -> check("foreign menu: the block keeps its own window", Scene.phase == Scene.Phase.NONE && Minecraft.getInstance().screen
                instanceof net.minecraft.client.gui.screens.inventory.EnchantmentScreen, "phase=" + Scene.phase + " screen=" + screenName()));
        step(4, () -> {
            Minecraft.getInstance().player.closeContainer();
            Scene.forgetOwnWindows();
        });
        step(10, () -> useAt(ENCHANT, new Vec3(0.5, 0.75, 0.6), Direction.UP));
        step(80, () -> check("foreign menu: on a new connection the scene plays again", Scene.phase == Scene.Phase.ACTIVE,
                "phase=" + Scene.phase + " screen=" + screenName()));
        step(2, () -> pressInScene(GLFW.GLFW_KEY_E));
        step(40, () -> {});
        step(2, () -> finish(Minecraft.getInstance()));
    }

    private static String screenName() {
        Screen screen = Minecraft.getInstance().screen;
        return screen == null ? "none" : screen.getClass().getSimpleName();
    }

    /^* A key pressed over the work, as the player presses it (E and Esc step away). ^/
    private static void pressInScene(int key) {
        if (Minecraft.getInstance().screen instanceof SceneScreen screen) {
            int scan = GLFW.glfwGetKeyScancode(key);
            screen.keyPressed(key, scan, 0);
            screen.keyReleased(key, scan, 0);
        }
    }

    *///?}
    private static Runnable enterAt(Vec3 feet, float yaw, BlockPos station, Vec3 local, Direction face) {
        // run while the act is being scheduled: these steps go in their place in the queue
        return () -> {
            step(2, () -> server(p -> p.teleportTo(p.serverLevel(), feet.x, feet.y, feet.z, yaw, 20f)));
            step(20, () -> useAt(station, local, face));
        };
    }

    private static Runnable enterSmithy(BlockPos station) {
        double y = station.equals(STONECUTTER) ? 0.56 : 1.0;
        return enterAt(new Vec3(station.getX() + 0.5, -60, station.getZ() + 3.4), 180f, station, new Vec3(0.5, y, 0.5), Direction.UP);
    }

    private static ItemStack tableCell(ServerPlayer p, int world) {
        TableStore.Table table = TableStore.get(p.serverLevel()).get(TABLE);
        return table == null ? ItemStack.EMPTY : table.items.get(world);
    }

    private static int tableWorld(int logical) {
        return TableGrid.worldIndex(Scene.station != null ? Scene.station.sideDirection() : Direction.SOUTH, logical / 3, logical % 3);
    }

    private static List<ItemStack> furnaceItems(ServerPlayer p, BlockPos pos) {
        return p.serverLevel().getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity f
                ? List.of(f.getItem(0), f.getItem(1), f.getItem(2)) : List.of();
    }

    private static void setFurnace(ServerPlayer p, BlockPos pos, ItemStack input, ItemStack fuel) {
        if (p.serverLevel().getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity f) {
            f.setItem(0, input);
            f.setItem(1, fuel);
            f.setItem(2, ItemStack.EMPTY);
        }
    }

    private static void quickFurnace(String name, BlockPos pos, Vec3 feet, float yaw, ItemStack food, ItemStack wrong) {
        quickStation(name, p -> {
            setFurnace(p, pos, ItemStack.EMPTY, ItemStack.EMPTY);
            p.getInventory().setItem(1, food.copyWithCount(20));
            p.getInventory().setItem(12, new ItemStack(Items.COAL, 10));
            p.getInventory().setItem(13, wrong.copy());
            p.getInventory().setItem(14, food.copyWithCount(10));
            p.getInventory().setItem(15, food.copyWithCount(3));
        }, enterAt(feet, yaw, pos, new Vec3(0.5, 0.6, 1.0), Direction.SOUTH), 90, p -> describeAll(furnaceItems(p, pos)), List.of(
                new QuickCase("what smelts goes in at the top (hotbar)", 1,
                        p -> inv(p, 1).isEmpty() && holds(furnaceItems(p, pos).get(0), food.getItem(), 20)),
                // with something to smelt the fire lights at once and burns one piece
                new QuickCase("fuel goes into the fire box (pack)", 12, p -> inv(p, 12).isEmpty()
                        && furnaceItems(p, pos).get(1).is(Items.COAL) && furnaceItems(p, pos).get(1).getCount() >= 9),
                new QuickCase("what neither smelts nor burns stays put", 13,
                        p -> holds(inv(p, 13), wrong.getItem(), wrong.getCount())),
                new QuickCase("a nearly full opening takes what fits, the rest stays", q -> furnaceItems(q, pos).get(0).setCount(60), 14,
                        p -> holds(inv(p, 14), food.getItem(), 6) && holds(furnaceItems(p, pos).get(0), food.getItem(), 64), 0),
                new QuickCase("an opening holding something else takes nothing",
                        q -> setFurnace(q, pos, new ItemStack(Items.GOLD_ORE), furnaceItems(q, pos).get(1).copy()), 15,
                        p -> holds(inv(p, 15), food.getItem(), 3) && furnaceItems(p, pos).get(0).is(Items.GOLD_ORE), 0)));
    }

    //? if fabric && <1.21 {
    /*// ---- recipes from a data pack: the table and the furnace work with what the server loaded ----------------

    /^* Only the disputed crafting recipe: avoids rerunning the full station walkthrough. ^/
    private static void polymorphAct(String which) {
        step(2, () -> server(p -> {
            try {
                java.nio.file.Path pack = p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.DATAPACK_DIR).resolve("cinematic_polymorph_test");
                java.nio.file.Path recipes = pack.resolve("data/cinematic_polymorph_test/recipes");
                java.nio.file.Files.createDirectories(recipes);
                java.nio.file.Files.writeString(pack.resolve("pack.mcmeta"),
                        "{\"pack\": {\"pack_format\": 15, \"description\": \"Cinematic Polymorph test\"}}");
                java.nio.file.Files.writeString(recipes.resolve("a_barrel.json"),
                        "{\"type\":\"minecraft:crafting_shapeless\",\"ingredients\":[{\"item\":\"minecraft:cobblestone\"}],\"result\":{\"item\":\"minecraft:barrel\"}}");
                java.nio.file.Files.writeString(recipes.resolve("b_crafting_table.json"),
                        "{\"type\":\"minecraft:crafting_shapeless\",\"ingredients\":[{\"item\":\"minecraft:cobblestone\"}],\"result\":{\"item\":\"minecraft:crafting_table\"}}");
                java.nio.file.Files.writeString(recipes.resolve("c_nugget.json"),
                        "{\"type\":\"minecraft:smelting\",\"ingredient\":{\"item\":\"minecraft:raw_iron\"},\"result\":\"minecraft:gold_nugget\",\"experience\":0,\"cookingtime\":100}");
                java.nio.file.Files.writeString(recipes.resolve("d_ingot.json"),
                        "{\"type\":\"minecraft:smelting\",\"ingredient\":{\"item\":\"minecraft:raw_iron\"},\"result\":\"minecraft:iron_ingot\",\"experience\":0,\"cookingtime\":100}");
                java.nio.file.Files.writeString(recipes.resolve("e_gold_sword.json"),
                        "{\"type\":\"minecraft:smithing_transform\",\"template\":{\"item\":\"minecraft:netherite_upgrade_smithing_template\"},"
                        + "\"base\":{\"item\":\"minecraft:diamond_sword\"},\"addition\":{\"item\":\"minecraft:netherite_ingot\"},"
                        + "\"result\":{\"item\":\"minecraft:golden_sword\"}}");
                java.nio.file.Files.writeString(recipes.resolve("f_iron_sword.json"),
                        "{\"type\":\"minecraft:smithing_transform\",\"template\":{\"item\":\"minecraft:netherite_upgrade_smithing_template\"},"
                        + "\"base\":{\"item\":\"minecraft:diamond_sword\"},\"addition\":{\"item\":\"minecraft:netherite_ingot\"},"
                        + "\"result\":{\"item\":\"minecraft:iron_sword\"}}");
            } catch (java.io.IOException e) {
                check("polymorph: conflict pack written", false, e.toString());
            }
        }));
        step(4, () -> cmd("datapack enable \"file/cinematic_polymorph_test\""));
        step(4, () -> cmd("reload"));
        step(60, () -> check("polymorph: both recipes loaded", Minecraft.getInstance().level.getRecipeManager()
                .byKey(new ResourceLocation("cinematic_polymorph_test", "a_barrel")).isPresent()
                && Minecraft.getInstance().level.getRecipeManager()
                .byKey(new ResourceLocation("cinematic_polymorph_test", "b_crafting_table")).isPresent(), "recipe reload"));
        if (which.equals("pf")) nextAct("pf", new Vec3(-3.2, -60, 4.6), 200f, FURNACE, new Vec3(0.5, 0.6, 1.0), Direction.SOUTH);
        else if (which.equals("ps")) smithyAct("ps", SMITHING);
        else nextAct("pm", new Vec3(0.7, -60, 5.6), 172f, TABLE, new Vec3(0.5, 1, 0.8), Direction.UP);
    }

    private static void polymorphScript() {
        step(3, () -> server(p -> {
            var store = TableStore.get(p.serverLevel());
            var table = store.getOrCreate(TABLE);
            for (int i = 0; i < 9; i++) table.items.set(i, ItemStack.EMPTY);
            table.items.set(4, new ItemStack(Items.COBBLESTONE));
            store.setDirty();
            dev.dyslexic.cinematic.table.TableActions.sync(p.serverLevel(), TABLE);
        }));
        step(15, () -> {
            boolean loaded = dev.dyslexic.cinematic.station.RecipeChoices.available();
            check("polymorph: expected test mod loaded", loaded, "loaded=" + loaded);
            check("polymorph: two results offered", table().choices().active(),
                    "selected=" + table().choices().selected());
            table().chose(1);
        });
        step(12, () -> {
            check("polymorph: second result preview", table().result.is(Items.CRAFTING_TABLE),
                    "result=" + describe(table().result));
            server(p -> check("polymorph: server records selection",
                    new ResourceLocation("cinematic_polymorph_test", "b_crafting_table")
                            .equals(dev.dyslexic.cinematic.station.RecipeChoices.chosen(p)),
                    "selected=" + dev.dyslexic.cinematic.station.RecipeChoices.chosen(p)));
        });
        step(2, () -> dev.dyslexic.cinematic.net.PacketDistributor.sendToServer(new dev.dyslexic.cinematic.net.StationActionPayload(TABLE,
                StationKind.TABLE, dev.dyslexic.cinematic.net.StationActionPayload.Action.CRAFT, 0, 0, false, 0)));
        step(20, () -> server(p -> check("polymorph: selected recipe crafted",
                p.inventoryMenu.getCarried().is(Items.CRAFTING_TABLE),
                "carried=" + describe(p.inventoryMenu.getCarried()))));
        step(2, () -> finish(Minecraft.getInstance()));
    }

    private static void polymorphFurnaceScript() {
        step(3, () -> server(p -> setFurnace(p, FURNACE, new ItemStack(Items.RAW_IRON, 2), new ItemStack(Items.COAL))));
        step(18, () -> {
            var strip = Scene.station.choices();
            check("polymorph furnace: competing results offered", strip != null && strip.active(),
                    "active=" + (strip != null && strip.active()));
            Scene.station.chose(0);
        });
        step(14, () -> {
            check("polymorph furnace: server choice reaches scene", Scene.station.choices().selected() == 0,
                    "selected=" + Scene.station.choices().selected());
            server(p -> check("polymorph furnace: block stores chosen recipe",
                    p.serverLevel().getBlockEntity(FURNACE) instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity furnace
                            && "cinematic_polymorph_test:c_nugget".equals(dev.dyslexic.cinematic.station.RecipeChoices.furnaceChoice(furnace, StationKind.FURNACE)),
                    "furnace selection"));
        });
        step(230, () -> server(p -> check("polymorph furnace: selected output smelted",
                furnaceItems(p, FURNACE).get(2).is(Items.GOLD_NUGGET),
                "output=" + describe(furnaceItems(p, FURNACE).get(2)))));
        step(2, () -> finish(Minecraft.getInstance()));
    }

    private static void polymorphSmithingScript() {
        step(3, () -> server(p -> {
            if (p.containerMenu instanceof net.minecraft.world.inventory.SmithingMenu menu) {
                menu.getSlot(0).set(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
                menu.getSlot(1).set(new ItemStack(Items.DIAMOND_SWORD));
                menu.getSlot(2).set(new ItemStack(Items.NETHERITE_INGOT));
                menu.slotsChanged(menu.getSlot(0).container);
                menu.broadcastChanges();
            }
        }));
        step(18, () -> {
            var strip = Scene.station.choices();
            check("polymorph smithing: competing results offered", strip != null && strip.active(),
                    "active=" + (strip != null && strip.active()));
            Scene.station.chose(0);
        });
        step(14, () -> {
            server(p -> check("polymorph smithing: selected result is in the real menu",
                    p.containerMenu.getSlot(3).getItem().is(Items.GOLDEN_SWORD),
                    "result=" + describe(p.containerMenu.getSlot(3).getItem())));
            check("polymorph smithing: scene shows selected result", Scene.station.choices().selected() == 0,
                    "selected=" + Scene.station.choices().selected());
        });
        step(2, () -> finish(Minecraft.getInstance()));
    }

    /^* The world's own data pack: a shaped recipe whose result carries a name (NBT), and a smelting recipe of its own. ^/
    private static void customRecipesAct() {
        step(2, () -> server(p -> {
            try {
                java.nio.file.Path pack = p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.DATAPACK_DIR).resolve("cinematic_test");
                java.nio.file.Path recipes = pack.resolve("data/cinematic_test/recipes");
                java.nio.file.Files.createDirectories(recipes);
                java.nio.file.Files.writeString(pack.resolve("pack.mcmeta"),
                        "{\"pack\": {\"pack_format\": 15, \"description\": \"Cinematic Interactions test recipes\"}}");
                java.nio.file.Files.writeString(recipes.resolve("named_emeralds.json"), """
                        {"type": "minecraft:crafting_shaped", "pattern": ["C", "S"],
                         "key": {"C": {"item": "minecraft:cobblestone"}, "S": {"item": "minecraft:stick"}},
                         "result": {"item": "minecraft:emerald", "count": 2, "nbt": "{display:{Name:'{\\"text\\":\\"Изумруд из датапака\\"}'}}"}}
                        """);
                java.nio.file.Files.writeString(recipes.resolve("golden_apple.json"), """
                        {"type": "minecraft:smelting", "ingredient": {"item": "minecraft:apple"},
                         "result": "minecraft:golden_apple", "experience": 0.5, "cookingtime": 40}
                        """);
            } catch (java.io.IOException e) {
                check("custom recipe pack written", false, e.toString());
            }
        }));
        step(4, () -> cmd("datapack enable \"file/cinematic_test\""));
        step(4, () -> cmd("reload"));
        step(60, () -> {
            var recipes = Minecraft.getInstance().level.getRecipeManager();
            check("custom recipes reach the client", recipes.byKey(new ResourceLocation("cinematic_test", "named_emeralds")).isPresent()
                    && recipes.byKey(new ResourceLocation("cinematic_test", "golden_apple")).isPresent(), "client recipe book after /reload");
        });
        nextAct("cr", new Vec3(0.7, -60, 5.6), 172f, TABLE, new Vec3(0.5, 1, 0.8), Direction.UP);
    }

    private static void customRecipesScript() {
        if (Scene.station != null && Scene.station.kind() == StationKind.FURNACE) {
            customSmeltingScript();
            return;
        }
        shot("cr5_table");
        step(4, () -> invClickItem(Items.COBBLESTONE));
        step(3, () -> point(0, 1));
        step(8, () -> clickCell(0, 1, 1));
        step(4, Showcase::stash);
        step(4, () -> invClickItem(Items.STICK));
        step(3, () -> point(1, 1));
        step(8, () -> clickCell(1, 1, 1));
        step(4, Showcase::stash);
        step(4, Showcase::pointAside);
        step(12, () -> shot("cr6_custom_recipe"));
        step(2, () -> pointAt(table().resultPosition()));
        step(8, () -> shot("cr7_custom_result"));
        step(2, Showcase::clickResult);
        step(30, () -> {
            ItemStack hand = carried();
            check("data pack recipe crafted on the table", hand.is(Items.EMERALD) && hand.getCount() == 2
                    && hand.getHoverName().getString().equals("Изумруд из датапака"), "carried=" + hand + " name=" + hand.getHoverName().getString());
        });
        step(2, () -> server(p -> {
            ItemStack hand = p.inventoryMenu.getCarried();
            check("server holds the named result", hand.is(Items.EMERALD) && hand.hasCustomHoverName(), "server carried=" + hand + " tag=" + hand.getTag());
        }));
        step(4, Showcase::stash);
        step(4, Scene::leave);
        step(45, () -> {});
        nextAct("cr", new Vec3(-3.2, -60, 4.6), 200f, FURNACE, new Vec3(0.5, 0.6, 1.0), Direction.SOUTH);
    }

    private static void customSmeltingScript() {
        shot("cr8_furnace");
        step(2, () -> server(p -> p.getInventory().add(new ItemStack(Items.APPLE, 2))));
        step(6, () -> invClickItem(Items.APPLE));
        step(3, () -> pointZone(FURNACE, FurnaceZones.INPUT));
        step(8, () -> shot("cr8_apple_over_input"));
        step(2, () -> clickZone(FurnaceZones.INPUT, 1));
        step(4, Showcase::stash);
        step(4, () -> invClickItem(Items.COAL));
        step(3, () -> pointZone(FURNACE, FurnaceZones.FUEL));
        step(8, () -> clickZone(FurnaceZones.FUEL, 1));
        step(4, Showcase::stash);
        step(4, Showcase::pointAside);
        step(70, () -> shot("cr9_smelted"));
        step(2, () -> server(p -> {
            ItemStack result = p.serverLevel().getBlockEntity(FURNACE) instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity furnace
                    ? furnace.getItem(FurnaceZones.RESULT) : ItemStack.EMPTY;
            check("data pack smelting in the furnace scene", result.is(Items.GOLDEN_APPLE), "result=" + describe(result));
        }));
        step(2, () -> check("watchers see the custom result in the furnace", ClientFurnaces.item(FURNACE, FurnaceZones.RESULT).is(Items.GOLDEN_APPLE),
                "client=" + describe(ClientFurnaces.item(FURNACE, FurnaceZones.RESULT))));
        step(4, Scene::leave);
        step(40, () -> finish(Minecraft.getInstance()));
    }

    // ---- a menu of another class where the station's own was expected ------------------------------------------

    /^*
     * What a mod that replaces a station's menu does (Penchant's enchanting table in NeoForge; no such mod is at hand for
     * 1.20.1): while the enchanting scene works, the server puts a menu of another class in front of the player. The scene
     * must step aside for that window at once (the client must not keep it half-made), the server must end the session,
     * and the same block must then open its own window directly, without the scene first, for the rest of the connection.
     ^/
    private static void foreignMenuAct() {
        step(2, () -> server(p -> {
            p.getInventory().add(new ItemStack(Items.IRON_SWORD));
            p.teleportTo(p.serverLevel(), -4.5, -60, 11.6, 180f, 20f);
        }));
        step(30, () -> use(ENCHANT, new Vec3(0.5, 0.75, 0.6), Direction.UP));
        step(80, () -> {
            check("the enchanting scene plays", Scene.phase == Scene.Phase.ACTIVE && Scene.station != null
                    && Scene.station.kind() == StationKind.ENCHANTING, "phase=" + Scene.phase);
            shot("fm1_scene");
            server(p -> p.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inventory, player) ->
                    net.minecraft.world.inventory.ChestMenu.threeRows(id, inventory), Component.literal("Foreign menu"))));
        });
        step(30, () -> {
            Minecraft mc = Minecraft.getInstance();
            check("the scene steps aside for the other menu", Scene.phase == Scene.Phase.NONE, "phase=" + Scene.phase);
            check("the other menu's own window is open", mc.screen instanceof net.minecraft.client.gui.screens.inventory.ContainerScreen
                    && mc.player.containerMenu instanceof net.minecraft.world.inventory.ChestMenu, "screen=" + mc.screen);
            server(p -> check("no session is left on the server", dev.dyslexic.cinematic.station.Sessions.get(p) == null,
                    "menu=" + p.containerMenu.getClass().getSimpleName()));
            shot("fm2_window");
        });
        // the slot change that used to reach a window that never opened
        step(4, () -> {
            Minecraft mc = Minecraft.getInstance();
            var menu = mc.player.containerMenu;
            for (int i = 0; i < menu.slots.size(); i++) {
                if (menu.slots.get(i).container == mc.player.getInventory() && menu.slots.get(i).getItem().is(Items.IRON_SWORD)) {
                    mc.gameMode.handleInventoryMouseClick(menu.containerId, i, 0, ClickType.QUICK_MOVE, mc.player);
                    break;
                }
            }
        });
        step(20, () -> {
            Minecraft mc = Minecraft.getInstance();
            var menu = mc.player.containerMenu;
            boolean placed = false;
            for (var slot : menu.slots) placed |= slot.container != mc.player.getInventory() && slot.getItem().is(Items.IRON_SWORD);
            check("the window works on (an item went in)", placed && mc.screen instanceof net.minecraft.client.gui.screens.inventory.ContainerScreen,
                    "screen=" + mc.screen);
            mc.screen.onClose();
        });
        step(10, () -> server(p -> {
            p.getInventory().clearOrCountMatchingItems(st -> st.is(Items.IRON_SWORD), -1, p.inventoryMenu.getCraftSlots());
            check("the server closed it", p.containerMenu == p.inventoryMenu, "menu=" + p.containerMenu.getClass().getSimpleName());
        }));
        // used again: this kind of block keeps its own window for the rest of the connection
        step(10, () -> use(ENCHANT, new Vec3(0.5, 0.75, 0.6), Direction.UP));
        step(30, () -> {
            Minecraft mc = Minecraft.getInstance();
            check("used again: its own window, no scene first", Scene.phase == Scene.Phase.NONE
                    && mc.screen instanceof net.minecraft.client.gui.screens.inventory.EnchantmentScreen, "screen=" + mc.screen + " phase=" + Scene.phase);
            shot("fm3_own_window");
            closeWindow();
        });
        step(10, () -> finish(Minecraft.getInstance()));
    }

    *///?} else if forge {
    /*// ---- recipes from a data pack: the table and the furnace work with what the server loaded ----------------

    /^* Only the disputed crafting recipe: avoids rerunning the full station walkthrough. ^/
    private static void polymorphAct(String which) {
        step(2, () -> server(p -> {
            try {
                java.nio.file.Path pack = p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.DATAPACK_DIR).resolve("cinematic_polymorph_test");
                java.nio.file.Path recipes = pack.resolve("data/cinematic_polymorph_test/recipes");
                java.nio.file.Files.createDirectories(recipes);
                java.nio.file.Files.writeString(pack.resolve("pack.mcmeta"),
                        "{\"pack\": {\"pack_format\": 15, \"description\": \"Cinematic Polymorph test\"}}");
                java.nio.file.Files.writeString(recipes.resolve("a_barrel.json"),
                        "{\"type\":\"minecraft:crafting_shapeless\",\"ingredients\":[{\"item\":\"minecraft:cobblestone\"}],\"result\":{\"item\":\"minecraft:barrel\"}}");
                java.nio.file.Files.writeString(recipes.resolve("b_crafting_table.json"),
                        "{\"type\":\"minecraft:crafting_shapeless\",\"ingredients\":[{\"item\":\"minecraft:cobblestone\"}],\"result\":{\"item\":\"minecraft:crafting_table\"}}");
                java.nio.file.Files.writeString(recipes.resolve("c_nugget.json"),
                        "{\"type\":\"minecraft:smelting\",\"ingredient\":{\"item\":\"minecraft:raw_iron\"},\"result\":\"minecraft:gold_nugget\",\"experience\":0,\"cookingtime\":100}");
                java.nio.file.Files.writeString(recipes.resolve("d_ingot.json"),
                        "{\"type\":\"minecraft:smelting\",\"ingredient\":{\"item\":\"minecraft:raw_iron\"},\"result\":\"minecraft:iron_ingot\",\"experience\":0,\"cookingtime\":100}");
                java.nio.file.Files.writeString(recipes.resolve("e_gold_sword.json"),
                        "{\"type\":\"minecraft:smithing_transform\",\"template\":{\"item\":\"minecraft:netherite_upgrade_smithing_template\"},"
                        + "\"base\":{\"item\":\"minecraft:diamond_sword\"},\"addition\":{\"item\":\"minecraft:netherite_ingot\"},"
                        + "\"result\":{\"item\":\"minecraft:golden_sword\"}}");
                java.nio.file.Files.writeString(recipes.resolve("f_iron_sword.json"),
                        "{\"type\":\"minecraft:smithing_transform\",\"template\":{\"item\":\"minecraft:netherite_upgrade_smithing_template\"},"
                        + "\"base\":{\"item\":\"minecraft:diamond_sword\"},\"addition\":{\"item\":\"minecraft:netherite_ingot\"},"
                        + "\"result\":{\"item\":\"minecraft:iron_sword\"}}");
            } catch (java.io.IOException e) {
                check("polymorph: conflict pack written", false, e.toString());
            }
        }));
        step(4, () -> cmd("datapack enable \"file/cinematic_polymorph_test\""));
        step(4, () -> cmd("reload"));
        step(60, () -> check("polymorph: both recipes loaded", Minecraft.getInstance().level.getRecipeManager()
                .byKey(new ResourceLocation("cinematic_polymorph_test", "a_barrel")).isPresent()
                && Minecraft.getInstance().level.getRecipeManager()
                .byKey(new ResourceLocation("cinematic_polymorph_test", "b_crafting_table")).isPresent(), "recipe reload"));
        if (which.equals("pf")) nextAct("pf", new Vec3(-3.2, -60, 4.6), 200f, FURNACE, new Vec3(0.5, 0.6, 1.0), Direction.SOUTH);
        else if (which.equals("ps")) smithyAct("ps", SMITHING);
        else nextAct("pm", new Vec3(0.7, -60, 5.6), 172f, TABLE, new Vec3(0.5, 1, 0.8), Direction.UP);
    }

    private static void polymorphScript() {
        step(3, () -> server(p -> {
            var store = TableStore.get(p.serverLevel());
            var table = store.getOrCreate(TABLE);
            for (int i = 0; i < 9; i++) table.items.set(i, ItemStack.EMPTY);
            table.items.set(4, new ItemStack(Items.COBBLESTONE));
            store.setDirty();
            dev.dyslexic.cinematic.table.TableActions.sync(p.serverLevel(), TABLE);
        }));
        step(15, () -> {
            boolean loaded = dev.dyslexic.cinematic.station.RecipeChoices.available();
            check("polymorph: expected test mod loaded", loaded, "loaded=" + loaded);
            check("polymorph: two results offered", table().choices().active(),
                    "selected=" + table().choices().selected());
            table().chose(1);
        });
        step(12, () -> {
            check("polymorph: second result preview", table().result.is(Items.CRAFTING_TABLE),
                    "result=" + describe(table().result));
            server(p -> check("polymorph: server records selection",
                    new ResourceLocation("cinematic_polymorph_test", "b_crafting_table")
                            .equals(dev.dyslexic.cinematic.station.RecipeChoices.chosen(p)),
                    "selected=" + dev.dyslexic.cinematic.station.RecipeChoices.chosen(p)));
        });
        step(2, () -> ClientNet.sendToServer(new dev.dyslexic.cinematic.net.StationActionPayload(TABLE,
                StationKind.TABLE, dev.dyslexic.cinematic.net.StationActionPayload.Action.CRAFT, 0, 0, false, 0)));
        step(20, () -> server(p -> check("polymorph: selected recipe crafted",
                p.inventoryMenu.getCarried().is(Items.CRAFTING_TABLE),
                "carried=" + describe(p.inventoryMenu.getCarried()))));
        step(2, () -> finish(Minecraft.getInstance()));
    }

    private static void polymorphFurnaceScript() {
        step(3, () -> server(p -> setFurnace(p, FURNACE, new ItemStack(Items.RAW_IRON, 2), new ItemStack(Items.COAL))));
        step(18, () -> {
            var strip = Scene.station.choices();
            check("polymorph furnace: competing results offered", strip != null && strip.active(),
                    "active=" + (strip != null && strip.active()));
            Scene.station.chose(0);
        });
        step(14, () -> {
            check("polymorph furnace: server choice reaches scene", Scene.station.choices().selected() == 0,
                    "selected=" + Scene.station.choices().selected());
            server(p -> check("polymorph furnace: block stores chosen recipe",
                    p.serverLevel().getBlockEntity(FURNACE) instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity furnace
                            && "cinematic_polymorph_test:c_nugget".equals(dev.dyslexic.cinematic.station.RecipeChoices.furnaceChoice(furnace, StationKind.FURNACE)),
                    "furnace selection"));
        });
        step(230, () -> server(p -> check("polymorph furnace: selected output smelted",
                furnaceItems(p, FURNACE).get(2).is(Items.GOLD_NUGGET),
                "output=" + describe(furnaceItems(p, FURNACE).get(2)))));
        step(2, () -> finish(Minecraft.getInstance()));
    }

    private static void polymorphSmithingScript() {
        step(3, () -> server(p -> {
            if (p.containerMenu instanceof net.minecraft.world.inventory.SmithingMenu menu) {
                menu.getSlot(0).set(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
                menu.getSlot(1).set(new ItemStack(Items.DIAMOND_SWORD));
                menu.getSlot(2).set(new ItemStack(Items.NETHERITE_INGOT));
                menu.slotsChanged(menu.getSlot(0).container);
                menu.broadcastChanges();
            }
        }));
        step(18, () -> {
            var strip = Scene.station.choices();
            check("polymorph smithing: competing results offered", strip != null && strip.active(),
                    "active=" + (strip != null && strip.active()));
            Scene.station.chose(0);
        });
        step(14, () -> {
            server(p -> check("polymorph smithing: selected result is in the real menu",
                    p.containerMenu.getSlot(3).getItem().is(Items.GOLDEN_SWORD),
                    "result=" + describe(p.containerMenu.getSlot(3).getItem())));
            check("polymorph smithing: scene shows selected result", Scene.station.choices().selected() == 0,
                    "selected=" + Scene.station.choices().selected());
        });
        step(2, () -> finish(Minecraft.getInstance()));
    }

    /^* The world's own data pack: a shaped recipe whose result carries a name (NBT), and a smelting recipe of its own. ^/
    private static void customRecipesAct() {
        step(2, () -> server(p -> {
            try {
                java.nio.file.Path pack = p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.DATAPACK_DIR).resolve("cinematic_test");
                java.nio.file.Path recipes = pack.resolve("data/cinematic_test/recipes");
                java.nio.file.Files.createDirectories(recipes);
                java.nio.file.Files.writeString(pack.resolve("pack.mcmeta"),
                        "{\"pack\": {\"pack_format\": 15, \"description\": \"Cinematic Interactions test recipes\"}}");
                java.nio.file.Files.writeString(recipes.resolve("named_emeralds.json"), """
                        {"type": "minecraft:crafting_shaped", "pattern": ["C", "S"],
                         "key": {"C": {"item": "minecraft:cobblestone"}, "S": {"item": "minecraft:stick"}},
                         "result": {"item": "minecraft:emerald", "count": 2, "nbt": "{display:{Name:'{\\"text\\":\\"Изумруд из датапака\\"}'}}"}}
                        """);
                java.nio.file.Files.writeString(recipes.resolve("golden_apple.json"), """
                        {"type": "minecraft:smelting", "ingredient": {"item": "minecraft:apple"},
                         "result": "minecraft:golden_apple", "experience": 0.5, "cookingtime": 40}
                        """);
            } catch (java.io.IOException e) {
                check("custom recipe pack written", false, e.toString());
            }
        }));
        step(4, () -> cmd("datapack enable \"file/cinematic_test\""));
        step(4, () -> cmd("reload"));
        step(60, () -> {
            var recipes = Minecraft.getInstance().level.getRecipeManager();
            check("custom recipes reach the client", recipes.byKey(new ResourceLocation("cinematic_test", "named_emeralds")).isPresent()
                    && recipes.byKey(new ResourceLocation("cinematic_test", "golden_apple")).isPresent(), "client recipe book after /reload");
        });
        nextAct("cr", new Vec3(0.7, -60, 5.6), 172f, TABLE, new Vec3(0.5, 1, 0.8), Direction.UP);
    }

    private static void customRecipesScript() {
        if (Scene.station != null && Scene.station.kind() == StationKind.FURNACE) {
            customSmeltingScript();
            return;
        }
        shot("cr5_table");
        step(4, () -> invClickItem(Items.COBBLESTONE));
        step(3, () -> point(0, 1));
        step(8, () -> clickCell(0, 1, 1));
        step(4, Showcase::stash);
        step(4, () -> invClickItem(Items.STICK));
        step(3, () -> point(1, 1));
        step(8, () -> clickCell(1, 1, 1));
        step(4, Showcase::stash);
        step(4, Showcase::pointAside);
        step(12, () -> shot("cr6_custom_recipe"));
        step(2, () -> pointAt(table().resultPosition()));
        step(8, () -> shot("cr7_custom_result"));
        step(2, Showcase::clickResult);
        step(30, () -> {
            ItemStack hand = carried();
            check("data pack recipe crafted on the table", hand.is(Items.EMERALD) && hand.getCount() == 2
                    && hand.getHoverName().getString().equals("Изумруд из датапака"), "carried=" + hand + " name=" + hand.getHoverName().getString());
        });
        step(2, () -> server(p -> {
            ItemStack hand = p.inventoryMenu.getCarried();
            check("server holds the named result", hand.is(Items.EMERALD) && hand.hasCustomHoverName(), "server carried=" + hand + " tag=" + hand.getTag());
        }));
        step(4, Showcase::stash);
        step(4, Scene::leave);
        step(45, () -> {});
        nextAct("cr", new Vec3(-3.2, -60, 4.6), 200f, FURNACE, new Vec3(0.5, 0.6, 1.0), Direction.SOUTH);
    }

    private static void customSmeltingScript() {
        shot("cr8_furnace");
        step(2, () -> server(p -> p.getInventory().add(new ItemStack(Items.APPLE, 2))));
        step(6, () -> invClickItem(Items.APPLE));
        step(3, () -> pointZone(FURNACE, FurnaceZones.INPUT));
        step(8, () -> shot("cr8_apple_over_input"));
        step(2, () -> clickZone(FurnaceZones.INPUT, 1));
        step(4, Showcase::stash);
        step(4, () -> invClickItem(Items.COAL));
        step(3, () -> pointZone(FURNACE, FurnaceZones.FUEL));
        step(8, () -> clickZone(FurnaceZones.FUEL, 1));
        step(4, Showcase::stash);
        step(4, Showcase::pointAside);
        step(70, () -> shot("cr9_smelted"));
        step(2, () -> server(p -> {
            ItemStack result = p.serverLevel().getBlockEntity(FURNACE) instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity furnace
                    ? furnace.getItem(FurnaceZones.RESULT) : ItemStack.EMPTY;
            check("data pack smelting in the furnace scene", result.is(Items.GOLDEN_APPLE), "result=" + describe(result));
        }));
        step(2, () -> check("watchers see the custom result in the furnace", ClientFurnaces.item(FURNACE, FurnaceZones.RESULT).is(Items.GOLDEN_APPLE),
                "client=" + describe(ClientFurnaces.item(FURNACE, FurnaceZones.RESULT))));
        step(4, Scene::leave);
        step(40, () -> finish(Minecraft.getInstance()));
    }

    // ---- a menu of another class where the station's own was expected ------------------------------------------

    /^*
     * What a mod that replaces a station's menu does (Penchant's enchanting table in NeoForge; no such mod is at hand for
     * 1.20.1): while the enchanting scene works, the server puts a menu of another class in front of the player. The scene
     * must step aside for that window at once (the client must not keep it half-made), the server must end the session,
     * and the same block must then open its own window directly, without the scene first, for the rest of the connection.
     ^/
    private static void foreignMenuAct() {
        step(2, () -> server(p -> {
            p.getInventory().add(new ItemStack(Items.IRON_SWORD));
            p.teleportTo(p.serverLevel(), -4.5, -60, 11.6, 180f, 20f);
        }));
        step(30, () -> use(ENCHANT, new Vec3(0.5, 0.75, 0.6), Direction.UP));
        step(80, () -> {
            check("the enchanting scene plays", Scene.phase == Scene.Phase.ACTIVE && Scene.station != null
                    && Scene.station.kind() == StationKind.ENCHANTING, "phase=" + Scene.phase);
            shot("fm1_scene");
            server(p -> p.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inventory, player) ->
                    net.minecraft.world.inventory.ChestMenu.threeRows(id, inventory), Component.literal("Foreign menu"))));
        });
        step(30, () -> {
            Minecraft mc = Minecraft.getInstance();
            check("the scene steps aside for the other menu", Scene.phase == Scene.Phase.NONE, "phase=" + Scene.phase);
            check("the other menu's own window is open", mc.screen instanceof net.minecraft.client.gui.screens.inventory.ContainerScreen
                    && mc.player.containerMenu instanceof net.minecraft.world.inventory.ChestMenu, "screen=" + mc.screen);
            server(p -> check("no session is left on the server", dev.dyslexic.cinematic.station.Sessions.get(p) == null,
                    "menu=" + p.containerMenu.getClass().getSimpleName()));
            shot("fm2_window");
        });
        // the slot change that used to reach a window that never opened
        step(4, () -> {
            Minecraft mc = Minecraft.getInstance();
            var menu = mc.player.containerMenu;
            for (int i = 0; i < menu.slots.size(); i++) {
                if (menu.slots.get(i).container == mc.player.getInventory() && menu.slots.get(i).getItem().is(Items.IRON_SWORD)) {
                    mc.gameMode.handleInventoryMouseClick(menu.containerId, i, 0, ClickType.QUICK_MOVE, mc.player);
                    break;
                }
            }
        });
        step(20, () -> {
            Minecraft mc = Minecraft.getInstance();
            var menu = mc.player.containerMenu;
            boolean placed = false;
            for (var slot : menu.slots) placed |= slot.container != mc.player.getInventory() && slot.getItem().is(Items.IRON_SWORD);
            check("the window works on (an item went in)", placed && mc.screen instanceof net.minecraft.client.gui.screens.inventory.ContainerScreen,
                    "screen=" + mc.screen);
            mc.screen.onClose();
        });
        step(10, () -> server(p -> {
            p.getInventory().clearOrCountMatchingItems(st -> st.is(Items.IRON_SWORD), -1, p.inventoryMenu.getCraftSlots());
            check("the server closed it", p.containerMenu == p.inventoryMenu, "menu=" + p.containerMenu.getClass().getSimpleName());
        }));
        // used again: this kind of block keeps its own window for the rest of the connection
        step(10, () -> use(ENCHANT, new Vec3(0.5, 0.75, 0.6), Direction.UP));
        step(30, () -> {
            Minecraft mc = Minecraft.getInstance();
            check("used again: its own window, no scene first", Scene.phase == Scene.Phase.NONE
                    && mc.screen instanceof net.minecraft.client.gui.screens.inventory.EnchantmentScreen, "screen=" + mc.screen + " phase=" + Scene.phase);
            shot("fm3_own_window");
            closeWindow();
        });
        step(10, () -> finish(Minecraft.getInstance()));
    }

    *///?}
    private static void quickMoveAct() {
        // the table: the recipe's own cell, then a cell with the same item, then the first free one, as the player reads the grid
        quickStation("table", p -> {
            TableStore store = TableStore.get(p.serverLevel());
            store.remove(TABLE);
            p.getInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 64));
            p.getInventory().setItem(9, new ItemStack(Items.OAK_PLANKS, 10));
            p.getInventory().setItem(10, new ItemStack(Items.OAK_PLANKS, 60));
            p.getInventory().setItem(11, new ItemStack(Items.IRON_SWORD));
            p.getInventory().setItem(12, new ItemStack(Items.STICK, 5));
        }, enterAt(new Vec3(0.7, -60, 5.6), 172f, TABLE, new Vec3(0.5, 1, 0.8), Direction.UP), 90,
                p -> { TableStore.Table t = TableStore.get(p.serverLevel()).get(TABLE); return t == null ? "[]" : describeAll(t.items); }, List.of(
                new QuickCase("a whole stack from the hotbar lands on the first cell", 0,
                        p -> inv(p, 0).isEmpty() && holds(tableCell(p, tableWorld(0)), Items.OAK_PLANKS, 64)),
                new QuickCase("a full cell is passed over for the next free one (pack)", p -> {}, 9,
                        p -> inv(p, 9).isEmpty() && holds(tableCell(p, tableWorld(1)), Items.OAK_PLANKS, 10), 16),
                new QuickCase("a partial stack tops up the cell with the same item, the rest stays", p -> {}, 10,
                        p -> holds(inv(p, 10), Items.OAK_PLANKS, 6) && holds(tableCell(p, tableWorld(1)), Items.OAK_PLANKS, 64), 16),
                new QuickCase("a full table takes nothing", q -> {
                    TableStore.Table t = TableStore.get(q.serverLevel()).getOrCreate(TABLE);
                    for (int i = 0; i < TableGrid.CELLS; i++) if (t.items.get(i).isEmpty()) t.items.set(i, new ItemStack(Items.DIRT, 64));
                }, 11, p -> holds(inv(p, 11), Items.IRON_SWORD, 1), 16)));
        // with a recipe chosen, the stick goes where its diagram wants it
        quickStation("table recipe", p -> {
            TableStore.get(p.serverLevel()).remove(TABLE);
            p.getInventory().setItem(12, new ItemStack(Items.STICK, 5));
            p.getInventory().setItem(13, new ItemStack(Items.COAL, 4));
        }, () -> {
            enterAt(new Vec3(0.7, -60, 5.6), 172f, TABLE, new Vec3(0.5, 1, 0.8), Direction.UP).run();
            step(95, () -> {
                var entry = table().guide.entries.stream().filter(e -> e.holder.id().getPath().equals("torch")).findFirst();
                entry.ifPresent(table().recipeBook::select);
            });
        }, 100, p -> { TableStore.Table t = TableStore.get(p.serverLevel()).get(TABLE); return t == null ? "[]" : describeAll(t.items); }, List.of(
                new QuickCase("the stick goes to the diagram's stick cell", p -> {}, 12, p -> {
                    var guide = table().guide;
                    for (int l = 0; l < 9; l++)
                        if (!guide.grid[l].isEmpty() && guide.grid[l].test(new ItemStack(Items.STICK)))
                            return holds(tableCell(p, tableWorld(l)), Items.STICK, 5) && inv(p, 12).isEmpty();
                    return false;
                }, 16),
                new QuickCase("the coal to the diagram's coal cell", p -> {}, 13, p -> {
                    var guide = table().guide;
                    for (int l = 0; l < 9; l++)
                        if (!guide.grid[l].isEmpty() && guide.grid[l].test(new ItemStack(Items.COAL)))
                            return holds(tableCell(p, tableWorld(l)), Items.COAL, 4) && inv(p, 13).isEmpty();
                    return false;
                }, 16)));
        step(2, () -> server(p -> TableStore.get(p.serverLevel()).remove(TABLE)));

        quickFurnace("furnace", FURNACE, new Vec3(-3.2, -60, 4.6), 200f, new ItemStack(Items.RAW_IRON), new ItemStack(Items.DIRT, 5));
        quickFurnace("smoker", SMOKER, new Vec3(2.5, -60, 5.4), 180f, new ItemStack(Items.BEEF), new ItemStack(Items.RAW_IRON, 5));
        quickFurnace("blast furnace", BLAST, new Vec3(4.5, -60, 5.4), 180f, new ItemStack(Items.RAW_IRON), new ItemStack(Items.BEEF, 5));

        quickStation("enchanting table", p -> {
            p.getInventory().setItem(0, new ItemStack(Items.LAPIS_LAZULI, 10));
            p.getInventory().setItem(9, new ItemStack(Items.IRON_SWORD));
            p.getInventory().setItem(10, new ItemStack(Items.IRON_PICKAXE));
        }, enterAt(new Vec3(-4.5, -60, 11.6), 180f, ENCHANT, new Vec3(0.5, 0.75, 0.6), Direction.UP), 90, p -> describeAll(menuItems(p)), List.of(
                new QuickCase("lapis into its own place", 0, p -> inv(p, 0).isEmpty() && holds(menuItems(p).get(1), Items.LAPIS_LAZULI, 10)),
                new QuickCase("the sword onto the book", 9, p -> inv(p, 9).isEmpty() && holds(menuItems(p).get(0), Items.IRON_SWORD, 1)),
                new QuickCase("a second tool finds the place taken and stays put", 10, p -> holds(inv(p, 10), Items.IRON_PICKAXE, 1))));

        quickStation("anvil", p -> {
            p.getInventory().setItem(0, new ItemStack(Items.IRON_SWORD));
            p.getInventory().setItem(9, new ItemStack(Items.IRON_INGOT, 5));
            p.getInventory().setItem(10, new ItemStack(Items.STICK, 3));
        }, enterSmithy(ANVIL), 90, p -> describeAll(menuItems(p)), List.of(
                new QuickCase("the sword onto the anvil", 0, p -> inv(p, 0).isEmpty() && holds(menuItems(p).get(0), Items.IRON_SWORD, 1)),
                new QuickCase("the ingots beside it", 9, p -> inv(p, 9).isEmpty() && holds(menuItems(p).get(1), Items.IRON_INGOT, 5)),
                new QuickCase("a full anvil takes nothing more", 10, p -> holds(inv(p, 10), Items.STICK, 3))));

        quickStation("grindstone", p -> {
            ItemStack worn = new ItemStack(Items.IRON_SWORD);
            worn.setDamageValue(100);
            p.getInventory().setItem(0, worn);
            p.getInventory().setItem(9, new ItemStack(Items.DIRT, 4));
        }, enterSmithy(GRINDSTONE), 90, p -> describeAll(menuItems(p)), List.of(
                new QuickCase("a worn sword onto the wheel", 0, p -> inv(p, 0).isEmpty() && menuItems(p).get(0).is(Items.IRON_SWORD)),
                new QuickCase("dirt does not go on a grindstone", 9, p -> holds(inv(p, 9), Items.DIRT, 4))));

        quickStation("smithing table", p -> {
            p.getInventory().setItem(0, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 2));
            p.getInventory().setItem(9, new ItemStack(Items.DIAMOND_SWORD));
            p.getInventory().setItem(10, new ItemStack(Items.DIRT, 4));
        }, enterSmithy(SMITHING), 90, p -> describeAll(menuItems(p)), List.of(
                new QuickCase("the template into its place", 0, p -> inv(p, 0).isEmpty() && holds(menuItems(p).get(0), Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 2)),
                new QuickCase("the sword onto it", 9, p -> inv(p, 9).isEmpty() && holds(menuItems(p).get(1), Items.DIAMOND_SWORD, 1)),
                new QuickCase("dirt stays put", 10, p -> holds(inv(p, 10), Items.DIRT, 4))));

        quickStation("stonecutter", p -> {
            p.getInventory().setItem(0, new ItemStack(Items.STONE, 12));
            p.getInventory().setItem(9, new ItemStack(Items.DIRT, 4));
        }, enterSmithy(STONECUTTER), 90, p -> describeAll(menuItems(p)), List.of(
                new QuickCase("dirt has nothing to be cut into: it stays", 9, p -> holds(inv(p, 9), Items.DIRT, 4)),
                new QuickCase("stone onto the saw", 0, p -> inv(p, 0).isEmpty() && holds(menuItems(p).get(0), Items.STONE, 12))));

        quickStation("loom", p -> {
            p.getInventory().setItem(0, new ItemStack(Items.WHITE_BANNER));
            p.getInventory().setItem(9, new ItemStack(Items.RED_DYE, 4));
            p.getInventory().setItem(10, new ItemStack(Items.DIRT, 4));
        }, enterSmithy(LOOM), 90, p -> describeAll(menuItems(p)), List.of(
                new QuickCase("the banner onto the loom", 0, p -> inv(p, 0).isEmpty() && holds(menuItems(p).get(0), Items.WHITE_BANNER, 1)),
                new QuickCase("the dye beside it", 9, p -> inv(p, 9).isEmpty() && holds(menuItems(p).get(1), Items.RED_DYE, 4)),
                new QuickCase("dirt stays put", 10, p -> holds(inv(p, 10), Items.DIRT, 4))));

        quickStation("cartography table", p -> {
            p.getInventory().setItem(0, new ItemStack(Items.PAPER, 3));
            p.getInventory().setItem(9, new ItemStack(Items.DIRT, 4));
        }, enterSmithy(CARTOGRAPHY), 90, p -> describeAll(menuItems(p)), List.of(
                new QuickCase("paper into its place", 0, p -> inv(p, 0).isEmpty() && holds(menuItems(p).get(1), Items.PAPER, 3)),
                new QuickCase("dirt stays put", 9, p -> holds(inv(p, 9), Items.DIRT, 4))));

        quickStation("brewing stand", p -> {
            if (p.serverLevel().getBlockEntity(BREWING) instanceof net.minecraft.world.level.block.entity.BrewingStandBlockEntity stand)
                for (int i = 0; i < 5; i++) stand.setItem(i, ItemStack.EMPTY);
            //? if >=1.21 {
            ItemStack water = net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION, net.minecraft.world.item.alchemy.Potions.WATER);
            //?} else if <1.21 {
            /*ItemStack water = net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(Items.POTION), net.minecraft.world.item.alchemy.Potions.WATER);
            *///?}
            p.getInventory().setItem(0, new ItemStack(Items.BLAZE_POWDER, 3));
            p.getInventory().setItem(9, new ItemStack(Items.NETHER_WART, 2));
            p.getInventory().setItem(10, water);
            p.getInventory().setItem(11, new ItemStack(Items.DIRT, 4));
        }, enterSmithy(BREWING), 90, p -> describeAll(menuItems(p)), List.of(
                // the stand takes one powder as fuel straight away
                new QuickCase("blaze powder to the foot", 0, p -> inv(p, 0).isEmpty() && menuItems(p).get(4).is(Items.BLAZE_POWDER)
                        && menuItems(p).get(4).getCount() >= 2),
                new QuickCase("nether wart into the bowl", 9, p -> inv(p, 9).isEmpty() && holds(menuItems(p).get(3), Items.NETHER_WART, 2)),
                new QuickCase("a bottle onto a foot", 10, p -> inv(p, 10).isEmpty() && menuItems(p).get(0).is(Items.POTION)),
                new QuickCase("dirt stays put", 11, p -> holds(inv(p, 11), Items.DIRT, 4))));

        //? if >=1.21 {
        quickStation("crafter", p -> {
            p.getInventory().setItem(9, new ItemStack(Items.OAK_PLANKS, 7));
        }, enterSmithy(CRAFTER), 90, p -> describeAll(menuItems(p)), List.of(
                new QuickCase("planks into the crafter's grid", 9, p -> inv(p, 9).isEmpty()
                        && menuItems(p).stream().anyMatch(st -> st.is(Items.OAK_PLANKS)))));

        //?}
        quickStation("beacon", p -> {
            p.getInventory().setItem(0, new ItemStack(Items.IRON_INGOT, 5));
            p.getInventory().setItem(9, new ItemStack(Items.DIRT, 4));
        }, enterAt(new Vec3(BEACON.getX() + 0.5, BEACON.getY(), BEACON.getZ() + 1.75), 180f, BEACON, new Vec3(0.5, 0.9, 0.5), Direction.UP), 120,
                p -> describeAll(menuItems(p)), List.of(
                new QuickCase("dirt is no payment: it stays", 9, p -> holds(inv(p, 9), Items.DIRT, 4)),
                new QuickCase("one ingot onto the beacon, the rest stays", 0, p -> holds(inv(p, 0), Items.IRON_INGOT, 4) && holds(menuItems(p).get(0), Items.IRON_INGOT, 1))));

        quickStation("lectern", p -> {
            p.getInventory().setItem(9, new ItemStack(Items.WRITABLE_BOOK));
        }, enterAt(new Vec3(LECTERN.getX() + 0.5, -60, LECTERN.getZ() + 2.6), 180f, LECTERN, new Vec3(0.5, 0.9, 0.6), Direction.UP), 110,
                p -> describeAll(menuItems(p)), List.of(
                new QuickCase("a lectern holding its book takes no other", 9, p -> holds(inv(p, 9), Items.WRITABLE_BOOK, 1))));

        quickStation("chiseled bookshelf", p -> {
            if (p.serverLevel().getBlockEntity(SHELF) instanceof net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity shelf) {
                //? if >=1.21 {
                for (int i = 0; i < 6; i++) shelf.setItem(i, i == 1 ? new ItemStack(Items.BOOK) : ItemStack.EMPTY);
                //?} else if <1.21 {
                /*// 1.20.1's setItem ignores anything but a book, an empty stack too: empty the places by taking the books out
                for (int i = 0; i < 6; i++) if (i == 1) shelf.setItem(i, new ItemStack(Items.BOOK)); else shelf.removeItem(i, 1);
                *///?}
            }
            p.getInventory().setItem(9, new ItemStack(Items.BOOK, 3));
            p.getInventory().setItem(10, new ItemStack(Items.DIRT, 4));
        }, enterAt(new Vec3(SHELF.getX() + 0.5, -60, SHELF.getZ() + 3.0), 180f, SHELF, SHELF_SLOTS[1], Direction.SOUTH), 90, p -> {
            List<ItemStack> books = new ArrayList<>();
            if (p.serverLevel().getBlockEntity(SHELF) instanceof net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity shelf)
                for (int i = 0; i < 6; i++) books.add(shelf.getItem(i));
            return describeAll(books);
        }, List.of(
                new QuickCase("dirt is no book: it stays", 10, p -> holds(inv(p, 10), Items.DIRT, 4)),
                new QuickCase("a book from the pack into the first empty place", p -> {}, 9, p -> holds(inv(p, 9), Items.BOOK, 2)
                        && p.serverLevel().getBlockEntity(SHELF) instanceof net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity shelf
                        && shelf.getItem(0).is(Items.BOOK), 30),
                new QuickCase("the next one skips the taken places", p -> {}, 9, p -> holds(inv(p, 9), Items.BOOK, 1)
                        && p.serverLevel().getBlockEntity(SHELF) instanceof net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity shelf
                        && shelf.getItem(2).is(Items.BOOK), 30),
                new QuickCase("a full shelf takes nothing", q -> {
                    if (q.serverLevel().getBlockEntity(SHELF) instanceof net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity shelf)
                        for (int i = 0; i < 6; i++) if (shelf.getItem(i).isEmpty()) shelf.setItem(i, new ItemStack(Items.BOOK));
                }, 9, p -> holds(inv(p, 9), Items.BOOK, 1), 30)));

        quickStation("campfire", p -> {
            if (p.serverLevel().getBlockEntity(CAMPFIRE) instanceof net.minecraft.world.level.block.entity.CampfireBlockEntity fire) {
                fire.getItems().set(0, new ItemStack(Items.CHICKEN));
                // a spot with no cooking time is done at once in vanilla's tick: give the chicken its time
                fire.cookingTime[0] = 600;
                fire.cookingProgress[0] = 0;
                for (int i = 1; i < 4; i++) fire.getItems().set(i, ItemStack.EMPTY);
                fire.setChanged();
                p.serverLevel().sendBlockUpdated(CAMPFIRE, fire.getBlockState(), fire.getBlockState(), 3);
            }
            p.getInventory().setItem(0, new ItemStack(Items.BEEF, 5));
            p.getInventory().setItem(9, new ItemStack(Items.DIRT, 4));
        }, () -> {
            step(2, () -> server(p -> p.teleportTo(p.serverLevel(), CAMPFIRE.getX() + 0.5, -60, CAMPFIRE.getZ() + 2.9, 180f, 24f)));
            //? if neoforge {
            step(30, () -> net.minecraft.client.KeyMapping.click(CampfireHint.SIT.getKey()));
            //?} else if fabric {
            /*step(30, () -> net.minecraft.client.KeyMapping.click(net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.getBoundKeyOf(CampfireHint.SIT)));
            *///?} else if forge {
            /*step(30, () -> net.minecraft.client.KeyMapping.click(CampfireHint.SIT.getKey()));
            *///?}
        }, 120, p -> p.serverLevel().getBlockEntity(CAMPFIRE) instanceof net.minecraft.world.level.block.entity.CampfireBlockEntity fire
                ? describeAll(fire.getItems()) : "-", List.of(
                new QuickCase("dirt does not cook: it stays", 9, p -> holds(inv(p, 9), Items.DIRT, 4)),
                new QuickCase("beef, one piece to each free spot, the rest stays", 0, p -> holds(inv(p, 0), Items.BEEF, 2)
                        && p.serverLevel().getBlockEntity(CAMPFIRE) instanceof net.minecraft.world.level.block.entity.CampfireBlockEntity fire
                        && fire.getItems().stream().filter(st -> st.is(Items.BEEF)).count() == 3 && fire.getItems().get(0).is(Items.CHICKEN))));

        quickStation("jukebox", p -> {
            //? if >=1.21 {
            if (p.serverLevel().getBlockEntity(JUKEBOX) instanceof net.minecraft.world.level.block.entity.JukeboxBlockEntity box) box.popOutTheItem();
            //?} else if <1.21 {
            /*if (p.serverLevel().getBlockEntity(JUKEBOX) instanceof net.minecraft.world.level.block.entity.JukeboxBlockEntity box) box.popOutRecord();
            *///?}
            p.getInventory().setItem(9, new ItemStack(Items.MUSIC_DISC_CAT));
            p.getInventory().setItem(10, new ItemStack(Items.DIRT, 4));
        }, () -> {
            step(2, () -> standLooking(new Vec3(JUKEBOX.getX() - 1.6, -60, JUKEBOX.getZ() + 0.5), Vec3.atCenterOf(JUKEBOX)));
            step(20, () -> useAt(JUKEBOX, new Vec3(0.5, 1, 0.5), Direction.UP));
        //? if >=1.21 {
        }, 90, p -> p.serverLevel().getBlockEntity(JUKEBOX) instanceof net.minecraft.world.level.block.entity.JukeboxBlockEntity box
                ? describe(box.getTheItem()) : "-", List.of(
        //?} else if <1.21 {
        /*}, 90, p -> p.serverLevel().getBlockEntity(JUKEBOX) instanceof net.minecraft.world.level.block.entity.JukeboxBlockEntity box
                ? describe(box.getFirstItem()) : "-", List.of(
        *///?}
                new QuickCase("dirt is no record: it stays", 10, p -> holds(inv(p, 10), Items.DIRT, 4)),
                new QuickCase("a record from the pack slides in and plays", p -> {}, 9, p -> inv(p, 9).isEmpty()
                        && p.serverLevel().getBlockEntity(JUKEBOX) instanceof net.minecraft.world.level.block.entity.JukeboxBlockEntity box
                        //? if >=1.21 {
                        && box.getTheItem().is(Items.MUSIC_DISC_CAT), 50)));
                        //?} else if <1.21 {
                        /*&& box.getFirstItem().is(Items.MUSIC_DISC_CAT), 50)));
                        *///?}
        step(2, () -> server(p -> {
            //? if >=1.21 {
            if (p.serverLevel().getBlockEntity(JUKEBOX) instanceof net.minecraft.world.level.block.entity.JukeboxBlockEntity box) box.popOutTheItem();
            //?} else if <1.21 {
            /*if (p.serverLevel().getBlockEntity(JUKEBOX) instanceof net.minecraft.world.level.block.entity.JukeboxBlockEntity box) box.popOutRecord();
            *///?}
        }));
        step(10, () -> finish(Minecraft.getInstance()));
    }

    // ---- the anvil's rules: an armour-only book on a sword, in the scene and in the plain window -----------

    private static final Map<String, String> ANVIL_RESULTS = new java.util.LinkedHashMap<>();

    private static boolean hasThorns(ItemStack stack) {
        //? if >=1.21 {
        var thorns = Minecraft.getInstance().level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.THORNS);
        //?} else if <1.21 {
        /*var thorns = net.minecraft.world.item.enchantment.Enchantments.THORNS;
        *///?}
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(thorns, stack) > 0;
    }

    private static void anvilRound(boolean creative, boolean scene) {
        String tag = (creative ? "creative" : "survival") + (scene ? " scene" : " window");
        step(4, () -> {
            //? if >=1.21 {
            Settings.SCENES.get(StationKind.ANVIL).set(scene);
            //?} else if <1.21 {
            /*Settings.set(Settings.SCENES.get(StationKind.ANVIL), scene);
            *///?}
            server(p -> {
                p.setGameMode(creative ? net.minecraft.world.level.GameType.CREATIVE : net.minecraft.world.level.GameType.SURVIVAL);
                p.setExperienceLevels(30);
                p.getInventory().clearOrCountMatchingItems(st -> st.is(Items.IRON_SWORD) || st.is(Items.ENCHANTED_BOOK), -1, p.inventoryMenu.getCraftSlots());
                //? if >=1.21 {
                var thorns = p.serverLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                        .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.THORNS);
                //?} else if <1.21 {
                /*var thorns = net.minecraft.world.item.enchantment.Enchantments.THORNS;
                *///?}
                p.getInventory().add(new ItemStack(Items.IRON_SWORD));
                p.getInventory().add(net.minecraft.world.item.EnchantedBookItem.createForEnchantment(
                        new net.minecraft.world.item.enchantment.EnchantmentInstance(thorns, 3)));
                p.teleportTo(p.serverLevel(), ANVIL.getX() + 0.5, -60, ANVIL.getZ() + 3.4, 180f, 20f);
            });
        });
        step(20, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atLowerCornerOf(ANVIL).add(0.5, 1.0, 0.5), Direction.UP, ANVIL, false));
        });
        step(scene ? 80 : 20, () -> {
            Minecraft mc = Minecraft.getInstance();
            boolean open = mc.player.containerMenu instanceof net.minecraft.world.inventory.AnvilMenu
                    && (scene ? Scene.phase == Scene.Phase.ACTIVE : mc.screen instanceof net.minecraft.client.gui.screens.inventory.AnvilScreen);
            check(tag + ": the anvil is open", open, "phase=" + Scene.phase + " screen=" + mc.screen);
            // sword first, then the book: as the scene and the window both put them, item first
            var menu = mc.player.containerMenu;
            for (net.minecraft.world.item.Item item : List.of(Items.IRON_SWORD, Items.ENCHANTED_BOOK)) {
                for (int i = 0; i < menu.slots.size(); i++) {
                    if (menu.slots.get(i).container == mc.player.getInventory() && menu.slots.get(i).getItem().is(item)) {
                        mc.gameMode.handleInventoryMouseClick(menu.containerId, i, 0, ClickType.QUICK_MOVE, mc.player);
                        break;
                    }
                }
            }
        });
        step(12, () -> {
            Minecraft mc = Minecraft.getInstance();
            ItemStack client = mc.player.containerMenu.getSlot(2).getItem().copy();
            server(p -> {
                ItemStack result = p.containerMenu instanceof net.minecraft.world.inventory.AnvilMenu menu ? menu.getSlot(2).getItem() : ItemStack.EMPTY;
                int cost = p.containerMenu instanceof net.minecraft.world.inventory.AnvilMenu menu ? menu.getCost() : -1;
                String line = (result.isEmpty() ? "no result" : describe(result) + (hasThorns(result) ? " with Thorns" : " without Thorns")) + ", cost " + cost;
                ANVIL_RESULTS.put(tag, line);
                REPORT.add("anvil " + tag + ": server " + line + "; client shows " + (client.isEmpty() ? "nothing" : describe(client)
                        + (hasThorns(client) ? " with Thorns" : "")) + "; mods " + loadedEnchantMods());
            });
            shot("av_" + (creative ? "creative" : "survival") + "_" + (scene ? "scene" : "window"));
        });
        step(4, () -> {
            if (scene) Scene.leave();
            else if (Minecraft.getInstance().screen != null) Minecraft.getInstance().screen.onClose();
        });
        step(scene ? 50 : 10, () -> {});
    }

    private static String loadedEnchantMods() {
        List<String> found = new ArrayList<>();
        for (String id : List.of("penchant", "apotheosis", "apothic_enchanting", "enchdesc", "easymagic", "enchantmentlevelcapindicator"))
            //? if neoforge {
            if (net.neoforged.fml.ModList.get().isLoaded(id)) found.add(id);
            //?} else if fabric {
            /*if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(id)) found.add(id);
            *///?} else if forge {
            /*if (net.minecraftforge.fml.ModList.get().isLoaded(id)) found.add(id);
            *///?}
        return found.isEmpty() ? "none" : String.join(",", found);
    }

    private static void anvilRulesAct() {
        for (boolean creative : new boolean[]{false, true}) {
            anvilRound(creative, true);
            anvilRound(creative, false);
        }
        step(4, () -> {
            for (String mode : List.of("survival", "creative")) {
                String inScene = ANVIL_RESULTS.get(mode + " scene"), inWindow = ANVIL_RESULTS.get(mode + " window");
                check("anvil, " + mode + ": the scene gives what the plain window gives", inScene != null && inScene.equals(inWindow),
                        "scene=" + inScene + " | window=" + inWindow);
            }
            //? if >=1.21 {
            Settings.SCENES.get(StationKind.ANVIL).set(true);
            //?} else if <1.21 {
            /*Settings.set(Settings.SCENES.get(StationKind.ANVIL), true);
            *///?}
            server(p -> p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL));
        });
        step(4, () -> finish(Minecraft.getInstance()));
    }

    // ---- a mod's own enchanting menu (Penchant): its window opens, the scene steps aside ---------------------

    private static boolean penchantWindow() {
        Screen screen = Minecraft.getInstance().screen;
        return screen != null && screen.getClass().getName().startsWith("archives.tater.penchant");
    }

    private static void penchantAct() {
        //? if neoforge {
        boolean penchant = net.neoforged.fml.ModList.get().isLoaded("penchant");
        //?} else if fabric {
        /*boolean penchant = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("penchant");
        *///?} else if forge {
        /*boolean penchant = net.minecraftforge.fml.ModList.get().isLoaded("penchant");
        *///?}
        step(2, () -> server(p -> {
            p.getInventory().add(new ItemStack(Items.IRON_SWORD));
            p.teleportTo(p.serverLevel(), -4.5, -60, 11.6, 180f, 20f);
        }));
        step(30, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atLowerCornerOf(ENCHANT).add(0.5, 0.75, 0.6), Direction.UP, ENCHANT, false));
        });
        if (!penchant) {
            // the scene plays as before
            step(60, () -> check("without Penchant the enchanting scene plays", Scene.phase != Scene.Phase.NONE
                    && Scene.station != null && Scene.station.kind() == StationKind.ENCHANTING, "phase=" + Scene.phase));
            step(4, Scene::leave);
            step(40, () -> finish(Minecraft.getInstance()));
            return;
        }
        step(30, () -> {
            Minecraft mc = Minecraft.getInstance();
            check("Penchant's own window opens at the enchanting table", penchantWindow(), "screen=" + mc.screen);
            check("the scene stepped aside for it", Scene.phase == Scene.Phase.NONE, "phase=" + Scene.phase);
            server(p -> {
                check("no session is left on the server", dev.dyslexic.cinematic.station.Sessions.get(p) == null, "session");
                check("the server keeps Penchant's menu open", p.containerMenu.getClass().getName().startsWith("archives.tater.penchant"),
                        "menu=" + p.containerMenu.getClass().getSimpleName());
            });
            shot("pn1_penchant_window");
        });
        // a sword into the table: the slot change that called into a window that never opened
        step(4, () -> {
            Minecraft mc = Minecraft.getInstance();
            var menu = mc.player.containerMenu;
            for (int i = 0; i < menu.slots.size(); i++) {
                if (menu.slots.get(i).container == mc.player.getInventory() && menu.slots.get(i).getItem().is(Items.IRON_SWORD)) {
                    mc.gameMode.handleInventoryMouseClick(menu.containerId, i, 0, ClickType.QUICK_MOVE, mc.player);
                    break;
                }
            }
        });
        step(20, () -> {
            Minecraft mc = Minecraft.getInstance();
            var menu = mc.player.containerMenu;
            boolean placed = false;
            for (var slot : menu.slots) placed |= slot.container != mc.player.getInventory() && slot.getItem().is(Items.IRON_SWORD);
            check("the sword went into Penchant's table and its window lives on", placed && penchantWindow(), "screen=" + mc.screen);
            shot("pn2_sword_in");
            mc.screen.onClose();
        });
        step(10, () -> {
            Minecraft mc = Minecraft.getInstance();
            check("closing gives the sword back", mc.screen == null && mc.player.containerMenu == mc.player.inventoryMenu
                    && mc.player.getInventory().countItem(Items.IRON_SWORD) == 1, "screen=" + mc.screen);
            server(p -> check("the server closed it too", p.containerMenu == p.inventoryMenu && dev.dyslexic.cinematic.station.Sessions.get(p) == null, "menu"));
        });
        // used again: straight to its window, no scene starting first
        step(10, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atLowerCornerOf(ENCHANT).add(0.5, 0.75, 0.6), Direction.UP, ENCHANT, false));
            check("used again, no scene starts", Scene.phase == Scene.Phase.NONE, "phase=" + Scene.phase);
        });
        step(20, () -> {
            check("Penchant's window opens again", penchantWindow(), "screen=" + Minecraft.getInstance().screen);
            if (penchantWindow()) Minecraft.getInstance().screen.onClose();
        });
        step(10, () -> server(p -> check("nothing left open", p.containerMenu == p.inventoryMenu && dev.dyslexic.cinematic.station.Sessions.get(p) == null, "menu")));
        step(4, () -> finish(Minecraft.getInstance()));
    }

    // ---- the field guide with JEI (and JER / JEP when installed) -------------------------------------

    private static double devSpineY;

    private static FieldGuide guide() {
        return Minecraft.getInstance().screen instanceof SceneScreen screen ? screen.guide : null;
    }

    private static void typeText(String text) {
        for (char c : text.toCharArray()) Minecraft.getInstance().screen.charTyped(c, 0);
    }

    private static boolean viewerOpen() {
        Screen screen = Minecraft.getInstance().screen;
        return screen != null && !(screen instanceof SceneScreen) && screen.getClass().getName().startsWith("mezz.jei");
    }

    private static void guideFind(String query, net.minecraft.world.item.Item item) {
        step(3, () -> { double[] at = guide().searchPoint(); pointGui(at[0], at[1]); screenClick(0); });
        step(3, () -> {
            double[] at = guide().searchPoint();
            pointGui(at[0], at[1]);
            // EditBox reads Ctrl from the keyboard itself, so the old query goes key by key
            for (int i = 0; i < 24; i++) Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_BACKSPACE, 0, 0);
            typeText(query);
        });
        step(6, () -> {
            int index = guide().indexOf(item);
            check("guide finds " + query, index >= 0 && index < 24, "index=" + index + " items=" + guide().itemCount());
            if (index >= 0) { double[] at = guide().entryPoint(index); pointGui(at[0], at[1]); }
        });
        step(3, () -> screenClick(0));
    }

    private static void guideButton(String key) {
        double[] at = guide().buttonPoint(key);
        check("guide button " + key, at != null, "drawn");
        if (at != null) { pointGui(at[0], at[1]); screenClick(0); }
    }

    private static void fieldGuideScript() {
        if (dev.dyslexic.cinematic.client.guide.RecipeViewers.get() == null) {
            // without JEI there is no guide: the scenes and the table's own reference work as before
            step(10, () -> {
                check("no recipe viewer: no field guide", guide() == null || !guide().present(), "absent");
                check("no recipe viewer: the table's reference is still there", Scene.station instanceof TableStation t
                        && t.guide.entries.size() > 100, "entries");
            });
            step(3, () -> finish(Minecraft.getInstance()));
            return;
        }
        if (Scene.station instanceof TableStation) {
            // developer mode puts the layout editor's gear on the guide's spot: the spine moves above it, and back
            boolean originalDev = Settings.developerMode();
            //? if >=1.21 {
            step(10, () -> { Settings.DEVELOPER_MODE.set(true); pointAside(); });
            //?} else if <1.21 {
            /*step(10, () -> { Settings.set(Settings.DEVELOPER_MODE, true); pointAside(); });
            *///?}
            step(6, () -> {
                double[] spine = guide().togglePoint(), gear = table().gridEditor.togglePoint();
                check("developer mode: the guide sits above the gear", gear[1] - spine[1] >= 20 && Math.abs(gear[0] - spine[0]) < 2,
                        "spine=" + Math.round(spine[1]) + " gear=" + Math.round(gear[1]));
                devSpineY = spine[1];
                pointGui(spine[0], spine[1]);
            });
            step(4, () -> { shot("ji0_dev_toggles"); screenClick(0); });
            step(16, () -> {
                check("developer mode: the spine opens the guide, not the editor", guide().opened() && !table().gridEditor.visible(), "opened");
                double[] at = guide().closePoint();
                pointGui(at[0], at[1]);
            });
            step(3, () -> screenClick(0));
            step(16, () -> {
                //? if >=1.21 {
                Settings.DEVELOPER_MODE.set(originalDev);
                //?} else if <1.21 {
                /*Settings.set(Settings.DEVELOPER_MODE, originalDev);
                *///?}
                pointAside();
            });
            step(4, () -> {
                double[] spine = guide().togglePoint(), gear = table().gridEditor.togglePoint();
                // the gear is hidden now; its spot is where the spine always stood
                check("without developer mode the guide is back in place", !guide().opened() && spine[1] - devSpineY >= 20
                        && Math.abs(spine[1] - gear[1]) < 3, "spine=" + Math.round(spine[1]) + " before=" + Math.round(devSpineY));
                shot("ji0_plain_toggle");
            });
            step(10, () -> { double[] at = guide().togglePoint(); pointGui(at[0], at[1]); shot("ji0_toggle"); screenClick(0); });
            step(16, () -> check("guide opens beside the table", guide().opened(), "opened"));
            guideFind("torch", Items.TORCH);
            step(8, () -> {
                check("guide: torch recipes at the table", guide().recipeCount() >= 1, "recipes=" + guide().recipeCount());
                pointAside();
                shot("ji1_guide_recipe");
            });
            step(3, () -> guideButton("show_table"));
            step(12, () -> {
                var chosen = table().guide.selected;
                check("guide shows the torch on the tabletop", chosen != null && chosen.holder.id().getPath().equals("torch"),
                        "selected=" + (chosen == null ? "-" : chosen.holder.id()));
                server(p -> check("looking a recipe up moves nothing", TableStore.get(p.serverLevel()).get(TABLE) == null
                        || TableStore.get(p.serverLevel()).get(TABLE).items.stream().allMatch(ItemStack::isEmpty), "table empty"));
                pointAside();
                shot("ji2_table_hint");
            });
            step(3, () -> guideButton("how_to_get"));
            step(14, () -> {
                check("JEI's page opens over the scene, which holds", viewerOpen() && Scene.phase == Scene.Phase.ACTIVE && Scene.consulting,
                        "screen=" + Minecraft.getInstance().screen + " phase=" + Scene.phase);
                server(p -> check("the session stays while JEI is open", dev.dyslexic.cinematic.station.Sessions.get(p) != null, "session"));
                shot("ji3_jei_page");
                if (viewerOpen()) Minecraft.getInstance().screen.onClose();
            });
            step(10, () -> {
                check("back from JEI to the work", Minecraft.getInstance().screen instanceof SceneScreen && Scene.phase == Scene.Phase.ACTIVE
                        && !Scene.consulting, "screen=" + Minecraft.getInstance().screen);
                pointAside();
                shot("ji4_back");
            });
            // JEI's own "show recipe" key over an entry, pressed through the game's keyboard handler as a real key is:
            // JEI's screen listener and the guide must not both open a page
            step(4, () -> { double[] at = guide().entryPoint(guide().indexOf(Items.TORCH)); pointGui(at[0], at[1]); });
            step(4, () -> {
                Minecraft mc = Minecraft.getInstance();
                int scan = GLFW.glfwGetKeyScancode(GLFW.GLFW_KEY_R);
                mc.keyboardHandler.keyPress(mc.getWindow().getWindow(), GLFW.GLFW_KEY_R, scan, GLFW.GLFW_PRESS, 0);
                mc.keyboardHandler.keyPress(mc.getWindow().getWindow(), GLFW.GLFW_KEY_R, scan, GLFW.GLFW_RELEASE, 0);
            });
            step(12, () -> {
                check("JEI's recipe key over the guide opens one page", viewerOpen() && Scene.consulting && Scene.phase == Scene.Phase.ACTIVE,
                        "screen=" + Minecraft.getInstance().screen);
                if (viewerOpen()) Minecraft.getInstance().screen.onClose();
            });
            step(10, () -> check("one close brings the work back", Minecraft.getInstance().screen instanceof SceneScreen
                    && Scene.phase == Scene.Phase.ACTIVE, "screen=" + Minecraft.getInstance().screen));
            //? if neoforge {
            if (net.neoforged.fml.ModList.get().isLoaded("jeresources")) {
            //?} else if fabric {
            /*if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("jeresources")) {
            *///?} else if forge {
            /*if (net.minecraftforge.fml.ModList.get().isLoaded("jeresources")) {
            *///?}
                guideFind("rotten", Items.ROTTEN_FLESH);
                step(8, () -> {
                    check("JER knows where rotten flesh comes from", guide().sourceCount() >= 1, "sources=" + guide().sourceCount());
                    double[] at = guide().tabPoint("sources");
                    pointGui(at[0], at[1]);
                    screenClick(0);
                });
                step(8, () -> {
                    check("guide: sources page", guide().tabName().equals("SOURCES"), "tab=" + guide().tabName());
                    pointAside();
                    shot("ji5_sources");
                    double[] at = guide().sourcePoint(0);
                    pointGui(at[0], at[1]);
                    screenClick(0);
                });
                step(14, () -> {
                    check("JER's own page opens over the scene", viewerOpen() && Scene.phase == Scene.Phase.ACTIVE, "screen=" + Minecraft.getInstance().screen);
                    shot("ji6_jer_page");
                    if (viewerOpen()) Minecraft.getInstance().screen.onClose();
                });
                step(10, () -> check("back from JER to the work", Minecraft.getInstance().screen instanceof SceneScreen
                        && Scene.phase == Scene.Phase.ACTIVE, "screen=" + Minecraft.getInstance().screen));
                step(3, () -> { double[] at = guide().tabPoint("recipes"); pointGui(at[0], at[1]); screenClick(0); });
            }
            // the scene ends while JEI covers it: no hanging screen, nothing lost
            guideFind("torch", Items.TORCH);
            step(16, () -> guideButton("uses"));
            step(14, () -> server(p -> p.teleportTo(p.serverLevel(), 0.5, -60, 12.5, 180f, 20f)));
            step(60, () -> {
                check("the scene let go under JEI", Scene.phase == Scene.Phase.NONE && viewerOpen(), "phase=" + Scene.phase);
                if (viewerOpen()) Minecraft.getInstance().screen.onClose();
            });
            step(10, () -> {
                check("JEI hands back nothing stale", Minecraft.getInstance().screen == null && !Scene.active(),
                        "screen=" + Minecraft.getInstance().screen);
                server(p -> check("no session left behind", dev.dyslexic.cinematic.station.Sessions.get(p) == null, "session"));
            });
            nextAct("ji", new Vec3(-3.2, -60, 4.6), 200f, FURNACE, new Vec3(0.5, 0.6, 1), Direction.SOUTH);
        } else if (Scene.station != null && Scene.station.kind() == StationKind.FURNACE) {
            step(10, () -> { double[] at = guide().togglePoint(); pointGui(at[0], at[1]); screenClick(0); });
            step(16, () -> check("guide opens beside the furnace", guide().opened(), "opened"));
            guideFind("iron ingot", Items.IRON_INGOT);
            step(8, () -> check("guide: iron ingot smelts here", guide().recipeCount() >= 1, "recipes=" + guide().recipeCount()));
            step(3, () -> guideButton("show_furnace"));
            step(16, () -> {
                check("the raw item shows in the furnace", !furnace().guideHint().isEmpty(), "hint=" + describe(furnace().guideHint()));
                pointAside();
                shot("ji7_furnace_hint");
            });
            step(4, Scene::leave);
            step(45, () -> {});
            nextAct("ji", new Vec3(2.5, -60, 5.4), 180f, SMOKER, new Vec3(0.5, 0.6, 1.0), Direction.SOUTH);
        } else {
            step(10, () -> { double[] at = guide().togglePoint(); pointGui(at[0], at[1]); screenClick(0); });
            step(16, () -> check("the smoker's villager: butcher", guide().profession() != null
                    && guide().profession().getString().equals(Component.translatable("entity.minecraft.villager.butcher").getString()),
                    "profession=" + guide().profession()));
            //? if neoforge {
            if (net.neoforged.fml.ModList.get().isLoaded("justenoughprofessions")) {
            //?} else if fabric {
            /*if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("justenoughprofessions")) {
            *///?} else if forge {
            /*if (net.minecraftforge.fml.ModList.get().isLoaded("justenoughprofessions")) {
            *///?}
                step(4, () -> {
                    check("JEP has the profession's page", guide().professionPageReady(), "page");
                    double[] at = guide().tabPoint("profession");
                    pointGui(at[0], at[1]);
                    screenClick(0);
                });
                step(8, () -> { pointAside(); shot("ji8_profession"); });
                step(3, () -> guideButton("open_jep"));
                step(14, () -> {
                    check("JEP's page opens over the scene", viewerOpen() && Scene.phase == Scene.Phase.ACTIVE, "screen=" + Minecraft.getInstance().screen);
                    shot("ji9_jep_page");
                    if (viewerOpen()) Minecraft.getInstance().screen.onClose();
                });
                step(10, () -> check("back from JEP to the work", Minecraft.getInstance().screen instanceof SceneScreen
                        && Scene.phase == Scene.Phase.ACTIVE, "screen=" + Minecraft.getInstance().screen));
            } else {
                step(4, () -> check("without JEP the profession tab stays hidden", !guide().professionPageReady(), "hidden"));
            }
            step(4, () -> { double[] at = guide().closePoint(); pointGui(at[0], at[1]); });
            step(4, () -> { shot("ji10_close_hover"); screenClick(0); });
            step(16, () -> {
                check("the ribbon folds the guide away", !guide().opened(), "closed");
                check("folding the guide leaves the work on", Scene.phase == Scene.Phase.ACTIVE, "phase=" + Scene.phase);
                pointAside();
                shot("ji11_closed");
            });
            step(4, Scene::leave);
            step(45, () -> finish(Minecraft.getInstance()));
        }
    }

    // ---- the crafting table -------------------------------------------------------------------------

    /** Real mouse clicks through SceneScreen, compared with the optional mods' own live renderer state. */
    private static void inventoryEffectsScript() {
        if (Scene.station instanceof TableStation) {
            float[] angle = new float[1];
            int[] scale = new int[1];
            step(3, InventoryEffectsProbe::defaults);
            step(3, () -> { drawerPoint(3); screenClick(0); });
            step(12, () -> { pointAside(); });
            step(30, () -> {
                check("inventory effects carried stack", Hands.carried().is(Items.REDSTONE) && InventoryEffectsProbe.cursor() == Items.REDSTONE,
                        "actual=" + Hands.carried() + " native=" + InventoryEffectsProbe.cursor());
                check("inventory effects native cursor particles", InventoryEffectsProbe.particles() > 0, "count=" + InventoryEffectsProbe.particles());
                check("inventory effects native physics", !InventoryEffectsProbe.hasInteractions() || InventoryEffectsProbe.physicsItem() == Items.REDSTONE,
                        "installed=" + InventoryEffectsProbe.hasInteractions() + " native=" + InventoryEffectsProbe.physicsItem());
                angle[0] = InventoryEffectsProbe.angle();
                shot("ip1_cursor");
                pointGui(40, 120);
            });
            step(4, () -> pointGui(Minecraft.getInstance().screen.width - 40, 120));
            step(4, () -> {
                check("inventory effects physics follows motion", !InventoryEffectsProbe.hasInteractions() || Float.isFinite(InventoryEffectsProbe.angle())
                        && Math.abs(InventoryEffectsProbe.angle() - angle[0]) > 0.1, "installed=" + InventoryEffectsProbe.hasInteractions() + " angle=" + InventoryEffectsProbe.angle());
                point(1, 1);
                InventoryEffectsProbe.clear();
            });
            step(25, () -> {
                check("inventory effects world hand hides GUI copy", Hands.itemInHand() && InventoryEffectsProbe.cursor() == Items.AIR,
                        "inHand=" + Hands.itemInHand() + " cursor=" + InventoryEffectsProbe.cursor());
                double[] at = Picking.toGui(Hands.heldPoint());
                check("inventory effects follow world hand", at != null && InventoryEffectsProbe.near(at[0], at[1]), "particles=" + InventoryEffectsProbe.particles());
                shot("ip2_hand");
                InventoryEffectsProbe.actionOnly();
                clickCell(1, 1, 1);
            });
            step(7, () -> {
                double[] at = Picking.toGui(table().cellPoint(4));
                check("inventory effects placement uses projected cell", InventoryEffectsProbe.particles() > 0 && at != null && InventoryEffectsProbe.near(at[0], at[1]),
                        //? if >=1.21 {
                        "particles=" + InventoryEffectsProbe.particles());
                        //?} else if <1.21 {
                        /*"particles=" + InventoryEffectsProbe.particles() + InventoryEffectsProbe.positions()
                                + (at == null ? " cell off screen" : String.format(java.util.Locale.ROOT, " cell (%.0f, %.0f)", at[0], at[1])));
                        *///?}
                server(p -> check("inventory effects real table placement", TableStore.get(p.serverLevel()).get(TABLE) != null
                        && TableStore.get(p.serverLevel()).get(TABLE).items.get(4).is(Items.REDSTONE), "server table"));
                shot("ip3_placed");
                drawerPoint(3); screenClick(0);
            });
            step(8, () -> { InventoryEffectsProbe.clear(); point(1, 1); });
            step(5, () -> clickCell(1, 1, 0));
            step(7, () -> {
                check("inventory effects taking from world cell", Hands.carried().is(Items.REDSTONE) && Hands.carried().getCount() == 1 && InventoryEffectsProbe.particles() > 0,
                        "carried=" + Hands.carried() + " particles=" + InventoryEffectsProbe.particles());
                drawerPoint(3); screenClick(0);
            });
            step(10, InventoryEffectsProbe::disable);
            step(2, () -> { drawerPoint(3); screenClick(0); pointAside(); });
            step(18, () -> {
                check("inventory effects respect particle toggles", InventoryEffectsProbe.particles() == 0 && Hands.carried().is(Items.REDSTONE), "no particles, normal transfer");
                scale[0] = Minecraft.getInstance().options.guiScale().get();
                Minecraft.getInstance().options.guiScale().set(3); Minecraft.getInstance().resizeDisplay();
                InventoryEffectsProbe.defaults();
                pointGui(40, 100);
            });
            step(14, () -> {
                check("inventory effects GUI scale coordinates", Math.abs(InventoryEffectsProbe.x() - 40) < 1 && Math.abs(InventoryEffectsProbe.y() - 100) < 1,
                        "native=" + InventoryEffectsProbe.x() + "," + InventoryEffectsProbe.y());
                shot("ip4_scale3");
                Minecraft.getInstance().options.guiScale().set(scale[0]); Minecraft.getInstance().resizeDisplay();
            });
            //? if >=1.21 {
            step(10, () -> { Settings.DEVELOPER_MODE.set(true); Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_G, 0, 0); });
            //?} else if <1.21 {
            /*step(10, () -> { Settings.set(Settings.DEVELOPER_MODE, true); Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_G, 0, 0); });
            *///?}
            step(15, () -> {
                check("inventory effects editor hides carried effects", table().gridEditor.visible() && InventoryEffectsProbe.cursor() == Items.AIR
                        && InventoryEffectsProbe.particles() == 0, "editor remains usable");
                shot("ip5_editor");
                Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0);
                //? if >=1.21 {
                Settings.DEVELOPER_MODE.set(false);
                //?} else if <1.21 {
                /*Settings.set(Settings.DEVELOPER_MODE, false);
                *///?}
            });
            step(8, () -> Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_LEFT_ALT, 0, 0));
            step(16, () -> {
                check("inventory effects free look hides carried effects", FreeLook.looking() && InventoryEffectsProbe.cursor() == Items.AIR
                        && InventoryEffectsProbe.particles() == 0, "no GUI trail over third-person view");
                Minecraft.getInstance().screen.keyReleased(GLFW.GLFW_KEY_LEFT_ALT, 0, 0);
            });
            step(16, Showcase::pointAside);
            step(8, () -> { drawerPoint(3); screenClick(0); Scene.leave(); });
            step(45, () -> check("inventory effects scene exits", Scene.phase == Scene.Phase.NONE, "phase=" + Scene.phase));
            nextAct("ip", new Vec3(-3.2, -60, 4.6), 200f, FURNACE, new Vec3(0.5, 0.6, 1), Direction.SOUTH);
        } else if (Scene.station instanceof FurnaceStation) {
            step(3, InventoryEffectsProbe::actionOnly);
            step(3, () -> { drawerPoint(5); screenClick(0); });
            step(10, () -> { InventoryEffectsProbe.clear(); pointZone(FURNACE, FurnaceZones.FUEL); });
            step(5, () -> screenClick(1));
            step(7, () -> {
                check("inventory effects furnace server transfer", ClientFurnaces.item(FURNACE, 1).is(Items.COAL), "fuel=" + ClientFurnaces.item(FURNACE, 1));
                check("inventory effects furnace action particles", InventoryEffectsProbe.particles() > 0, "particles=" + InventoryEffectsProbe.particles());
                shot("ip6_furnace"); drawerPoint(5); screenClick(0);
            });
            step(8, Scene::leave);
            step(45, () -> {});
            nextAct("ip", new Vec3(8.5, -60, 8.4), 180f, ANVIL, new Vec3(0.5, 1, 0.7), Direction.UP);
        } else {
            int[] ambientParticles = {0};
            step(3, () -> server(p -> p.getInventory().setItem(3, p.getInventory().getItem(17).copy())));
            step(10, InventoryEffectsProbe::actionOnly);
            step(3, () -> { drawerPoint(3); screenClick(0); });
            step(10, () -> { InventoryEffectsProbe.clear(); pointAt(((AnvilStation) Scene.station).layoutPoint(0)); });
            step(5, () -> screenClick(0));
            step(7, () -> {
                check("inventory effects vanilla menu transfer", ((AnvilStation) Scene.station).slot(0).is(Items.IRON_PICKAXE), "real anvil menu input");
                check("inventory effects vanilla menu particles", InventoryEffectsProbe.particles() > 0, "particles=" + InventoryEffectsProbe.particles());
                shot("ip7_anvil");
                InventoryEffectsProbe.allSlots(); pointAside();
                server(p -> p.getInventory().setItem(8, new ItemStack(Items.TORCH, 16)));
            });
            for (int i = 0; i < 12; i++) step(8, () -> ambientParticles[0] = Math.max(ambientParticles[0], InventoryEffectsProbe.particles()));
            step(1, () -> {
                check("inventory effects stationary slot particles", ambientParticles[0] > 0, "peak=" + ambientParticles[0]);
                shot("ip8_slots"); InventoryEffectsProbe.defaults();
                server(p -> p.getInventory().setItem(4, p.getInventory().getItem(19).copy()));
            });
            step(8, () -> { drawerPoint(4); screenClick(0); });
            step(8, () -> pointAt(((AnvilStation) Scene.station).layoutPoint(1)));
            step(5, () -> screenClick(0));
            step(7, () -> {
                check("inventory effects real forge recipe", !((AnvilStation) Scene.station).slot(2).isEmpty(), "enchantment output available");
                pointAt(((AnvilStation) Scene.station).restPoint(0));
            });
            step(5, () -> { InventoryEffectsProbe.actionOnly(); screenClick(0); });
            step(4, () -> check("inventory effects special move hides GUI copy", ((AnvilStation) Scene.station).moving()
                    && InventoryEffectsProbe.cursor() == Items.AIR, "hammer animation owns the carried item"));
            step(13, () -> {
                check("inventory effects animation beat particles", InventoryEffectsProbe.particles() > 0
                        && ((AnvilStation) Scene.station).moving(), "particles=" + InventoryEffectsProbe.particles());
                shot("ip9_forge_beat");
            });
            step(27, Showcase::pointAside);
            step(8, () -> {
                check("inventory effects restore cursor after forging", Hands.carried().is(Items.IRON_PICKAXE)
                        && Hands.carried().isEnchanted() && InventoryEffectsProbe.cursor() == Items.IRON_PICKAXE,
                        "real output=" + Hands.carried() + " enchanted=" + Hands.carried().isEnchanted()
                                + " inHand=" + Hands.itemInHand() + " cursor=" + InventoryEffectsProbe.cursor());
                shot("ipa_forged"); Scene.leave();
            });
            step(50, () -> finish(Minecraft.getInstance()));
        }
    }

    private static void drawerPoint(int inventorySlot) {
        Minecraft mc = Minecraft.getInstance();
        double scale = net.minecraft.util.Mth.clamp(mc.screen.height * 0.068 / 18, 0.6, 1.25);
        pointGui(mc.screen.width / 2.0 + (inventorySlot - 4) * 18 * scale, 6 + 14 * scale);
    }

    //? if <1.21 {
    /*/^* Targeted regression: opening a normal crafting menu must take ownership of items left on the table. ^/
    private static void tableWindowScript() {
        step(2, Scene::leave);
        step(40, () -> server(player -> {
            var level = player.serverLevel();
            var store = TableStore.get(level);
            var table = store.getOrCreate(TABLE);
            table.items.set(4, new ItemStack(Items.DIAMOND, 3));
            store.setDirty();
            check("table window: scene ended", dev.dyslexic.cinematic.station.Sessions.get(player) == null,
                    "session=" + dev.dyslexic.cinematic.station.Sessions.get(player));
            player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                    (id, inventory, owner) -> new net.minecraft.world.inventory.CraftingMenu(id, inventory,
                            net.minecraft.world.inventory.ContainerLevelAccess.create(level, TABLE)),
                    Component.translatable("container.crafting")));
            check("table window: real crafting menu opened", player.containerMenu instanceof net.minecraft.world.inventory.CraftingMenu,
                    "menu=" + player.containerMenu.getClass().getName());
            check("table window: stack moved into center cell", player.containerMenu.slots.get(5).getItem().is(Items.DIAMOND)
                            && player.containerMenu.slots.get(5).getItem().getCount() == 3,
                    "center=" + player.containerMenu.slots.get(5).getItem());
            check("table window: physical table cleared", store.get(TABLE) == null, "stored=" + store.get(TABLE));
            player.closeContainer();
        }));
        step(8, () -> finish(Minecraft.getInstance()));
    }

    *///?}
    private static void tableScript() {
        shot("05_active");
        tableGridAlignmentScript();
        tableGridEditorScript();
        tableRecipeBookScript();
        tableVisibilityScript();
        tableGlintVisibilityScript();
        step(4, () -> inventoryClick(36, 0));                // planks in hand
        step(3, () -> point(0, 0));
        step(12, () -> shot("06_carry"));
        step(2, () -> clickCell(0, 0, 1));
        step(6, () -> point(0, 1));
        step(8, () -> clickCell(0, 1, 1));
        step(6, () -> point(0, 2));
        step(8, () -> clickCell(0, 2, 1));
        step(6, () -> inventoryClick(36, 0));                // rest of the planks back
        step(4, () -> inventoryClick(37, 0));                // sticks
        step(3, () -> point(1, 1));
        step(8, () -> clickCell(1, 1, 1));
        step(6, () -> point(2, 1));
        step(8, () -> clickCell(2, 1, 1));
        step(6, () -> inventoryClick(37, 0));
        step(4, Showcase::pointAside);
        step(10, () -> shot("07_recipe"));
        step(2, () -> pointAt(table().resultPosition()));
        step(8, () -> shot("08_result_hover"));
        step(2, Showcase::clickResult);
        step(3, () -> shot("09_crafting_a"));
        step(3, () -> shot("09_crafting_b"));
        step(14, () -> shot("10_crafted"));
        step(2, () -> inventoryClick(44, 0));                // pickaxe into the last hotbar slot
        step(4, () -> inventoryClick(36, 0));                // planks again: three sticks' worth
        step(3, () -> point(0, 1));
        step(6, () -> clickCell(0, 1, 1));
        step(3, () -> clickCell(0, 1, 1));
        step(3, () -> clickCell(0, 1, 1));
        step(4, () -> point(1, 1));
        step(6, () -> clickCell(1, 1, 1));
        step(3, () -> clickCell(1, 1, 1));
        step(3, () -> clickCell(1, 1, 1));
        step(4, () -> inventoryClick(36, 0));
        step(4, Showcase::pointAside);
        step(8, () -> shot("10b_sticks"));
        step(2, () -> table().craft(true));                  // shift-click: craft all into the inventory
        step(14, () -> report("after craft-all"));
        step(4, () -> inventoryClick(38, 0));                // cobblestone
        step(3, () -> point(1, 1));
        step(8, () -> clickCell(1, 1, 0));
        step(6, () -> inventoryClick(40, 0));                // iron
        step(3, () -> point(0, 2));
        step(8, () -> clickCell(0, 2, 1));
        step(5, () -> point(2, 2));
        step(8, () -> clickCell(2, 2, 1));
        step(5, () -> inventoryClick(40, 0));
        step(4, () -> inventoryClick(39, 0));                // redstone, reached with the left hand
        step(3, () -> point(1, 0));
        step(12, () -> shot("11_left_hand"));
        step(2, () -> clickCell(1, 0, 0));
        step(6, Showcase::pointAside);
        step(8, () -> shot("12_items"));
        step(4, Showcase::hoverDrawer);
        step(10, () -> shot("13_drawer"));
        step(2, Showcase::pointAside);
        step(6, () -> report("table before leaving"));
        step(2, Scene::leave);
        step(50, () -> shot("09_after"));
        nextAct("3", new Vec3(-3.2, -60, 4.6), 200f, FURNACE, new Vec3(0.5, 0.6, 1.0), Direction.SOUTH);
    }

    /** Texture-derived targets must stay independent of TableGrid, or a misplaced grid would test itself. */
    private static void tableGridEditorScript() {
        TableLayout[] original = TableLayouts.profiles();
        int active = TableLayouts.selected();
        int[] previousGuiScale = new int[1];
        boolean originalDev = Settings.developerMode();
        step(2, () -> {
            //? if >=1.21 {
            Settings.DEVELOPER_MODE.set(false);
            //?} else if <1.21 {
            /*Settings.set(Settings.DEVELOPER_MODE, false);
            *///?}
            Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_G, 0, 0);
            double[] at = table().gridEditor.togglePoint();
            check("table editor hidden with developer mode off", !table().gridEditor.editing()
                    && !table().gridEditor.over(at[0], at[1]), "G and icon unavailable");
            //? if >=1.21 {
            Settings.DEVELOPER_MODE.set(true);
            //?} else if <1.21 {
            /*Settings.set(Settings.DEVELOPER_MODE, true);
            *///?}
        });
        step(2, () -> {
            TableLayout turned = new TableLayout(1, -1, 90, 1, 1, 1, 1);
            Vec3 first = turned.cell(TABLE, 0);
            check("editor quarter-turn geometry", Math.abs(first.x - (TABLE.getX() + 0.375)) < 1e-9
                    && Math.abs(first.z - (TABLE.getZ() + 0.625)) < 1e-9
                    && Math.abs(first.y - (TABLE.getY() + 1.0625)) < 1e-9, "independent 90-degree reference=" + first);
            TableLayout bounded = new TableLayout(100, -100, 45, 1.2, 1.2, Double.NaN, Double.POSITIVE_INFINITY);
            boolean inside = true;
            for (double x : new double[]{3.5 / 16, 12.5 / 16}) for (double z : new double[]{3.5 / 16, 12.5 / 16}) {
                Vec3 p = bounded.point(BlockPos.ZERO, x, z);
                inside &= p.x >= 0.009 && p.x <= 0.991 && p.z >= 0.009 && p.z <= 0.991;
            }
            check("editor clamps malformed layouts", inside && bounded.height() == 0 && bounded.itemScale() == 1,
                    "rotated corners stay on the block");
        });
        step(2, () -> inventoryClick(36, 0));
        step(2, () -> {
            double[] at = table().gridEditor.togglePoint();
            pointGui(at[0], at[1]);
            screenClick(0);
        });
        step(20, () -> {
            check("editor opens without replacing scene", table().gridEditor.editing() && Minecraft.getInstance().screen instanceof SceneScreen
                    && !table().showsInventory(), "same scene, inventory hidden");
            shot("0d0_editor_open");
        });
        editorSliderStep(2, 24);
        editorSliderStep(3, 90);
        editorSliderStep(4, 80);
        editorSliderStep(5, 0.5);
        editorSliderStep(6, 75);
        step(16, () -> {
            check("editor live preview", Math.abs(TableLayouts.current().rotation() - 24) < 0.1
                    && Math.abs(TableLayouts.current().height() - 0.5) < 0.01, "layout=" + TableLayouts.current());
            shot("0d1_editor_rotated");
            pointAt(table().cellPoint(4));
            screenClick(1);
        });
        step(3, () -> {
            check("editor consumes item clicks", Hands.carried().getCount() == 24 && ClientTables.get(TABLE) == null,
                    "carried=" + Hands.carried());
            Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0);
        });
        step(25, () -> {
            check("editor cancel restores profile", !table().gridEditor.editing() && TableLayouts.current().equals(original[active])
                    && Scene.phase == Scene.Phase.ACTIVE, "scene remains active");
            inventoryClick(36, 0);
        });
        step(3, () -> Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_G, 0, 0));
        step(20, () -> {
            previousGuiScale[0] = Minecraft.getInstance().options.guiScale().get();
            Minecraft.getInstance().options.guiScale().set(3);
            Minecraft.getInstance().resizeDisplay();
        });
        step(8, () -> {
            shot("0d5_editor_gui_scale_3");
            Minecraft.getInstance().options.guiScale().set(previousGuiScale[0]);
            Minecraft.getInstance().resizeDisplay();
        });
        step(8, () -> {
            editorControl(80, 249);
            Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_W, 0, 0);
            check("editor movement keys do not leave", Scene.phase == Scene.Phase.ACTIVE && table().gridEditor.editing(), "still editing");
        });
        editorSliderStep(2, 30);
        editorSliderStep(3, 85);
        editorSliderStep(4, 90);
        editorSliderStep(6, 80);
        step(15, () -> editorControl(145, 38));
        step(12, () -> {
            check("editor profile switching", Math.abs(TableLayouts.current().rotation() - original[(active + 1) % 3].rotation()) < 0.1,
                    "other profile restored");
            editorControl(18, 38);
        });
        step(20, () -> {
            check("editor profile draft retained", Math.abs(TableLayouts.current().rotation() - 30) < 0.1, "draft survives switching");
            Vec3 hit = table().cellPoint(4);
            pointAt(hit);
            screenClick(0);
            pointAt(hit.add(0.04, 0, -0.025));
            double[] mouse = Picking.mouseGui();
            Minecraft.getInstance().screen.mouseDragged(mouse[0], mouse[1], 0, 4, -2);
            Minecraft.getInstance().screen.mouseReleased(mouse[0], mouse[1], 0);
        });
        step(20, () -> {
            check("editor drag translates grid", TableLayouts.current().x() > 0.4 && TableLayouts.current().z() < -0.2,
                    "layout=" + TableLayouts.current());
            shot("0d2_editor_dragged");
            editorControl(42, 273);
        });
        step(25, () -> {
            check("editor saves profile", !table().gridEditor.editing() && java.nio.file.Files.exists(TableLayouts.path())
                    && TableLayouts.profiles()[active].rotation() == 30, "saved=" + TableLayouts.path());
            try {
                check("editor persists values", java.nio.file.Files.readString(TableLayouts.path()).contains("30.0"), "JSON contains rotation");
            } catch (java.io.IOException e) { throw new IllegalStateException(e); }
        });
        step(3, () -> Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_G, 0, 0));
        step(18, () -> editorControl(80, 249));
        step(20, () -> {
            check("editor reset preview", TableLayouts.current().equals(TableLayout.VANILLA), "vanilla preview");
            Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0);
        });
        step(25, () -> check("editor reset can be cancelled", Math.abs(TableLayouts.current().rotation() - 30) < 0.1,
                "saved layout retained"));
        for (int i = 0; i < 9; i++) {
            final int cell = i;
            step(2, () -> pointAt(table().cellPoint(cell)));
            step(4, () -> check("editor transformed picking " + cell, table().hoverCell == cell, "hover=" + table().hoverCell));
        }
        step(3, () -> {
            double[] at = table().recipeBook.togglePoint();
            pointGui(at[0], at[1]);
            screenClick(0);
        });
        step(18, () -> recipeSearch("wooden_pickaxe"));
        step(5, () -> pointRecipe("wooden_pickaxe"));
        step(3, () -> screenClick(0));
        step(15, () -> {
            shot("0d3_editor_recipe_ghosts");
            double[] at = table().recipeBook.actionPoint();
            pointGui(at[0], at[1]);
            screenClick(0);
        });
        step(60, () -> {
            check("editor recipe layout reaches adjusted cells", !table().result.isEmpty() && table().result.is(Items.WOODEN_PICKAXE)
                    && table().guide.missingCount == 0, "result=" + table().result);
            shot("0d4_editor_laid_out");
        });
        for (int[] rc : new int[][]{{0,0},{0,1},{0,2},{1,1},{2,1}}) {
            step(2, () -> pointAt(table().cellPoint(TableGrid.worldIndex(table().sideDirection(), rc[0], rc[1]))));
            step(4, () -> screenClick(0));
            step(4, () -> inventoryClick(rc[0] == 0 ? 36 : 37, 0));
        }
        step(3, () -> recipeControl(113, 194));
        step(3, () -> recipeControl(113, 12));
        step(20, () -> {
            check("editor walkthrough restores ingredients", ClientTables.get(TABLE) == null
                    || ClientTables.get(TABLE).list().stream().allMatch(ItemStack::isEmpty), "table emptied");
            check("editor restore original profiles", TableLayouts.save(original, active), "original profiles restored");
        });
        //? if >=1.21 {
        step(25, () -> { Settings.DEVELOPER_MODE.set(originalDev); pointAside(); });
        //?} else if <1.21 {
        /*step(25, () -> { Settings.set(Settings.DEVELOPER_MODE, originalDev); pointAside(); });
        *///?}
    }

    private static void editorSliderStep(int field, double value) {
        step(3, () -> {
            double[] at = table().gridEditor.sliderPoint(field, value);
            pointGui(at[0], at[1]);
            screenClick(0);
            Minecraft.getInstance().screen.mouseReleased(at[0], at[1], 0);
        });
    }

    private static void editorControl(double x, double y) {
        double[] at = table().gridEditor.controlPoint(x, y);
        pointGui(at[0], at[1]);
        screenClick(0);
        Minecraft.getInstance().screen.mouseReleased(at[0], at[1], 0);
    }

    private static void tableGridAlignmentScript() {
        step(2, () -> {
            boolean aligned = true;
            for (int i = 0; i < 9; i++) {
                double x = (5 + 3 * (i % 3)) / 16.0;
                double z = (5 + 3 * (i / 3)) / 16.0;
                aligned &= Math.abs(TableGrid.cellX(i) - x) < 1e-9 && Math.abs(TableGrid.cellZ(i) - z) < 1e-9;
            }
            check("table cells centered on texture", aligned && TableGrid.cellX(4) == 0.5 && TableGrid.cellZ(4) == 0.5,
                    "texture centres 5, 8, 11 on both axes");
            double low = 3.5 / 16.0, high = 12.5 / 16.0, epsilon = 0.0001;
            boolean bounds = TableGrid.cellAt(low - epsilon, 0.5) == -1 && TableGrid.cellAt(high, 0.5) == -1
                    && TableGrid.cellAt(0.5, low - epsilon) == -1 && TableGrid.cellAt(0.5, high) == -1
                    && TableGrid.cellAt(low + epsilon, low + epsilon) == 0
                    && TableGrid.cellAt(high - epsilon, high - epsilon) == 8;
            for (int line = 1; line <= 2; line++) {
                double boundary = (3.5 + 3 * line) / 16.0;
                bounds &= TableGrid.cellAt(boundary - epsilon, 0.5) == 3 + line - 1
                        && TableGrid.cellAt(boundary + epsilon, 0.5) == 3 + line;
            }
            check("table picking follows painted boundaries", bounds, "line centres delimit adjacent cells");
        });
        for (int i = 0; i < 9; i++) {
            final int cell = i;
            step(2, () -> pointAt(new Vec3(TABLE.getX() + (5 + 3 * (cell % 3)) / 16.0, TABLE.getY() + 1.0,
                    TABLE.getZ() + (5 + 3 * (cell / 3)) / 16.0)));
            step(5, () -> {
                check("table texture cell reachable " + cell, table().hoverCell == cell, "hover=" + table().hoverCell);
                shot("0q" + cell + "_aligned_cell");
            });
        }
        step(2, Showcase::pointAside);
    }

    /** Exercise the real reference controls and prove that the diagram never edits server inventories. */
    private static void tableRecipeBookScript() {
        step(4, () -> {
            var units = List.of(new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.BIRCH_PLANKS));
            int[] assignment = TableRecipeGuide.match(List.of(Ingredient.of(Items.OAK_PLANKS, Items.BIRCH_PLANKS),
                    Ingredient.of(Items.OAK_PLANKS)), units);
            check("recipe tag matching", assignment[0] == 1 && assignment[1] == 0, java.util.Arrays.toString(assignment));
            int[] repeated = TableRecipeGuide.match(List.of(Ingredient.of(Items.DIAMOND), Ingredient.of(Items.DIAMOND)),
                    List.of(new ItemStack(Items.DIAMOND)));
            check("recipe duplicate quantities", java.util.Arrays.stream(repeated).filter(i -> i >= 0).count() == 1,
                    java.util.Arrays.toString(repeated));
            var book = table().recipeBook;
            double[] at = book.togglePoint();
            pointGui(at[0], at[1]);
            screenClick(0);
        });
        step(16, Showcase::pointAside);
        step(4, () -> shot("0r1_recipe_reference"));
        step(2, () -> recipeSearch("diamond_pickaxe"));
        step(4, () -> pointRecipe("diamond_pickaxe"));
        step(3, () -> screenClick(0));
        step(12, () -> {
            var guide = table().guide;
            check("recipe missing diamonds", guide.selected != null && guide.missingCount == 3 && !guide.selected.craftable,
                    "missing=" + guide.missingCount);
            check("recipe search movement keys", Scene.phase == Scene.Phase.ACTIVE, "phase=" + Scene.phase);
            ClientTables.Entry contents = ClientTables.get(TABLE);
            check("recipe guide is local", (contents == null || contents.list().stream().allMatch(ItemStack::isEmpty))
                    && Hands.carried().isEmpty(), "no real items placed");
            server(p -> {
                TableStore.Table stored = TableStore.get(p.serverLevel()).get(TABLE);
                check("recipe guide server unchanged", (stored == null || stored.items.stream().allMatch(ItemStack::isEmpty))
                        && p.getInventory().getItem(0).getCount() == 24 && p.getInventory().getItem(1).getCount() == 12,
                        "planks=" + p.getInventory().getItem(0).getCount() + " sticks=" + p.getInventory().getItem(1).getCount());
            });
            pointAside();
        });
        step(4, () -> shot("0r2_missing_diamonds"));
        step(2, () -> recipeControl(12, 52)); // only craftable recipes
        //? if >=1.21 {
        step(4, () -> check("recipe availability filter", table().recipePoint(ResourceLocation.withDefaultNamespace("diamond_pickaxe")) == null,
        //?} else if <1.21 {
        /*step(4, () -> check("recipe availability filter", table().recipePoint(new ResourceLocation("diamond_pickaxe")) == null,
        *///?}
                "diamond pickaxe hidden"));
        step(2, () -> recipeSearch("wooden_pickaxe"));
        step(4, () -> pointRecipe("wooden_pickaxe"));
        step(3, () -> screenClick(0));
        step(10, () -> {
            var guide = table().guide;
            check("recipe available planks and sticks", guide.selected != null && guide.selected.craftable && guide.missingCount == 0,
                    "missing=" + guide.missingCount);
            pointAside();
        });
        step(4, () -> shot("0r3_available_guide"));
        step(2, () -> inventoryClick(36, 0));
        step(3, () -> recipeControl(64, 12)); // clicking reference with a carried stack must not place it
        step(3, () -> check("recipe panel consumes carried click", Hands.carried().getCount() == 24 && ClientTables.get(TABLE) == null,
                "carried=" + describe(Hands.carried())));
        step(2, () -> point(0, 0));
        step(8, () -> clickCell(0, 0, 1));
        step(8, () -> {
            check("recipe placed ingredient", table().guide.placed[0], "first plank accepted");
            shot("0r4_partial_recipe");
        });
        step(2, () -> inventoryClick(36, 0));
        step(2, () -> inventoryClick(38, 0));
        step(2, () -> point(0, 1));
        step(8, () -> clickCell(0, 1, 1));
        step(8, () -> {
            check("recipe wrong ingredient", table().guide.wrong(1), "cobblestone instead of planks");
            shot("0r5_wrong_ingredient");
        });
        step(2, () -> inventoryClick(38, 0));
        step(2, () -> clickCell(0, 1, 0));
        step(6, () -> inventoryClick(38, 0));
        step(2, () -> point(0, 0));
        step(6, () -> clickCell(0, 0, 0));
        step(6, () -> inventoryClick(36, 0));
        step(6, () -> {
            var contents = ClientTables.get(TABLE);
            check("recipe walkthrough restores inventory", (contents == null || contents.list().stream().allMatch(ItemStack::isEmpty))
                    && Hands.carried().isEmpty() && Minecraft.getInstance().player.getInventory().getItem(0).getCount() == 24,
                    "planks=" + Minecraft.getInstance().player.getInventory().getItem(0).getCount());
        });
        step(2, () -> recipeControl(113, 12)); // fold away, keep the diagram
        step(15, () -> {
            check("recipe folded guide retained", table().guide.selected != null, "selection retained");
            pointAside();
            shot("0r6_folded_reference");
            var at = table().recipeBook.togglePoint();
            pointGui(at[0], at[1]);
            screenClick(0);
        });
        step(15, () -> recipeControl(113, 194));
        step(3, () -> check("recipe clear guide", table().guide.selected == null, "cleared"));
        tableRecipeLayoutScript();
        step(2, () -> recipeControl(113, 12));
        step(15, Showcase::pointAside);
    }

    private static void tableRecipeLayoutScript() {
        step(2, () -> server(p -> {
            p.getInventory().setItem(8, new ItemStack(Items.OAK_LOG));
            p.containerMenu.broadcastChanges();
        }));
        step(8, () -> recipeSearch("minecraft:oak_planks"));
        step(3, () -> pointRecipe("oak_planks"));
        step(3, () -> screenClick(0));
        step(4, () -> inventoryClick(44, 0));
        step(3, () -> point(2, 2));
        step(6, () -> clickCell(2, 2, 0));
        step(8, () -> {
            var guide = table().guide;
            check("recipe shapeless follows placement", guide.placed[8] && guide.grid[0].isEmpty() && guide.missingCount == 0,
                    "log at bottom right");
            shot("0r7_shapeless_recipe");
        });
        step(2, () -> clickCell(2, 2, 0));
        step(6, () -> inventoryClick(44, 0));
        step(4, () -> server(p -> {
            p.getInventory().setItem(8, ItemStack.EMPTY);
            p.containerMenu.broadcastChanges();
        }));
        step(6, () -> recipeSearch("minecraft:stick"));
        step(3, () -> pointRecipe("stick"));
        step(3, () -> screenClick(0));
        step(6, () -> {
            var guide = table().guide;
            check("recipe narrow pattern centered", !guide.grid[1].isEmpty() && !guide.grid[4].isEmpty() && guide.grid[0].isEmpty(),
                    "one-column pattern");
        });
        step(2, () -> inventoryClick(36, 0));
        step(3, () -> point(1, 0));
        step(6, () -> clickCell(1, 0, 1));
        step(8, () -> {
            var guide = table().guide;
            check("recipe shaped follows offset", guide.placed[3] && (!guide.grid[0].isEmpty() || !guide.grid[6].isEmpty()) && guide.grid[1].isEmpty(),
                    "pattern moved to left column");
            shot("0r8_offset_recipe");
        });
        step(2, () -> inventoryClick(36, 0));
        step(2, () -> clickCell(1, 0, 0));
        step(6, () -> inventoryClick(36, 0));
        step(3, () -> recipeSearch("minecraft:wooden_axe"));
        step(3, () -> pointRecipe("wooden_axe"));
        step(3, () -> screenClick(0));
        step(3, () -> inventoryClick(37, 0));
        step(3, () -> point(2, 0));
        step(6, () -> clickCell(2, 0, 1));
        step(8, () -> {
            var guide = table().guide;
            check("recipe shaped follows mirror", guide.placed[6] && guide.grid[7].isEmpty() && guide.grid[4].test(new ItemStack(Items.OAK_PLANKS)),
                    "mirrored axe pattern");
            shot("0r9_mirrored_recipe");
        });
        step(2, () -> inventoryClick(37, 0));
        step(2, () -> clickCell(2, 0, 0));
        step(6, () -> inventoryClick(37, 0));
        step(4, () -> {
            BlockPos testPos = TABLE.offset(100, 0, 0);
            //? if >=1.21 {
            var entry = table().guide.entries.stream().filter(e -> e.holder.id().equals(ResourceLocation.withDefaultNamespace("wooden_pickaxe"))).findFirst().orElseThrow();
            //?} else if <1.21 {
            /*var entry = table().guide.entries.stream().filter(e -> e.holder.id().equals(new ResourceLocation("wooden_pickaxe"))).findFirst().orElseThrow();
            *///?}
            Direction[] sides = {Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST};
            int[] firstCells = {0, 8, 6, 2};
            try {
                for (int i = 0; i < sides.length; i++) {
                    var contents = new ClientTables.Entry();
                    contents.items[firstCells[i]] = new ItemStack(Items.OAK_PLANKS);
                    ClientTables.TABLES.put(testPos, contents);
                    var guide = new TableRecipeGuide(new TableStation(testPos, sides[i]));
                    guide.select(entry);
                    check("recipe orientation " + sides[i], guide.placed[0], "world cell=" + firstCells[i]);
                }
            } finally {
                ClientTables.TABLES.remove(testPos);
            }
        });
        step(2, () -> recipeControl(113, 194));
        step(6, () -> {
            var player = Minecraft.getInstance().player;
            var contents = ClientTables.get(TABLE);
            check("recipe layout checks restore inventory", (contents == null || contents.list().stream().allMatch(ItemStack::isEmpty))
                            && Hands.carried().isEmpty() && player.getInventory().getItem(0).getCount() == 24
                            && player.getInventory().getItem(1).getCount() == 12 && player.getInventory().getItem(8).isEmpty(),
                    "planks=" + player.getInventory().getItem(0).getCount() + " sticks=" + player.getInventory().getItem(1).getCount());
        });
    }

    private static void recipeControl(int x, int y) {
        var at = table().recipeBook.controlPoint(x, y);
        pointGui(at[0], at[1]);
        screenClick(0);
    }

    private static void recipeSearch(String query) {
        recipeControl(30, 34);
        var screen = Minecraft.getInstance().screen;
        screen.keyPressed(GLFW.GLFW_KEY_END, 0, 0);
        for (int i = 0; i < 96; i++) screen.keyPressed(GLFW.GLFW_KEY_BACKSPACE, 0, 0);
        for (char c : query.toCharArray()) {
            if (c >= 'a' && c <= 'z') screen.keyPressed(GLFW.GLFW_KEY_A + c - 'a', 0, 0);
            screen.charTyped(c, 0);
        }
        screen.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0); // first Esc releases search focus
    }

    private static void pointRecipe(String id) {
        //? if >=1.21 {
        var at = table().recipePoint(ResourceLocation.withDefaultNamespace(id));
        //?} else if <1.21 {
        /*var at = table().recipePoint(new ResourceLocation(id));
        *///?}
        check("recipe found " + id, at != null, "reference lookup");
        if (at != null) pointGui(at[0], at[1]);
    }

    /** The centre cube hides the far cell; a cube beside it must remain solid. */
    private static void tableVisibilityScript() {
        step(4, () -> inventoryClick(38, 0));
        step(3, () -> point(1, 1));
        step(6, () -> clickCell(1, 1, 1));
        step(3, () -> point(1, 0));
        step(6, () -> clickCell(1, 0, 1));
        step(6, () -> inventoryClick(38, 0));
        step(2, Showcase::pointAside);
        step(8, () -> {
            shot("05a_occlusion_solid");
            checkTableOpacity("table cubes at rest", false);
        });
        step(2, () -> inventoryClick(40, 0));
        step(3, () -> point(0, 1));
        step(1, () -> shot("05b_occlusion_fading"));
        step(6, () -> {
            shot("05c_occlusion_clear");
            checkTableOpacity("table far cell revealed", true);
        });
        step(2, () -> clickCell(0, 1, 1));
        step(8, () -> {
            Direction side = table().sideDirection();
            int centre = TableGrid.worldIndex(side, 1, 1);
            int far = TableGrid.worldIndex(side, 0, 1);
            int beside = TableGrid.worldIndex(side, 1, 0);
            server(p -> {
                TableStore.Table stored = TableStore.get(p.serverLevel()).get(TABLE);
                boolean correct = stored != null && stored.items.get(centre).is(Items.COBBLESTONE)
                        && stored.items.get(centre).getCount() == 1 && stored.items.get(beside).is(Items.COBBLESTONE)
                        && stored.items.get(beside).getCount() == 1 && stored.items.get(far).is(Items.IRON_INGOT)
                        && stored.items.get(far).getCount() == 1;
                check("placement behind the table cube", correct, "centre=" + centre + " far=" + far + " beside=" + beside
                        + " stored=" + (stored == null ? "-" : stored.items));
            });
            shot("05d_occlusion_placed");
        });
        step(2, () -> inventoryClick(40, 0));
        step(2, Showcase::pointAside);
        step(8, () -> {
            shot("05e_occlusion_recovered");
            checkTableOpacity("table cubes recover opacity", false);
        });
        // Restore the original inventory and empty grid before the crafting walkthrough.
        step(2, () -> point(0, 1));
        step(6, () -> clickCell(0, 1, 0));
        step(6, () -> inventoryClick(40, 0));
        step(2, () -> point(1, 1));
        step(6, () -> clickCell(1, 1, 0));
        step(6, () -> inventoryClick(38, 0));
        step(2, () -> point(1, 0));
        step(6, () -> clickCell(1, 0, 0));
        step(6, () -> inventoryClick(38, 0));
        step(2, Showcase::pointAside);
    }

    private static void checkTableOpacity(String name, boolean occluded) {
        TableStation station = table();
        if (station == null) {
            check(name, false, "no table scene");
            return;
        }
        Direction side = station.sideDirection();
        int centre = TableGrid.worldIndex(side, 1, 1);
        int beside = TableGrid.worldIndex(side, 1, 0);
        double centreOpacity = station.itemOpacity(centre);
        double besideOpacity = station.itemOpacity(beside);
        int expectedHover = occluded ? TableGrid.worldIndex(side, 0, 1) : -1;
        boolean correct = station.hoverCell == expectedHover && besideOpacity >= 0.98
                && (occluded ? centreOpacity < 0.6 : centreOpacity >= 0.98);
        check(name, correct, String.format("centre=%.3f beside=%.3f hover=%d expected=%d", centreOpacity, besideOpacity,
                station.hoverCell, expectedHover));
    }

    /** Glint requests two render buffers together, so exercise it while the cube is translucent. */
    private static void tableGlintVisibilityScript() {
        step(4, () -> server(p -> {
            //? if >=1.21 {
            p.getInventory().getItem(2).set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            //?} else if <1.21 {
            /*p.getInventory().getItem(2).enchant(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING, 1);
            *///?}
            p.containerMenu.broadcastChanges();
        }));
        step(8, () -> inventoryClick(38, 0));
        step(3, () -> point(1, 1));
        step(6, () -> clickCell(1, 1, 1));
        step(6, () -> inventoryClick(38, 0));
        step(2, Showcase::pointAside);
        step(8, () -> shot("05f_glint_solid"));
        step(2, () -> point(0, 1));
        step(8, () -> {
            shot("05g_glint_faded");
            checkTableOpacity("glint cube reveals the far cell", true);
            int centre = TableGrid.worldIndex(table().sideDirection(), 1, 1);
            ClientTables.Entry entry = ClientTables.get(TABLE);
            check("faded cube keeps its glint", entry != null && entry.items[centre].hasFoil(),
                    "stack=" + (entry == null ? "-" : entry.items[centre]));
        });
        step(2, Showcase::pointAside);
        step(8, () -> {
            shot("05h_glint_recovered");
            checkTableOpacity("glint cube recovers opacity", false);
        });
        step(2, () -> point(1, 1));
        step(6, () -> clickCell(1, 1, 0));
        step(6, () -> inventoryClick(38, 0));
        step(6, () -> server(p -> {
            ItemStack cobblestone = p.getInventory().getItem(2);
            //? if >=1.21 {
            cobblestone.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
            //?} else if <1.21 {
            /*cobblestone.removeTagKey("Enchantments");
            *///?}
            p.containerMenu.broadcastChanges();
            TableStore.Table stored = TableStore.get(p.serverLevel()).get(TABLE);
            boolean empty = stored == null || stored.items.stream().allMatch(ItemStack::isEmpty);
            check("visibility checks restore ingredients", empty && cobblestone.is(Items.COBBLESTONE)
                            && cobblestone.getCount() == 40 && p.getInventory().getItem(4).is(Items.IRON_INGOT)
                            && p.getInventory().getItem(4).getCount() == 5 && p.containerMenu.getCarried().isEmpty(),
                    "tableEmpty=" + empty + " cobblestone=" + cobblestone + " iron=" + p.getInventory().getItem(4)
                            + " carried=" + p.containerMenu.getCarried());
        }));
        step(8, Showcase::pointAside);
    }

    private static TableStation table() {
        return Scene.station(TableStation.class);
    }

    private static Vec3 cellPoint(int row, int col) {
        int index = TableGrid.worldIndex(Scene.station.sideDirection(), row, col);
        return new Vec3(TABLE.getX() + TableGrid.cellX(index), TABLE.getY() + 1.0, TABLE.getZ() + TableGrid.cellZ(index));
    }

    private static void point(int row, int col) {
        pointAt(cellPoint(row, col));
    }

    private static void clickCell(int row, int col, int button) {
        int expected = TableGrid.worldIndex(Scene.station.sideDirection(), row, col);
        TableStation station = table();
        if (station == null || station.hoverCell != expected) {
            REPORT.add("cursor missed cell r" + row + "c" + col + ": hover=" + (station == null ? "-" : station.hoverCell) + " expected=" + expected);
        }
        // A whole click: a held stack is put down when the button comes up, as over vanilla slots.
        screenClick(button);
        screenRelease(button);
    }

    private static void clickResult() {
        TableStation station = table();
        if (station == null || !station.hoverResult) {
            REPORT.add("cursor missed the result");
        }
        screenClick(0);
    }

    // ---- the furnaces -------------------------------------------------------------------------------

    private static FurnaceStation furnace() { return Scene.station(FurnaceStation.class); }

    private static void furnaceEditorScript() {
        FurnaceStation station = furnace();
        StationKind kind = station.kind();
        FurnaceAppearance[] original = FurnaceLayouts.profiles(kind);
        int selected = FurnaceLayouts.selected(kind);
        boolean originalDev = Settings.developerMode();
        String prefix = "p" + kind.name().toLowerCase(java.util.Locale.ROOT);
        int[] originalGui = new int[1];
        StationKind[] otherKinds = java.util.Arrays.stream(new StationKind[]{StationKind.FURNACE, StationKind.SMOKER, StationKind.BLAST_FURNACE})
                .filter(k -> k != kind).toArray(StationKind[]::new);
        FurnaceAppearance[][] otherProfiles = {FurnaceLayouts.profiles(otherKinds[0]), FurnaceLayouts.profiles(otherKinds[1])};
        step(2, () -> {
            //? if >=1.21 {
            Settings.DEVELOPER_MODE.set(false);
            //?} else if <1.21 {
            /*Settings.set(Settings.DEVELOPER_MODE, false);
            *///?}
            Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_G, 0, 0);
            double[] at = station.gridEditor.togglePoint();
            check(prefix + " developer mode off", !station.gridEditor.editing() && !station.gridEditor.over(at[0], at[1]), "no G or icon");
            FurnaceAppearance.Region shifted = FurnaceAppearance.Region.VANILLA.with(0, 1).with(1, 1);
            FurnaceAppearance independent = FurnaceAppearance.VANILLA.with(0, shifted);
            double[] base = station.layout.center(0);
            Vec3 expected = FurnaceZones.point(station.pos, station.sideDirection(), station.layout.zone(0).face(), base[0] + 1.0 / 16, base[1] + 1.0 / 16, 0);
            check(prefix + " independent pixel geometry", independent.point(station.layout, station.pos, station.sideDirection(), 0, base[0], base[1], 0, false).distanceTo(expected) < 1e-9,
                    "one texture pixel on the original face");
            FurnaceAppearance.Region malformed = new FurnaceAppearance.Region(Double.NaN, 999, 999, -999, Double.POSITIVE_INFINITY, 999, -999, 999, -999, 999);
            check(prefix + " malformed values bounded", malformed.u() == 0 && malformed.v() == 6 && malformed.itemScale() == 1.5 && malformed.out() == 0, "finite clamped values");
            //? if >=1.21 {
            Settings.DEVELOPER_MODE.set(true);
            //?} else if <1.21 {
            /*Settings.set(Settings.DEVELOPER_MODE, true);
            *///?}
        });
        // Pick a real coal stack, then make sure editor clicks cannot insert it.
        step(3, () -> inventoryClick(16, 0));
        step(3, () -> {
            double[] at = station.gridEditor.togglePoint(); pointGui(at[0], at[1]); screenClick(0);
        });
        step(20, () -> {
            check(prefix + " opens in scene", station.gridEditor.editing() && !station.showsInventory(), "inventory hidden");
            shot(prefix + "_open");
        });
        furnaceEditorSlider(0, 0.5);
        furnaceEditorSlider(1, 0.5);
        furnaceEditorSlider(2, 85);
        furnaceEditorSlider(3, 90);
        furnaceEditorSlider(4, 0.5);
        furnaceEditorSlider(5, 80);
        step(18, () -> {
            check(prefix + " preview", Math.abs(station.geometry().input().u() - 0.5) < 0.01
                    && Math.abs(station.geometry().input().itemScale() - 0.8) < 0.01, "live geometry");
            pointAt(station.geometry().center(station.layout, station.pos, station.sideDirection(), 0));
            screenClick(1);
        });
        step(3, () -> {
            check(prefix + " editor consumes carried clicks", Hands.carried().is(Items.COAL) && Hands.carried().getCount() == 16
                    && ClientFurnaces.item(station.pos, 0).isEmpty() && ClientFurnaces.item(station.pos, 1).isEmpty(), "real stack untouched");
            furnaceEditorControl(80, 90);
        });
        furnaceEditorSlider(0, 120);
        furnaceEditorSlider(1, 110);
        furnaceEditorSlider(2, 0.25);
        furnaceEditorSlider(3, -0.25);
        step(18, () -> {
            check(prefix + " advanced independent", Math.abs(station.geometry().input().itemU() - 0.25) < 0.01
                    && Math.abs(station.geometry().input().clickWidth() - 1.2) < 0.01, "item and target offsets");
            shot(prefix + "_advanced");
            Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0);
        });
        step(25, () -> {
            check(prefix + " cancel", station.geometry().equals(original[selected]) && Scene.phase == Scene.Phase.ACTIVE, "saved profile restored");
            inventoryClick(16, 0);
        });
        step(3, () -> Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_G, 0, 0));
        step(20, () -> furnaceEditorControl(80, 298));
        furnaceEditorSlider(0, 0.5);
        furnaceEditorSlider(4, 0.5);
        step(18, () -> {
            Vec3 start = station.geometry().center(station.layout, station.pos, station.sideDirection(), 0);
            pointAt(start); screenClick(0);
            pointAt(start.add(station.right().scale(0.025)));
            double[] at = Picking.mouseGui();
            Minecraft.getInstance().screen.mouseDragged(at[0], at[1], 0, 3, 0);
            Minecraft.getInstance().screen.mouseReleased(at[0], at[1], 0);
        });
        step(20, () -> {
            check(prefix + " drag", station.geometry().input().u() > 0.7, "zone follows face drag");
            furnaceEditorControl(160, 38);
        });
        step(20, () -> {
            check(prefix + " profile switching", station.geometry().equals(original[(selected + 1) % 3]), "independent profile");
            furnaceEditorControl(18, 38);
        });
        step(20, () -> {
            check(prefix + " draft retained", station.geometry().input().u() > 0.7, "draft survived switching");
            furnaceEditorControl(160, 65);
        });
        furnaceEditorSlider(0, -0.25);
        furnaceEditorSlider(1, 0.25);
        furnaceEditorSlider(2, 90);
        furnaceEditorSlider(4, 0.25);
        step(15, () -> furnaceEditorControl(160, 65));
        furnaceEditorSlider(0, 0.25);
        furnaceEditorSlider(1, 0.25);
        furnaceEditorSlider(5, 90);
        step(18, () -> {
            check(prefix + " all slots independently edited", Math.abs(station.geometry().fuel().u() + 0.25) < 0.01
                    && Math.abs(station.geometry().result().itemScale() - 0.9) < 0.01, "fuel and output settings");
            shot(prefix + "_all_zones");
            furnaceEditorControl(160, 65);
        });
        furnaceEditorSlider(0, 0.5);
        furnaceEditorSlider(1, 0.5);
        step(18, () -> {
            check(prefix + " gauge preview", Math.abs(station.geometry().cooking().v() - 0.5) < 0.01, "indicator moved independently");
            shot(prefix + "_gauge");
            furnaceEditorControl(160, 65);
        });
        furnaceEditorSlider(0, -0.5);
        furnaceEditorSlider(1, 0.25);
        furnaceEditorSlider(4, 0.125);
        step(18, () -> {
            check(prefix + " fuel gauge preview", Math.abs(station.geometry().burning().v() - 0.25) < 0.01
                    && Math.abs(station.geometry().cooking().v() - 0.5) < 0.01, "two independent indicators");
            shot(prefix + "_fuel_gauge");
            originalGui[0] = Minecraft.getInstance().options.guiScale().get();
            Minecraft.getInstance().options.guiScale().set(3); Minecraft.getInstance().resizeDisplay();
        });
        step(8, () -> {
            shot(prefix + "_gui_3");
            Minecraft.getInstance().options.guiScale().set(originalGui[0]); Minecraft.getInstance().resizeDisplay();
        });
        step(8, () -> {
            Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_W, 0, 0);
            check(prefix + " movement blocked", station.gridEditor.editing() && Scene.phase == Scene.Phase.ACTIVE, "same scene");
            furnaceEditorControl(45, 323);
        });
        step(25, () -> {
            check(prefix + " saves JSON", !station.gridEditor.editing() && java.nio.file.Files.exists(FurnaceLayouts.path())
                    && FurnaceLayouts.profiles(kind)[selected].input().u() > 0.7, "local profile saved");
            boolean boundedGlow = true;
            for (var glow : station.layout.glows) for (int zone = 0; zone < 3; zone++) {
                if (station.layout.zone(zone).face() != glow.face()) continue;
                double[] rect = station.geometry().glowRect(station.layout, glow, zone);
                if (rect != null) boundedGlow &= rect[0] >= 0 && rect[1] <= 1 && rect[2] >= 0 && rect[3] <= 1;
            }
            check(prefix + " warm light bounded", boundedGlow, "light remains on the configured face");
            check(prefix + " furnace types independent", java.util.Arrays.equals(otherProfiles[0], FurnaceLayouts.profiles(otherKinds[0]))
                    && java.util.Arrays.equals(otherProfiles[1], FurnaceLayouts.profiles(otherKinds[1])), "other furnaces unchanged");
            //? if >=1.21 {
            Settings.DEVELOPER_MODE.set(false);
            //?} else if <1.21 {
            /*Settings.set(Settings.DEVELOPER_MODE, false);
            *///?}
            Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_G, 0, 0);
            check(prefix + " saved layout with dev off", !station.gridEditor.editing() && station.geometry().input().u() > 0.7, "geometry applies, editor unavailable");
        });
        for (int i = 0; i < 3; i++) {
            int zone = i;
            step(3, () -> pointAt(station.geometry().center(station.layout, station.pos, station.sideDirection(), zone)));
            step(5, () -> check(prefix + " adjusted picking " + zone, station.hoverZone == zone, "hover=" + station.hoverZone));
        }
        step(3, () -> inventoryClick(16, 0));
        step(3, () -> pointAt(station.geometry().center(station.layout, station.pos, station.sideDirection(), 1)));
        step(5, () -> clickZone(1, 1));
        step(8, () -> {
            check(prefix + " real placement", ClientFurnaces.item(station.pos, 1).getCount() == 1 && Hands.carried().getCount() == 15, "one coal in logical fuel slot");
            inventoryClick(16, 0);
            server(p -> p.serverLevel().setBlockAndUpdate(station.pos, p.serverLevel().getBlockState(station.pos)
                    .setValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT, true)));
        });
        step(8, () -> {
            shot(prefix + "_adjusted_warm_light");
            server(p -> p.serverLevel().setBlockAndUpdate(station.pos, p.serverLevel().getBlockState(station.pos)
                    .setValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT, false)));
        });
        step(3, () -> pointAt(station.geometry().center(station.layout, station.pos, station.sideDirection(), 1)));
        step(5, () -> clickZone(1, 0));
        step(8, () -> inventoryClick(16, 0));
        step(3, () -> {
            //? if >=1.21 {
            Settings.DEVELOPER_MODE.set(true);
            //?} else if <1.21 {
            /*Settings.set(Settings.DEVELOPER_MODE, true);
            *///?}
            Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_G, 0, 0);
        });
        step(20, () -> furnaceEditorSliderNow(0, -0.5));
        //? if >=1.21 {
        step(18, () -> Settings.DEVELOPER_MODE.set(false));
        //?} else if <1.21 {
        /*step(18, () -> Settings.set(Settings.DEVELOPER_MODE, false));
        *///?}
        step(25, () -> {
            check(prefix + " disabling dev cancels draft", !station.gridEditor.editing() && station.geometry().equals(FurnaceLayouts.profiles(kind)[selected]), "saved geometry restored");
            check(prefix + " inventory restored", Hands.carried().isEmpty() && Minecraft.getInstance().player.getInventory().getItem(16).getCount() == 16
                    && ClientFurnaces.item(station.pos, 1).isEmpty(), "coal returned");
            check(prefix + " restore profiles", FurnaceLayouts.save(kind, original, selected), "original settings restored");
            //? if >=1.21 {
            Settings.DEVELOPER_MODE.set(originalDev); pointAside();
            //?} else if <1.21 {
            /*Settings.set(Settings.DEVELOPER_MODE, originalDev); pointAside();
            *///?}
        });
        step(25, () -> {});
    }

    /** Real scene input against edited spots, followed by the station's normal recipe walkthrough. */
    private static Runnable surfaceEditorScript() {
        SurfaceStation<?> station = (SurfaceStation<?>) Scene.station;
        StationKind kind = station.kind(), other = kind == StationKind.SMITHING ? StationKind.CARTOGRAPHY : StationKind.SMITHING;
        String prefix = kind == StationKind.SMITHING ? "sx" : "mx";
        SurfaceAppearance[] original = SurfaceLayouts.profiles(kind), untouched = SurfaceLayouts.profiles(other);
        int selected = SurfaceLayouts.selected(kind);
        boolean dev = Settings.developerMode();
        int[] stackCount = {0}, guiScale = {0};
        step(2, () -> {
            //? if >=1.21 {
            Settings.DEVELOPER_MODE.set(false);
            //?} else if <1.21 {
            /*Settings.set(Settings.DEVELOPER_MODE, false);
            *///?}
            Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_G, 0, 0);
            double[] at = station.gridEditor.togglePoint(); pointGui(at[0], at[1]); screenClick(0);
            check(prefix + " developer gate", !station.gridEditor.editing() && !station.gridEditor.over(at[0], at[1]), "G and icon unavailable");
            SurfaceAppearance moved = SurfaceAppearance.VANILLA.with(0, SurfaceAppearance.Region.VANILLA.with(0, 1).with(1, 1).with(5, 1));
            Vec3 p = moved.point(kind, Vec3.ZERO, Direction.SOUTH, 0);
            double expectedX = kind == StationKind.SMITHING ? -0.26 + 1.0 / 16 : -0.04 + 1.0 / 16;
            double expectedZ = kind == StationKind.SMITHING ? -0.12 + 1.0 / 16 : 0.04 + 1.0 / 16;
            check(prefix + " independent pixel geometry", p.distanceTo(new Vec3(expectedX, 1.0 / 16, expectedZ)) < 1e-9, "independent south-facing reference=" + p);
            var malformed = new SurfaceAppearance.Region(100, -100, Double.NaN, 100, 100, Double.POSITIVE_INFINITY, Double.NaN);
            check(prefix + " malformed values bounded", malformed.x() == 6 && malformed.z() == -6 && malformed.rotation() == 0
                    && malformed.width() == 1.5 && malformed.height() == 0 && malformed.itemScale() == 1, "finite values");
            //? if >=1.21 {
            Settings.DEVELOPER_MODE.set(true);
            //?} else if <1.21 {
            /*Settings.set(Settings.DEVELOPER_MODE, true);
            *///?}
            stackCount[0] = Minecraft.getInstance().player.getInventory().getItem(0).getCount();
            invClick(0, 0);
        });
        step(4, () -> { double[] at = station.gridEditor.togglePoint(); pointGui(at[0], at[1]); screenClick(0); });
        step(20, () -> {
            check(prefix + " editor opens", station.gridEditor.editing() && !station.showsInventory() && Minecraft.getInstance().screen instanceof SceneScreen, "same scene");
            pointAside();
            shot(prefix + "0_editor_open");
        });
        surfaceSlider(0, 0.5); surfaceSlider(1, 0.25); surfaceSlider(2, 20); surfaceSlider(3, 90);
        surfaceSlider(4, 85); surfaceSlider(5, 0.25); surfaceSlider(6, 90);
        step(18, () -> {
            check(prefix + " live preview", Math.abs(station.geometry().first().rotation() - 20) < 0.1 && Math.abs(station.surfaceScale(0) - 0.9) < 0.01, "rotation and size");
            pointAside();
            shot(prefix + "1_editor_preview");
            pointAt(station.restPoint(0)); screenClick(1);
        });
        step(4, () -> {
            check(prefix + " editing does not move ingredients", carried().getCount() == stackCount[0] && station.slot(0).isEmpty(), "carried stack unchanged");
            Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0);
        });
        step(25, () -> {
            check(prefix + " cancel restores saved", !station.gridEditor.editing() && station.geometry().equals(original[selected]), "scene stays active");
            invClick(0, 0);
        });
        step(4, () -> Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_G, 0, 0));
        step(20, () -> surfaceControl(90, 314));
        surfaceSlider(0, -0.25); surfaceSlider(1, -0.25); surfaceSlider(2, 12); surfaceSlider(3, 90);
        surfaceSlider(4, 90); surfaceSlider(5, 0.25); surfaceSlider(6, 85);
        step(18, () -> surfaceControl(150, 38));
        step(18, () -> {
            check(prefix + " profiles separate", station.geometry().equals(original[(selected + 1) % 3]), "other profile");
            surfaceControl(18, 38);
        });
        step(20, () -> {
            check(prefix + " profile draft retained", Math.abs(station.geometry().first().rotation() - 12) < 0.1, "switch back");
            Vec3 at = station.restPoint(0); pointAt(at); screenClick(0);
            pointAt(at.add(station.right().scale(0.025)));
            double[] mouse = Picking.mouseGui();
            Minecraft.getInstance().screen.mouseDragged(mouse[0], mouse[1], 0, 3, 0);
            Minecraft.getInstance().screen.mouseReleased(mouse[0], mouse[1], 0);
        });
        step(20, () -> {
            check(prefix + " world drag", station.geometry().first().x() > 0.1, "drag along working right axis");
            pointAside();
            shot(prefix + "2_editor_drag");
            surfaceControl(150, 65);
        });
        surfaceSlider(0, -0.5); surfaceSlider(1, 0.25); surfaceSlider(2, -12); surfaceSlider(5, 0.5); surfaceSlider(6, 90);
        if (kind == StationKind.SMITHING) {
            step(5, () -> surfaceControl(150, 65));
            surfaceSlider(0, -0.25); surfaceSlider(1, 0.25); surfaceSlider(2, -20); surfaceSlider(5, 0.25); surfaceSlider(6, 85);
        }
        step(18, () -> {
            check(prefix + " zones independent", station.geometry().first().rotation() > 11.9 && station.geometry().second().rotation() < -11.9, "independent rotation and heights");
            Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_W, 0, 0);
            check(prefix + " movement blocked", Scene.phase == Scene.Phase.ACTIVE && station.gridEditor.editing(), "still editing");
            guiScale[0] = Minecraft.getInstance().options.guiScale().get();
            Minecraft.getInstance().options.guiScale().set(3); Minecraft.getInstance().resizeDisplay();
        });
        step(8, () -> {
            pointAside();
            shot(prefix + "3_editor_gui3");
            Minecraft.getInstance().options.guiScale().set(guiScale[0]); Minecraft.getInstance().resizeDisplay();
        });
        step(8, () -> surfaceControl(40, 343));
        step(25, () -> {
            check(prefix + " saved JSON", !station.gridEditor.editing() && java.nio.file.Files.exists(SurfaceLayouts.path())
                    && SurfaceLayouts.profiles(kind)[selected].first().rotation() == 12, "local profiles written");
            check(prefix + " station types independent", java.util.Arrays.equals(untouched, SurfaceLayouts.profiles(other)), "other station untouched");
            //? if >=1.21 {
            Settings.DEVELOPER_MODE.set(false);
            //?} else if <1.21 {
            /*Settings.set(Settings.DEVELOPER_MODE, false);
            *///?}
            Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_G, 0, 0);
            check(prefix + " saved geometry with dev off", !station.gridEditor.editing() && station.geometry().first().rotation() > 11.9, "saved layout remains active");
        });
        for (int i = 0; i < SurfaceAppearance.count(kind); i++) {
            int spot = i;
            step(3, () -> pointAt(station.restPoint(spot)));
            step(4, () -> check(prefix + " edited picking " + spot, station.surfaceSlot(false) == spot, "raised rotated contact plane"));
        }
        //? if >=1.21 {
        step(3, () -> { Settings.DEVELOPER_MODE.set(true); Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_G, 0, 0); });
        //?} else if <1.21 {
        /*step(3, () -> { Settings.set(Settings.DEVELOPER_MODE, true); Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_G, 0, 0); });
        *///?}
        step(18, () -> surfaceControl(90, 314));
        //? if >=1.21 {
        step(25, () -> { check(prefix + " reset preview", station.geometry().equals(SurfaceAppearance.VANILLA), "vanilla draft"); Settings.DEVELOPER_MODE.set(false); });
        //?} else if <1.21 {
        /*step(25, () -> { check(prefix + " reset preview", station.geometry().equals(SurfaceAppearance.VANILLA), "vanilla draft"); Settings.set(Settings.DEVELOPER_MODE, false); });
        *///?}
        step(25, () -> {
            check(prefix + " disabling cancels draft", !station.gridEditor.editing() && station.geometry().first().rotation() > 11.9, "saved layout restored");
            check(prefix + " inventory restored", carried().isEmpty() && Minecraft.getInstance().player.getInventory().getItem(0).getCount() == stackCount[0], "no items created or lost");
            pointAside();
        });
        return () -> {
            check(prefix + " restore profiles", SurfaceLayouts.save(kind, original, selected), "original profiles restored");
            //? if >=1.21 {
            Settings.DEVELOPER_MODE.set(dev);
            //?} else if <1.21 {
            /*Settings.set(Settings.DEVELOPER_MODE, dev);
            *///?}
        };
    }

    /** Input, persistence and real operations exercise the same edited geometry. */
    private static Runnable spatialEditorScript() {
        SpatialTarget target = (SpatialTarget) Scene.station;
        ClientStation station = target.layoutStation();
        StationKind kind = station.kind();
        SpatialEditor editor = target.layoutEditor();
        String prefix = "x" + act;
        SpatialAppearance[] original = SpatialLayouts.profiles(kind);
        //? if >=1.21 {
        StationKind otherKind = kind == StationKind.BREWING ? StationKind.CRAFTER : StationKind.BREWING;
        //?} else if <1.21 {
        /*StationKind otherKind = kind == StationKind.BREWING ? StationKind.CHISELED_BOOKSHELF : StationKind.BREWING;
        *///?}
        SpatialAppearance[] otherProfiles = SpatialLayouts.profiles(otherKind);
        int selected = SpatialLayouts.selected(kind);
        boolean dev = Settings.developerMode();
        ItemStack hand = carried().copy();
        int[] guiScale = {0};
        step(2, () -> {
            //? if >=1.21 {
            Settings.DEVELOPER_MODE.set(false);
            //?} else if <1.21 {
            /*Settings.set(Settings.DEVELOPER_MODE, false);
            *///?}
            Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_G, 0, 0);
            double[] at = editor.togglePoint(); pointGui(at[0], at[1]); screenClick(0);
            check(prefix + " developer gate", !editor.editing() && !editor.over(at[0], at[1]), "G and gear unavailable");
            // (the crafter's flat and upright grid references are checked in later versions; 1.20.1 has no crafter)
            //? if >=1.21 {
            var flat = SpatialAppearance.Frame.of(StationKind.CRAFTER, BlockPos.ZERO, Direction.SOUTH, Direction.UP);
            var front = SpatialAppearance.Frame.of(StationKind.CRAFTER, BlockPos.ZERO, Direction.SOUTH, Direction.SOUTH);
            check(prefix + " independent flat geometry", SpatialAppearance.VANILLA.point(flat, 0).distanceTo(new Vec3(5 / 16.0, 1, 5 / 16.0)) < 1e-9, "texture reference");
            check(prefix + " independent upright geometry", SpatialAppearance.VANILLA.point(front, 0).distanceTo(new Vec3(5 / 16.0, 11 / 16.0, 1)) < 1e-9, "texture reference");
            //?}
            var invalid = new SpatialAppearance(null, java.util.Arrays.asList((SurfaceAppearance.Region) null));
            check(prefix + " malformed profile normalized", invalid.equals(SpatialAppearance.VANILLA), "missing slots restored");
            if (kind == StationKind.LOOM) {
                var loomFrame = SpatialAppearance.Frame.of(kind, BlockPos.ZERO, Direction.SOUTH, Direction.UP);
                check(prefix + " original loom corners", SpatialAppearance.VANILLA.point(loomFrame, 1).distanceTo(new Vec3(0.87, 1, 0.84)) < 1e-9
                        && SpatialAppearance.VANILLA.point(loomFrame, 2).distanceTo(new Vec3(0.13, 1, 0.84)) < 1e-9, "original corner references");
                var lowered = SpatialAppearance.VANILLA.with(0, SurfaceAppearance.Region.VANILLA.with(5, -1));
                check(prefix + " cloth can move down", Math.abs(lowered.point(loomFrame, 0).y - 15 / 16.0) < 1e-9, "lower model surfaces supported");
            } else if (kind == StationKind.ENCHANTING) {
                var enchantFrame = SpatialAppearance.Frame.of(kind, BlockPos.ZERO, Direction.SOUTH, Direction.UP);
                check(prefix + " original enchanting positions", SpatialAppearance.VANILLA.point(enchantFrame, 0).distanceTo(new Vec3(0.5, 1.37, 0.5)) < 1e-9
                        && SpatialAppearance.VANILLA.point(enchantFrame, 1).distanceTo(new Vec3(0.94, 1.02, 0.68)) < 1e-9
                        && SpatialAppearance.VANILLA.point(enchantFrame, 3).distanceTo(new Vec3(0.5, 1.67, 0.38)) < 1e-9, "original item, socket and crown");
            }
            //? if >=1.21 {
            Settings.DEVELOPER_MODE.set(true);
            //?} else if <1.21 {
            /*Settings.set(Settings.DEVELOPER_MODE, true);
            *///?}
        });
        step(5, () -> { pointAside(); shot(prefix + "0_gear"); });
        //? if >=1.21 {
        step(2, () -> { double[] gear = editor.togglePoint(); pointGui(gear[0], gear[1]); screenClick(0); });
        //?} else if <1.21 {
        /*step(2, () -> {
            double[] gear = editor.togglePoint();
            pointGui(gear[0], gear[1]);
            check(prefix + " gear offered", editor.over(gear[0], gear[1]) && !editor.visible(),
                    "visible=" + editor.visible() + " developer=" + Settings.developerMode() + " busy=" + station.busy() + " at=" + gear[0] + "," + gear[1]);
            screenClick(0);
        });
        *///?}
        step(22, () -> {
            check(prefix + " editor opens", editor.editing() && !station.showsInventory(), "same scene");
            pointAside(); shot(prefix + "0_editor_open");
        });
        spatialSlider(0, 0.25); spatialSlider(1, -0.25); spatialSlider(2, 5);
        spatialSlider(3, 95); spatialSlider(4, 95); spatialSlider(5, 0.25); spatialSlider(6, 90);
        step(22, () -> {
            check(prefix + " whole layout preview", Math.abs(target.geometry().group().rotation() - 5) < 0.1, "smooth transform");
            pointAside(); shot(prefix + "1_editor_preview");
            pointAt(target.layoutPoint(0)); screenClick(1);
            check(prefix + " preview leaves items intact", ItemStack.matches(hand, carried()), "no menu click");
            Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0);
        });
        step(25, () -> {
            check(prefix + " cancel restores saved", !editor.editing() && target.geometry().equals(original[selected]), "still at station");
            Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_G, 0, 0);
        });
        step(20, () -> spatialControl(90, 314));
        spatialSlider(0, 0.125); spatialSlider(1, -0.125); spatialSlider(2, 3);
        spatialSlider(3, 95); spatialSlider(4, 95); spatialSlider(5, 0.125); spatialSlider(6, 95);
        step(15, () -> spatialControl(150, 65));
        spatialSlider(0, 0.25); spatialSlider(1, 0.125); spatialSlider(2, -8); spatialSlider(6, 90);
        step(20, () -> {
            check(prefix + " slots independent", target.geometry().region(0).rotation() < -7.9 && target.geometry().region(1).rotation() == 0, "only first slot changed");
            Vec3 at = target.layoutPoint(0); pointAt(at); screenClick(0);
            pointAt(at.add(target.layoutFrame().across().scale(0.0125)));
            double[] mouse = Picking.mouseGui();
            Minecraft.getInstance().screen.mouseDragged(mouse[0], mouse[1], 0, 3, 0);
            Minecraft.getInstance().screen.mouseReleased(mouse[0], mouse[1], 0);
        });
        step(20, () -> {
            check(prefix + " direct world drag", target.geometry().region(0).x() > 0.3, "dragged first spot");
            pointAside(); shot(prefix + "2_editor_drag");
        });
        if (kind == StationKind.BREWING) {
            step(3, () -> {
                Vec3 at = target.layoutPoint(3); pointAt(at); screenClick(0);
                pointAt(at.add(0, 0.0125, 0));
                double[] mouse = Picking.mouseGui();
                Minecraft.getInstance().screen.mouseDragged(mouse[0], mouse[1], 0, 0, -3);
                Minecraft.getInstance().screen.mouseReleased(mouse[0], mouse[1], 0);
            });
            step(20, () -> check(prefix + " bowl drag at eye level", target.geometry().region(3).height() > 0.1, "raised the bowl directly"));
        }
        if (kind == StationKind.ENCHANTING) {
            step(3, () -> {
                Vec3 at = target.layoutPoint(4); pointAt(at); screenClick(0);
                pointAt(at.add(0, 0.0125, 0)); double[] mouse = Picking.mouseGui();
                Minecraft.getInstance().screen.mouseDragged(mouse[0], mouse[1], 0, 0, -3);
                Minecraft.getInstance().screen.mouseReleased(mouse[0], mouse[1], 0);
            });
            step(20, () -> {
                check(prefix + " floating offer drag", target.geometry().region(4).height() > 0.1 && target.geometry().region(1).height() == 0, "offer moves independently");
                shot(prefix + "2b_offer_drag");
            });
        }
        step(3, () -> spatialControl(150, 38));
        step(20, () -> {
            check(prefix + " profiles separate", target.geometry().equals(original[(selected + 1) % 3]), "other profile"); spatialControl(18, 38);
        });
        step(20, () -> {
            check(prefix + " draft retained", target.geometry().region(0).rotation() < -7.9, "back to edited profile");
            Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_W, 0, 0);
            check(prefix + " movement blocked", editor.editing() && Scene.phase == Scene.Phase.ACTIVE, "no accidental exit");
            guiScale[0] = Minecraft.getInstance().options.guiScale().get();
            Minecraft.getInstance().options.guiScale().set(3); Minecraft.getInstance().resizeDisplay();
        });
        step(12, () -> {
            pointAside(); shot(prefix + "3_editor_gui3");
            Minecraft.getInstance().options.guiScale().set(guiScale[0]); Minecraft.getInstance().resizeDisplay();
        });
        step(12, () -> spatialControl(40, 343));
        step(25, () -> {
            check(prefix + " saved JSON", !editor.editing() && java.nio.file.Files.exists(SpatialLayouts.path())
                    && SpatialLayouts.profiles(kind)[selected].region(0).rotation() == -8, "saved local profile");
            check(prefix + " station profiles independent", java.util.Arrays.equals(otherProfiles, SpatialLayouts.profiles(otherKind)), "other type untouched");
            if (kind == StationKind.CHISELED_BOOKSHELF) {
                var mc = Minecraft.getInstance();
                var state = mc.level.getBlockState(station.pos);
                var model = mc.getBlockRenderer().getBlockModel(state);
                //? if neoforge {
                var data = model.getModelData(mc.level, station.pos, state, net.neoforged.neoforge.client.model.data.ModelData.EMPTY);
                boolean occupiedPicture = false;
                //?} else if fabric {
                /*// what the shelf's wrapped model hands the Fabric renderer for this block (NeoForge: its model data)
                boolean occupiedPicture = !(model instanceof StationModels);
                *///?} else if forge {
                /*var data = model.getModelData(mc.level, station.pos, state, net.minecraftforge.client.model.data.ModelData.EMPTY);
                boolean occupiedPicture = false;
                *///?}
                for (Direction face : new Direction[]{null, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN})
                    //? if neoforge {
                    occupiedPicture |= model.getQuads(state, face, RandomSource.create(1), data, null).stream()
                    //?} else if fabric {
                    /*occupiedPicture |= model instanceof StationModels wrapped && wrapped.quadsAt(state, station.pos, face, RandomSource.create(1)).stream()
                    *///?} else if forge {
                    /*occupiedPicture |= model.getQuads(state, face, RandomSource.create(1), data, null).stream()
                    *///?}
                            .anyMatch(q -> q.getSprite().contents().name().getPath().contains("chiseled_bookshelf_occupied"));
                check(prefix + " original spines hidden", !occupiedPicture, "real block state untouched");
                var slotted = ShelfBooks.inSlot(station.pos, station.sideDirection(), 0, 0);
                check(prefix + " book and hand contact agree", slotted.at(0, 0, OpenBook.STACK / 2).distanceTo(target.layoutPoint(0)) < 1e-8, "shared spine");
            }
            //? if >=1.21 {
            Settings.DEVELOPER_MODE.set(false);
            //?} else if <1.21 {
            /*Settings.set(Settings.DEVELOPER_MODE, false);
            *///?}
            Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_G, 0, 0);
            check(prefix + " saved with developer off", !editor.editing() && target.geometry().region(0).rotation() < -7.9, "geometry still active");
        });
        step(2, () -> check(prefix + " settings profile switch", LayoutProfiles.select(kind, (selected + 1) % 3), "saved selection changed"));
        step(25, () -> {
            check(prefix + " settings profile applies", target.geometry().equals(original[(selected + 1) % 3]), "actual world geometry changed");
            check(prefix + " settings preserve geometry", SpatialLayouts.profiles(kind)[selected].region(0).rotation() == -8, "edited profile retained");
            check(prefix + " settings restore selection", LayoutProfiles.select(kind, selected), "same profiles as the scene editor");
        });
        step(25, () -> check(prefix + " selected geometry returns", target.geometry().region(0).rotation() == -8, "smoothly restored"));
        for (int i = 0; i < SpatialAppearance.count(kind); i++) {
            int slot = i;
            step(3, () -> pointAt(target.layoutPoint(slot)));
            step(5, () -> {
                check(prefix + " edited picking " + slot, target.layoutPick() == slot, "surface contact");
                if (kind == StationKind.CHISELED_BOOKSHELF)
                    check(prefix + " initial use picking " + slot, ShelfStation.slotAt(station.pos, station.sideDirection(), target.layoutPoint(slot)) == slot,
                            "initial use and vanilla fallback follow layout");
            });
        }
        //? if >=1.21 {
        step(3, () -> { Settings.DEVELOPER_MODE.set(true); Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_G, 0, 0); });
        //?} else if <1.21 {
        /*step(3, () -> { Settings.set(Settings.DEVELOPER_MODE, true); Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_G, 0, 0); });
        *///?}
        step(20, () -> spatialControl(90, 314));
        step(25, () -> {
            check(prefix + " reset preview", target.geometry().equals(SpatialAppearance.VANILLA), "vanilla draft");
            //? if >=1.21 {
            Settings.DEVELOPER_MODE.set(false);
            //?} else if <1.21 {
            /*Settings.set(Settings.DEVELOPER_MODE, false);
            *///?}
        });
        step(25, () -> {
            check(prefix + " disabling cancels draft", !editor.editing() && target.geometry().region(0).rotation() < -7.9, "saved profile restored");
            pointAside();
        });
        return () -> {
            check(prefix + " restore profiles", SpatialLayouts.save(kind, original, selected), "original profiles restored");
            //? if >=1.21 {
            Settings.DEVELOPER_MODE.set(dev);
            //?} else if <1.21 {
            /*Settings.set(Settings.DEVELOPER_MODE, dev);
            *///?}
        };
    }
    private static void spatialControl(double x, double y) {
        SpatialEditor editor = ((SpatialTarget) Scene.station).layoutEditor();
        double[] at = editor.controlPoint(x, y); pointGui(at[0], at[1]); screenClick(0);
        Minecraft.getInstance().screen.mouseReleased(at[0], at[1], 0);
    }
    private static void spatialSlider(int row, double value) {
        step(3, () -> {
            SpatialEditor editor = ((SpatialTarget) Scene.station).layoutEditor();
            double[] at = editor.sliderPoint(row, value); pointGui(at[0], at[1]); screenClick(0);
            Minecraft.getInstance().screen.mouseReleased(at[0], at[1], 0);
        });
    }

    private static void surfaceControl(double x, double y) {
        SurfaceStation<?> station = (SurfaceStation<?>) Scene.station;
        double[] at = station.gridEditor.controlPoint(x, y); pointGui(at[0], at[1]); screenClick(0);
        Minecraft.getInstance().screen.mouseReleased(at[0], at[1], 0);
    }
    private static void surfaceSlider(int row, double value) {
        step(3, () -> {
            SurfaceStation<?> station = (SurfaceStation<?>) Scene.station;
            double[] at = station.gridEditor.sliderPoint(row, value); pointGui(at[0], at[1]); screenClick(0);
            Minecraft.getInstance().screen.mouseReleased(at[0], at[1], 0);
        });
    }

    private static void furnaceEditorControl(double x, double y) {
        double[] at = furnace().gridEditor.controlPoint(x, y); pointGui(at[0], at[1]); screenClick(0);
        Minecraft.getInstance().screen.mouseReleased(at[0], at[1], 0);
    }
    private static void furnaceEditorSliderNow(int row, double value) {
        double[] at = furnace().gridEditor.sliderPoint(row, value); pointGui(at[0], at[1]); screenClick(0);
        Minecraft.getInstance().screen.mouseReleased(at[0], at[1], 0);
    }
    private static void furnaceEditorSlider(int row, double value) { step(3, () -> furnaceEditorSliderNow(row, value)); }

    private static void developerOptionsScript() {
        step(8, () -> {
            Minecraft mc = Minecraft.getInstance();
            //? if neoforge {
            var mod = net.neoforged.fml.ModList.get().getModContainerById(Cinematic.MODID).orElseThrow();
            mc.setScreen(mod.getCustomExtension(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class).orElseThrow().createScreen(mod, null));
            //?} else if fabric {
            /*mc.setScreen(CinematicClient.settingsScreen(null));
            *///?} else if forge {
            /*var mod = net.minecraftforge.fml.ModList.get().getModContainerById(Cinematic.MODID).orElseThrow();
            mc.setScreen(mod.getCustomExtension(net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class).orElseThrow()
                    .screenFunction().apply(mc, null));
            *///?}
        });
        step(15, () -> {
            check("advanced settings tab opens", clickSettingsSection(Minecraft.getInstance().screen,
                    Component.translatable("cinematic.settings.tab.advanced").getString()), "tab button");
            if (Minecraft.getInstance().screen instanceof SettingsScreen settings) settings.showDeveloper();
        });
        step(18, () -> {
            verifyProfileControls();
            shot("pz_developer_settings");
            check("developer mode defaults off", !Settings.DEVELOPER_MODE.getDefault(), "editor access is opt-in");
        });
        step(3, () -> Minecraft.getInstance().setScreen(null));
    }

    private static void profileWidgets(net.minecraft.client.gui.components.events.GuiEventListener node, List<Object> result) {
        if (node instanceof net.minecraft.client.gui.components.CycleButton<?> cycle && cycle.getValue() instanceof Integer
                || node.getClass().getName().endsWith("IntegerSliderEntry")) result.add(node);
        if (node instanceof net.minecraft.client.gui.components.events.ContainerEventHandler parent)
            for (var child : parent.children()) profileWidgets(child, result);
    }
    private static void verifyProfileControls() {
        List<Object> selectors = new ArrayList<>(); profileWidgets(Minecraft.getInstance().screen, selectors);
        //? if >=1.21 {
        check("settings have a profile for every built-in station", selectors.size() == StationKind.values().length - 1, "selectors=" + selectors.size());
        if (selectors.size() != StationKind.values().length - 1) return;
        //?} else if <1.21 {
        /*check("settings have a profile for every built-in station", selectors.size() == LayoutProfiles.supportedCount(), "selectors=" + selectors.size());
        if (selectors.size() != LayoutProfiles.supportedCount()) return;
        *///?}
        int selected = LayoutProfiles.selected(StationKind.TABLE), next = (selected + 1) % 3;
        //? if >=1.21 {
        Object first = selectors.getFirst();
        //?} else if <1.21 {
        /*Object first = selectors.get(0);
        *///?}
        try {
            if (first instanceof net.minecraft.client.gui.components.CycleButton<?> cycle) {
                cycle.onPress();
                check("profile choice waits for Done", LayoutProfiles.selected(StationKind.TABLE) == selected && cycle.getValue().equals(next),
                        "cancel does not write a profile");
                while (!cycle.getValue().equals(selected)) cycle.onPress();
            } else {
                first.getClass().getMethod("setValue", int.class).invoke(first, next + 1);
                check("Cloth unsaved profile stays local", LayoutProfiles.selected(StationKind.TABLE) == selected, "cancel does not write a profile");
                first.getClass().getMethod("save").invoke(first);
                check("Cloth profile slider saves", LayoutProfiles.selected(StationKind.TABLE) == next, "shared JSON selection");
                first.getClass().getMethod("setValue", int.class).invoke(first, selected + 1);
                first.getClass().getMethod("save").invoke(first);
            }
            check("settings profile restored", LayoutProfiles.selected(StationKind.TABLE) == selected, "test preserves selection");
            int anvilProfile = LayoutProfiles.selected(StationKind.ANVIL);
            check("all station profile files writable", LayoutProfiles.select(StationKind.ANVIL, (anvilProfile + 1) % 3)
                    && LayoutProfiles.select(StationKind.ANVIL, anvilProfile), "format migration and selection saved");
        } catch (ReflectiveOperationException error) { check("profile control API", false, error.toString()); }
    }

    /** NeoForge pairs a section label with an Edit button; Cloth exposes a named tab directly. */
    private static boolean clickSettingsSection(net.minecraft.client.gui.components.events.GuiEventListener node, String label) {
        net.minecraft.client.gui.components.AbstractWidget target = null;
        if (node instanceof net.minecraft.client.gui.components.AbstractButton button && button.getMessage().getString().equals(label)) target = button;
        if (target == null && node instanceof net.minecraft.client.gui.components.events.ContainerEventHandler container) {
            for (var child : container.children()) if (clickSettingsSection(child, label)) return true;
            boolean named = container.children().stream().anyMatch(child -> child instanceof net.minecraft.client.gui.components.AbstractWidget widget
                    && widget.getMessage().getString().contains(label));
            if (named) for (var child : container.children()) if (child instanceof net.minecraft.client.gui.components.Button button) { target = button; break; }
        }
        if (target == null) return false;
        double x = target.getX() + target.getWidth() / 2.0, y = target.getY() + target.getHeight() / 2.0;
        Minecraft.getInstance().screen.mouseClicked(x, y, 0);
        Minecraft.getInstance().screen.mouseReleased(x, y, 0);
        return true;
    }

    //? if <1.21 {
    /*/^* Simulates another player's break packet reaching the server during the furnace scene. ^/
    private static void furnaceBreakScript() {
        step(10, () -> check("furnace break starts in the scene", Scene.phase == Scene.Phase.ACTIVE,
                "phase=" + Scene.phase));
        step(2, () -> server(p -> p.serverLevel().setBlockAndUpdate(FURNACE, Blocks.AIR.defaultBlockState())));
        step(30, () -> {
            check("removed furnace closes the scene", Scene.phase == Scene.Phase.NONE,
                    "phase=" + Scene.phase);
            server(p -> check("removed furnace ends the server session", dev.dyslexic.cinematic.station.Sessions.get(p) == null,
                    "session=" + dev.dyslexic.cinematic.station.Sessions.get(p)));
            shot("fb_removed");
        });
        step(2, () -> finish(Minecraft.getInstance()));
    }

    *///?}
    private static void furnaceScript() {
        furnaceEditorScript();
        shot("35_active");
        step(4, () -> inventoryClick(41, 0));                // coal in hand
        step(3, () -> pointZone(FURNACE, FurnaceZones.FUEL));
        step(12, () -> shot("36_carry_fuel"));
        step(2, () -> clickZone(FurnaceZones.FUEL, 0));      // all of it into the fire box
        step(8, () -> shot("37_fueled"));
        step(4, () -> inventoryClick(42, 0));                // raw iron
        step(3, () -> pointZone(FURNACE, FurnaceZones.INPUT));
        step(12, () -> shot("38_carry_ore"));
        step(2, () -> clickZone(FurnaceZones.INPUT, 1));
        step(6, () -> clickZone(FurnaceZones.INPUT, 1));
        step(6, () -> clickZone(FurnaceZones.INPUT, 1));
        step(6, () -> inventoryClick(42, 0));                // the rest back
        step(4, () -> pointLedge(FURNACE));
        step(30, () -> shot("39_lit"));
        step(2, () -> pointZone(FURNACE, FurnaceZones.INPUT));
        step(8, () -> shot("40_input_tooltip"));
        step(2, () -> pointLedge(FURNACE));
        step(380, () -> shot("42_smelted"));
        step(2, () -> pointZone(FURNACE, FurnaceZones.RESULT));
        step(10, () -> shot("43_result_hover"));
        step(2, () -> clickZone(FurnaceZones.RESULT, 0));
        step(10, () -> shot("44_ingot_in_hand"));
        step(2, () -> inventoryClick(43, 0));                // the ingots into hotbar slot 8
        step(4, () -> inventoryClick(40, 0));                // iron ingots are no fuel
        step(3, () -> pointZone(FURNACE, FurnaceZones.FUEL));
        step(10, () -> shot("45_not_fuel"));
        step(2, () -> clickZone(FurnaceZones.FUEL, 0));      // refused, stays in hand
        step(4, () -> inventoryClick(40, 0));
        step(4, () -> pointLedge(FURNACE));
        step(6, () -> report("furnace before leaving"));
        step(2, Scene::leave);
        step(50, () -> shot("39_after"));
        nextAct("4", new Vec3(2.5, -60, 5.4), 180f, SMOKER, new Vec3(0.5, 0.6, 1.0), Direction.SOUTH);
    }

    private static void smokerScript() {
        furnaceEditorScript();
        shot("45_active");
        step(4, () -> inventoryClick(9, 0));                 // a stack of coal
        step(3, () -> pointZone(SMOKER, FurnaceZones.FUEL));
        step(12, () -> shot("46_carry_fuel"));
        step(2, () -> clickZone(FurnaceZones.FUEL, 0));
        step(6, () -> inventoryClick(11, 0));                // raw beef
        step(3, () -> pointZone(SMOKER, FurnaceZones.INPUT));
        step(12, () -> shot("47_carry_food"));
        step(2, () -> clickZone(FurnaceZones.INPUT, 1));
        step(6, () -> clickZone(FurnaceZones.INPUT, 1));
        step(6, () -> inventoryClick(11, 0));
        step(4, () -> pointLedge(SMOKER));
        step(40, () -> shot("48_lit"));
        step(2, () -> pointZone(SMOKER, FurnaceZones.INPUT));
        step(8, () -> shot("49_input_tooltip"));
        step(2, () -> pointLedge(SMOKER));
        step(180, () -> shot("50_smoked"));
        step(2, () -> pointZone(SMOKER, FurnaceZones.RESULT));
        step(10, () -> shot("51_result_hover"));
        step(2, () -> report("smoker before taking"));
        step(2, () -> {
            pointZone(SMOKER, FurnaceZones.RESULT);
            FurnaceStation station = Scene.station(FurnaceStation.class);
            if (station != null) {
                station.click(0, true);                      // shift-click: straight into the inventory
            }
        });
        step(10, () -> report("smoker after taking"));
        step(2, Scene::leave);
        step(50, () -> shot("49_after"));
        nextAct("5", new Vec3(4.5, -60, 5.4), 180f, BLAST, new Vec3(0.5, 0.6, 1.0), Direction.SOUTH);
    }

    private static void blastScript() {
        furnaceEditorScript();
        shot("55_active");
        step(4, () -> inventoryClick(16, 0));                // coal
        step(3, () -> pointZone(BLAST, FurnaceZones.FUEL));
        step(12, () -> shot("56_carry_fuel"));
        step(2, () -> clickZone(FurnaceZones.FUEL, 0));
        step(6, () -> inventoryClick(15, 0));                // raw iron, charged from the top
        step(3, () -> pointZone(BLAST, FurnaceZones.INPUT));
        step(12, () -> shot("57_carry_ore"));
        step(2, () -> clickZone(FurnaceZones.INPUT, 0));
        step(8, () -> shot("58_charged"));
        step(4, () -> pointZone(BLAST, FurnaceZones.RESULT));
        step(40, () -> shot("59_lit"));
        step(170, () -> shot("60_blasted"));
        step(4, () -> shot("61_result_hover"));
        step(2, () -> report("blast furnace before taking"));
        step(2, () -> pointZone(BLAST, FurnaceZones.RESULT));
        step(2, () -> clickZone(FurnaceZones.RESULT, 0));
        step(10, () -> shot("62_ingots_in_hand"));
        step(2, () -> inventoryClick(17, 0));
        step(6, () -> report("blast furnace after taking"));
        step(2, Scene::leave);
        step(50, () -> shot("59_after"));
        nextAct("6", new Vec3(-4.5, -60, 11.6), 180f, ENCHANT, new Vec3(0.5, 0.75, 0.6), Direction.UP);
    }

    private static void pointZone(BlockPos pos, int slot) {
        StationKind kind = StationKind.of(Minecraft.getInstance().level.getBlockState(pos));
        FurnaceLayout layout = FurnaceLayout.of(kind);
        FurnaceLayout.Zone zone = layout.zone(slot);
        double[] c = layout.center(slot);
        pointAt(FurnaceZones.point(pos, Direction.SOUTH, zone.face(), c[0], c[1], 0));
    }

    private static void pointLedge(BlockPos pos) {
        pointAt(FurnaceZones.point(pos, Direction.SOUTH, 0.5, 0.45, 0));
    }

    private static void clickZone(int zone, int button) {
        FurnaceStation station = Scene.station(FurnaceStation.class);
        if (station == null || station.hoverZone != zone) {
            REPORT.add("cursor missed furnace zone " + zone + ": hover=" + (station == null ? "-" : station.hoverZone));
        }
        screenClick(button);
    }

    // ---- the enchanting table -----------------------------------------------------------------------

    private static void enchantScript() {
        Runnable restoreLayout = spatialEditorScript();
        shot("65_active");
        step(2, Showcase::pointAside);
        step(8, () -> {
            shot("65a_lapis_socket_empty");
            Minecraft mc = Minecraft.getInstance();
            double[] at = Picking.toGui(enchant().corner());
            double width = mc.getWindow().getGuiScaledWidth();
            double height = mc.getWindow().getGuiScaledHeight();
            boolean visible = at != null && at[0] > width * 0.1 && at[0] < width * 0.9
                    && at[1] > height * 0.15 && at[1] < height * 0.85;
            check("lapis socket stays in the usable frame", visible,
                    at == null ? "behind camera" : String.format("screen=%.3f,%.3f", at[0] / width, at[1] / height));
        });
        step(2, () -> pointAt(enchant().corner()));
        step(6, () -> {
            shot("65b_lapis_socket_hover");
            check("empty lapis socket is reachable", enchant().target == EnchantStation.Target.LAPIS && enchant().lapis() == 0,
                    "target=" + enchant().target + " lapis=" + enchant().lapis());
        });
        step(4, () -> invClickItem(Items.DIAMOND_SWORD));    // the diamond sword
        step(3, () -> pointAt(EnchantFx.top(ENCHANT).add(0, 0, 0.1)));
        step(12, () -> shot("66_carry_sword"));
        step(2, () -> clickEnchant(EnchantStation.Target.SURFACE, 0));
        step(20, () -> shot("67_sword_floats"));
        step(2, () -> invClickItem(Items.LAPIS_LAZULI));     // lapis
        step(3, () -> pointAt(enchant().corner()));
        step(10, () -> shot("68_carry_lapis"));
        step(2, () -> clickEnchant(EnchantStation.Target.LAPIS, 0));
        step(8, () -> {
            shot("68a_lapis_socket_filled");
            checkLapisCount("lapis placed in the visible socket", 12);
        });
        step(4, Showcase::pointAside);
        step(16, () -> shot("69_offers"));
        step(2, () -> pointAt(enchant().sigilPos(0)));
        step(10, () -> shot("70_hover_first"));
        step(2, () -> pointAt(enchant().sigilPos(2)));
        step(10, () -> shot("71_hover_third"));
        step(2, () -> report("enchanting before the rite"));
        step(2, () -> clickEnchant(EnchantStation.Target.OFFER, 0));      // the third, strongest offer
        step(2, () -> {
            //? if >=1.21 {
            boolean dev = Settings.developerMode(); Settings.DEVELOPER_MODE.set(true);
            //?} else if <1.21 {
            /*boolean dev = Settings.developerMode(); Settings.set(Settings.DEVELOPER_MODE, true);
            *///?}
            Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_G, 0, 0);
            check("enchant editor blocked during rite", !enchant().gridEditor.editing() && enchant().busy(), "ritual continues");
            //? if >=1.21 {
            Settings.DEVELOPER_MODE.set(dev); shot("72_rite_010");
            //?} else if <1.21 {
            /*Settings.set(Settings.DEVELOPER_MODE, dev); shot("72_rite_010");
            *///?}
        });
        step(3, () -> shot("72_rite_025"));
        step(3, () -> shot("72_rite_040"));
        step(3, () -> shot("72_rite_055"));
        step(3, () -> shot("72_rite_070"));
        step(6, () -> shot("72_rite_100"));
        step(10, () -> shot("72_rite_150"));
        step(20, () -> shot("73_enchanted"));
        step(2, () -> {
            checkLapisCount("lapis consumed by the third offer", 9);
            check("edited enchanting produced an enchanted sword", enchant().item().is(Items.DIAMOND_SWORD) && enchant().item().isEnchanted(), "vanilla enchantments applied");
        });
        step(2, () -> pointAt(enchant().itemPos()));
        step(8, () -> shot("74_item_hover"));
        step(2, () -> report("enchanting after the rite"));
        step(2, () -> clickEnchant(EnchantStation.Target.ITEM, 0));
        step(8, () -> shot("75_sword_in_hand"));
        step(2, () -> invClick(18, 0));
        step(6, () -> report("enchanting before leaving"));
        step(2, restoreLayout);
        step(25, Showcase::pointAside);
        step(2, Scene::leave);
        step(50, () -> shot("69_after"));
        step(2, () -> report("enchanting after leaving"));
        smithyAct("a", ANVIL);
    }

    private static EnchantStation enchant() {
        return Scene.station(EnchantStation.class);
    }

    private static void checkLapisCount(String name, int expected) {
        check(name + " client", enchant() != null && enchant().lapis() == expected,
                "lapis=" + (enchant() == null ? "-" : enchant().lapis()) + " expected=" + expected);
        server(p -> {
            int count = p.containerMenu instanceof EnchantmentMenu menu ? menu.getSlot(1).getItem().getCount() : -1;
            check(name + " server", count == expected, "lapis=" + count + " expected=" + expected);
        });
    }

    private static void clickEnchant(EnchantStation.Target expected, int button) {
        EnchantStation station = enchant();
        if (station == null || station.target != expected) {
            REPORT.add("cursor missed enchanting target " + expected + ": was " + (station == null ? "-" : station.target));
        }
        screenClick(button);
    }

    // ---- the smithy ----------------------------------------------------------------------------------

    /** Walks up to one of the smithy's blocks from the south and uses it. */
    private static void smithyAct(String next, BlockPos station) {
        double y = station.equals(STONECUTTER) ? 0.56 : 1.0;
        nextAct(next, new Vec3(station.getX() + 0.5, -60, station.getZ() + 3.4), 180f, station, new Vec3(0.5, y, 0.5), Direction.UP);
    }

    /** Shots through a move: {@code times} in seconds since it started. */
    private static void moveShots(String prefix, double... times) {
        double at = 0;
        for (double time : times) {
            // a step runs one tick after its delay runs out
            int ticks = (int) Math.max(2, Math.round((time - at) * 20));
            at += ticks / 20.0;
            String name = String.format("%s_%03d", prefix, Math.round(time * 100));
            step(ticks - 1, () -> shot(name));
        }
    }

    private static AnvilStation anvil() {
        return Scene.station(AnvilStation.class);
    }

    private static void clickAnvil(AnvilStation.Target expected, int button) {
        AnvilStation station = anvil();
        if (station == null || station.target != expected) {
            REPORT.add("cursor missed anvil target " + expected + ": was " + (station == null ? "-" : station.target));
        }
        screenClick(button);
    }

    private static void anvilScript() {
        Runnable restoreLayout = spatialEditorScript();
        var model = HammerModel.model();
        //? if neoforge {
        var quads = model.getQuads(null, null, RandomSource.create(42), ModelData.EMPTY, null);
        //?} else if fabric {
        /*var quads = model.getQuads(null, null, RandomSource.create(42));
        *///?} else if forge {
        /*var quads = model.getQuads(null, null, RandomSource.create(42), ModelData.EMPTY, null);
        *///?}
        var textures = new HashSet<ResourceLocation>();
        for (var quad : quads) {
            textures.add(quad.getSprite().contents().name());
        }
        check("imported hammer model and textures loaded", model != Minecraft.getInstance().getModelManager().getMissingModel()
                && quads.size() == 24 && textures.size() == 4
                && textures.stream().allMatch(texture -> texture.getNamespace().equals(Cinematic.MODID)
                        && texture.getPath().startsWith("item/prop/hammer_")), "faces=" + quads.size() + " textures=" + textures);
        shot("a5_active");
        step(4, () -> invClickItem(Items.IRON_PICKAXE));         // a worn pickaxe
        step(3, () -> pointAt(anvil().restPoint(0)));
        step(12, () -> shot("a6_carry_pickaxe"));
        step(2, () -> clickAnvil(AnvilStation.Target.SURFACE, 0));
        step(10, () -> shot("a7_pickaxe_laid"));
        step(2, () -> invClickItem(Items.ENCHANTED_BOOK));       // Efficiency III goes into it
        step(3, () -> pointAt(anvil().restPoint(1)));
        step(10, () -> shot("a8_carry_book"));
        step(2, () -> clickAnvil(AnvilStation.Target.SURFACE, 0));
        step(4, Showcase::pointAside);
        step(14, () -> shot("a9_pile_cost"));
        step(2, () -> pointAt(anvil().restPoint(1)));
        step(8, () -> shot("aa_pile_tooltip"));
        step(2, () -> report("anvil before forging"));
        step(2, () -> clickAnvil(AnvilStation.Target.STACK, 0));
        moveShots("ab_forge", 0.25, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 1.3, 1.45, 1.65, 1.85, 2.05);
        step(10, () -> shot("ac_forged"));
        step(2, () -> {
            ItemStack result = Minecraft.getInstance().player.containerMenu.getCarried();
            check("hammer forging produces enchanted pickaxe client", result.is(Items.IRON_PICKAXE) && result.isEnchanted(),
                    "result=" + result);
            server(p -> {
                ItemStack forged = p.containerMenu.getCarried();
                check("hammer forging produces enchanted pickaxe server", forged.is(Items.IRON_PICKAXE) && forged.isEnchanted()
                        && p.containerMenu.getSlot(0).getItem().isEmpty() && p.containerMenu.getSlot(1).getItem().isEmpty(),
                        "result=" + forged);
            });
        });
        step(2, () -> report("anvil after forging"));
        // the new pickaxe back on the anvil, to engrave a name
        step(2, () -> pointAt(anvil().restPoint(0)));
        step(6, () -> clickAnvil(AnvilStation.Target.SURFACE, 0));
        step(8, () -> pointAt(anvil().namePoint()));
        step(8, () -> shot("ad_name_hover"));
        step(2, () -> clickAnvil(AnvilStation.Target.NAME, 0));
        step(6, () -> shot("ae_name_field"));
        step(2, () -> SceneScreen.commitEdit("Кирка мастера"));
        step(4, Showcase::pointAside);
        step(10, () -> shot("af_renamed"));
        step(2, () -> pointAt(anvil().restPoint(0)));
        step(6, () -> clickAnvil(AnvilStation.Target.STACK, 0));
        step(46, () -> shot("ag_named"));
        step(2, () -> invClick(28, 0));
        step(6, () -> report("anvil before leaving"));
        step(2, () -> server(p -> {
            ItemStack named = p.getInventory().getItem(28);
            check("hammer engraving preserves the forged item", named.is(Items.IRON_PICKAXE) && named.isEnchanted()
                    && named.getHoverName().getString().equals("Кирка мастера"), "result=" + named.getHoverName().getString());
        }));
        step(2, restoreLayout);
        step(2, Scene::leave);
        step(50, () -> shot("a9_after"));
        smithyAct("g", GRINDSTONE);
    }

    private static GrindstoneStation grindstone() {
        return Scene.station(GrindstoneStation.class);
    }

    private static void clickGrindstone(GrindstoneStation.Target expected, int button) {
        GrindstoneStation station = grindstone();
        if (station == null || station.target != expected) {
            REPORT.add("cursor missed grindstone target " + expected + ": was " + (station == null ? "-" : station.target));
        }
        screenClick(button);
    }

    private static void grindstoneScript() {
        Runnable restoreLayout = spatialEditorScript();
        shot("g5_active");
        step(4, () -> invClickItem(Items.GOLDEN_SWORD));         // Sharpness III, Looting II
        step(3, () -> pointAt(grindstone().restPoint(0)));
        step(12, () -> shot("g6_carry_sword"));
        step(2, () -> clickGrindstone(GrindstoneStation.Target.PLACE, 0));
        step(4, Showcase::pointAside);
        step(10, () -> shot("g7_on_wheel"));
        step(2, () -> pointAt(grindstone().restPoint(0)));
        step(8, () -> shot("g8_tooltip"));
        step(2, () -> report("grindstone before grinding"));
        step(2, () -> clickGrindstone(GrindstoneStation.Target.ITEM, 0));
        moveShots("g9_grind", 0.15, 0.4, 0.62, 0.8, 1.0, 1.2, 1.4, 1.55, 1.75, 1.95, 2.15, 2.4);
        step(12, () -> shot("ga_ground"));
        step(30, () -> report("grindstone after grinding"));
        step(2, () -> invClick(29, 0));
        step(4, Showcase::pointAside);
        step(2, restoreLayout);
        step(2, Scene::leave);
        step(50, () -> shot("g9_after"));
        smithyAct("s", SMITHING);
    }

    private static SmithingStation smithing() {
        return Scene.station(SmithingStation.class);
    }

    private static void clickSmithing(SmithingStation.Target expected, int button) {
        SmithingStation station = smithing();
        if (station == null || station.target != expected) {
            REPORT.add("cursor missed smithing target " + expected + ": was " + (station == null ? "-" : station.target));
        }
        screenClick(button);
    }

    /** Takes an item from the inventory and lays it on its spot of the smithing table. */
    private static void laySmithing(net.minecraft.world.item.Item item, int slot, String shotName, int button) {
        step(4, () -> invClickItem(item));
        step(3, () -> pointAt(smithing().restPoint(slot)));
        if (shotName != null) {
            step(10, () -> shot(shotName));
        }
        step(4, () -> clickSmithing(SmithingStation.Target.PLACE, button));
    }

    private static void smithingScript() {
        Runnable restoreLayout = surfaceEditorScript();
        shot("s5_active");
        laySmithing(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 0, "s6_carry_template", 0);
        laySmithing(Items.DIAMOND_CHESTPLATE, 1, null, 0);
        laySmithing(Items.NETHERITE_INGOT, 2, "s6_carry_ingot", 0);
        step(4, Showcase::pointAside);
        step(10, () -> shot("s7_laid"));
        step(2, () -> pointAt(smithing().restPoint(1)));
        step(8, () -> shot("s8_tooltip"));
        step(2, () -> report("smithing before the upgrade"));
        step(2, () -> clickSmithing(SmithingStation.Target.ITEM, 0));
        moveShots("s9_upgrade", 0.2, 0.4, 0.5, 0.75, 0.95, 1.15, 1.3, 1.4, 1.6, 1.9, 2.2, 2.6);
        step(10, () -> shot("sa_netherite"));
        step(2, () -> check("edited smithing produces netherite", carried().is(Items.NETHERITE_CHESTPLATE), "real vanilla result=" + carried()));
        step(2, () -> report("smithing after the upgrade"));
        // the new chestplate goes straight back with a coast trim of amethyst
        step(3, () -> pointAt(smithing().restPoint(1)));
        step(6, () -> clickSmithing(SmithingStation.Target.PLACE, 0));
        laySmithing(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, 0, null, 0);
        laySmithing(Items.AMETHYST_SHARD, 2, null, 1);                // one shard; the other two back
        step(4, () -> invClick(30, 0));
        step(2, () -> pointAt(smithing().restPoint(1)));
        step(8, () -> shot("sb_trim_laid"));
        step(2, () -> clickSmithing(SmithingStation.Target.ITEM, 0));
        moveShots("sc_trim", 0.5, 0.95, 1.2, 1.4, 1.9);
        step(16, () -> shot("sd_trimmed"));
        //? if >=1.21 {
        step(2, () -> check("edited smithing applies trim", carried().has(DataComponents.TRIM), "real trim=" + carried()));
        //?} else if <1.21 {
        /*step(2, () -> check("edited smithing applies trim", net.minecraft.world.item.armortrim.ArmorTrim.getTrim(Minecraft.getInstance().level.registryAccess(), carried()).isPresent(), "real trim=" + carried()));
        *///?}
        step(2, () -> invClick(31, 0));
        step(6, () -> report("smithing after the trim"));
        step(2, restoreLayout);
        step(25, Showcase::pointAside);
        step(2, Scene::leave);
        step(50, () -> shot("s9_after"));
        smithyAct("c", STONECUTTER);
    }

    private static StonecutterStation stonecutter() {
        return Scene.station(StonecutterStation.class);
    }

    private static void clickStonecutter(StonecutterStation.Target expected, int button) {
        StonecutterStation station = stonecutter();
        if (station == null || station.target != expected) {
            REPORT.add("cursor missed stonecutter target " + expected + ": was " + (station == null ? "-" : station.target));
        }
        screenClick(button);
    }

    /** Index of the shape that makes {@code item} on the stonecutter's dial, or -1. */
    private static int shape(net.minecraft.world.item.Item item) {
        Minecraft mc = Minecraft.getInstance();
        var menu = stonecutter().menu();
        for (int i = 0; menu != null && i < menu.getRecipes().size(); i++) {
            //? if >=1.21 {
            if (menu.getRecipes().get(i).value().getResultItem(mc.level.registryAccess()).is(item)) {
            //?} else if <1.21 {
            /*if (menu.getRecipes().get(i).getResultItem(mc.level.registryAccess()).is(item)) {
            *///?}
                return i;
            }
        }
        REPORT.add("no stonecutter shape for " + item);
        return 0;
    }

    private static void stonecutterScript() {
        Runnable restoreLayout = spatialEditorScript();
        shot("c5_active");
        step(4, () -> invClickItem(Items.STONE));
        step(3, () -> pointAt(stonecutter().restPoint(0)));
        step(10, () -> shot("c6_carry_stone"));
        step(2, () -> clickStonecutter(StonecutterStation.Target.PLACE, 0));
        step(4, Showcase::pointAside);
        step(14, () -> shot("c7_dial"));
        step(2, () -> pointAt(stonecutter().shapePoint(shape(Items.STONE_BRICKS))));
        step(8, () -> shot("c8_hover_bricks"));
        step(2, () -> clickStonecutter(StonecutterStation.Target.SHAPE, 0));
        moveShots("c9_cut", 0.1, 0.25, 0.4, 0.5, 0.6, 0.75, 0.95);
        step(8, () -> shot("ca_bricks_in_hand"));
        step(2, () -> invClick(32, 0));
        step(2, () -> pointAt(stonecutter().shapePoint(shape(Items.STONE_SLAB))));
        step(6, () -> clickStonecutter(StonecutterStation.Target.SHAPE, 0));
        step(12, () -> shot("cb_cut_slab"));
        step(14, () -> invClick(33, 0));
        step(2, () -> report("stonecutter after two cuts"));
        step(2, () -> pointAt(stonecutter().shapePoint(shape(Items.STONE_STAIRS))));
        step(6, () -> {
            StonecutterStation station = stonecutter();
            if (station == null || station.target != StonecutterStation.Target.SHAPE) {
                REPORT.add("cursor missed the stairs");
            }
            if (station != null) {
                station.click(0, true);                              // shift: the whole stack
            }
        });
        moveShots("cc_cut_all", 0.3, 0.6, 0.9, 1.3);
        step(10, () -> report("stonecutter after cutting all"));
        step(2, () -> invClickItem(Items.COBBLED_DEEPSLATE));      // many shapes: the dial turns
        step(3, () -> pointAt(stonecutter().restPoint(0)));
        step(6, () -> clickStonecutter(StonecutterStation.Target.PLACE, 0));
        step(4, Showcase::pointAside);
        step(14, () -> shot("cd_dial_many"));
        step(2, () -> {
            for (int i = 0; i < 4; i++) {
                stonecutter().scroll(-1);
            }
        });
        step(12, () -> shot("ce_dial_turned"));
        step(4, () -> report("stonecutter before leaving"));
        step(2, restoreLayout);
        step(2, Scene::leave);
        step(50, () -> shot("c9_after"));
        restock();
        smithyAct("l", LOOM);
    }

    // ---- the second workshop ---------------------------------------------------------------------------

    /** Fresh things for the second workshop in the main inventory (the first workshop's results are reported by now). */
    private static void restock() {
        step(2, () -> server(p -> {
            ServerLevel level = p.serverLevel();
            for (int i = 9; i < 36; i++) {
                p.getInventory().setItem(i, ItemStack.EMPTY);
            }
            ItemStack map = net.minecraft.world.item.MapItem.create(level, 11, 14, (byte) 0, true, false);
            // chart the land round the workshop, as walking about with the map would
            var data = net.minecraft.world.item.MapItem.getSavedData(map, level);
            for (int pass = 0; pass < 8 && data != null; pass++) {
                ((net.minecraft.world.item.MapItem) Items.FILLED_MAP).update(level, p, data);
            }
            //? if >=1.21 {
            ItemStack water = net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION,
                    net.minecraft.world.item.alchemy.Potions.WATER);
            //?} else if <1.21 {
            /*ItemStack water = net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(Items.POTION), net.minecraft.world.item.alchemy.Potions.WATER);
            *///?}
            ItemStack[] stock = {new ItemStack(Items.WHITE_BANNER, 2), new ItemStack(Items.RED_DYE, 4), map, new ItemStack(Items.PAPER, 2),
                    new ItemStack(Items.GLASS_PANE), water.copy(), water.copy(), water.copy(), new ItemStack(Items.NETHER_WART, 2),
                    new ItemStack(Items.BLAZE_POWDER, 2), new ItemStack(Items.CHICKEN, 3), new ItemStack(Items.MUTTON, 2),
                    new ItemStack(Items.OAK_PLANKS, 8), new ItemStack(Items.GOLD_INGOT, 2), new ItemStack(Items.BEEF, 3)};
            for (int i = 0; i < stock.length; i++) {
                p.getInventory().setItem(9 + i, stock[i]);
            }
            p.inventoryMenu.broadcastChanges();
        }));
    }

    /** Puts what is in hand into the first empty inventory slot. */
    private static void stash() {
        LocalPlayer player = Minecraft.getInstance().player;
        for (int i = 9; i < 36; i++) {
            if (player.getInventory().getItem(i).isEmpty()) {
                invClick(i, 0);
                return;
            }
        }
        REPORT.add("no empty slot to stash " + player.containerMenu.getCarried());
    }

    private static ItemStack carried() {
        return Minecraft.getInstance().player.containerMenu.getCarried();
    }

    private static LoomStation loom() {
        return Scene.station(LoomStation.class);
    }

    private static void clickLoom(LoomStation.Target expected, int button) {
        LoomStation station = loom();
        if (station == null || station.target != expected) {
            REPORT.add("cursor missed loom target " + expected + ": was " + (station == null ? "-" : station.target));
        }
        screenClick(button);
    }

    private static void pointPattern(String asset) {
        LoomStation station = loom();
        int k = station == null ? -1 : station.patternIndex(asset);
        double[] at = k < 0 ? null : station.boardCell(k);
        if (at == null) {
            REPORT.add("pattern " + asset + " not on the board");
            return;
        }
        pointGui(at[0], at[1]);
    }

    private static void loomScript() {
        Runnable restoreLayout = spatialEditorScript();
        shot("l5_active");
        step(4, () -> invClickItem(Items.WHITE_BANNER));
        step(3, () -> pointAt(loom().restPoint(0)));
        step(10, () -> shot("l6_carry_banner"));
        step(2, () -> clickLoom(LoomStation.Target.PLACE, 1));         // one banner onto the warp
        step(4, Showcase::stash);
        step(4, () -> invClickItem(Items.RED_DYE));
        step(6, () -> pointAt(loom().restPoint(1)));
        step(1, () -> clickLoom(LoomStation.Target.PLACE, 0));
        step(4, Showcase::pointAside);
        step(16, () -> {
            //? if >=1.21 {
            shot("l7_board"); Settings.DEVELOPER_MODE.set(true);
            //?} else if <1.21 {
            /*shot("l7_board"); Settings.set(Settings.DEVELOPER_MODE, true);
            *///?}
            double[] gear = loom().gridEditor.togglePoint(); pointGui(gear[0], gear[1]); screenClick(0);
        });
        step(22, () -> {
            check("loom gear accessible with pattern board", loom().gridEditor.editing() && loom().boardCell(0) == null,
                    "editor consumes input, patterns hidden");
            check("loom live inputs preserved", loom().slot(0).is(Items.WHITE_BANNER) && loom().slot(1).is(Items.RED_DYE), "banner and dye intact");
            pointAside(); shot("xl4_editor_with_cloth"); Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0);
        });
        //? if >=1.21 {
        step(25, () -> { Settings.DEVELOPER_MODE.set(false); check("loom patterns return after editor", loom().boardCell(0) != null, "board visible"); });
        //?} else if <1.21 {
        /*step(25, () -> { Settings.set(Settings.DEVELOPER_MODE, false); check("loom patterns return after editor", loom().boardCell(0) != null, "board visible"); });
        *///?}
        step(3, () -> server(p -> p.getInventory().add(new ItemStack(Items.FLOWER_BANNER_PATTERN))));
        step(5, () -> invClickItem(Items.FLOWER_BANNER_PATTERN));
        step(3, () -> pointAt(loom().restPoint(2)));
        step(5, () -> clickLoom(LoomStation.Target.PLACE, 0));
        step(8, () -> {
            check("loom edited pattern slot accepts template", loom().slot(2).is(Items.FLOWER_BANNER_PATTERN)
                    && loom().patternIndex("minecraft:flower") >= 0, "vanilla special pattern available");
            pointAt(loom().restPoint(2));
        });
        step(5, () -> clickLoom(LoomStation.Target.ITEM, 0));
        step(5, () -> {
            check("loom template retrieved from edited slot", carried().is(Items.FLOWER_BANNER_PATTERN) && loom().slot(2).isEmpty(), "same logical slot");
            stash();
        });
        step(2, () -> loom().scroll(-1));
        step(12, () -> shot("l7b_board_scrolled"));
        step(2, () -> loom().scroll(1));
        step(12, () -> pointPattern("minecraft:cross"));
        step(10, () -> shot("l8_hover_cross"));
        step(2, () -> report("loom before weaving"));
        step(2, () -> pointPattern("minecraft:cross"));
        step(1, () -> check("loom pattern under the cursor", loom() != null && loom().target == LoomStation.Target.PATTERN,
                "target=" + (loom() == null ? "-" : loom().target)));
        step(1, () -> clickLoom(LoomStation.Target.PATTERN, 0));
        step(1, () -> {
            //? if >=1.21 {
            Settings.DEVELOPER_MODE.set(true); Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_G, 0, 0);
            //?} else if <1.21 {
            /*Settings.set(Settings.DEVELOPER_MODE, true); Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_G, 0, 0);
            *///?}
            check("loom editor blocked during weaving", !loom().gridEditor.editing() && loom().moving(), "shuttle continues");
            //? if >=1.21 {
            Settings.DEVELOPER_MODE.set(false);
            //?} else if <1.21 {
            /*Settings.set(Settings.DEVELOPER_MODE, false);
            *///?}
        });
        moveShots("l9_weave", 0.15, 0.4, 0.7, 1.0, 1.3, 1.6, 1.9, 2.1, 2.3);
        step(12, () -> shot("la_woven"));
        step(2, () -> {
            //? if >=1.21 {
            var layers = carried().getOrDefault(net.minecraft.core.component.DataComponents.BANNER_PATTERNS,
                    net.minecraft.world.level.block.entity.BannerPatternLayers.EMPTY).layers();
            //?} else if <1.21 {
            /*var layers = LoomStation.layersOf(carried());
            *///?}
            check("woven banner in hand", carried().is(Items.WHITE_BANNER) && layers.size() == 1
                    //? if >=1.21 {
                    && layers.get(0).pattern().value().assetId().getPath().equals("cross"), "carried=" + carried() + " layers=" + layers.size());
                    //?} else if <1.21 {
                    /*&& layers.get(0).pattern().unwrapKey().map(k -> k.location().getPath()).orElse("").equals("cross"),
                    "carried=" + carried() + " layers=" + layers.size());
                    *///?}
        });
        step(2, Showcase::stash);
        step(4, () -> report("loom after weaving"));
        step(2, restoreLayout);
        step(25, Showcase::pointAside);
        step(2, Scene::leave);
        step(50, () -> shot("l9_after"));
        smithyAct("m", CARTOGRAPHY);
    }

    private static CartographyStation cartography() {
        return Scene.station(CartographyStation.class);
    }

    private static void clickCartography(CartographyStation.Target expected, int button) {
        CartographyStation station = cartography();
        if (station == null || station.target != expected) {
            REPORT.add("cursor missed cartography target " + expected + ": was " + (station == null ? "-" : station.target));
        }
        screenClick(button);
    }

    private static void cartographyScript() {
        Runnable restoreLayout = surfaceEditorScript();
        shot("m5_active");
        step(4, () -> invClickItem(Items.FILLED_MAP));
        step(3, () -> pointAt(cartography().restPoint(0)));
        step(6, () -> clickCartography(CartographyStation.Target.PLACE, 0));
        step(4, () -> invClickItem(Items.PAPER));
        step(3, () -> pointAt(cartography().restPoint(1)));
        step(10, () -> shot("m6_carry_paper"));
        step(2, () -> clickCartography(CartographyStation.Target.PLACE, 1));
        step(4, Showcase::stash);
        step(4, Showcase::pointAside);
        step(10, () -> shot("m7_laid"));
        step(2, () -> pointAt(cartography().restPoint(0)));
        step(8, () -> shot("m8_tooltip"));
        step(2, () -> report("cartography before zooming"));
        step(2, () -> clickCartography(CartographyStation.Target.ITEM, 0));
        moveShots("m9_zoom", 0.2, 0.45, 0.7, 1.0, 1.3, 1.6, 1.9);
        step(10, () -> shot("ma_zoomed"));
        step(2, () -> check("zoomed map in hand", carried().is(Items.FILLED_MAP), "carried=" + carried()));
        step(2, Showcase::stash);
        // Copy with the edited map and addition spots before locking one of the resulting maps.
        step(8, () -> server(p -> p.getInventory().add(new ItemStack(Items.MAP))));
        step(4, () -> invClickItem(Items.FILLED_MAP));
        step(3, () -> pointAt(cartography().restPoint(0)));
        step(6, () -> clickCartography(CartographyStation.Target.PLACE, 1));
        step(4, Showcase::stash);
        step(4, () -> invClickItem(Items.MAP));
        step(3, () -> pointAt(cartography().restPoint(1)));
        step(6, () -> clickCartography(CartographyStation.Target.PLACE, 0));
        step(4, () -> pointAt(cartography().restPoint(0)));
        step(6, () -> clickCartography(CartographyStation.Target.ITEM, 0));
        moveShots("mc_copy", 0.3, 0.6, 1.0, 1.2, 1.4, 1.7);
        step(10, () -> check("copied maps in hand", carried().is(Items.FILLED_MAP) && carried().getCount() == 2,
                "carried=" + carried()));
        step(2, Showcase::stash);
        // lock the new map under glass
        step(4, () -> {
            LocalPlayer player = Minecraft.getInstance().player;
            for (int i = 35; i >= 9; i--) {
                if (player.getInventory().getItem(i).is(Items.FILLED_MAP)) {
                    invClick(i, 0);
                    return;
                }
            }
        });
        step(3, () -> pointAt(cartography().restPoint(0)));
        step(6, () -> clickCartography(CartographyStation.Target.PLACE, 0));
        step(4, () -> invClickItem(Items.GLASS_PANE));
        step(3, () -> pointAt(cartography().restPoint(1)));
        step(6, () -> clickCartography(CartographyStation.Target.PLACE, 0));
        step(4, () -> pointAt(cartography().restPoint(0)));
        step(6, () -> clickCartography(CartographyStation.Target.ITEM, 0));
        moveShots("mb_lock", 0.3, 0.6, 1.0, 1.2, 1.4, 1.7);
        step(10, () -> server(p -> {
            ItemStack held = p.containerMenu.getCarried();
            var data = net.minecraft.world.item.MapItem.getSavedData(held, p.level());
            check("locked map in hand", held.is(Items.FILLED_MAP) && data != null && data.locked, "carried=" + held);
        }));
        step(2, Showcase::stash);
        step(4, () -> report("cartography before leaving"));
        step(2, restoreLayout);
        step(25, Showcase::pointAside);
        step(2, Scene::leave);
        step(50, () -> shot("m9_after"));
        smithyAct("b", BREWING);
    }

    private static BrewingStation brewing() {
        return Scene.station(BrewingStation.class);
    }

    private static void clickBrewing(BrewingStation.Target expected, int button) {
        BrewingStation station = brewing();
        if (station == null || station.target != expected) {
            REPORT.add("cursor missed brewing target " + expected + ": was " + (station == null ? "-" : station.target));
        }
        screenClick(button);
    }

    private static void brewingScript() {
        Runnable restoreLayout = spatialEditorScript();
        shot("b5_active");
        for (int i = 0; i < 3; i++) {
            int bottle = i;
            step(4, () -> invClickItem(Items.POTION));
            step(3, () -> pointAt(brewing().handle(bottle)));
            if (i == 0) {
                step(10, () -> shot("b6_carry_bottle"));
            }
            step(6, () -> clickBrewing(BrewingStation.Target.PLACE, 0));
        }
        step(4, () -> invClickItem(Items.BLAZE_POWDER));
        step(3, () -> pointAt(brewing().handle(BrewingStation.FUEL)));
        step(6, () -> clickBrewing(BrewingStation.Target.PLACE, 0));
        step(4, () -> invClickItem(Items.NETHER_WART));
        step(3, () -> pointAt(brewing().handle(BrewingStation.INGREDIENT)));
        step(10, () -> shot("b6_carry_wart"));
        step(2, () -> clickBrewing(BrewingStation.Target.PLACE, 1));
        step(4, Showcase::stash);
        step(4, Showcase::pointAside);
        step(30, () -> shot("b7_brewing_start"));
        step(2, () -> {
            Minecraft mc = Minecraft.getInstance();
            net.minecraft.world.level.block.state.BlockState empty = Blocks.BREWING_STAND.defaultBlockState();
            net.minecraft.world.level.block.state.BlockState full = empty;
            for (var bottle : net.minecraft.world.level.block.BrewingStandBlock.HAS_BOTTLE) {
                full = full.setValue(bottle, true);
            }
            // the stand's multipart model picks its parts by the state: count what it would draw for each
            int withBottles = 0, without = 0;
            for (Direction face : new Direction[]{null, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN}) {
                withBottles += mc.getBlockRenderer().getBlockModel(full).getQuads(full, face, RandomSource.create(1)).size();
                without += mc.getBlockRenderer().getBlockModel(empty).getQuads(empty, face, RandomSource.create(1)).size();
            }
            check("stand's own bottle pictures left out", withBottles == without, "quads with bottles=" + withBottles + " without=" + without);
            boolean known = mc.level.getBlockEntity(BREWING) instanceof net.minecraft.world.level.block.entity.BrewingStandBlockEntity stand
                    && ClientBrewing.item(stand, 0).is(Items.POTION) && ClientBrewing.item(stand, 2).is(Items.POTION);
            check("client knows the potions in the stand", known, "");
        });
        step(100, () -> shot("b8_brewing_mid"));
        step(2, () -> pointAt(brewing().handle(BrewingStation.INGREDIENT)));
        step(10, () -> shot("b8_ingredient_tooltip"));
        step(2, Showcase::pointAside);
        step(150, () -> shot("b8_brewing_late"));
        step(150, () -> shot("b9_done"));
        step(4, () -> server(p -> {
            if (p.serverLevel().getBlockEntity(BREWING) instanceof net.minecraft.world.level.block.entity.BrewingStandBlockEntity stand) {
                int awkward = 0;
                for (int i = 0; i < 3; i++) {
                    //? if >=1.21 {
                    var contents = stand.getItem(i).get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
                    if (contents != null && contents.is(net.minecraft.world.item.alchemy.Potions.AWKWARD)) {
                    //?} else if <1.21 {
                    /*if (net.minecraft.world.item.alchemy.PotionUtils.getPotion(stand.getItem(i)) == net.minecraft.world.item.alchemy.Potions.AWKWARD) {
                    *///?}
                        awkward++;
                    }
                }
                check("three awkward potions brewed", awkward == 3, "awkward=" + awkward + " wart=" + stand.getItem(3));
            }
        }));
        // the brew changed the potions in place: watchers are told without anyone touching the stand
        step(4, () -> {
            int awkward = 0;
            if (Minecraft.getInstance().level.getBlockEntity(BREWING) instanceof net.minecraft.world.level.block.entity.BrewingStandBlockEntity stand) {
                for (int i = 0; i < 3; i++) {
                    //? if >=1.21 {
                    var contents = ClientBrewing.item(stand, i).get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
                    if (contents != null && contents.is(net.minecraft.world.item.alchemy.Potions.AWKWARD)) awkward++;
                    //?} else if <1.21 {
                    /*if (net.minecraft.world.item.alchemy.PotionUtils.getPotion(ClientBrewing.item(stand, i)) == net.minecraft.world.item.alchemy.Potions.AWKWARD) awkward++;
                    *///?}
                }
            }
            check("client sees the brewed potions", awkward == 3, "awkward=" + awkward);
            //? if neoforge {
            if (net.neoforged.fml.ModList.get().isLoaded("amendments")
            //?} else if fabric {
            /*if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("amendments")
            *///?} else if forge {
            /*if (net.minecraftforge.fml.ModList.get().isLoaded("amendments")
            *///?}
                    && Minecraft.getInstance().level.getBlockEntity(BREWING) instanceof net.minecraft.world.level.block.entity.BrewingStandBlockEntity stand) {
                // Amendments syncs the stand through its own update packet: that data still reaches the client
                check("Amendments' own stand data reaches the client", stand.getItem(0).is(Items.POTION) && stand.getItem(2).is(Items.POTION),
                        "slot0=" + describe(stand.getItem(0)));
            }
        });
        step(2, () -> pointAt(brewing().handle(0)));
        step(6, () -> clickBrewing(BrewingStation.Target.ITEM, 0));
        step(8, () -> shot("b9_bottle_in_hand"));
        step(2, Showcase::stash);
        step(4, restoreLayout);
        step(4, () -> report("brewing before leaving"));
        step(2, Scene::leave);
        step(50, () -> shot("b9_after"));
        // far away the chunk is dropped; coming back, the stand's contents arrive with the chunk again
        step(2, () -> server(p -> p.teleportTo(p.serverLevel(), 600.5, -60, 600.5, 0f, 20f)));
        step(80, () -> check("brewing stand forgotten with its chunk",
                !(Minecraft.getInstance().level.getBlockEntity(BREWING) instanceof net.minecraft.world.level.block.entity.BrewingStandBlockEntity), "unloaded"));
        step(2, () -> server(p -> p.teleportTo(p.serverLevel(), BREWING.getX() + 0.5, -60, BREWING.getZ() + 3.5, 180f, 20f)));
        step(80, () -> {
            String client = "-";
            int potions = 0;
            if (Minecraft.getInstance().level.getBlockEntity(BREWING) instanceof net.minecraft.world.level.block.entity.BrewingStandBlockEntity stand) {
                client = "";
                for (int i = 0; i < 3; i++) {
                    client += describe(ClientBrewing.item(stand, i)) + " ";
                    if (ClientBrewing.item(stand, i).is(Items.POTION)) potions++;
                }
            }
            int seen = potions;
            String shown = client;
            server(p -> {
                int real = 0;
                if (p.serverLevel().getBlockEntity(BREWING) instanceof net.minecraft.world.level.block.entity.BrewingStandBlockEntity stand)
                    for (int i = 0; i < 3; i++) if (stand.getItem(i).is(Items.POTION)) real++;
                check("the stand's potions come back with its chunk", real > 0 && seen == real, "server=" + real + " client=" + shown);
            });
        });
        step(4, () -> shot("b9_chunk_back"));
        //? if >=1.21 {
        smithyAct("r", CRAFTER);
        //?} else if <1.21 {
        /*// the crafter's act comes next in later versions; 1.20.1 has no crafter
        campfireAct();
        *///?}
    }

    //? if >=1.21 {
    private static CrafterStation crafter() {
        return Scene.station(CrafterStation.class);
    }

    private static void clickCrafter(CrafterStation.Target expected, int button) {
        CrafterStation station = crafter();
        if (station == null || station.target != expected) {
            REPORT.add("cursor missed crafter target " + expected + ": was " + (station == null ? "-" : station.target));
        }
        screenClick(button);
    }

    private static void crafterScript() {
        Runnable restoreLayout = spatialEditorScript();
        shot("r5_active");
        step(4, () -> invClickItem(Items.OAK_PLANKS));
        for (int cell : new int[]{0, 1, 3, 4}) {
            step(3, () -> pointAt(crafter().cell(cell)));
            if (cell == 0) {
                step(10, () -> shot("r6_carry_planks"));
            }
            step(5, () -> clickCrafter(CrafterStation.Target.CELL, 1));
        }
        step(4, Showcase::stash);
        step(4, Showcase::pointAside);
        step(14, () -> shot("r7_grid"));
        step(2, () -> check("crafter shows a crafting table", crafter() != null && crafter().slot(CrafterStation.RESULT).is(Items.CRAFTING_TABLE),
                "result=" + (crafter() == null ? "-" : crafter().slot(CrafterStation.RESULT))));
        step(2, () -> pointAt(crafter().cell(8)));
        step(6, () -> clickCrafter(CrafterStation.Target.CELL, 0));      // an empty window, empty hand: shut it
        step(10, () -> shot("r8_shut"));
        step(2, () -> check("crafter window shut", crafter() != null && crafter().menu() != null && crafter().menu().isSlotDisabled(8), ""));
        step(2, () -> pointAt(crafter().resultPoint()));
        step(10, () -> shot("r8_result_tooltip"));
        step(2, () -> server(p -> p.serverLevel().setBlockAndUpdate(CRAFTER.east(), Blocks.REDSTONE_BLOCK.defaultBlockState())));
        step(5, () -> shot("r9_signal"));
        step(10, () -> shot("r9_crafted"));
        step(2, () -> server(p -> {
            List<ItemEntity> made = p.serverLevel().getEntitiesOfClass(ItemEntity.class, new AABB(CRAFTER).inflate(3));
            // thrown out of its mouth towards the player, who may have picked it up already
            boolean table = made.stream().anyMatch(e -> e.getItem().is(Items.CRAFTING_TABLE)) || p.getInventory().contains(new ItemStack(Items.CRAFTING_TABLE));
            check("crafter made a crafting table on the signal", table, "items=" + made.size());
            p.serverLevel().setBlockAndUpdate(CRAFTER.east(), Blocks.AIR.defaultBlockState());
        }));
        step(4, restoreLayout);
        step(4, () -> report("crafter before leaving"));
        step(2, Scene::leave);
        step(50, () -> shot("r9_after"));
        if (ONLY) step(2, () -> finish(Minecraft.getInstance()));
        else campfireAct();
    }

    //?}
    private static final long[] beefPace = new long[3];

    /** Cooked beef dropped round the campfire or already picked up by the player. */
    private static long cookedBeef(ServerPlayer p, BlockPos fire) {
        long count = 0;
        var box = new net.minecraft.world.phys.AABB(fire.getX() - 4, fire.getY() - 2, fire.getZ() - 4,
                fire.getX() + 5, fire.getY() + 3, fire.getZ() + 5);
        for (var item : p.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box)) {
            if (item.getItem().is(Items.COOKED_BEEF)) count += item.getItem().getCount();
        }
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (p.getInventory().getItem(i).is(Items.COOKED_BEEF)) count += p.getInventory().getItem(i).getCount();
        }
        ItemStack carried = p.containerMenu.getCarried();
        return carried.is(Items.COOKED_BEEF) ? count + carried.getCount() : count;
    }

    private static CampfireStation campfire() {
        return Scene.station(CampfireStation.class);
    }

    private static void clickCampfire(CampfireStation.Target expected, int button) {
        CampfireStation station = campfire();
        if (station == null || station.target != expected) {
            REPORT.add("cursor missed campfire target " + expected + ": was " + (station == null ? "-" : station.target));
        }
        screenClick(button);
    }

    /** Looks at the campfire (the invitation shows) and presses the sit key, as a player would. */
    private static void campfireAct() {
        step(2, () -> {
            act = "f";
            //? if >=1.21 {
            server(p -> p.teleportTo(p.serverLevel(), CAMPFIRE.getX() + 0.5, -60, CAMPFIRE.getZ() + 2.9, 180f, 24f));
            //?} else if <1.21 {
            /*server(p -> {
                if ("f".equals(FROM)) {
                    p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
                    p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
                }
                p.teleportTo(p.serverLevel(), CAMPFIRE.getX() + 0.5, -60, CAMPFIRE.getZ() + 2.9, 180f, 24f);
            });
            *///?}
        });
        step(30, () -> shot("f0_hint"));
        campfireHintFadeScript("f");
        step(2, () -> {
            Minecraft mc = Minecraft.getInstance();
            boolean looking = mc.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(CAMPFIRE);
            check("looking at the campfire for the hint", looking, "hit=" + mc.hitResult);
            introShot = 0;
            outroShot = 0;
            waitingForActive = true;
            //? if neoforge {
            net.minecraft.client.KeyMapping.click(CampfireHint.SIT.getKey());
            //?} else if fabric {
            /*net.minecraft.client.KeyMapping.click(net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.getBoundKeyOf(CampfireHint.SIT));
            *///?} else if forge {
            /*net.minecraft.client.KeyMapping.click(CampfireHint.SIT.getKey());
            *///?}
        });
        step(6, () -> check("sit key starts the campfire scene", Scene.station instanceof CampfireStation, "station=" + Scene.station));
    }

    /** Film the low-opacity tail, where vanilla Font used to turn transparent text fully opaque again. */
    private static void campfireHintFadeScript(String prefix) {
        step(2, () -> Minecraft.getInstance().player.setYRot(90f));
        step(1, () -> shot(prefix + "0_hint_fade_100"));
        step(0, () -> shot(prefix + "0_hint_fade_150"));
        step(0, () -> shot(prefix + "0_hint_fade_200"));
        step(0, () -> shot(prefix + "0_hint_fade_250"));
        step(0, () -> shot(prefix + "0_hint_fade_300"));
        step(0, () -> shot(prefix + "0_hint_fade_350"));
        step(4, () -> shot(prefix + "0_hint_fade_600"));
        step(2, () -> Minecraft.getInstance().player.setYRot(180f));
        step(20, () -> shot(prefix + "0_hint_restored"));
    }

    private static void campfireScript() {
        Runnable restoreLayout = spatialEditorScript();
        shot("f5_active");
        step(4, () -> invClickItem(Items.CHICKEN));
        step(3, () -> pointAt(campfire().spot(0)));
        step(10, () -> shot("f6_carry_chicken"));
        step(2, () -> clickCampfire(CampfireStation.Target.PLACE, 1));
        step(4, () -> pointAt(campfire().spot(2)));
        step(6, () -> clickCampfire(CampfireStation.Target.PLACE, 1));
        step(4, Showcase::stash);
        step(4, () -> invClickItem(Items.MUTTON));
        step(3, () -> pointAt(campfire().spot(1)));
        step(6, () -> clickCampfire(CampfireStation.Target.PLACE, 1));
        step(4, Showcase::stash);
        step(4, Showcase::pointAside);
        step(14, () -> shot("f7_food_on_fire"));
        step(2, () -> server(p -> {
            int food = 0;
            if (p.serverLevel().getBlockEntity(CAMPFIRE) instanceof net.minecraft.world.level.block.entity.CampfireBlockEntity fire) {
                for (ItemStack stack : fire.getItems()) {
                    food += stack.isEmpty() ? 0 : 1;
                }
            }
            check("three pieces on the fire", food == 3, "food=" + food);
        }));
        int[] beforeCooldown = {0};
        step(2, () -> {
            CampfireStation station = campfire();
            check("campfire timers reach the seated player", station != null && station.syncedCookingTime(0) > 0
                    && station.syncedProgress(0) > 0 && station.syncedLit(),
                    "time=" + (station == null ? "-" : station.syncedProgress(0) + "/" + station.syncedCookingTime(0)));
            beforeCooldown[0] = station == null ? 0 : station.syncedProgress(0);
            server(p -> {
                var level = p.serverLevel();
                var state = level.getBlockState(CAMPFIRE);
                level.setBlock(CAMPFIRE, state.setValue(CampfireBlock.LIT, false), 3);
            });
        });
        step(20, () -> shot("f7_cooling"));
        step(2, () -> {
            CampfireStation station = campfire();
            check("extinguished campfire cools its progress", station != null && !station.syncedLit()
                    && station.syncedProgress(0) < beforeCooldown[0],
                    "progress=" + (station == null ? "-" : station.syncedProgress(0)));
            server(p -> {
                var level = p.serverLevel();
                var state = level.getBlockState(CAMPFIRE);
                level.setBlock(CAMPFIRE, state.setValue(CampfireBlock.LIT, true), 3);
            });
        });
        step(12, () -> shot("f7_relit"));
        step(2, () -> check("relit campfire resumes cooking", campfire() != null && campfire().syncedLit()
                && campfire().syncedProgress(0) > 0, "progress=" + (campfire() == null ? "-" : campfire().syncedProgress(0))));
        step(2, () -> pointAt(campfire().spot(farSpot())));
        step(14, () -> shot("f8_lean_far_spot"));
        step(2, () -> check("leaning in to reach the far side of the fire",
                CameraRig.last() != null && Minecraft.getInstance().player != null
                        && Math.hypot(CameraRig.last().pos().x - CAMPFIRE.getX() - 0.5, CameraRig.last().pos().z - CAMPFIRE.getZ() - 0.5) < 1.2,
                "camera=" + (CameraRig.last() == null ? "-" : CameraRig.last().pos())));
        step(2, () -> pointAt(campfire().spot(0)));
        step(8, () -> shot("f8_hover_food"));
        step(2, () -> clickCampfire(CampfireStation.Target.FOOD, 0));
        step(8, () -> shot("f8_taken"));
        step(2, () -> check("chicken taken off the fire", carried().is(Items.CHICKEN), "carried=" + carried()));
        step(2, () -> pointAt(campfire().spot(0)));
        step(6, () -> clickCampfire(CampfireStation.Target.PLACE, 0));
        step(60, () -> shot("f9_resting"));
        // Reproduce a report about raw beef staying visible at 0% when placed from the seated scene.
        step(4, () -> invClickItem(Items.BEEF));
        step(3, () -> pointAt(campfire().spot(3)));
        step(6, () -> clickCampfire(CampfireStation.Target.PLACE, 1));
        step(4, Showcase::stash);
        step(24, () -> {
            CampfireStation station = campfire();
            check("seated beef timer advances on the client", station != null && station.syncedLit()
                            && station.syncedCookingTime(3) > 0 && station.syncedProgress(3) > 0,
                    "timer=" + (station == null ? "-" : station.syncedProgress(3) + "/" + station.syncedCookingTime(3)));
            server(p -> {
                if (p.level().getBlockEntity(CAMPFIRE) instanceof net.minecraft.world.level.block.entity.CampfireBlockEntity fire) {
                    check("seated beef cooks on the server", fire.getItems().get(3).is(Items.BEEF)
                                    && fire.cookingTime[3] > 0 && fire.cookingProgress[3] > 0,
                            "timer=" + fire.cookingProgress[3] + "/" + fire.cookingTime[3]);
                    beefPace[0] = p.level().getGameTime();
                    beefPace[1] = fire.cookingProgress[3];
                }
            });
        });
        // The repair must leave a healthy fire alone: one step of progress per game tick, as in vanilla.
        step(20, () -> server(p -> {
            if (p.level().getBlockEntity(CAMPFIRE) instanceof net.minecraft.world.level.block.entity.CampfireBlockEntity fire) {
                long ticks = p.level().getGameTime() - beefPace[0], cooked = fire.cookingProgress[3] - beefPace[1];
                check("seated beef cooks at the vanilla pace", ticks > 0 && cooked > 0 && cooked <= ticks,
                        "progress +" + cooked + " in " + ticks + " ticks");
            }
        }));
        step(2, () -> server(p -> {
            if (p.level().getBlockEntity(CAMPFIRE) instanceof net.minecraft.world.level.block.entity.CampfireBlockEntity fire) {
                fire.cookingTime[3] = 0; // Simulate a timer lost after another block-entity update.
                fire.cookingProgress[3] = 0;
                fire.setChanged();
            }
        }));
        step(12, () -> {
            CampfireStation station = campfire();
            check("seated beef recovers a missing timer", station != null && station.syncedCookingTime(3) > 0
                            && station.syncedProgress(3) > 0,
                    "timer=" + (station == null ? "-" : station.syncedProgress(3) + "/" + station.syncedCookingTime(3)));
        });
        // Finish the piece: exactly one cooked beef pops off, as from the vanilla ticker.
        step(2, () -> server(p -> {
            beefPace[2] = cookedBeef(p, CAMPFIRE);
            if (p.level().getBlockEntity(CAMPFIRE) instanceof net.minecraft.world.level.block.entity.CampfireBlockEntity fire) {
                fire.cookingProgress[3] = Math.max(0, fire.cookingTime[3] - 4);
                fire.setChanged();
            }
        }));
        step(16, () -> server(p -> {
            if (p.level().getBlockEntity(CAMPFIRE) instanceof net.minecraft.world.level.block.entity.CampfireBlockEntity fire) {
                long cooked = cookedBeef(p, CAMPFIRE) - beefPace[2];
                check("seated beef pops off cooked once", fire.getItems().get(3).isEmpty() && cooked == 1,
                        "spot=" + fire.getItems().get(3) + " cooked=" + cooked);
            }
        }));
        step(4, () -> report("campfire before standing up"));
        step(2, Scene::leave);
        step(50, () -> shot("f9_after"));
        step(2, restoreLayout);
        soulCampfireAct();
    }

    // ---- the soul campfire ----------------------------------------------------------------------------

    private static void soulCampfireAct() {
        step(2, () -> {
            act = "u";
            server(p -> p.teleportTo(p.serverLevel(), SOUL_CAMPFIRE.getX() + 0.5, -60, SOUL_CAMPFIRE.getZ() + 2.9, 180f, 24f));
        });
        step(30, () -> shot("u0_hint"));
        campfireHintFadeScript("u");
        step(2, () -> {
            introShot = 0;
            outroShot = 0;
            waitingForActive = true;
            //? if neoforge {
            net.minecraft.client.KeyMapping.click(CampfireHint.SIT.getKey());
            //?} else if fabric {
            /*net.minecraft.client.KeyMapping.click(net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.getBoundKeyOf(CampfireHint.SIT));
            *///?} else if forge {
            /*net.minecraft.client.KeyMapping.click(CampfireHint.SIT.getKey());
            *///?}
        });
        step(6, () -> check("sit key works at a soul campfire too", campfire() != null && campfire().pos.equals(SOUL_CAMPFIRE),
                "station=" + Scene.station));
    }

    private static void soulCampfireScript() {
        Runnable restoreLayout = spatialEditorScript();
        shot("u5_active");
        step(4, () -> invClickItem(Items.CHICKEN));
        step(3, () -> pointAt(campfire().spot(0)));
        step(6, () -> clickCampfire(CampfireStation.Target.PLACE, 1));
        step(2, Showcase::stash);
        step(2, () -> server(p -> {
            if (p.serverLevel().getBlockEntity(SOUL_CAMPFIRE) instanceof net.minecraft.world.level.block.entity.CampfireBlockEntity fire) {
                fire.cookingTime[0] = 80; // A short recipe for filming both the rising bar and its disappearance.
                fire.setChanged();
            }
        }));
        step(12, () -> shot("u6_cooking_early"));
        int[] early = {0};
        step(2, () -> {
            CampfireStation station = campfire();
            check("soul campfire has its own cooking timer", station != null && station.syncedCookingTime(0) == 80
                    && station.syncedProgress(0) > 0 && station.syncedLit(),
                    "time=" + (station == null ? "-" : station.syncedProgress(0) + "/" + station.syncedCookingTime(0)));
            early[0] = station == null ? 0 : station.syncedProgress(0);
        });
        step(24, () -> shot("u7_cooking_late"));
        step(2, () -> check("soul campfire progress rises", campfire() != null && campfire().syncedProgress(0) > early[0],
                "progress=" + (campfire() == null ? "-" : campfire().syncedProgress(0))));
        step(52, () -> shot("u8_cooked"));
        step(2, () -> check("cooking indicator clears when food finishes", campfire() != null && campfire().syncedCookingTime(0) == 0
                && Minecraft.getInstance().level.getBlockEntity(SOUL_CAMPFIRE) instanceof net.minecraft.world.level.block.entity.CampfireBlockEntity fire
                && fire.getItems().get(0).isEmpty(), "time=" + (campfire() == null ? "-" : campfire().syncedCookingTime(0))));
        // Raw beef from the seated scene, on its own spot of the soul fire.
        step(4, () -> invClickItem(Items.BEEF));
        step(3, () -> pointAt(campfire().spot(1)));
        step(6, () -> clickCampfire(CampfireStation.Target.PLACE, 1));
        step(4, Showcase::stash);
        step(24, () -> {
            CampfireStation station = campfire();
            check("soul campfire beef timer advances on the client", station != null && station.syncedLit()
                            && station.syncedCookingTime(1) > 0 && station.syncedProgress(1) > 0,
                    "timer=" + (station == null ? "-" : station.syncedProgress(1) + "/" + station.syncedCookingTime(1)));
            server(p -> {
                if (p.level().getBlockEntity(SOUL_CAMPFIRE) instanceof net.minecraft.world.level.block.entity.CampfireBlockEntity fire) {
                    check("soul campfire beef cooks on the server", fire.getItems().get(1).is(Items.BEEF)
                                    && fire.cookingTime[1] > 0 && fire.cookingProgress[1] > 0,
                            "timer=" + fire.cookingProgress[1] + "/" + fire.cookingTime[1]);
                    beefPace[0] = p.level().getGameTime();
                    beefPace[1] = fire.cookingProgress[1];
                }
            });
        });
        step(20, () -> server(p -> {
            if (p.level().getBlockEntity(SOUL_CAMPFIRE) instanceof net.minecraft.world.level.block.entity.CampfireBlockEntity fire) {
                long ticks = p.level().getGameTime() - beefPace[0], cooked = fire.cookingProgress[1] - beefPace[1];
                check("soul campfire beef cooks at the vanilla pace", ticks > 0 && cooked > 0 && cooked <= ticks,
                        "progress +" + cooked + " in " + ticks + " ticks");
                beefPace[2] = cookedBeef(p, SOUL_CAMPFIRE);
                fire.cookingProgress[1] = Math.max(0, fire.cookingTime[1] - 4);
                fire.setChanged();
            }
        }));
        step(16, () -> server(p -> {
            if (p.level().getBlockEntity(SOUL_CAMPFIRE) instanceof net.minecraft.world.level.block.entity.CampfireBlockEntity fire) {
                long cooked = cookedBeef(p, SOUL_CAMPFIRE) - beefPace[2];
                check("soul campfire beef pops off cooked once", fire.getItems().get(1).isEmpty() && cooked == 1,
                        "spot=" + fire.getItems().get(1) + " cooked=" + cooked);
            }
        }));
        step(2, restoreLayout);
        step(2, Scene::leave);
        step(50, () -> shot("u9_after"));
        lecternAct();
    }

    // ---- the lectern ----------------------------------------------------------------------------------

    /** A written book of a few pages, to read on the lectern. */
    /** A book and quill with a few pages of notes in it. */
    private static ItemStack notesBook() {
        ItemStack book = new ItemStack(Items.WRITABLE_BOOK);
        //? if >=1.21 {
        book.set(DataComponents.WRITABLE_BOOK_CONTENT, new net.minecraft.world.item.component.WritableBookContent(List.of(
                net.minecraft.server.network.Filterable.passThrough("Записки мастера.\n\nКниги на резной полке достают по одной: корешок под рукой чуть выдвигается, а клик вытягивает книгу."),
                net.minecraft.server.network.Filterable.passThrough("Книгу с текстом можно раскрыть прямо у полки и читать, как на кафедре: лист за листом."),
                net.minecraft.server.network.Filterable.passThrough("Зачарованные книги мерцают на полке, а их чары видны в подсказке."))));
        //?} else if <1.21 {
        /*// in 1.20.1 the pages of a book and quill are plain strings in its tag
        net.minecraft.nbt.ListTag pages = new net.minecraft.nbt.ListTag();
        for (String page : List.of(
                "Записки мастера.\n\nКниги на резной полке достают по одной: корешок под рукой чуть выдвигается, а клик вытягивает книгу.",
                "Книгу с текстом можно раскрыть прямо у полки и читать, как на кафедре: лист за листом.",
                "Зачарованные книги мерцают на полке, а их чары видны в подсказке.")) {
            pages.add(net.minecraft.nbt.StringTag.valueOf(page));
        }
        book.getOrCreateTag().put("pages", pages);
        *///?}
        return book;
    }

    private static ItemStack storyBook() {
        String[][] text = {
                {"Кинематографичные взаимодействия", "Эта книга лежит на кафедре, чтобы её можно было прочитать прямо в мире. Камера склоняется над страницами, а рука переворачивает лист за уголок."},
                {"Глава первая", "В мастерской пахнет деревом и углём. На верстаке лежат доски, в печи догорает уголь, а у стены ждёт наковальня с тяжёлым молотом."},
                {"", "Если подойти к столу зачарований, над книгой загораются руны. Лазурит ложится в чашу, предмет поднимается в воздух, и полки отдают ему свои знаки."},
                {"Глава вторая", "Ткацкий станок держит белый флаг. Челнок летает из руки в руку, и узор проступает ряд за рядом, пока не станет красным крестом."},
                {"", "У варочной стойки можно присесть. Бутылки стоят на её каменных ножках, незерский нарост тает в чаше, и капли стекают вниз."},
                {"Глава третья", "Маяк стоит на пирамиде из железа, золота, изумрудов и алмазов. Над ним висят силы, а плата парит над стеклом."},
                {"Конец", "Закрой книгу и забери её с собой. Или оставь на кафедре: следующий читатель откроет её на той же странице."},
        };
        //? if >=1.21 {
        List<net.minecraft.server.network.Filterable<net.minecraft.network.chat.Component>> pages = new ArrayList<>();
        //?} else if <1.21 {
        /*// in 1.20.1 the pages of a written book are text components as JSON in its tag
        net.minecraft.nbt.ListTag pages = new net.minecraft.nbt.ListTag();
        *///?}
        for (String[] page : text) {
            net.minecraft.network.chat.MutableComponent c = net.minecraft.network.chat.Component.empty();
            if (!page[0].isEmpty()) {
                c.append(net.minecraft.network.chat.Component.literal(page[0]).withStyle(net.minecraft.ChatFormatting.BOLD,
                        net.minecraft.ChatFormatting.DARK_RED)).append("\n\n");
            }
            c.append(page[1]);
            //? if >=1.21 {
            pages.add(net.minecraft.server.network.Filterable.passThrough(c));
            //?} else if <1.21 {
            /*pages.add(net.minecraft.nbt.StringTag.valueOf(net.minecraft.network.chat.Component.Serializer.toJson(c)));
            *///?}
        }
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        //? if >=1.21 {
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new net.minecraft.world.item.component.WrittenBookContent(
                net.minecraft.server.network.Filterable.passThrough("Мастерская"), "dcvke", 0, pages, true));
        //?} else if <1.21 {
        /*net.minecraft.nbt.CompoundTag tag = book.getOrCreateTag();
        tag.putString("title", "Мастерская");
        tag.putString("author", "dcvke");
        tag.putInt("generation", 0);
        tag.putBoolean("resolved", true);
        tag.put("pages", pages);
        *///?}
        return book;
    }

    private static LecternStation lectern() {
        return Scene.station(LecternStation.class);
    }

    private static void lecternAct() {
        nextAct("k", new Vec3(LECTERN.getX() + 0.5, -60, LECTERN.getZ() + 2.6), 180f, LECTERN, new Vec3(0.5, 0.9, 0.6), Direction.UP);
    }

    private static int lecternPage(ServerPlayer p) {
        return p.serverLevel().getBlockEntity(LECTERN) instanceof net.minecraft.world.level.block.entity.LecternBlockEntity lectern ? lectern.getPage() : -1;
    }

    private static void lecternScript() {
        Runnable restoreLayout = spatialEditorScript();
        step(2, Scene::leave);
        step(40, () -> { check("lectern profile outside scene", StationDisplayLayouts.customLectern() && Scene.phase == Scene.Phase.NONE, "vanilla display uses saved geometry"); shot("xk4_book_outside"); });
        step(2, () -> useAt(LECTERN, new Vec3(0.5, 0.9, 0.6), Direction.UP));
        step(65, () -> { check("lectern reopens with saved geometry", lectern() != null && Scene.phase == Scene.Phase.ACTIVE, "book remains readable"); shot("k5_active"); });
        step(2, () -> check("lectern book open at the first spread", lectern() != null && lectern().spread() == 0,
                "spread=" + (lectern() == null ? "-" : lectern().spread())));
        step(2, () -> pointAt(lectern().pageCenter(false)));
        step(10, () -> shot("k6_peek"));
        step(2, () -> screenClick(0));
        step(2, () -> shot("k6_turn_010"));
        step(2, () -> shot("k6_turn_020"));
        step(2, () -> shot("k6_turn_030"));
        step(2, () -> shot("k6_turn_040"));
        step(3, () -> shot("k6_turn_055"));
        step(14, () -> shot("k7_spread1"));
        step(2, () -> screenClick(0));
        step(20, () -> shot("k7_spread2"));
        step(2, () -> server(p -> check("lectern turned to page 4 (the left of the third spread)", lecternPage(p) == 3, "page=" + lecternPage(p))));
        step(2, () -> pointAt(lectern().pageCenter(true)));
        step(10, () -> shot("k7_peek_back"));
        step(2, () -> lectern().scroll(1));
        step(4, () -> shot("k7_back_020"));
        step(16, () -> shot("k7_back_done"));
        step(2, () -> server(p -> check("lectern turned back by the wheel", lecternPage(p) == 1, "page=" + lecternPage(p))));
        step(2, () -> lectern().keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT));
        step(4, () -> lectern().keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT));
        step(4, () -> lectern().keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT));
        step(30, () -> shot("k8_last_spread"));
        step(2, () -> check("arrow keys turned to the last spread", lectern() != null && lectern().spread() == 3,
                "spread=" + (lectern() == null ? "-" : lectern().spread())));
        step(2, () -> pointAt(lectern().pageCenter(false)));
        step(4, () -> lectern().click(0, true));
        step(5, () -> shot("k8_take_025"));
        step(5, () -> shot("k8_take_050"));
        step(5, () -> shot("k8_take_075"));
        step(30, () -> server(p -> {
            //? if >=1.21 {
            boolean taken = p.getInventory().contains(stack -> stack.is(Items.WRITTEN_BOOK));
            //?} else if <1.21 {
            /*boolean taken = p.getInventory().hasAnyMatching(stack -> stack.is(Items.WRITTEN_BOOK));
            *///?}
            boolean empty = !p.serverLevel().getBlockState(LECTERN).getValue(net.minecraft.world.level.block.LecternBlock.HAS_BOOK);
            check("book taken off the lectern", taken && empty, "inventory=" + taken + " lecternEmpty=" + empty);
        }));
        //? if fabric {
        /*// the frames since then drew the empty lectern under the edited layout: its renderer returns early without a
        // book, and the layout's pose must come off there too (Fabric runs the game's own bytecode, LecternRendererMixin)
        step(2, () -> check("empty lectern drawn under a saved layout", StationDisplayLayouts.customLectern()
                && Minecraft.getInstance().level.getBlockEntity(LECTERN) instanceof net.minecraft.world.level.block.entity.LecternBlockEntity lectern
                && !lectern.hasBook(), "custom=" + StationDisplayLayouts.customLectern()));
        *///?}
        step(2, restoreLayout);
        step(30, () -> shot("k9_after"));
        step(2, () -> server(p -> {
            // put it back for the third-person shot
            net.minecraft.world.level.block.LecternBlock.tryPlaceBook(null, p.serverLevel(), LECTERN, p.serverLevel().getBlockState(LECTERN), storyBook());
        }));
        shelfAct();
    }

    // ---- the chiseled bookshelf -------------------------------------------------------------------------

    private static ShelfStation shelf() {
        return Scene.station(ShelfStation.class);
    }

    private static void clickShelf(ShelfStation.Target expected, int button) {
        ShelfStation station = shelf();
        if (station == null || station.target != expected) {
            REPORT.add("cursor missed shelf target " + expected + ": was " + (station == null ? "-" : station.target + " slot " + station.hoverSlot));
        }
        screenClick(button);
    }

    private static ItemStack shelfItem(ServerPlayer p, int slot) {
        return p.serverLevel().getBlockEntity(SHELF) instanceof net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity shelf
                ? shelf.getItem(slot) : ItemStack.EMPTY;
    }

    /** Selects a hotbar slot on both sides, with {@code stack} put in it. */
    private static void hold(int slot, ItemStack stack) {
        server(p -> {
            p.getInventory().setItem(slot, stack);
            p.getInventory().selected = slot;
        });
        Minecraft.getInstance().player.getInventory().selected = slot;
        Minecraft.getInstance().getConnection().send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(slot));
    }

    private static void shelfAct() {
        if (ONLY && !"e".equals(FROM)) {
            step(2, () -> finish(Minecraft.getInstance()));
            return;
        }
        step(2, () -> hold(7, ItemStack.EMPTY));
        nextAct("e", new Vec3(SHELF.getX() + 0.5, -60, SHELF.getZ() + 3.0), 180f, SHELF, SHELF_SLOTS[1], Direction.SOUTH);
    }

    private static void shelfScript() {
        Runnable restoreLayout = spatialEditorScript();
        shot("e5_active");
        // an enchanted book slides out under the cursor and says what it carries
        step(2, () -> pointAt(shelf().slotPoint(0)));
        step(12, () -> shot("e6_hover_enchanted"));
        step(2, () -> clickShelf(ShelfStation.Target.BOOK, 0));
        moveShots("e7_take", 0.15, 0.3, 0.45, 0.6, 0.8, 1.0);
        step(10, () -> {
            ItemStack carried = Minecraft.getInstance().player.containerMenu.getCarried();
            check("shelf: the enchanted book taken into the hand", carried.is(Items.ENCHANTED_BOOK), "carried=" + describe(carried));
            server(p -> check("shelf: its place is empty on the server", shelfItem(p, 0).isEmpty(), "slot 0=" + describe(shelfItem(p, 0))));
        });
        // into the empty place
        step(2, () -> pointAt(shelf().slotPoint(2)));
        step(8, () -> shot("e8_place_hover"));
        step(2, () -> clickShelf(ShelfStation.Target.PLACE, 0));
        moveShots("e9_put", 0.15, 0.3, 0.45, 0.6, 0.8);
        step(14, () -> {
            shot("ea_put_done");
            server(p -> check("shelf: the book stands in the other place", shelfItem(p, 2).is(Items.ENCHANTED_BOOK) && p.containerMenu.getCarried().isEmpty(),
                    "slot 2=" + describe(shelfItem(p, 2)) + " carried=" + describe(p.containerMenu.getCarried())));
        });
        // the master's notes: drawn out, opened and read
        step(2, () -> pointAt(shelf().slotPoint(1)));
        step(10, () -> shot("eb_hover_notes"));
        step(2, () -> clickShelf(ShelfStation.Target.BOOK, 0));
        moveShots("ec_open", 0.15, 0.35, 0.55, 0.75, 0.95, 1.15, 1.3, 1.5);
        step(10, () -> {
            shot("ed_reading");
            check("shelf: the notes open in the hands", shelf() != null && shelf().reading() && shelf().spread() == 0,
                    "reading=" + (shelf() != null && shelf().reading()) + " spread=" + (shelf() == null ? "-" : shelf().spread()));
            server(p -> check("shelf: the notes stay on the shelf while read", shelfItem(p, 1).is(Items.WRITABLE_BOOK), "slot 1=" + describe(shelfItem(p, 1))));
        });
        step(2, () -> pointAt(shelf().pageCenter(false)));
        step(8, () -> shot("ee_peek"));
        step(2, () -> screenClick(0));
        step(4, () -> shot("ee_turn_020"));
        step(4, () -> shot("ee_turn_040"));
        step(16, () -> {
            shot("ef_spread1");
            check("shelf: a leaf of the notes turned", shelf() != null && shelf().spread() == 1, "spread=" + (shelf() == null ? "-" : shelf().spread()));
        });
        step(2, () -> Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0));
        moveShots("eg_back", 0.2, 0.45, 0.7, 0.95, 1.15, 1.35);
        step(14, () -> {
            shot("eh_back_done");
            check("shelf: Esc put the notes back first", shelf() != null && !shelf().reading() && Scene.phase == Scene.Phase.ACTIVE,
                    "phase=" + Scene.phase);
        });
        // the plain book straight into the inventory
        step(2, () -> pointAt(shelf().slotPoint(4)));
        step(8, () -> {
            ShelfStation station = shelf();
            if (station == null || station.target != ShelfStation.Target.BOOK) {
                REPORT.add("cursor missed the plain book");
            }
            if (station != null) {
                station.click(0, true);
            }
        });
        moveShots("ei_bag", 0.3, 0.6, 0.9);
        step(10, () -> server(p -> check("shelf: the plain book into the inventory", shelfItem(p, 4).isEmpty()
                //? if >=1.21 {
                && p.getInventory().contains(stack -> stack.is(Items.BOOK)), "slot 4=" + describe(shelfItem(p, 4)))));
                //?} else if <1.21 {
                /*&& p.getInventory().hasAnyMatching(stack -> stack.is(Items.BOOK)), "slot 4=" + describe(shelfItem(p, 4)))));
                *///?}
        step(4, () -> report("shelf before leaving"));
        step(2, Scene::leave);
        step(40, () -> shot("e9_after"));
        step(4, restoreLayout);
        // a book in hand used on an empty place: the scene puts it straight in
        step(2, () -> {
            act = "ej";
            hold(7, new ItemStack(Items.BOOK));
        });
        step(10, () -> {
            introShot = 0;
            useAt(SHELF, SHELF_SLOTS[4], Direction.SOUTH);
        });
        step(100, () -> server(p -> check("shelf: the book in hand went into the place used", shelfItem(p, 4).is(Items.BOOK) && p.getInventory().getItem(7).isEmpty(),
                "slot 4=" + describe(shelfItem(p, 4)) + " hand=" + describe(p.getInventory().getItem(7)))));
        step(2, () -> shot("ej_put_from_hand"));
        step(2, Scene::leave);
        // the scene turned off: using the shelf takes the book out the vanilla way
        step(40, () -> {
            act = "ek";
            //? if >=1.21 {
            Settings.SCENES.get(StationKind.CHISELED_BOOKSHELF).set(false);
            //?} else if <1.21 {
            /*Settings.set(Settings.SCENES.get(StationKind.CHISELED_BOOKSHELF), false);
            *///?}
        });
        step(4, () -> useAt(SHELF, SHELF_SLOTS[3], Direction.SOUTH));
        step(20, () -> {
            check("shelf, scene off: no scene", Scene.phase == Scene.Phase.NONE, "phase=" + Scene.phase);
            server(p -> check("shelf, scene off: the written book taken out the vanilla way", shelfItem(p, 3).isEmpty()
                    //? if >=1.21 {
                    && p.getInventory().contains(stack -> stack.is(Items.WRITTEN_BOOK)), "slot 3=" + describe(shelfItem(p, 3))));
            Settings.SCENES.get(StationKind.CHISELED_BOOKSHELF).set(true);
                    //?} else if <1.21 {
                    /*&& p.getInventory().hasAnyMatching(stack -> stack.is(Items.WRITTEN_BOOK)), "slot 3=" + describe(shelfItem(p, 3))));
            Settings.set(Settings.SCENES.get(StationKind.CHISELED_BOOKSHELF), true);
                    *///?}
        });
        beaconAct();
    }

    // ---- the beacon -----------------------------------------------------------------------------------

    private static BeaconStation beacon() {
        return Scene.station(BeaconStation.class);
    }

    private static void clickBeacon(BeaconStation.Target expected, int button) {
        BeaconStation station = beacon();
        if (station == null || station.target != expected) {
            REPORT.add("cursor missed beacon target " + expected + ": was " + (station == null ? "-" : station.target));
        }
        screenClick(button);
    }

    private static void beaconAct() {
        nextAct("n", new Vec3(BEACON.getX() + 0.5, BEACON.getY(), BEACON.getZ() + 1.75), 180f, BEACON, new Vec3(0.5, 0.9, 0.5), Direction.UP);
    }

    private static void beaconScript() {
        Runnable restoreLayout = spatialEditorScript();
        shot("n5_active");
        step(2, () -> check("beacon stands on a full pyramid", beacon() != null && beacon().menu() != null && beacon().menu().getLevels() == 4,
                "levels=" + (beacon() == null || beacon().menu() == null ? "-" : beacon().menu().getLevels())));
        step(2, () -> pointAt(beacon().powerPos(0)));
        step(10, () -> shot("n6_hover_speed"));
        step(2, () -> clickBeacon(BeaconStation.Target.POWER, 0));
        step(6, () -> pointAt(beacon().powerPos(5)));
        step(4, () -> clickBeacon(BeaconStation.Target.POWER, 0));
        step(8, () -> shot("n6_chosen"));
        step(2, () -> invClickItem(Items.GOLD_INGOT));
        step(3, () -> pointAt(beacon().paymentPos()));
        step(10, () -> shot("n7_carry_gold"));
        step(2, () -> clickBeacon(BeaconStation.Target.TOP, 0));
        step(4, Showcase::stash);
        step(4, () -> pointAt(beacon().paymentPos()));
        step(10, () -> shot("n7_payment_tooltip"));
        step(2, () -> clickBeacon(BeaconStation.Target.PAYMENT, 0));
        moveShots("n8_light", 0.2, 0.45, 0.62, 0.72, 0.85, 1.05, 1.4, 1.9, 2.6);
        step(10, () -> server(p -> {
            //? if >=1.21 {
            net.minecraft.nbt.CompoundTag tag = p.serverLevel().getBlockEntity(BEACON).saveWithoutMetadata(p.serverLevel().registryAccess());
            String primary = tag.getString("primary_effect"), secondary = tag.getString("secondary_effect");
            //?} else if <1.21 {
            /*// 1.20.1 keeps the powers as effect ids
            net.minecraft.nbt.CompoundTag tag = p.serverLevel().getBlockEntity(BEACON).saveWithoutMetadata();
            var first = net.minecraft.world.effect.MobEffect.byId(tag.getInt("Primary"));
            var second = net.minecraft.world.effect.MobEffect.byId(tag.getInt("Secondary"));
            String primary = first == null ? "-" : String.valueOf(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(first));
            String secondary = second == null ? "-" : String.valueOf(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(second));
            *///?}
            check("beacon lit with speed and regeneration", primary.equals("minecraft:speed") && secondary.equals("minecraft:regeneration"),
                    "primary=" + primary + " secondary=" + secondary);
        }));
        step(2, () -> check("payment used up", beacon() != null && beacon().slot(0).isEmpty(), "slot=" + (beacon() == null ? "-" : beacon().slot(0))));
        step(4, () -> report("beacon before leaving"));
        step(2, restoreLayout);
        step(2, Scene::leave);
        step(50, () -> shot("n9_after"));
        heightAct();
    }

    /** The campfire spot furthest from where the player sits. */
    private static int farSpot() {
        CampfireStation station = campfire();
        Vec3 eye = Minecraft.getInstance().player.position();
        int far = 0;
        for (int j = 1; j < 4; j++) {
            if (station.spot(j).distanceToSqr(eye) > station.spot(far).distanceToSqr(eye)) {
                far = j;
            }
        }
        return far;
    }

    // ---- heights: the camera stays with the player's head ------------------------------------------

    /** Where height case {@code i} stands. */
    private static BlockPos heightSpot(int i) {
        return new BlockPos(HEIGHTS_X - 4 * i, -60, 22);
    }

    private static void buildHeights(ServerLevel level) {
        net.minecraft.world.level.block.state.BlockState stone = Blocks.STONE_BRICKS.defaultBlockState();
        net.minecraft.world.level.block.state.BlockState table = Blocks.CRAFTING_TABLE.defaultBlockState();
        net.minecraft.world.level.block.state.BlockState south = Blocks.FURNACE.defaultBlockState().setValue(AbstractFurnaceBlock.FACING, Direction.SOUTH);
        // 0: a table on a pillar, its top above the head
        BlockPos b = heightSpot(0);
        level.setBlockAndUpdate(b, stone);
        level.setBlockAndUpdate(b.above(), table);
        // 1: a furnace on a block, its front at eye level
        b = heightSpot(1);
        level.setBlockAndUpdate(b, stone);
        level.setBlockAndUpdate(b.above(), south);
        // 2: a furnace two blocks up
        b = heightSpot(2);
        level.setBlockAndUpdate(b, stone);
        level.setBlockAndUpdate(b.above(), stone);
        level.setBlockAndUpdate(b.above(2), south);
        // 3: a table on the ground in front of a floor raised one block
        b = heightSpot(3);
        level.setBlockAndUpdate(b, table);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = 1; dz <= 3; dz++) {
                level.setBlockAndUpdate(b.offset(dx, 0, dz), Blocks.SPRUCE_PLANKS.defaultBlockState());
            }
        }
        // 4: a table sunk into the ground, its top flush with it
        b = heightSpot(4);
        level.setBlockAndUpdate(b.below(), table);
        // 5: a table at the bottom of a hole, its top a block below the feet
        b = heightSpot(5);
        level.setBlockAndUpdate(b.below(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(b.below(2), table);
        // 6: a furnace sunk into the ground, its front to a pit on the player's side
        b = heightSpot(6);
        level.setBlockAndUpdate(b.below(), south);
        level.setBlockAndUpdate(b.below().south(), Blocks.AIR.defaultBlockState());
        // 7: a furnace sunk the same way, its front to a pit on the far side: only its top shows
        b = heightSpot(7);
        level.setBlockAndUpdate(b.below(), Blocks.FURNACE.defaultBlockState().setValue(AbstractFurnaceBlock.FACING, Direction.NORTH));
        level.setBlockAndUpdate(b.below().north(), Blocks.AIR.defaultBlockState());
        // 8: a table with a slab lying on its top
        b = heightSpot(8);
        level.setBlockAndUpdate(b, table);
        level.setBlockAndUpdate(b.above(), Blocks.STONE_SLAB.defaultBlockState());
        // 9: a table the player stands on
        b = heightSpot(9);
        level.setBlockAndUpdate(b, table);
        // 10: a furnace with lava in front of it
        b = heightSpot(10);
        level.setBlockAndUpdate(b, south);
        for (int dz = 1; dz <= 2; dz++) {
            level.setBlockAndUpdate(b.south(dz).below(), Blocks.LAVA.defaultBlockState());
        }
        // 11: a table to fall from (the ground under the player gives way)
        level.setBlockAndUpdate(heightSpot(11), table);
        // 12: a furnace at the foot of a raised floor, its front against the floor: only its top shows
        b = heightSpot(12);
        level.setBlockAndUpdate(b, south);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = 1; dz <= 3; dz++) {
                level.setBlockAndUpdate(b.offset(dx, 0, dz), Blocks.SPRUCE_PLANKS.defaultBlockState());
            }
        }
        // 13: a furnace sunk into the ground, its front to a pit on its east side, the player to the south
        b = heightSpot(13);
        level.setBlockAndUpdate(b.below(), Blocks.FURNACE.defaultBlockState().setValue(AbstractFurnaceBlock.FACING, Direction.EAST));
        level.setBlockAndUpdate(b.below().east(), Blocks.AIR.defaultBlockState());
        // 14: a brewing stand on the ground in front of a raised floor
        b = heightSpot(14);
        level.setBlockAndUpdate(b, Blocks.BREWING_STAND.defaultBlockState());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = 1; dz <= 3; dz++) {
                level.setBlockAndUpdate(b.offset(dx, 0, dz), Blocks.SPRUCE_PLANKS.defaultBlockState());
            }
        }
        // 16-18: a furnace, a smoker and a blast furnace sunk into the ground, their fronts to a pit, the player far off
        net.minecraft.world.level.block.Block[] furnaces = {Blocks.FURNACE, Blocks.SMOKER, Blocks.BLAST_FURNACE};
        for (int k = 0; k < 3; k++) {
            b = heightSpot(16 + k);
            level.setBlockAndUpdate(b.below(), furnaces[k].defaultBlockState().setValue(AbstractFurnaceBlock.FACING, Direction.SOUTH));
            level.setBlockAndUpdate(b.below().south(), Blocks.AIR.defaultBlockState());
        }
        // 19: a furnace on the ground, a raised floor beginning a block in front of it
        b = heightSpot(19);
        level.setBlockAndUpdate(b, south);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = 2; dz <= 4; dz++) {
                level.setBlockAndUpdate(b.offset(dx, 0, dz), Blocks.SPRUCE_PLANKS.defaultBlockState());
            }
        }
        // 20: a furnace with a wall between it and the player (the way goes round)
        b = heightSpot(20);
        level.setBlockAndUpdate(b, south);
        for (int dx = -1; dx <= 1; dx++) {
            level.setBlockAndUpdate(b.offset(dx, 0, 2), stone);
            level.setBlockAndUpdate(b.offset(dx, 1, 2), stone);
        }
        // 21: a furnace buried in the ground: only its top shows
        level.setBlockAndUpdate(heightSpot(21).below(), south);
        // 22: a furnace facing a wall
        b = heightSpot(22);
        level.setBlockAndUpdate(b, Blocks.FURNACE.defaultBlockState().setValue(AbstractFurnaceBlock.FACING, Direction.NORTH));
        level.setBlockAndUpdate(b.north(), stone);
        level.setBlockAndUpdate(b.north().above(), stone);
        // 23: a furnace with lava all over the ground before it, far round to either side
        b = heightSpot(23);
        level.setBlockAndUpdate(b, south);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = 1; dz <= 3; dz++) {
                level.setBlockAndUpdate(b.offset(dx, -1, dz), Blocks.LAVA.defaultBlockState());
            }
        }
        // 15: a lectern with a carpet laid in front of it (the carpet is stood on, not in the way)
        b = heightSpot(15);
        level.setBlockAndUpdate(b, Blocks.LECTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LecternBlock.FACING, Direction.SOUTH));
        net.minecraft.world.level.block.LecternBlock.tryPlaceBook(null, level, b, level.getBlockState(b), storyBook());
        for (int dz = 1; dz <= 3; dz++) {
            level.setBlockAndUpdate(b.south(dz), Blocks.RED_CARPET.defaultBlockState());
        }
    }

    /** Stands the player at {@code feet} looking at {@code point}. */
    private static void standLooking(Vec3 feet, Vec3 point) {
        server(p -> {
            Vec3 eye = feet.add(0, p.getEyeHeight(), 0);
            Vec3 d = point.subtract(eye);
            float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
            float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)));
            p.teleportTo(p.serverLevel(), feet.x, feet.y, feet.z, yaw, pitch);
        });
    }

    private static void closeWindow() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.screen != null && !(mc.screen instanceof SceneScreen)) {
            mc.player.closeContainer();
        }
    }

    /** The camera's height over the player's eyes right now. */
    private static double cameraOverEyes() {
        Minecraft mc = Minecraft.getInstance();
        return CameraRig.last() == null || mc.player == null ? 99 : CameraRig.last().pos().y - mc.player.getEyeY();
    }

    /**
     * One height case: the player stands at {@code feet} (relative to the case's spot), uses the station and gets
     * either the scene ({@code scene}, then {@code during} runs in it) or the block's own window.
     */
    private static void heightCase(int i, String name, BlockPos station, Vec3 feet, Vec3 local, Direction face, boolean scene,
            @javax.annotation.Nullable Runnable during) {
        heightCase(i, name, station, feet, local, face, scene, during, true);
    }

    private static void heightCase(int i, String name, BlockPos station, Vec3 feet, Vec3 local, Direction face, boolean scene,
            @javax.annotation.Nullable Runnable during, boolean leave) {
        BlockPos base = heightSpot(i);
        Vec3 at = Vec3.atLowerCornerOf(base).add(feet);
        step(2, () -> {
            act = name;
            // a window left open by the case before (a scene that did not start) would take the use
            closeWindow();
            standLooking(at, Vec3.atCenterOf(station));
        });
        step(18, () -> shot(name + "_0_before"));
        step(2, () -> {
            introShot = 0;
            outroShot = 0;
            Scene.refused = "";
            Minecraft mc = Minecraft.getInstance();
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(station).add(local), face, station, false));
        });
        if (scene) {
            // (a long way round takes a while)
            step(90, () -> {
                check(name + ": a scene", Scene.phase == Scene.Phase.ACTIVE, "phase=" + Scene.phase + " refused=" + Scene.refused
                        + String.format(" camera over eyes=%.2f crouching=%s", cameraOverEyes(), Minecraft.getInstance().player.isCrouching()));
                shot(name + "_5_scene");
            });
            if (during != null) {
                step(2, during);
            }
            if (leave) {
                step(2, () -> {
                    if (Scene.phase == Scene.Phase.ACTIVE) {
                        Scene.leave();
                    }
                });
            }
            step(36, () -> {});
        } else {
            // the hand reaches out to the block as the window opens, as vanilla's use swings it
            step(1, () -> check(name + ": the hand reaches out", Minecraft.getInstance().player.swinging, "swing=" + Minecraft.getInstance().player.swingTime));
            step(15, () -> {
                Minecraft mc = Minecraft.getInstance();
                check(name + ": its own window", mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> && !Scene.active(),
                        "screen=" + (mc.screen == null ? "-" : mc.screen.getClass().getSimpleName()) + " phase=" + Scene.phase + " why=" + Scene.refused);
                shot(name + "_5_window");
            });
            step(4, Showcase::closeWindow);
        }
    }

    private static void heightAct() {
        if (EDITOR_BATCH) { developerOptionsScript(); jukeboxAct(); return; }
        if (stopHere("h")) {
            return;
        }
        BlockPos b;
        b = heightSpot(0);
        heightCase(0, "h0_table_on_pillar", b.above(), new Vec3(0.5, 0, 2.2), new Vec3(0.5, 0.5, 1.0), Direction.SOUTH, false, null);
        b = heightSpot(1);
        heightCase(1, "h1_furnace_on_block", b.above(), new Vec3(0.5, 0, 2.4), new Vec3(0.5, 0.5, 1.0), Direction.SOUTH, true,
                () -> check("furnace on a block: the camera stays with the head, the player stands up",
                        cameraOverEyes() <= 0.17 && !Minecraft.getInstance().player.isCrouching(), String.format("over eyes=%.2f", cameraOverEyes())));
        b = heightSpot(2);
        heightCase(2, "h2_furnace_two_up", b.above(2), new Vec3(0.5, 0, 2.0), new Vec3(0.5, 0.5, 1.0), Direction.SOUTH, false, null);
        b = heightSpot(3);
        heightCase(3, "h3_table_below_floor", b, new Vec3(0.5, 1, 2.4), new Vec3(0.5, 1.0, 0.7), Direction.UP, true,
                () -> check("table below the floor: the player crouches to it", Minecraft.getInstance().player.isCrouching() && cameraOverEyes() >= -0.97,
                        String.format("over eyes=%.2f", cameraOverEyes())));
        b = heightSpot(4);
        heightCase(4, "h4_table_flush", b.below(), new Vec3(0.5, 0, 2.2), new Vec3(0.5, 1.0, 0.7), Direction.UP, true,
                () -> check("table flush with the ground: the player bends down to it", Minecraft.getInstance().player.isCrouching(), ""));
        b = heightSpot(5);
        heightCase(5, "h5_table_in_a_hole", b.below(2), new Vec3(0.5, 0, 1.9), new Vec3(0.5, 1.0, 0.5), Direction.UP, false, null);
        b = heightSpot(6);
        heightCase(6, "h6_furnace_sunk_facing", b.below(), new Vec3(0.5, 0, 2.35), new Vec3(0.5, 0.5, 1.0), Direction.SOUTH, true,
                () -> check("sunk furnace facing the player: bending down to it", Scene.station instanceof FurnaceStation
                        && Minecraft.getInstance().player.isCrouching(), String.format("over eyes=%.2f", cameraOverEyes())));
        b = heightSpot(7);
        heightCase(7, "h7_furnace_sunk_away", b.below(), new Vec3(0.5, 0, 1.4), new Vec3(0.5, 1.0, 0.6), Direction.UP, true,
                () -> check("sunk furnace facing away: the player walks round to its front", Scene.station instanceof FurnaceStation
                        && Minecraft.getInstance().player.getZ() < heightSpot(7).getZ() - 0.4, String.format("z=%.2f", Minecraft.getInstance().player.getZ())));
        b = heightSpot(8);
        heightCase(8, "h8_table_covered", b, new Vec3(0.5, 0, 2.2), new Vec3(0.5, 0.5, 1.0), Direction.SOUTH, false, null);
        b = heightSpot(9);
        heightCase(9, "h9_standing_on_table", b, new Vec3(0.5, 1, 0.5), new Vec3(0.5, 1.0, 0.5), Direction.UP, false, null);
        b = heightSpot(10);
        heightCase(10, "ha_lava_in_front", b, new Vec3(0.5, 0, 3.4), new Vec3(0.5, 0.5, 1.0), Direction.SOUTH, true,
                () -> check("lava before the furnace: the player goes round it and works from beside, crouching",
                        Scene.station instanceof FurnaceStation && Math.abs(Scene.station.swing) > 1 && Minecraft.getInstance().player.isCrouching()
                                && Math.abs(Minecraft.getInstance().player.getX() - (heightSpot(10).getX() + 0.5)) > 0.75,
                        String.format("swing=%.0f x=%.2f z=%.2f", Scene.station == null ? 0 : Scene.station.swing,
                                Minecraft.getInstance().player.getX() - heightSpot(10).getX(), Minecraft.getInstance().player.getZ() - heightSpot(10).getZ())));
        heightCase(23, "hp_lava_all_round", heightSpot(23), new Vec3(0.5, 0, 4.6), new Vec3(0.5, 0.5, 1.0), Direction.SOUTH, false, null);
        String[] names = {"hg_furnace_sunk_far", "hh_smoker_sunk_far", "hi_blast_furnace_sunk_far"};
        for (int k = 0; k < 3; k++) {
            int spot = 16 + k;
            heightCase(spot, names[k], heightSpot(spot).below(), new Vec3(0.5, 0, 4.6), new Vec3(0.5, 1.0, 0.6), Direction.UP, true,
                    () -> {
                        check(names[spot - 16] + ": walked to the edge of the pit and bends down", Scene.station instanceof FurnaceStation
                                && Minecraft.getInstance().player.isCrouching() && Minecraft.getInstance().player.getZ() < heightSpot(spot).getZ() + 2.3,
                                String.format("z=%.2f", Minecraft.getInstance().player.getZ() - heightSpot(spot).getZ()));
                        Vec3 eye = CameraRig.last() == null ? Vec3.ZERO : CameraRig.last().pos();
                        double arm = Math.max(Hands.position(true).distanceTo(eye), Hands.position(false).distanceTo(eye));
                        check(names[spot - 16] + ": the resting hands are not stretched down into the pit", arm < 1.15, String.format("hands from the camera=%.2f", arm));
                    });
        }
        heightCase(19, "hj_furnace_below_floor_far", heightSpot(19), new Vec3(0.5, 1, 4.6), new Vec3(0.5, 0.5, 1.0), Direction.SOUTH, true,
                () -> check("furnace below the floor's edge: walked to the edge and bends down", Scene.station instanceof FurnaceStation
                        && Minecraft.getInstance().player.isCrouching(), String.format("z=%.2f", Minecraft.getInstance().player.getZ() - heightSpot(19).getZ())));
        heightCase(20, "hk_furnace_behind_wall", heightSpot(20), new Vec3(0.5, 0, 4.5), new Vec3(0.5, 0.5, 1.0), Direction.SOUTH, true,
                () -> check("furnace behind a wall: the player went round it", Scene.station instanceof FurnaceStation
                        && Math.abs(Minecraft.getInstance().player.getZ() - heightSpot(20).getZ() - 1.42) < 0.2,
                        String.format("z=%.2f", Minecraft.getInstance().player.getZ() - heightSpot(20).getZ())));
        heightCase(21, "hm_furnace_buried", heightSpot(21).below(), new Vec3(0.5, 0, 3.0), new Vec3(0.5, 1.0, 0.5), Direction.UP, false, null);
        heightCase(22, "hn_furnace_facing_wall", heightSpot(22), new Vec3(0.5, 0, 1.6), new Vec3(0.5, 1.0, 0.5), Direction.UP, false, null);
        b = heightSpot(12);
        heightCase(12, "hc_furnace_front_against_floor", b, new Vec3(0.5, 1, 2.2), new Vec3(0.5, 1.0, 0.6), Direction.UP, false, null);
        b = heightSpot(13);
        heightCase(13, "hd_furnace_sunk_sideways", b.below(), new Vec3(0.9, 0, 1.6), new Vec3(0.5, 1.0, 0.5), Direction.UP, true,
                () -> check("sunk furnace facing sideways: the player walks round to its front", Scene.station instanceof FurnaceStation
                        && Minecraft.getInstance().player.getX() > heightSpot(13).getX() + 1.4, String.format("x=%.2f", Minecraft.getInstance().player.getX() - heightSpot(13).getX())));
        b = heightSpot(14);
        heightCase(14, "he_brewing_below_floor", b, new Vec3(0.5, 1, 2.3), new Vec3(0.5, 0.5, 0.5), Direction.SOUTH, true,
                () -> check("brewing stand below the floor: bending down to it", Scene.station instanceof BrewingStation
                        && Minecraft.getInstance().player.isCrouching() && cameraOverEyes() >= -0.97, String.format("over eyes=%.2f", cameraOverEyes())));
        b = heightSpot(15);
        heightCase(15, "hf_lectern_carpet_in_front", b, new Vec3(0.5, 0.0625, 2.6), new Vec3(0.5, 0.9, 0.6), Direction.UP, true,
                () -> check("lectern with a carpet in front: reading it", Scene.station instanceof LecternStation, "station=" + Scene.station));
        b = heightSpot(11);
        heightCase(11, "hb_ground_gives_way", b, new Vec3(0.5, 0, 2.2), new Vec3(0.5, 1.0, 0.7), Direction.UP, true, () -> {
            server(p -> {
                BlockPos under = p.blockPosition().below();
                p.serverLevel().setBlockAndUpdate(under, Blocks.AIR.defaultBlockState());
                p.serverLevel().setBlockAndUpdate(under.below(), Blocks.AIR.defaultBlockState());
            });
        }, false);
        step(2, () -> {
            check("falling lets the player go", Scene.phase != Scene.Phase.ACTIVE, "phase=" + Scene.phase);
            shot("hb_ground_gives_way_6_fallen");
        });
        optionsAct();
    }

    // ---- settings: the screen, the height logic off, a scene turned off -----------------------------

    private static void optionsAct() {
        if (stopHere("o")) {
            return;
        }
        step(30, () -> {
            act = "o";
            closeWindow();
            Minecraft mc = Minecraft.getInstance();
            //? if neoforge {
            net.neoforged.fml.ModContainer mod = net.neoforged.fml.ModList.get().getModContainerById(Cinematic.MODID).orElseThrow();
            Screen screen = mod.getCustomExtension(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class).orElseThrow().createScreen(mod, null);
            mc.setScreen(screen);
            //?} else if fabric {
            /*mc.setScreen(CinematicClient.settingsScreen(null));
            *///?} else if forge {
            /*net.minecraftforge.fml.ModContainer mod = net.minecraftforge.fml.ModList.get().getModContainerById(Cinematic.MODID).orElseThrow();
            Screen screen = mod.getCustomExtension(net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class).orElseThrow()
                    .screenFunction().apply(mc, null);
            mc.setScreen(screen);
            *///?}
            check("the settings screen opens", mc.screen instanceof SettingsScreen, mc.screen == null ? "-" : mc.screen.getClass().getName());
            pointGui(mc.getWindow().getGuiScaledWidth() * 0.5, mc.getWindow().getGuiScaledHeight() - 4);
        });
        step(20, () -> shot("o1_settings_general"));
        // over the first option, for its description under the list
        step(2, () -> pointGui(Minecraft.getInstance().getWindow().getGuiScaledWidth() * 0.4, 78));
        step(16, () -> shot("o2_settings_description"));
        step(2, () -> clickWidget(Component.translatable("cinematic.settings.tab.stations").getString()));
        step(16, () -> shot("o3_settings_stations"));
        step(2, () -> clickWidget(Component.translatable("cinematic.settings.tab.camera").getString()));
        step(16, () -> shot("o4_settings_camera"));
        step(2, () -> check("advanced tab opens", clickSettingsSection(Minecraft.getInstance().screen,
                Component.translatable("cinematic.settings.tab.advanced").getString()), "client-only switch visible in-game"));
        step(16, () -> shot("o5_settings_advanced"));
        step(2, () -> { if (Minecraft.getInstance().screen instanceof SettingsScreen settings) settings.showDeveloper(); });
        step(16, () -> { verifyProfileControls(); shot("o6_settings_profiles"); });
        step(2, () -> Minecraft.getInstance().setScreen(null));

        // the height logic off: no window, a word on why
        //? if >=1.21 {
        step(10, () -> Settings.HEIGHT_LOGIC.set(false));
        //?} else if <1.21 {
        /*step(10, () -> Settings.set(Settings.HEIGHT_LOGIC, false));
        *///?}
        noticeCase(0, "o6_too_high", heightSpot(0).above(), new Vec3(0.5, 0, 2.2), new Vec3(0.5, 0.5, 1.0), Direction.SOUTH, "too_high");
        noticeCase(5, "o6_too_low", heightSpot(5).below(2), new Vec3(0.5, 0, 1.9), new Vec3(0.5, 1.0, 0.5), Direction.UP, "too_low");
        noticeCase(22, "o6_facing_wall", heightSpot(22), new Vec3(0.5, 0, 1.6), new Vec3(0.5, 1.0, 0.5), Direction.UP, "no_way");
        noticeCase(8, "o6_covered", heightSpot(8), new Vec3(0.5, 0, 2.2), new Vec3(0.5, 0.5, 1.0), Direction.SOUTH, "covered");
        noticeCase(9, "o6_on_top", heightSpot(9), new Vec3(0.5, 1, 0.5), new Vec3(0.5, 1.0, 0.5), Direction.UP, "on_top");
        noticeCase(23, "o6_no_way", heightSpot(23), new Vec3(0.5, 0, 4.6), new Vec3(0.5, 0.5, 1.0), Direction.SOUTH, "no_way");
        //? if >=1.21 {
        step(2, () -> Settings.HEIGHT_LOGIC.set(true));
        //?} else if <1.21 {
        /*step(2, () -> Settings.set(Settings.HEIGHT_LOGIC, true));
        *///?}

        // the pace: twice as fast, the lean is over in 0.6 s instead of 1.2 (standing right at the table, no walk)
        Vec3 atTable = Vec3.atLowerCornerOf(heightSpot(4)).add(0.5, 0, 1.36);
        BlockPos flush = heightSpot(4).below();
        step(4, () -> {
            //? if neoforge {
            Settings.APPROACH_SPEED.set(200);
            Settings.QUICK_RETURN.set(false);
            //?} else if fabric {
            /*Settings.APPROACH_SPEED.set(200);
            Settings.QUICK_MODE.set(0);
            *///?} else if forge {
            /*Settings.set(Settings.APPROACH_SPEED, 200);
            Settings.set(Settings.QUICK_RETURN, false);
            *///?}
        });
        paceCase("o9_pace_fast", atTable, flush, true);
        //? if >=1.21 {
        step(4, () -> Settings.APPROACH_SPEED.set(100));
        //?} else if <1.21 {
        /*step(4, () -> Settings.set(Settings.APPROACH_SPEED, 100));
        *///?}
        paceCase("o9_pace_usual", atTable, flush, false);
        // the camera's lean: none, the camera stays at the eyes and looks at the work
        step(4, () -> {
            //? if neoforge {
            Settings.LEAN_STRENGTH.set(0);
            Settings.QUICK_RETURN.set(true);
            //?} else if fabric {
            /*Settings.LEAN_STRENGTH.set(0);
            Settings.QUICK_MODE.set(1);
            *///?} else if forge {
            /*Settings.set(Settings.LEAN_STRENGTH, 0);
            Settings.set(Settings.QUICK_RETURN, true);
            *///?}
        });
        step(2, () -> {
            act = "o8_lean_0";
            standLooking(atTable, Vec3.atCenterOf(flush));
        });
        step(10, () -> useAt(flush, new Vec3(0.5, 1.0, 0.7), Direction.UP));
        step(60, () -> {
            Minecraft mc = Minecraft.getInstance();
            Vec3 eye = mc.player.getEyePosition();
            double off = CameraRig.last() == null ? 99 : CameraRig.last().pos().distanceTo(eye);
            check("camera lean 0%: the camera stays at the eyes", Scene.phase == Scene.Phase.ACTIVE && off < 0.08, String.format("off the eyes=%.3f", off));
            shot("o8_lean_0");
        });
        step(2, Scene::leave);
        //? if >=1.21 {
        step(36, () -> Settings.LEAN_STRENGTH.set(100));
        //?} else if <1.21 {
        /*step(36, () -> Settings.set(Settings.LEAN_STRENGTH, 100));
        *///?}
        step(2, () -> {
            act = "o8_lean_100";
            standLooking(atTable, Vec3.atCenterOf(flush));
        });
        step(10, () -> useAt(flush, new Vec3(0.5, 1.0, 0.7), Direction.UP));
        step(60, () -> shot("o8_lean_100"));
        step(2, Scene::leave);
        step(36, () -> {});

        // a scene turned off: the block keeps its usual window
        //? if >=1.21 {
        step(2, () -> Settings.SCENES.get(StationKind.TABLE).set(false));
        //?} else if <1.21 {
        /*step(2, () -> Settings.set(Settings.SCENES.get(StationKind.TABLE), false));
        *///?}
        heightCase(4, "o7_table_scene_off", heightSpot(4).below(), new Vec3(0.5, 0, 2.2), new Vec3(0.5, 1.0, 0.7), Direction.UP, false, null);
        //? if >=1.21 {
        step(2, () -> Settings.SCENES.get(StationKind.TABLE).set(true));
        //?} else if <1.21 {
        /*step(2, () -> Settings.set(Settings.SCENES.get(StationKind.TABLE), true));
        *///?}
        transitionAct();
    }

    // ---- moving over from one station to the next without stepping away ------------------------------

    private static void transitionAct() {
        if (stopHere("t")) {
            return;
        }
        // at the table, with planks in hand; the furnace stands right beside it
        step(10, () -> {
            act = "t";
            closeWindow();
            standLooking(new Vec3(0.5, -60, 3.36), Vec3.atCenterOf(TABLE));
        });
        step(10, () -> useAt(TABLE, new Vec3(0.5, 1.0, 0.8), Direction.UP));
        step(70, () -> check("moving over: working at the table", Scene.station instanceof TableStation && Scene.phase == Scene.Phase.ACTIVE,
                "phase=" + Scene.phase));
        step(2, () -> pointGui(Minecraft.getInstance().getWindow().getGuiScaledWidth() * 0.5, Minecraft.getInstance().getWindow().getGuiScaledHeight() * 0.6));
        step(4, () -> invClickItem(Items.OAK_PLANKS));
        step(4, () -> pointAt(new Vec3(FURNACE.getX() + 0.6, FURNACE.getY() + 1.0, FURNACE.getZ() + 0.75)));
        step(8, () -> {
            check("moving over: the furnace beside is offered", FURNACE.equals(Scene.neighbor), "neighbor=" + Scene.neighbor
                    + " cursor=" + java.util.Arrays.toString(Picking.mouseGui()));
            shot("t0_offer_furnace");
        });
        step(2, () -> {
            // film the move over itself
            act = "t2";
            introShot = 0;
            screenClick(1);
        });
        step(2, () -> check("moving over: the item in hand is put away", Hands.carried().isEmpty(), "carried=" + describe(Hands.carried())));
        step(60, () -> {
            check("moving over: working at the furnace, no window", Scene.station instanceof FurnaceStation && Scene.phase == Scene.Phase.ACTIVE
                    && Minecraft.getInstance().screen instanceof SceneScreen, "phase=" + Scene.phase + " station=" + Scene.station);
            shot("t5_at_furnace");
            server(p -> {
                dev.dyslexic.cinematic.station.Sessions.Session s = dev.dyslexic.cinematic.station.Sessions.get(p);
                boolean planks = false;
                for (ItemStack stack : p.getInventory().items) {
                    planks |= stack.is(Items.OAK_PLANKS);
                }
                check("moving over: the server moved the session, the planks are back", s != null && s.kind() == StationKind.FURNACE
                        && planks && p.containerMenu.getCarried().isEmpty(), "session=" + (s == null ? "-" : s.kind()) + " planks=" + planks);
            });
        });
        // and back to the table, beside the furnace
        step(2, () -> pointAt(new Vec3(TABLE.getX() + 0.3, TABLE.getY() + 0.9, TABLE.getZ() + 1.0)));
        step(8, () -> {
            check("moving over: the table beside is offered", TABLE.equals(Scene.neighbor), "neighbor=" + Scene.neighbor
                    + " cursor=" + java.util.Arrays.toString(Picking.mouseGui()));
            shot("t6_offer_table");
        });
        step(2, () -> {
            act = "t7";
            introShot = 0;
            screenClick(1);
        });
        step(60, () -> {
            check("moving over: back at the table", Scene.station instanceof TableStation && Scene.phase == Scene.Phase.ACTIVE, "phase=" + Scene.phase);
            shot("t9_back_at_table");
        });
        step(2, Scene::leave);
        // two steps over: from the loom to the cartography table, walking to it
        step(40, () -> {
            act = "u_t";
            standLooking(new Vec3(LOOM.getX() + 0.5, -60, LOOM.getZ() + 1.86), Vec3.atCenterOf(LOOM));
        });
        step(10, () -> useAt(LOOM, new Vec3(0.5, 1.0, 0.7), Direction.UP));
        step(70, () -> check("moving over: working at the loom", Scene.station instanceof LoomStation && Scene.phase == Scene.Phase.ACTIVE,
                "phase=" + Scene.phase));
        step(2, () -> pointAt(new Vec3(CARTOGRAPHY.getX() + 0.3, CARTOGRAPHY.getY() + 1.0, CARTOGRAPHY.getZ() + 0.6)));
        step(8, () -> {
            check("moving over: the cartography table is offered", CARTOGRAPHY.equals(Scene.neighbor), "neighbor=" + Scene.neighbor
                    + " cursor=" + java.util.Arrays.toString(Picking.mouseGui()));
            shot("t10_offer_cartography");
        });
        step(2, () -> {
            act = "t11";
            introShot = 0;
            screenClick(1);
        });
        step(90, () -> {
            // (it stands at the side nearest the loom, between the two)
            check("moving over: walked to the cartography table", Scene.station instanceof CartographyStation && Scene.phase == Scene.Phase.ACTIVE
                    && Minecraft.getInstance().player.getX() > LOOM.getX() + 1.2, String.format("phase=%s x=%.2f side=%s",
                    Scene.phase, Minecraft.getInstance().player.getX(), Scene.station == null ? "-" : Scene.station.sideDirection()));
            shot("t15_at_cartography");
        });
        step(2, Scene::leave);
        layoutAct();
    }

    // ---- laying out a recipe from the reference --------------------------------------------------

    private static void layoutAct() {
        if (stopHere("y")) {
            return;
        }
        // a clear table, and planks and sticks for a wooden pickaxe
        step(40, () -> {
            act = "y";
            closeWindow();
            server(p -> {
                dev.dyslexic.cinematic.table.TableActions.spill(p.serverLevel(), TABLE);
                p.getInventory().add(new ItemStack(Items.OAK_PLANKS, 6));
                p.getInventory().add(new ItemStack(Items.STICK, 4));
            });
            standLooking(new Vec3(0.5, -60, 3.36), Vec3.atCenterOf(TABLE));
        });
        step(30, () -> useAt(TABLE, new Vec3(0.5, 1.0, 0.8), Direction.UP));
        step(70, () -> check("lay out: at the table", Scene.station instanceof TableStation && Scene.phase == Scene.Phase.ACTIVE, "phase=" + Scene.phase));
        step(4, () -> {
            var at = table().recipeBook.togglePoint();
            pointGui(at[0], at[1]);
            screenClick(0);
        });
        step(15, () -> recipeSearch("wooden_pickaxe"));
        step(4, () -> pointRecipe("wooden_pickaxe"));
        step(3, () -> screenClick(0));
        step(10, () -> {
            double[] at = table().recipeBook.actionPoint();
            pointGui(at[0], at[1]);
        });
        step(6, () -> {
            shot("y0_lay_button");
            screenClick(0);
        });
        step(9, () -> shot("y1_laying"));
        step(9, () -> shot("y2_laying"));
        step(45, () -> {
            ClientTables.Entry entry = ClientTables.get(TABLE);
            int laid = 0;
            for (int i = 0; entry != null && i < 9; i++) {
                laid += entry.items[i].isEmpty() ? 0 : 1;
            }
            ItemStack result = entry == null ? ItemStack.EMPTY : dev.dyslexic.cinematic.table.TableCrafting.match(Minecraft.getInstance().level,
                    entry.list(), Scene.station.sideDirection()).map(dev.dyslexic.cinematic.table.TableCrafting.Match::result).orElse(ItemStack.EMPTY);
            check("lay out: the pickaxe laid out from the inventory", laid == 5 && result.is(Items.WOODEN_PICKAXE), "laid=" + laid + " result=" + describe(result));
            shot("y3_laid_out");
        });
        // a recipe there is not enough for: what there is goes down, the rest stays see-through
        step(4, () -> server(p -> dev.dyslexic.cinematic.table.TableActions.spill(p.serverLevel(), TABLE)));
        step(20, () -> check("lay out: the table cleared", ClientTables.get(TABLE) == null || ClientTables.get(TABLE).list().stream().allMatch(ItemStack::isEmpty), ""));
        step(4, () -> {
            useAt(TABLE, new Vec3(0.5, 1.0, 0.8), Direction.UP);
        });
        step(70, () -> {
            var at = table().recipeBook.togglePoint();
            pointGui(at[0], at[1]);
            screenClick(0);
        });
        step(15, () -> recipeSearch("diamond_pickaxe"));
        step(4, () -> pointRecipe("diamond_pickaxe"));
        step(3, () -> screenClick(0));
        step(10, () -> {
            double[] at = table().recipeBook.actionPoint();
            pointGui(at[0], at[1]);
            screenClick(0);
        });
        step(40, () -> {
            ClientTables.Entry entry = ClientTables.get(TABLE);
            int laid = 0;
            for (int i = 0; entry != null && i < 9; i++) {
                laid += entry.items[i].isEmpty() ? 0 : 1;
            }
            check("lay out: only the sticks go down, the diamonds stay see-through", laid == 2 && table().guide.missingCount == 3,
                    "laid=" + laid + " missing=" + table().guide.missingCount);
            pointAside();
            shot("y4_partial");
        });
        step(2, () -> recipeControl(113, 194));
        step(2, () -> recipeControl(113, 12));
        tableSpreadScript();
        tableLayAllScript();
        lookAct();
    }

    // ---- sharing a stack with a left-drag, and laying out every set at once ---------------------------

    private static final List<ItemStack> savedInventory = new ArrayList<>();

    /** Puts the inventory aside and gives exactly {@code stacks}, so that the counts below are known. */
    private static void testInventory(ItemStack... stacks) {
        server(p -> {
            if (savedInventory.isEmpty()) {
                for (int i = 0; i < 36; i++) savedInventory.add(p.getInventory().getItem(i).copy());
            }
            for (int i = 0; i < 36; i++) p.getInventory().setItem(i, i < stacks.length ? stacks[i].copy() : ItemStack.EMPTY);
            p.inventoryMenu.setCarried(ItemStack.EMPTY);
            p.inventoryMenu.broadcastChanges();
        });
    }

    private static void restoreInventory() {
        server(p -> {
            for (int i = 0; i < 36 && i < savedInventory.size(); i++) p.getInventory().setItem(i, savedInventory.get(i));
            savedInventory.clear();
            p.inventoryMenu.setCarried(ItemStack.EMPTY);
            p.inventoryMenu.broadcastChanges();
        });
    }

    /** Clears the table without spilling (spilt items would be picked up again and spoil the counts). */
    private static void wipeTable(ServerPlayer p) {
        TableStore.get(p.serverLevel()).remove(TABLE);
        dev.dyslexic.cinematic.table.TableActions.sync(p.serverLevel(), TABLE);
    }

    private static void putOnTable(ServerPlayer p, int row, int col, ItemStack stack) {
        TableStore store = TableStore.get(p.serverLevel());
        store.getOrCreate(TABLE).items.set(TableGrid.worldIndex(Scene.station.sideDirection(), row, col), stack.copy());
        store.setDirty();
        dev.dyslexic.cinematic.table.TableActions.sync(p.serverLevel(), TABLE);
    }

    private static ItemStack onTable(ServerPlayer p, int row, int col) {
        TableStore.Table stored = TableStore.get(p.serverLevel()).get(TABLE);
        return stored == null ? ItemStack.EMPTY : stored.items.get(TableGrid.worldIndex(Scene.station.sideDirection(), row, col));
    }

    /** Every item of a kind the player can account for: inventory, hand and table top. */
    private static int heldEverywhere(ServerPlayer p, net.minecraft.world.item.Item item) {
        int total = p.inventoryMenu.getCarried().is(item) ? p.inventoryMenu.getCarried().getCount() : 0;
        for (int i = 0; i < 36; i++) if (p.getInventory().getItem(i).is(item)) total += p.getInventory().getItem(i).getCount();
        TableStore.Table stored = TableStore.get(p.serverLevel()).get(TABLE);
        for (int i = 0; stored != null && i < TableGrid.CELLS; i++) if (stored.items.get(i).is(item)) total += stored.items.get(i).getCount();
        return total;
    }

    private static void screenPress(int button) { screenClick(button); }

    private static void screenDrag(int button) {
        double[] at = Picking.mouseGui();
        Minecraft.getInstance().screen.mouseDragged(at[0], at[1], button, 1, 0);
    }

    private static void screenRelease(int button) {
        double[] at = Picking.mouseGui();
        Minecraft.getInstance().screen.mouseReleased(at[0], at[1], button);
    }

    private static void tableSpreadScript() {
        // singleplayer never encodes packets: the new SPREAD mask and PLACE count go through the codec here
        step(2, () -> {
            var spread = new dev.dyslexic.cinematic.net.StationActionPayload(TABLE, StationKind.TABLE,
                    dev.dyslexic.cinematic.net.StationActionPayload.Action.SPREAD, 0b111111111, 0, false, 0);
            var place = new dev.dyslexic.cinematic.net.StationActionPayload(TABLE, StationKind.TABLE,
                    dev.dyslexic.cinematic.net.StationActionPayload.Action.PLACE, 8, -1, true, 99);
            var quick = new dev.dyslexic.cinematic.net.StationActionPayload(TABLE, StationKind.BREWING,
                    dev.dyslexic.cinematic.net.StationActionPayload.Action.QUICK, 35, 0, true, 0);
            for (var sent : List.of(spread, place, quick)) {
                var buf = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
                //? if >=1.21 {
                dev.dyslexic.cinematic.net.StationActionPayload.CODEC.encode(buf, sent);
                var read = dev.dyslexic.cinematic.net.StationActionPayload.CODEC.decode(buf);
                //?} else if <1.21 {
                /*sent.write(buf);
                var read = dev.dyslexic.cinematic.net.StationActionPayload.read(buf);
                *///?}
                check("station action " + sent.action() + " survives the codec", read.equals(sent) && buf.readableBytes() == 0,
                        "read=" + read);
                buf.release();
            }
        });
        // ten planks dragged over three cells: three each, one stays in hand; passing a cell again adds nothing
        step(4, () -> {
            testInventory();
            server(p -> {
                wipeTable(p);
                p.inventoryMenu.setCarried(new ItemStack(Items.BIRCH_PLANKS, 10));
                p.inventoryMenu.broadcastChanges();
            });
        });
        step(8, () -> point(0, 0));
        step(4, () -> screenPress(0));
        step(3, () -> point(0, 1));
        step(3, () -> screenDrag(0));
        step(3, () -> point(0, 2));
        step(3, () -> screenDrag(0));
        step(3, () -> point(0, 1));
        step(3, () -> screenDrag(0));
        step(6, () -> {
            check("spread: three cells gathered, three each", Integer.bitCount(table().spreadCells()) == 3 && table().spreadShare() == 3,
                    "cells=" + Integer.bitCount(table().spreadCells()) + " share=" + table().spreadShare());
            shot("y5_spread_preview");
        });
        step(2, () -> screenRelease(0));
        step(16, () -> {
            server(p -> check("spread: 3 + 3 + 3 on the table, 1 in hand, nothing made or lost",
                    onTable(p, 0, 0).getCount() == 3 && onTable(p, 0, 1).getCount() == 3 && onTable(p, 0, 2).getCount() == 3
                            && p.inventoryMenu.getCarried().getCount() == 1 && heldEverywhere(p, Items.BIRCH_PLANKS) == 10,
                    describe(onTable(p, 0, 0)) + " " + describe(onTable(p, 0, 1)) + " " + describe(onTable(p, 0, 2))
                            + " hand=" + describe(p.inventoryMenu.getCarried()) + " total=" + heldEverywhere(p, Items.BIRCH_PLANKS)));
            shot("y6_spread_done");
        });
        // an almost full stack takes only what fits; a cell with another item is passed over
        step(4, () -> server(p -> {
            wipeTable(p);
            putOnTable(p, 1, 0, new ItemStack(Items.BIRCH_PLANKS, 62));
            putOnTable(p, 1, 1, new ItemStack(Items.STICK));
            p.inventoryMenu.setCarried(new ItemStack(Items.BIRCH_PLANKS, 10));
            p.inventoryMenu.broadcastChanges();
        }));
        step(10, () -> point(1, 0));
        step(4, () -> screenPress(0));
        step(3, () -> point(1, 1));
        step(3, () -> screenDrag(0));
        step(3, () -> point(1, 2));
        step(3, () -> screenDrag(0));
        step(4, () -> check("spread: the other item is passed over", Integer.bitCount(table().spreadCells()) == 2,
                "cells=" + Integer.bitCount(table().spreadCells())));
        step(2, () -> screenRelease(0));
        step(16, () -> server(p -> check("spread: 62 + 2 = a full stack, 5 beside it, 3 in hand, the stick untouched",
                onTable(p, 1, 0).getCount() == 64 && onTable(p, 1, 2).getCount() == 5 && onTable(p, 1, 1).is(Items.STICK)
                        && onTable(p, 1, 1).getCount() == 1 && p.inventoryMenu.getCarried().getCount() == 3
                        && heldEverywhere(p, Items.BIRCH_PLANKS) == 72,
                describe(onTable(p, 1, 0)) + " " + describe(onTable(p, 1, 1)) + " " + describe(onTable(p, 1, 2))
                        + " hand=" + describe(p.inventoryMenu.getCarried()))));
        // a plain click still puts the whole stack down
        step(4, () -> server(p -> {
            wipeTable(p);
            p.inventoryMenu.setCarried(new ItemStack(Items.BIRCH_PLANKS, 10));
            p.inventoryMenu.broadcastChanges();
        }));
        step(10, () -> point(2, 2));
        step(4, () -> clickCell(2, 2, 0));
        step(16, () -> server(p -> check("spread: a plain click lays the whole stack, as before",
                onTable(p, 2, 2).getCount() == 10 && p.inventoryMenu.getCarried().isEmpty(),
                describe(onTable(p, 2, 2)) + " hand=" + describe(p.inventoryMenu.getCarried()))));
        // two items cannot be shared between three cells: the third is not taken in
        step(4, () -> server(p -> {
            wipeTable(p);
            p.inventoryMenu.setCarried(new ItemStack(Items.BIRCH_PLANKS, 2));
            p.inventoryMenu.broadcastChanges();
        }));
        step(10, () -> point(0, 0));
        step(4, () -> screenPress(0));
        step(3, () -> point(0, 1));
        step(3, () -> screenDrag(0));
        step(3, () -> point(0, 2));
        step(3, () -> screenDrag(0));
        step(2, () -> screenRelease(0));
        step(16, () -> server(p -> check("spread: no more cells than items", onTable(p, 0, 0).getCount() == 1
                && onTable(p, 0, 1).getCount() == 1 && onTable(p, 0, 2).isEmpty() && p.inventoryMenu.getCarried().isEmpty(),
                describe(onTable(p, 0, 0)) + " " + describe(onTable(p, 0, 1)) + " " + describe(onTable(p, 0, 2)))));
        step(4, () -> server(Showcase::wipeTable));
    }

    private static void tableLayAllScript() {
        // torches: the top cell takes the kind of coal there is more of, every set is whole
        step(6, () -> testInventory(new ItemStack(Items.COAL, 5), new ItemStack(Items.CHARCOAL, 7), new ItemStack(Items.STICK, 20)));
        step(10, () -> {
            var at = table().recipeBook.togglePoint();
            pointGui(at[0], at[1]);
            screenClick(0);
        });
        step(15, () -> recipeSearch("torch"));
        step(4, () -> pointRecipe("torch"));
        step(3, () -> screenClick(0));
        step(6, () -> {
            var max = table().guide.maxLayout();
            check("lay out all: seven torch sets offered", max.sets() == 7 && max.current() == 0, "sets=" + max.sets() + " moves=" + max.moves().size());
            table().guide.layOut(true);
        });
        step(8, () -> shot("y7_lay_all_moving"));
        step(40, () -> {
            server(p -> check("lay out all: 7 charcoal over 7 sticks, the coal stays", heldEverywhere(p, Items.CHARCOAL) == 7
                    && heldEverywhere(p, Items.COAL) == 5 && heldEverywhere(p, Items.STICK) == 20
                    && TableStore.get(p.serverLevel()).get(TABLE) != null
                    && TableStore.get(p.serverLevel()).get(TABLE).items.stream().mapToInt(s -> s.is(Items.CHARCOAL) || s.is(Items.STICK) ? s.getCount() : 0).sum() == 14,
                    "charcoal=" + heldEverywhere(p, Items.CHARCOAL) + " sticks=" + heldEverywhere(p, Items.STICK)));
            check("lay out all: the table shows the torch", table().result.is(Items.TORCH), "result=" + describe(table().result));
            pointAside();
            shot("y8_lay_all_torches");
        });
        step(4, () -> table().craft(true));
        step(30, () -> server(p -> check("lay out all: a real craft of every set gives 28 torches", heldEverywhere(p, Items.TORCH) == 28
                && heldEverywhere(p, Items.CHARCOAL) == 0 && heldEverywhere(p, Items.STICK) == 13,
                "torches=" + heldEverywhere(p, Items.TORCH) + " sticks=" + heldEverywhere(p, Items.STICK))));
        // eyes of ender: one set first, then every set up to the pearl's stack limit of sixteen
        step(4, () -> {
            testInventory(new ItemStack(Items.ENDER_PEARL, 16), new ItemStack(Items.ENDER_PEARL, 16), new ItemStack(Items.ENDER_PEARL, 8),
                    new ItemStack(Items.BLAZE_POWDER, 40));
            server(Showcase::wipeTable);
        });
        step(10, () -> recipeSearch("ender_eye"));
        step(4, () -> pointRecipe("ender_eye"));
        step(3, () -> screenClick(0));
        step(6, () -> table().guide.layOut(false));
        step(30, () -> {
            var max = table().guide.maxLayout();
            check("lay out all: one set lies, sixteen offered", max.current() == 1 && max.sets() == 16, "current=" + max.current() + " sets=" + max.sets());
            table().guide.layOut(true);
        });
        step(40, () -> server(p -> {
            TableStore.Table stored = TableStore.get(p.serverLevel()).get(TABLE);
            int pearls = 0, powder = 0;
            for (int i = 0; stored != null && i < TableGrid.CELLS; i++) {
                if (stored.items.get(i).is(Items.ENDER_PEARL)) pearls = Math.max(pearls, stored.items.get(i).getCount());
                if (stored.items.get(i).is(Items.BLAZE_POWDER)) powder = Math.max(powder, stored.items.get(i).getCount());
            }
            check("lay out all: topped up to the pearl's stack limit, nothing lost", pearls == 16 && powder == 16
                    && heldEverywhere(p, Items.ENDER_PEARL) == 40 && heldEverywhere(p, Items.BLAZE_POWDER) == 40,
                    "pearls=" + pearls + " powder=" + powder + " total=" + heldEverywhere(p, Items.ENDER_PEARL));
        }));
        step(2, () -> shot("y9_lay_all_pearls"));
        // nothing to promise: no diamonds for a single pickaxe
        step(4, () -> {
            testInventory(new ItemStack(Items.STICK, 10));
            server(Showcase::wipeTable);
        });
        step(10, () -> recipeSearch("diamond_pickaxe"));
        step(4, () -> pointRecipe("diamond_pickaxe"));
        step(3, () -> screenClick(0));
        step(8, () -> {
            var max = table().guide.maxLayout();
            check("lay out all: no full set, nothing offered", max.moves().isEmpty() && max.current() == 0, "sets=" + max.sets());
            table().guide.layOut(true);
        });
        step(20, () -> server(p -> check("lay out all: nothing laid without a full set", TableStore.get(p.serverLevel()).get(TABLE) == null
                && heldEverywhere(p, Items.STICK) == 10, "sticks=" + heldEverywhere(p, Items.STICK))));
        step(2, () -> recipeControl(113, 194));
        step(2, () -> recipeControl(113, 12));
        step(4, Showcase::restoreInventory);
    }

    // ---- looking round from behind while at work ----------------------------------------------------

    private static void lookAct() {
        if (stopHere("v")) {
            return;
        }
        step(10, () -> {
            act = "v";
            if (!(Scene.station instanceof TableStation) || Scene.phase != Scene.Phase.ACTIVE) {
                closeWindow();
                standLooking(new Vec3(0.5, -60, 3.36), Vec3.atCenterOf(TABLE));
            }
        });
        step(10, () -> {
            if (Scene.phase == Scene.Phase.NONE) {
                useAt(TABLE, new Vec3(0.5, 1.0, 0.8), Direction.UP);
            }
        });
        step(70, () -> {
            pointAside();
            Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_LEFT_ALT, 0, 0);
        });
        step(30, () -> {
            Minecraft mc = Minecraft.getInstance();
            double away = CameraRig.last() == null ? 0 : CameraRig.last().pos().distanceTo(mc.player.getEyePosition());
            check("look round: the camera goes behind the player, the body is drawn", FreeLook.amount() > 0.95
                    && mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK && away > 1.4, String.format("amount=%.2f camera=%s away=%.2f",
                    FreeLook.amount(), mc.options.getCameraType(), away));
            shot("v1_look");
        });
        step(2, () -> Minecraft.getInstance().mouseHandler.xpos += 500);
        step(14, () -> shot("v2_look_turned"));
        step(2, () -> Minecraft.getInstance().mouseHandler.xpos += 700);
        step(14, () -> shot("v3_look_behind"));
        step(2, () -> Minecraft.getInstance().screen.keyReleased(GLFW.GLFW_KEY_LEFT_ALT, 0, 0));
        step(30, () -> {
            Minecraft mc = Minecraft.getInstance();
            check("look round: letting go, back at work", FreeLook.amount() < 0.02 && mc.options.getCameraType().isFirstPerson()
                    && Scene.phase == Scene.Phase.ACTIVE, String.format("amount=%.2f camera=%s phase=%s", FreeLook.amount(), mc.options.getCameraType(), Scene.phase));
            shot("v4_back");
        });
        // pressed on and off, as the settings allow
        step(2, () -> {
            //? if >=1.21 {
            Settings.LOOK_TOGGLES.set(true);
            //?} else if <1.21 {
            /*Settings.set(Settings.LOOK_TOGGLES, true);
            *///?}
            Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_LEFT_ALT, 0, 0);
            Minecraft.getInstance().screen.keyReleased(GLFW.GLFW_KEY_LEFT_ALT, 0, 0);
        });
        step(30, () -> check("look round, pressed on: still looking after the key is let go", FreeLook.amount() > 0.95, String.format("amount=%.2f", FreeLook.amount())));
        step(2, () -> Minecraft.getInstance().screen.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0));
        step(30, () -> check("look round: Esc comes back to work first", FreeLook.amount() < 0.02 && Scene.phase == Scene.Phase.ACTIVE,
                String.format("amount=%.2f phase=%s", FreeLook.amount(), Scene.phase)));
        //? if >=1.21 {
        step(2, () -> Settings.LOOK_TOGGLES.set(false));
        //?} else if <1.21 {
        /*step(2, () -> Settings.set(Settings.LOOK_TOGGLES, false));
        *///?}
        step(2, Scene::leave);
        jukeboxAct();
    }

    // ---- the jukebox ----------------------------------------------------------------------------------

    private static void jukeboxAct() {
        if (stopHere("j")) return;
        boolean developer = Settings.developerMode();
        step(35, () -> {
            //? if >=1.21 {
            act = "jx"; closeWindow(); Settings.DEVELOPER_MODE.set(true);
            //?} else if <1.21 {
            /*act = "jx"; closeWindow(); Settings.set(Settings.DEVELOPER_MODE, true);
            *///?}
            hold(8, new ItemStack(Items.MUSIC_DISC_CAT));
            standLooking(new Vec3(JUKEBOX.getX() - 1.6, -60, JUKEBOX.getZ() + 0.5), Vec3.atCenterOf(JUKEBOX));
        });
        step(20, () -> useAt(JUKEBOX, new Vec3(0.5, 1, 0.5), Direction.UP));
        step(75, () -> {
            check("jukebox waits for editing", Scene.phase == Scene.Phase.ACTIVE && Scene.station.kind() == StationKind.JUKEBOX && !Scene.station.busy(), "no automatic insertion in developer mode");
            Runnable restoreLayout = spatialEditorScript();
            //? if >=1.21 {
            step(2, Scene::leave); step(45, () -> Settings.DEVELOPER_MODE.set(false));
            //?} else if <1.21 {
            /*step(2, Scene::leave); step(45, () -> Settings.set(Settings.DEVELOPER_MODE, false));
            *///?}
            jukeboxOperations(developer, restoreLayout);
        });
    }

    private static void jukeboxOperations(boolean developer, Runnable restoreLayout) {
        step(40, () -> {
            act = "j";
            closeWindow();
            server(p -> {
                p.getInventory().setItem(8, new ItemStack(Items.MUSIC_DISC_CAT));
                p.getInventory().selected = 8;
            });
            Minecraft.getInstance().player.getInventory().selected = 8;
            Minecraft.getInstance().getConnection().send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(8));
            standLooking(new Vec3(JUKEBOX.getX() - 1.6, -60, JUKEBOX.getZ() + 0.5), Vec3.atCenterOf(JUKEBOX));
        });
        step(30, () -> shot("j0_before"));
        step(4, () -> {
            introShot = 0;
            moveShot = 0;
            useAt(JUKEBOX, new Vec3(0.5, 1.0, 0.5), Direction.UP);
        });
        step(110, () -> {
            check("jukebox: a short scene, over by itself", Scene.phase == Scene.Phase.NONE, "phase=" + Scene.phase);
            server(p -> check("jukebox: the record went in and plays", p.serverLevel().getBlockState(JUKEBOX).getValue(net.minecraft.world.level.block.JukeboxBlock.HAS_RECORD)
                    && p.getInventory().getItem(8).isEmpty(), "slot 8=" + describe(p.getInventory().getItem(8))));
        });
        step(30, () -> {
            act = "j2";
            introShot = 0;
            moveShot = 0;
            useAt(JUKEBOX, new Vec3(0.5, 1.0, 0.5), Direction.UP);
        });
        step(110, () -> server(p -> {
            boolean back = false;
            for (ItemStack stack : p.getInventory().items) {
                back |= stack.is(Items.MUSIC_DISC_CAT);
            }
            check("jukebox: the record taken out into the inventory", !p.serverLevel().getBlockState(JUKEBOX).getValue(net.minecraft.world.level.block.JukeboxBlock.HAS_RECORD)
                    && back, "back=" + back);
        }));
        // the scene turned off: the jukebox works as in vanilla
        step(20, () -> {
            act = "j3";
            //? if >=1.21 {
            Settings.SCENES.get(StationKind.JUKEBOX).set(false);
            //?} else if <1.21 {
            /*Settings.set(Settings.SCENES.get(StationKind.JUKEBOX), false);
            *///?}
            server(p -> {
                for (int i = 0; i < p.getInventory().items.size(); i++) {
                    if (p.getInventory().items.get(i).is(Items.MUSIC_DISC_CAT)) {
                        p.getInventory().setItem(8, p.getInventory().removeItemNoUpdate(i));
                        break;
                    }
                }
            });
        });
        step(10, () -> useAt(JUKEBOX, new Vec3(0.5, 1.0, 0.5), Direction.UP));
        step(20, () -> {
            check("jukebox, scene off: no scene", Scene.phase == Scene.Phase.NONE, "phase=" + Scene.phase);
            server(p -> check("jukebox, scene off: the record went in the vanilla way",
                    p.serverLevel().getBlockState(JUKEBOX).getValue(net.minecraft.world.level.block.JukeboxBlock.HAS_RECORD), ""));
        });
        step(10, () -> useAt(JUKEBOX, new Vec3(0.5, 1.0, 0.5), Direction.UP));
        step(20, () -> {
            server(p -> check("jukebox, scene off: the record pops out the vanilla way",
                    !p.serverLevel().getBlockState(JUKEBOX).getValue(net.minecraft.world.level.block.JukeboxBlock.HAS_RECORD), ""));
            //? if >=1.21 {
            Settings.SCENES.get(StationKind.JUKEBOX).set(true);
            //?} else if <1.21 {
            /*Settings.set(Settings.SCENES.get(StationKind.JUKEBOX), true);
            *///?}
            restoreLayout.run();
            //? if >=1.21 {
            Settings.DEVELOPER_MODE.set(developer);
            //?} else if <1.21 {
            /*Settings.set(Settings.DEVELOPER_MODE, developer);
            *///?}
        });
        quickAct();
    }

    // ---- the quick way back to the station just stepped away from ------------------------------------

    private static void quickAct() {
        if (EDITOR_BATCH) { step(5, () -> finish(Minecraft.getInstance())); return; }
        if (stopHere("q")) {
            return;
        }
        Vec3 atTable = new Vec3(0.5, -60, 3.36);
        step(40, () -> {
            act = "q";
            closeWindow();
            //? if >=1.21 {
            Settings.QUICK_WINDOW.set(12);
            //?} else if <1.21 {
            /*Settings.set(Settings.QUICK_WINDOW, 12);
            *///?}
            standLooking(atTable, Vec3.atCenterOf(TABLE));
        });
        step(10, () -> useAt(TABLE, new Vec3(0.5, 1.0, 0.8), Direction.UP));
        step(70, () -> check("quick return: the first time, the usual way in", Scene.phase == Scene.Phase.ACTIVE && !Scene.quick, "quick=" + Scene.quick));
        step(2, Scene::leave);
        // straight back: a quick way in (the usual one would still be leaning in by now)
        step(30, () -> {
            act = "q2";
            introShot = 0;
            useAt(TABLE, new Vec3(0.5, 1.0, 0.8), Direction.UP);
        });
        step(34, () -> {
            check("quick return: back at the same table, at work within a second and a bit", Scene.quick && Scene.phase == Scene.Phase.ACTIVE,
                    "quick=" + Scene.quick + " phase=" + Scene.phase);
            shot("q3_quick_back");
        });
        step(2, Scene::leave);
        // another station: the usual way
        step(30, () -> {
            act = "q4";
            introShot = 0;
            useAt(FURNACE, new Vec3(0.5, 0.6, 1.0), Direction.SOUTH);
        });
        step(20, () -> check("quick return: another station, the usual way in", !Scene.quick && Scene.phase == Scene.Phase.INTRO,
                "quick=" + Scene.quick + " phase=" + Scene.phase));
        step(80, Scene::leave);
        // after the window: the usual way again
        step(30, () -> {
            //? if >=1.21 {
            Settings.QUICK_WINDOW.set(5);
            //?} else if <1.21 {
            /*Settings.set(Settings.QUICK_WINDOW, 5);
            *///?}
            standLooking(atTable, Vec3.atCenterOf(TABLE));
        });
        step(10, () -> useAt(TABLE, new Vec3(0.5, 1.0, 0.8), Direction.UP));
        step(70, Scene::leave);
        step(130, () -> useAt(TABLE, new Vec3(0.5, 1.0, 0.8), Direction.UP));
        step(20, () -> check("quick return: past the window, the usual way in", !Scene.quick && Scene.phase == Scene.Phase.INTRO,
                "quick=" + Scene.quick + " phase=" + Scene.phase));
        step(70, Scene::leave);
        // turned off: always the usual way
        step(30, () -> {
            //? if neoforge {
            Settings.QUICK_WINDOW.set(12);
            Settings.QUICK_RETURN.set(false);
            //?} else if fabric {
            /*Settings.QUICK_WINDOW.set(12);
            Settings.QUICK_MODE.set(0);
            *///?} else if forge {
            /*Settings.set(Settings.QUICK_WINDOW, 12);
            Settings.set(Settings.QUICK_RETURN, false);
            *///?}
            useAt(TABLE, new Vec3(0.5, 1.0, 0.8), Direction.UP);
        });
        step(20, () -> check("quick return turned off: the usual way in", !Scene.quick && Scene.phase == Scene.Phase.INTRO,
                "quick=" + Scene.quick + " phase=" + Scene.phase));
        step(70, () -> {
            //? if neoforge {
            Settings.QUICK_RETURN.set(true);
            //?} else if fabric {
            /*Scene.leave();
            Settings.QUICK_MODE.set(2);
            Settings.APPROACH_SPEED.set(200);
        });
        step(35, () -> standLooking(atTable, Vec3.atCenterOf(TABLE)));
        step(10, () -> useAt(TABLE, new Vec3(0.5, 1.0, 0.8), Direction.UP));
        step(38, () -> check("always quick: first entry is short and pace is fixed",
                Scene.quick && Scene.phase == Scene.Phase.ACTIVE && Settings.approachSpeed() == 1.0,
                "quick=" + Scene.quick + " phase=" + Scene.phase));
        step(2, () -> Settings.ARM_STYLE.set(1));
        step(12, () -> shot("q5_natural_hands"));
        step(2, () -> Settings.ARM_STYLE.set(2));
        step(12, () -> shot("q6_floating_hands"));
        step(12, () -> {
            Settings.ARM_STYLE.set(0);
            Settings.QUICK_MODE.set(1);
            Settings.APPROACH_SPEED.set(100);
            *///?} else if forge {
            /*Settings.set(Settings.QUICK_RETURN, true);
            *///?}
            Scene.leave();
        });
        watchAct();
    }

    // ---- what others see: another player at work, watched -----------------------------------------------

    /** The stand-in for another player, made on this client only. */
    private static net.minecraft.client.player.RemotePlayer smith;
    private static final int SMITH_ID = 424242;

    /** The stand-in steps up to a station: standing (or crouching) at {@code feet}, facing {@code yaw}, at work there. */
    private static void smithAt(Vec3 feet, float yaw, StationKind kind, BlockPos pos, boolean crouch) {
        smith.moveTo(feet.x, feet.y, feet.z, yaw, 30f);
        smith.setYHeadRot(yaw);
        smith.yBodyRot = yaw;
        smith.yBodyRotO = yaw;
        smith.yHeadRotO = yaw;
        smith.setShiftKeyDown(crouch);
        smith.setPose(crouch ? net.minecraft.world.entity.Pose.CROUCHING : net.minecraft.world.entity.Pose.STANDING);
        ClientPayloads.AT_STATION.put(SMITH_ID, new ClientPayloads.AtStation(kind, pos));
    }

    /** The server's word to onlookers that the stand-in began a move at a station. */
    private static void smithMoves(BlockPos pos, StationKind kind, Direction side, List<ItemStack> items, ItemStack result, boolean bag) {
        ClientPayloads.menuDisplay(new dev.dyslexic.cinematic.net.MenuDisplayPayload(pos, kind, side.get2DDataValue(), items));
        ClientPayloads.menuFx(new dev.dyslexic.cinematic.net.MenuFxPayload(pos, kind, SMITH_ID, dev.dyslexic.cinematic.net.MenuFxPayload.Cue.MOVE,
                bag ? 1 : 0, items, result));
    }

    private static void smithShelf(dev.dyslexic.cinematic.net.MenuFxPayload.Cue cue, int slot, ItemStack book) {
        ClientPayloads.menuFx(new dev.dyslexic.cinematic.net.MenuFxPayload(SHELF, StationKind.CHISELED_BOOKSHELF, SMITH_ID, cue, slot, List.of(), book));
    }

    private static void watchAct() {
        if (stopHere("w")) {
            return;
        }
        step(40, () -> {
            act = "w";
            closeWindow();
            Minecraft mc = Minecraft.getInstance();
            smith = new net.minecraft.client.player.RemotePlayer(mc.level, new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes(new byte[]{7}), "Smith"));
            smith.setId(SMITH_ID);
            //? if >=1.21 {
            mc.level.addEntity(smith);
            //?} else if <1.21 {
            /*mc.level.addPlayer(SMITH_ID, smith);
            *///?}
            // the anvil faces east: the smith stands at its south side, the onlooker off to the east, seeing their right arm
            smithAt(new Vec3(ANVIL.getX() + 0.5, -60, ANVIL.getZ() + 0.5 + 0.86), 180f, StationKind.ANVIL, ANVIL, false);
            standLooking(new Vec3(ANVIL.getX() + 3.1, -60, ANVIL.getZ() + 1.7), new Vec3(ANVIL.getX() + 0.5, -59.0, ANVIL.getZ() + 1.0));
        });
        step(20, () -> shot("w0_smith"));
        step(2, () -> {
            //? if >=1.21 {
            var enchantments = Minecraft.getInstance().level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            //?}
            ItemStack forged = new ItemStack(Items.IRON_PICKAXE);
            //? if >=1.21 {
            forged.enchant(enchantments.getOrThrow(Enchantments.EFFICIENCY), 3);
            //?} else if <1.21 {
            /*forged.enchant(Enchantments.BLOCK_EFFICIENCY, 3);
            *///?}
            smithMoves(ANVIL, StationKind.ANVIL, Direction.SOUTH, List.of(new ItemStack(Items.IRON_PICKAXE),
                    //? if >=1.21 {
                    EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantments.getOrThrow(Enchantments.EFFICIENCY), 3))), forged, false);
                    //?} else if <1.21 {
                    /*EnchantedBookItem.createForEnchantment(new EnchantmentInstance(Enchantments.BLOCK_EFFICIENCY, 3))), forged, false);
                    *///?}
        });
        step(1, () -> check("watching: onlookers are told the smith began forging", ClientMenus.sinceMove(SMITH_ID) < 0.3
                && ClientMenus.BLOCKS.get(ANVIL) != null && ClientMenus.BLOCKS.get(ANVIL).moveResult.isEnchanted(), "since=" + ClientMenus.sinceMove(SMITH_ID)));
        moveShots("w1_anvil", 0.2, 0.45, 0.6, 0.66, 0.7, 0.8, 1.02, 1.3, 1.5, 1.75, 2.05);
        step(2, () -> check("watching: the hammer is put away after forging", PropLayer.hammer(smith) == 0, "shown=" + PropLayer.hammer(smith)));
        // the grindstone faces south: the smith stands south of it, the onlooker to the south-east
        step(20, () -> {
            smithAt(new Vec3(GRINDSTONE.getX() + 0.5, -60, GRINDSTONE.getZ() + 0.5 + 0.9), 180f, StationKind.GRINDSTONE, GRINDSTONE, false);
            standLooking(new Vec3(GRINDSTONE.getX() + 3.0, -60, GRINDSTONE.getZ() + 2.0), new Vec3(GRINDSTONE.getX() + 0.5, -59.2, GRINDSTONE.getZ() + 1.0));
        });
        step(16, () -> {
            //? if >=1.21 {
            var enchantments = Minecraft.getInstance().level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            //?}
            ItemStack sword = new ItemStack(Items.GOLDEN_SWORD);
            //? if >=1.21 {
            sword.enchant(enchantments.getOrThrow(Enchantments.SHARPNESS), 3);
            //?} else if <1.21 {
            /*sword.enchant(Enchantments.SHARPNESS, 3);
            *///?}
            smithMoves(GRINDSTONE, StationKind.GRINDSTONE, Direction.SOUTH, List.of(sword, ItemStack.EMPTY), new ItemStack(Items.GOLDEN_SWORD), false);
        });
        moveShots("w2_grind", 0.2, 0.45, 0.7, 1.0, 1.3, 1.55, 1.9, 2.2);
        // the stonecutter faces south too
        step(20, () -> {
            smithAt(new Vec3(STONECUTTER.getX() + 0.5, -60, STONECUTTER.getZ() + 0.5 + 0.95), 180f, StationKind.STONECUTTER, STONECUTTER, false);
            standLooking(new Vec3(STONECUTTER.getX() - 2.3, -60, STONECUTTER.getZ() + 2.1), new Vec3(STONECUTTER.getX() + 0.5, -59.3, STONECUTTER.getZ() + 1.0));
        });
        step(16, () -> smithMoves(STONECUTTER, StationKind.STONECUTTER, Direction.SOUTH, List.of(new ItemStack(Items.STONE, 24)),
                new ItemStack(Items.STONE_BRICKS), false));
        moveShots("w3_cut", 0.15, 0.3, 0.45, 0.6, 0.8, 1.0);
        // the chiseled bookshelf: a book taken, another read and put back
        step(20, () -> {
            smithAt(new Vec3(SHELF.getX() + 0.5, -60, SHELF.getZ() + 0.5 + 1.77), 180f, StationKind.CHISELED_BOOKSHELF, SHELF, true);
            standLooking(new Vec3(SHELF.getX() + 2.4, -60, SHELF.getZ() + 1.4), new Vec3(SHELF.getX() + 0.3, -59.4, SHELF.getZ() + 1.9));
        });
        step(16, () -> smithShelf(dev.dyslexic.cinematic.net.MenuFxPayload.Cue.PULL, 5, new ItemStack(Items.ENCHANTED_BOOK)));
        moveShots("w4_pull", 0.1, 0.3, 0.5, 0.75);
        step(30, () -> smithShelf(dev.dyslexic.cinematic.net.MenuFxPayload.Cue.READ, 1, new ItemStack(Items.WRITABLE_BOOK)));
        moveShots("w5_read", 0.25, 0.6, 0.9, 1.3, 2.0);
        step(2, () -> check("watching: the stand-in reads a book off the shelf", ShelfBooks.reading(SMITH_ID), ""));
        step(2, () -> smithShelf(dev.dyslexic.cinematic.net.MenuFxPayload.Cue.READ_END, 1, new ItemStack(Items.WRITABLE_BOOK)));
        moveShots("w6_back", 0.2, 0.5, 0.85, 1.2);
        step(10, () -> {
            check("watching: the book is back on the shelf", !ShelfBooks.reading(SMITH_ID), "");
            ClientPayloads.AT_STATION.remove(SMITH_ID);
            Minecraft.getInstance().level.removeEntity(SMITH_ID, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        });
        step(30, Showcase::thirdPersonShots);
    }

    private static void useAt(BlockPos station, Vec3 local, Direction face) {
        Minecraft mc = Minecraft.getInstance();
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(station).add(local), face, station, false));
    }

    /** Standing right at a table: {@code fast} — the lean is over by 0.9 s, otherwise it is still going on. */
    private static void paceCase(String name, Vec3 feet, BlockPos station, boolean fast) {
        step(2, () -> {
            act = name;
            standLooking(feet, Vec3.atCenterOf(station));
        });
        step(10, () -> useAt(station, new Vec3(0.5, 1.0, 0.7), Direction.UP));
        step(18, () -> {
            check(name + ": " + (fast ? "already working" : "still leaning in") + " after 0.9 s",
                    fast ? Scene.phase == Scene.Phase.ACTIVE : Scene.phase == Scene.Phase.INTRO, "phase=" + Scene.phase + " why=" + Scene.refused);
            shot(name);
        });
        step(30, () -> {
            if (Scene.phase == Scene.Phase.ACTIVE) {
                Scene.leave();
            }
        });
        step(36, () -> {});
    }

    /** Clicks the widget of the open screen whose text is {@code text} (a tab of the settings screen). */
    private static void clickWidget(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) {
            return;
        }
        for (net.minecraft.client.gui.components.events.GuiEventListener child : mc.screen.children()) {
            if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget && widget.getMessage().getString().equals(text)) {
                double x = widget.getX() + widget.getWidth() / 2.0, y = widget.getY() + widget.getHeight() / 2.0;
                mc.screen.mouseClicked(x, y, 0);
                mc.screen.mouseReleased(x, y, 0);
                return;
            }
        }
        REPORT.add("no widget \"" + text + "\" on " + mc.screen.getClass().getSimpleName());
    }

    /**
     * A height case with the height logic off: no window opens and a notice says why ({@code reason}: the end of its
     * translation key).
     */
    private static void noticeCase(int i, String name, BlockPos station, Vec3 feet, Vec3 local, Direction face, String reason) {
        Vec3 at = Vec3.atLowerCornerOf(heightSpot(i)).add(feet);
        step(2, () -> {
            act = name;
            closeWindow();
            standLooking(at, Vec3.atCenterOf(station));
        });
        step(18, () -> {
            Scene.refused = "";
            Minecraft mc = Minecraft.getInstance();
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(station).add(local), face, station, false));
        });
        step(12, () -> {
            Minecraft mc = Minecraft.getInstance();
            String want = net.minecraft.locale.Language.getInstance().getOrDefault("cinematic.notice." + reason).split("%s")[0];
            String got = ReachNotice.text();
            check(name + ": a notice instead of a window", mc.screen == null && !Scene.active() && got != null && got.contains(want),
                    "screen=" + (mc.screen == null ? "-" : mc.screen.getClass().getSimpleName()) + " notice=" + got + " why=" + Scene.refused);
            shot(name);
        });
    }

    // ---- how others see it --------------------------------------------------------------------------

    private static void thirdPersonShots() {
        //? if >=1.21 {
        step(2, () -> server(p -> p.teleportTo(p.serverLevel(), -0.4, -60, 5.2, 168f, 12f)));
        //?} else if <1.21 {
        /*step(2, () -> server(p -> {
            p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
            p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
            p.teleportTo(p.serverLevel(), -0.4, -60, 5.2, 168f, 12f);
        }));
        *///?}
        step(30, () -> shot("80_workshop"));
        poseShot("81_pose_furnace", StationKind.FURNACE, FURNACE);
        poseShot("82_pose_table", StationKind.TABLE, TABLE);
        poseShot("83_pose_enchanting", StationKind.ENCHANTING, ENCHANT);
        // mid-blow at the anvil when the shot is taken
        step(2, () -> ClientMenus.MOVE_BY_ENTITY.put(Minecraft.getInstance().player.getId(), Scene.clock() - 0.2));
        poseShot("84_pose_anvil", StationKind.ANVIL, ANVIL);
        poseShot("85_pose_grindstone", StationKind.GRINDSTONE, GRINDSTONE);
        poseShot("86_pose_stonecutter", StationKind.STONECUTTER, STONECUTTER);
        poseShot("87_pose_loom", StationKind.LOOM, LOOM);
        poseShot("88_pose_brewing", StationKind.BREWING, BREWING);
        poseShot("89_pose_campfire", StationKind.CAMPFIRE, CAMPFIRE);
        poseShot("8a_pose_lectern", StationKind.LECTERN, LECTERN);
        poseShot("8b_pose_beacon", StationKind.BEACON, BEACON);
        poseShot("8c_pose_shelf", StationKind.CHISELED_BOOKSHELF, SHELF);
        step(2, () -> {
            Minecraft mc = Minecraft.getInstance();
            ClientPayloads.AT_STATION.remove(mc.player.getId());
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        });
        step(4, () -> server(p -> p.serverLevel().destroyBlock(TABLE, true)));
        step(10, () -> server(p -> {
            List<ItemEntity> drops = p.serverLevel().getEntitiesOfClass(ItemEntity.class, new AABB(TABLE).inflate(2));
            StringBuilder s = new StringBuilder();
            for (ItemEntity drop : drops) {
                s.append(drop.getItem().getCount()).append('x').append(drop.getItem().getItem()).append(' ');
            }
            REPORT.add("table broken, drops: " + s + "| stored: " + (TableStore.get(p.serverLevel()).get(TABLE) != null));
        }));
        step(6, () -> report("the end"));
        step(6, () -> finish(Minecraft.getInstance()));
    }

    private static void poseShot(String name, StationKind kind, BlockPos pos) {
        step(2, () -> {
            Minecraft mc = Minecraft.getInstance();
            ClientPayloads.AT_STATION.put(mc.player.getId(), new ClientPayloads.AtStation(kind, pos));
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            mc.player.setYRot(0f);
            mc.player.setYHeadRot(0f);
            mc.player.setYBodyRot(0f);
            mc.player.setXRot(10f);
        });
        step(8, () -> shot(name));
    }

    // ---- simulated cursor -------------------------------------------------------------------------

    private static void step(int delay, Runnable action) {
        STEPS.add(new Step(delay, action));
    }

    private static void pointAt(Vec3 world) {
        double[] gui = Picking.toGui(world);
        if (gui != null) {
            pointGui(gui[0], gui[1]);
        }
    }

    private static void pointGui(double gx, double gy) {
        Minecraft mc = Minecraft.getInstance();
        double sx = mc.getWindow().getScreenWidth() / (double) mc.getWindow().getGuiScaledWidth();
        double sy = mc.getWindow().getScreenHeight() / (double) mc.getWindow().getGuiScaledHeight();
        mc.mouseHandler.xpos = gx * sx;
        mc.mouseHandler.ypos = gy * sy;
    }

    private static void pointAside() {
        Minecraft mc = Minecraft.getInstance();
        pointGui(mc.getWindow().getGuiScaledWidth() * 0.86, mc.getWindow().getGuiScaledHeight() * 0.62);
    }

    private static void hoverDrawer() {
        Minecraft mc = Minecraft.getInstance();
        pointGui(mc.getWindow().getGuiScaledWidth() * 0.5 + 20, 14);
    }

    private static void screenClick(int button) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            double[] at = Picking.mouseGui();
            mc.screen.mouseClicked(at[0], at[1], button);
        }
    }

    /** A click on an inventory-menu slot (hotbar 36..44, main 9..35). */
    private static void inventoryClick(int slot, int button) {
        Minecraft mc = Minecraft.getInstance();
        mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, slot, button, ClickType.PICKUP, mc.player);
    }

    /** A click on an inventory slot (0..35) through whatever menu is open. */
    private static void invClick(int inventoryIndex, int button) {
        LocalPlayer player = Minecraft.getInstance().player;
        List<Slot> slots = player.containerMenu.slots;
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).container == player.getInventory() && slots.get(i).getContainerSlot() == inventoryIndex) {
                inventoryClick(i, button);
                return;
            }
        }
        REPORT.add("no menu slot for inventory " + inventoryIndex);
    }

    /** Picks up the first stack of an item from the inventory, through whatever menu is open. */
    private static void invClickItem(net.minecraft.world.item.Item item) {
        LocalPlayer player = Minecraft.getInstance().player;
        for (int i = 0; i < 36; i++) {
            if (player.getInventory().getItem(i).is(item)) {
                invClick(i, 0);
                return;
            }
        }
        REPORT.add("no " + item + " in the inventory");
    }

    // ---- reporting ------------------------------------------------------------------------------

    private static void check(String name, boolean passed, String details) {
        String line = "check " + (passed ? "passed " : "FAILED ") + name + ": " + details;
        REPORT.add(line);
        Cinematic.LOGGER.info("[Showcase] {}", line);
    }

    private static String describe(ItemStack stack) {
        return stack.isEmpty() ? "-" : stack.getCount() + "x" + stack.getItem();
    }

    private static void report(String when) {
        Minecraft mc = Minecraft.getInstance();
        REPORT.add(when + " client carried=" + mc.player.containerMenu.getCarried() + " menu=" + mc.player.containerMenu.getClass().getSimpleName());
        server(p -> {
            StringBuilder s = new StringBuilder();
            TableStore.Table table = TableStore.get(p.serverLevel()).get(TABLE);
            if (table != null) {
                s.append("table: ");
                for (int i = 0; i < TableGrid.CELLS; i++) {
                    s.append(i).append('=').append(describe(table.items.get(i))).append(' ');
                }
            }
            for (BlockPos pos : new BlockPos[]{FURNACE, SMOKER, BLAST}) {
                if (p.serverLevel().getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity f) {
                    s.append("| ").append(p.serverLevel().getBlockState(pos).getBlock().getName().getString()).append(": ");
                    for (int slot = 0; slot < 3; slot++) {
                        s.append(describe(f.getItem(slot))).append(' ');
                    }
                    s.append(String.format("lit=%d cook=%d/%d ", f.litTime, f.cookingProgress, f.cookingTotalTime));
                }
            }
            StringBuilder inv = new StringBuilder();
            for (int i = 0; i < p.getInventory().items.size(); i++) {
                ItemStack stack = p.getInventory().items.get(i);
                if (!stack.isEmpty()) {
                    inv.append(i).append(':').append(describe(stack));
                    if (stack.isEnchanted()) {
                        //? if >=1.21 {
                        inv.append(stack.getEnchantments());
                        //?} else if <1.21 {
                        /*inv.append(stack.getEnchantmentTags());
                        *///?}
                    }
                    //? if >=1.21 {
                    if (stack.has(DataComponents.STORED_ENCHANTMENTS)) {
                        inv.append(stack.get(DataComponents.STORED_ENCHANTMENTS));
                    //?} else if <1.21 {
                    /*if (stack.is(Items.ENCHANTED_BOOK)) {
                        inv.append(net.minecraft.world.item.EnchantedBookItem.getEnchantments(stack));
                    *///?}
                    }
                    //? if >=1.21 {
                    if (stack.has(DataComponents.CUSTOM_NAME)) {
                    //?} else if <1.21 {
                    /*if (stack.hasCustomHoverName()) {
                    *///?}
                        inv.append('«').append(stack.getHoverName().getString()).append('»');
                    }
                    //? if >=1.21 {
                    if (stack.has(DataComponents.TRIM)) {
                        inv.append("[trim ").append(stack.get(DataComponents.TRIM).material().getRegisteredName()).append(']');
                    //?} else if <1.21 {
                    /*var trim = net.minecraft.world.item.armortrim.ArmorTrim.getTrim(p.serverLevel().registryAccess(), stack);
                    if (trim.isPresent()) {
                        inv.append("[trim ").append(trim.get().material().unwrapKey().map(k -> k.location().toString()).orElse("?")).append(']');
                    *///?}
                    }
                    if (stack.isDamaged()) {
                        inv.append("{damage ").append(stack.getDamageValue()).append('}');
                    }
                    inv.append(' ');
                }
            }
            if (p.containerMenu != p.inventoryMenu) {
                s.append("| menu slots: ");
                for (int slot = 0; slot < 4 && slot < p.containerMenu.slots.size(); slot++) {
                    s.append(slot).append('=').append(describe(p.containerMenu.getSlot(slot).getItem())).append(' ');
                }
            }
            s.append("| anvil=").append(p.serverLevel().getBlockState(ANVIL).getBlock().getName().getString()).append(' ');
            String line = when + " server: " + s + "| inventory: " + inv + "| carried=" + p.containerMenu.getCarried()
                    + " menu=" + p.containerMenu.getClass().getSimpleName()
                    + String.format(" xp=%d+%.2f", p.experienceLevel, p.experienceProgress)
                    + String.format(" pos=%.2f %.2f %.2f yaw %.1f pitch %.1f crouch=%s", p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot(),
                    p.isCrouching());
            Cinematic.LOGGER.info("[Showcase] {}", line);
            REPORT.add(line);
        });
    }

    private static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        String hover = Scene.station instanceof TableStation ts ? "cell=" + ts.hoverCell + " result=" + ts.hoverResult
                + String.format(" centreOpacity=%.3f", ts.itemOpacity(4))
                : Scene.station instanceof FurnaceStation fs ? "zone=" + fs.hoverZone
                : Scene.station instanceof EnchantStation es ? "target=" + es.target + " offer=" + es.hoverOffer
                : Scene.station instanceof AnvilStation as ? "target=" + as.target + " move=" + String.format("%.2f", as.moveTime())
                : Scene.station instanceof GrindstoneStation gs ? "target=" + gs.target + " move=" + String.format("%.2f", gs.moveTime())
                : Scene.station instanceof SmithingStation ss ? "target=" + ss.target + " move=" + String.format("%.2f", ss.moveTime())
                : Scene.station instanceof LoomStation ls ? "target=" + ls.target + " pattern=" + ls.hoverPattern + " move=" + String.format("%.2f", ls.moveTime())
                : Scene.station instanceof CartographyStation ms ? "target=" + ms.target + " move=" + String.format("%.2f", ms.moveTime())
                : Scene.station instanceof BrewingStation bs ? "target=" + bs.target + " slot=" + bs.hoverSlot
                //? if >=1.21 {
                : Scene.station instanceof CrafterStation rs ? "target=" + rs.target + " cell=" + rs.hoverCell
                //?}
                : Scene.station instanceof CampfireStation fs2 ? "target=" + fs2.target + " spot=" + fs2.hoverSpot
                : Scene.station instanceof ShelfStation es ? "target=" + es.target + " slot=" + es.hoverSlot + " move=" + es.moveName()
                : Scene.station instanceof StonecutterStation cs ? "target=" + cs.target + " shape=" + cs.hoverShape
                + " move=" + String.format("%.2f", cs.moveTime()) : "-";
        String line = String.format("%s fps=%d phase=%s t=%.2f %s", name, mc.getFps(), Scene.phase, Scene.time, hover);
        REPORT.add(line);
        Cinematic.LOGGER.info("[Showcase] {}", line);
        mc.getToasts().clear();
        FrameProbe.shotTaken();
        Screenshot.grab(mc.gameDirectory, "showcase_" + name + ".png", mc.getMainRenderTarget(), message -> {});
    }

    private static void finish(Minecraft mc) {
        done = true;
        for (String line : REPORT) {
            Cinematic.LOGGER.info("[Showcase] report {}", line);
        }
        mc.stop();
    }

    private static void server(Consumer<ServerPlayer> action) {
        Minecraft mc = Minecraft.getInstance();
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null || mc.player == null) {
            return;
        }
        UUID id = mc.player.getUUID();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) {
                action.accept(player);
            }
        });
    }

    private static void cmd(String command) {
        server(p -> p.server.getCommands().performPrefixedCommand(
                p.createCommandSourceStack().withSuppressedOutput().withPermission(4), command));
    }
}
