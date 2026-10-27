package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.Cinematic;
import dev.dyslexic.cinematic.station.StationKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.Items;
import java.util.List;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Manual opt-in integration check against a separate, unmodded Minecraft server. */
public final class ClientOnlySmoke {
    private ClientOnlySmoke() {}

    private static final boolean ENABLED = Boolean.getBoolean("cinematic.clientOnlySmoke");
    private static final BlockPos TABLE = new BlockPos(0, 64, 1);
    private static final BlockPos FURNACE = new BlockPos(1, 64, 1);
    private static final BlockPos ANVIL = new BlockPos(1, 64, 3);
    private static int stage;
    private static int ticks;
    private static Vec3 original;

    public static void onScreen(Screen screen) {
        if (!ENABLED || stage != 0 || !(screen instanceof TitleScreen)) return;
        stage = 1;
        //? if !forge {
        Settings.CLIENT_ONLY_MODE.set(true);
        //?} else if forge {
        /*Settings.set(Settings.CLIENT_ONLY_MODE, true);
        *///?}
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> ConnectScreen.startConnecting(screen, mc, ServerAddress.parseString("127.0.0.1:25586"),
                //? if >=1.21 {
                new ServerData("Client-only smoke", "127.0.0.1:25586", ServerData.Type.OTHER), false, null));
                //?} else if <1.21 {
                /*new ServerData("Client-only smoke", "127.0.0.1:25586", false), false));
                *///?}
    }

    public static void tick() {
        if (!ENABLED || stage == 0 || stage == 99 || stage == -1) return;
        Minecraft mc = Minecraft.getInstance();
        if (++ticks > 600) {
            StringBuilder grid = new StringBuilder();
            if (mc.player != null && mc.player.containerMenu instanceof CraftingMenu menu) {
                for (int slot = 0; slot <= 9; slot++) grid.append(' ').append(slot).append('=').append(menu.getSlot(slot).getItem());
                grid.append(" carried=").append(menu.getCarried());
            }
            fail("timed out at stage " + stage + grid);
            return;
        }
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (ClientOnly.serverHasMod()) {
            fail("the test server unexpectedly has Cinematic Interactions");
            return;
        }
        switch (stage) {
            case 1 -> {
                if (!StationKind.TABLE.matches(mc.level.getBlockState(TABLE)) || mc.player.distanceToSqr(TABLE.getCenter()) > 9
                        || Math.abs(mc.player.getX() - 0.9) > 0.05
                        || mc.player.getInventory().getItem(0).getItem() != Items.OAK_LOG) return;
                original = mc.player.position();
                var hit = new BlockHitResult(new Vec3(0.5, 65, 1.5), Direction.UP, TABLE, false);
                clickAsPlayer(mc, hit);
                if (mc.player.swinging) {
                    fail("vanilla hand swung before the client-only scene");
                    return;
                }
                Cinematic.LOGGER.info("[ClientOnlySmoke] used table at {} without an initial swing", original);
                stage = 2;
                ticks = 0;
            }
            case 2 -> {
                //? if <1.21 {
                /*// The first synthetic use can race the server's final spawn/chunk packets.
                // Repeat the normal use-key path once the connection has settled.
                if (ticks == 40 && mc.player.containerMenu == mc.player.inventoryMenu && !Scene.active()) {
                    clickAsPlayer(mc, new BlockHitResult(new Vec3(0.5, 65, 1.5), Direction.UP, TABLE, false));
                    Cinematic.LOGGER.info("[ClientOnlySmoke] retried table use after spawn settled");
                }
                *///?}
                if (ticks == 80) Cinematic.LOGGER.info("[ClientOnlySmoke] waiting: screen={}, menu={}, scene={}, refused={}",
                        mc.screen == null ? "none" : mc.screen.getClass().getSimpleName(),
                        mc.player.containerMenu.getClass().getSimpleName(), Scene.phase, Scene.refused);
                if (!(mc.player.containerMenu instanceof CraftingMenu) || !Scene.clientOnly()) return;
                if (Scene.phase != Scene.Phase.ACTIVE) return;
                if (mc.player.position().distanceToSqr(original) > 0.01) {
                    fail("the player moved during the scene");
                    return;
                }
                CraftingMenu menu = (CraftingMenu) mc.player.containerMenu;
                mc.gameMode.handleInventoryMouseClick(menu.containerId, 37, 0, ClickType.PICKUP, mc.player);
                TableStation table = Scene.station(TableStation.class);
                table.hoverCell = 4;
                table.click(1, false);
                mc.gameMode.handleInventoryMouseClick(menu.containerId, 37, 0, ClickType.PICKUP, mc.player);
                stage = 3;
                ticks = 0;
            }
            case 3 -> {
                if (!(mc.player.containerMenu instanceof CraftingMenu menu)) {
                    fail("the vanilla crafting menu closed before the recipe synced");
                    return;
                }
                if (menu.getSlot(0).getItem().getItem() != Items.OAK_PLANKS) return;
                Scene.station(TableStation.class).craft(false);
                stage = 4;
                ticks = 0;
            }
            case 4 -> {
                if (!(mc.player.containerMenu instanceof CraftingMenu menu)) {
                    fail("the vanilla crafting menu closed before taking the result");
                    return;
                }
                if (menu.getCarried().getItem() != Items.OAK_PLANKS) return;
                stage = 40;
                ticks = 0;
            }
            // A left-drag of the four planks over two cells: the vanilla server shares them two and two.
            case 40 -> {
                pointAt(mc, Scene.station(TableStation.class).cellPoint(0));
                stage = 41;
            }
            case 41 -> {
                TableStation table = Scene.station(TableStation.class);
                if (table.hoverCell != 0 || !(mc.screen instanceof SceneScreen)) return;
                double[] at = Picking.mouseGui();
                mc.screen.mouseClicked(at[0], at[1], 0);
                pointAt(mc, table.cellPoint(8));
                stage = 42;
            }
            case 42 -> {
                TableStation table = Scene.station(TableStation.class);
                double[] at = Picking.mouseGui();
                mc.screen.mouseDragged(at[0], at[1], 0, 1, 0);
                if (Integer.bitCount(table.spreadCells()) != 2) {
                    fail("the left-drag did not gather two cells: " + Integer.bitCount(table.spreadCells()));
                    return;
                }
                mc.screen.mouseReleased(at[0], at[1], 0);
                stage = 43;
                ticks = 0;
            }
            case 43 -> {
                if (!(mc.player.containerMenu instanceof CraftingMenu menu)) {
                    fail("the vanilla crafting menu closed during the left-drag");
                    return;
                }
                Direction side = Scene.station.sideDirection();
                int a = menuSlot(side, 0), b = menuSlot(side, 8);
                if (menu.getSlot(a).getItem().getCount() != 2 || menu.getSlot(b).getItem().getCount() != 2 || !menu.getCarried().isEmpty()) return;
                Cinematic.LOGGER.info("[ClientOnlySmoke] left-drag shared 4 planks as 2 + 2 through vanilla quick-craft clicks");
                mc.gameMode.handleInventoryMouseClick(menu.containerId, a, 0, ClickType.QUICK_MOVE, mc.player);
                mc.gameMode.handleInventoryMouseClick(menu.containerId, b, 0, ClickType.QUICK_MOVE, mc.player);
                stage = 44;
                ticks = 0;
            }
            // Shift + "Lay out" for sticks: the four planks are two whole sets, laid with vanilla clicks only.
            case 44 -> {
                if (!(mc.player.containerMenu instanceof CraftingMenu menu)) return;
                for (int slot = 1; slot <= 9; slot++) if (!menu.getSlot(slot).getItem().isEmpty()) return;
                TableStation table = Scene.station(TableStation.class);
                // the hands are still pressing the last craft together: laying out waits, as the button does
                if (table.busy()) return;
                var guide = table.guide;
                //? if <1.21 {
                /*// The normal UI indexes lazily when the recipe book opens; this smoke test selects directly.
                guide.tick();
                *///?}
                var stick = guide.entries.stream().filter(e -> e.holder.id().getPath().equals("stick")).findFirst();
                if (stick.isEmpty()) return;
                guide.select(stick.get());
                guide.tick();
                if (guide.maxLayout().sets() != 2) {
                    if (ticks > 40) fail("Shift + Lay out offered " + guide.maxLayout().sets() + " stick sets instead of 2");
                    return;
                }
                table.guide.layOut(true);
                if (!table.busy()) {
                    fail("Shift + Lay out did not start laying");
                    return;
                }
                stage = 45;
                ticks = 0;
            }
            case 45 -> {
                if (!(mc.player.containerMenu instanceof CraftingMenu menu)) return;
                if (!menu.getSlot(0).getItem().is(Items.STICK) || Scene.station.busy()) return;
                int planks = 0;
                for (int slot = 1; slot <= 9; slot++) planks += menu.getSlot(slot).getItem().is(Items.OAK_PLANKS) ? menu.getSlot(slot).getItem().getCount() : 0;
                if (planks != 4) return;
                Cinematic.LOGGER.info("[ClientOnlySmoke] Shift + Lay out put two stick sets down");
                Scene.station(TableStation.class).craft(true);
                stage = 46;
                ticks = 0;
            }
            case 46 -> {
                if (mc.player.getInventory().countItem(Items.STICK) != 8) return;
                Cinematic.LOGGER.info("[ClientOnlySmoke] crafting every laid set gave 8 sticks");
                if (dev.dyslexic.cinematic.client.guide.RecipeViewers.get() != null) {
                    // with JEI installed: its page opens over the scene and hands the work back, the vanilla menu kept
                    Scene.consult(() -> dev.dyslexic.cinematic.client.guide.RecipeViewers.get().showHowToGet(new net.minecraft.world.item.ItemStack(Items.STICK)));
                    stage = 47;
                } else {
                    stage = 49;
                }
                ticks = 0;
            }
            case 47 -> {
                if (mc.screen == null || !mc.screen.getClass().getName().startsWith("mezz.jei")) return;
                if (!Scene.clientOnly() || Scene.phase != Scene.Phase.ACTIVE || !(mc.player.containerMenu instanceof CraftingMenu)) {
                    fail("JEI's page over a client-only scene dropped the scene or the vanilla menu");
                    return;
                }
                mc.screen.onClose();
                stage = 48;
                ticks = 0;
            }
            case 48 -> {
                if (!(mc.screen instanceof SceneScreen)) return;
                if (!Scene.clientOnly() || Scene.phase != Scene.Phase.ACTIVE || !(mc.player.containerMenu instanceof CraftingMenu)) {
                    fail("back from JEI, the client-only scene or its vanilla menu was gone");
                    return;
                }
                Cinematic.LOGGER.info("[ClientOnlySmoke] JEI's page opened over the client-only scene and handed the work back");
                stage = 49;
                ticks = 0;
            }
            // Shift-click on the sticks in the drawer: onto the table with vanilla clicks only, carried by the hand
            case 49 -> {
                if (!(mc.screen instanceof SceneScreen screen) || Scene.station.busy()) return;
                int slot = inventorySlot(mc, Items.STICK);
                if (slot < 0) {
                    fail("no sticks to shift-click onto the table");
                    return;
                }
                screen.panelClick(slot < 9 ? 36 + slot : slot, 0, true);
                stage = 50;
                ticks = 0;
            }
            case 50 -> {
                if (!(mc.player.containerMenu instanceof CraftingMenu menu) || Scene.station.busy()) return;
                int onTable = 0;
                for (int i = 1; i <= 9; i++) onTable += menu.getSlot(i).getItem().is(Items.STICK) ? menu.getSlot(i).getItem().getCount() : 0;
                if (onTable != 8 || mc.player.getInventory().countItem(Items.STICK) != 0 || !menu.getCarried().isEmpty()) {
                    if (ticks > 60) fail("shift-click put " + onTable + " sticks on the table");
                    return;
                }
                Cinematic.LOGGER.info("[ClientOnlySmoke] shift-click laid all 8 sticks on one cell with vanilla clicks");
                Scene.leave();
                stage = 5;
                ticks = 0;
            }
            case 5 -> {
                if (Scene.active() || mc.player.containerMenu != mc.player.inventoryMenu) return;
                if (mc.player.position().distanceToSqr(original) > 0.01) {
                    fail("the player moved on exit");
                    return;
                }
                if (!StationKind.FURNACE.matches(mc.level.getBlockState(FURNACE))
                        || !StationKind.ANVIL.matches(mc.level.getBlockState(ANVIL))) return;
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(new Vec3(1.5, 64.5, 2.0), Direction.SOUTH, FURNACE, false));
                stage = 6;
                ticks = 0;
            }
            case 6 -> {
                if (!Scene.clientOnly() || Scene.phase != Scene.Phase.ACTIVE
                        || Scene.station.kind() != StationKind.FURNACE || !(mc.player.containerMenu instanceof AbstractFurnaceMenu)
                        || !(mc.screen instanceof SceneScreen screen)) return;
                // shift-clicks as vanilla routes them: raw iron in at the top, sticks into the fire, dirt nowhere
                for (var item : List.of(Items.RAW_IRON, Items.STICK, Items.DIRT)) {
                    int slot = inventorySlot(mc, item);
                    if (slot >= 0) screen.panelClick(slot < 9 ? 36 + slot : slot, 0, true);
                }
                dirtSlot = inventorySlot(mc, Items.DIRT);
                stage = 60;
                ticks = 0;
            }
            case 60 -> {
                if (!(mc.player.containerMenu instanceof AbstractFurnaceMenu menu)) return;
                boolean ore = menu.getSlot(0).getItem().is(Items.RAW_IRON), fuel = menu.getSlot(1).getItem().is(Items.STICK);
                boolean dirtStays = dirtSlot >= 0 && mc.player.getInventory().getItem(dirtSlot).is(Items.DIRT);
                if (!ore || !fuel || !dirtStays) {
                    if (ticks > 60) fail("furnace shift-clicks: ore=" + ore + " fuel=" + fuel + " dirt stays=" + dirtStays
                            + " furnace=" + menu.getSlot(0).getItem() + "/" + menu.getSlot(1).getItem() + "/" + menu.getSlot(2).getItem()
                            + " sticks in pack=" + mc.player.getInventory().countItem(Items.STICK));
                    return;
                }
                Cinematic.LOGGER.info("[ClientOnlySmoke] furnace shift-clicks: raw iron in at the top, sticks into the fire, dirt left where it was");
                Scene.leave();
                stage = 7;
                ticks = 0;
            }
            case 7 -> {
                if (Scene.active() || mc.player.containerMenu != mc.player.inventoryMenu) return;
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(new Vec3(1, 64.5, 3.5), Direction.WEST, ANVIL, false));
                stage = 8;
                ticks = 0;
            }
            case 8 -> {
                if (!Scene.clientOnly() || Scene.phase != Scene.Phase.ACTIVE
                        || Scene.station.kind() != StationKind.ANVIL || !(mc.player.containerMenu instanceof AnvilMenu)) return;
                Scene.leave();
                stage = 9;
                ticks = 0;
            }
            case 9 -> {
                if (Scene.active() || mc.player.containerMenu != mc.player.inventoryMenu) return;
                if (mc.player.position().distanceToSqr(original) > 0.01) {
                    fail("the player moved during a furnace or anvil scene");
                    return;
                }
                Cinematic.LOGGER.info("[ClientOnlySmoke] PASS: vanilla server, crafting result, left-drag sharing, Shift lay-out of every set, shift-click into table and furnace, furnace and anvil scenes, menu close, no player movement");
                stage = 99;
                mc.stop();
            }
            default -> {}
        }
    }

    /** Puts the cursor over a point of the world, as a player's mouse would. */
    private static void pointAt(Minecraft mc, Vec3 world) {
        double[] gui = Picking.toGui(world);
        if (gui == null) return;
        mc.mouseHandler.xpos = gui[0] * mc.getWindow().getScreenWidth() / mc.getWindow().getGuiScaledWidth();
        mc.mouseHandler.ypos = gui[1] * mc.getWindow().getScreenHeight() / mc.getWindow().getGuiScaledHeight();
    }

    /** The vanilla crafting menu slot (1..9) under a table cell seen from {@code side}. */
    private static int menuSlot(Direction side, int world) {
        for (int logical = 0; logical < 9; logical++)
            if (dev.dyslexic.cinematic.table.TableGrid.worldIndex(side, logical / 3, logical % 3) == world) return logical + 1;
        return -1;
    }

    /** Exercise Minecraft's actual use-key path, including its predicted hand animation. */
    private static void clickAsPlayer(Minecraft mc, BlockHitResult hit) {
        mc.hitResult = hit;
        //? if neoforge {
        try {
            var use = Minecraft.class.getDeclaredMethod("startUseItem");
            use.setAccessible(true);
            use.invoke(mc);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot simulate the use key", e);
        }
        //?} else if fabric {
        /*((dev.dyslexic.cinematic.mixin.MinecraftUseInvoker) mc).cinematic$startUseItem();
        *///?} else if forge {
        /*try {
            // by its SRG name: Forge maps it to the name of the running game (development or a real install)
            var use = net.minecraftforge.fml.util.ObfuscationReflectionHelper.findMethod(Minecraft.class, "m_91277_");
            use.invoke(mc);
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new IllegalStateException("Cannot simulate the use key", e);
        }
        *///?}
    }

    private static int dirtSlot = -1;

    private static int inventorySlot(Minecraft mc, net.minecraft.world.item.Item item) {
        for (int i = 0; i < 36; i++) if (mc.player.getInventory().getItem(i).is(item)) return i;
        return -1;
    }

    private static void fail(String reason) {
        Cinematic.LOGGER.error("[ClientOnlySmoke] FAIL: {}", reason);
        stage = -1;
        Minecraft.getInstance().stop();
    }
}
