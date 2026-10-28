package dev.dyslexic.cinematic.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * An invisible screen over the scene while the hands are at work. It frees the cursor and draws a glass drawer
 * with the inventory at the top of the frame: the hotbar always, the rest when the cursor goes up there.
 * Clicks elsewhere go to the station. A station may also ask for a line of text (the anvil's new name).
 * It is a container screen over the open menu, so the drawer gets vanilla's inventory shortcuts (double-click,
 * drag-to-spread, F, middle-click) and mods that work on container screens (Mouse Tweaks) find its slots.
 */
public final class SceneScreen extends AbstractContainerScreen<AbstractContainerMenu> {
    private static final int SLOT = 18;
    private static final int PAD = 5;
    private static final int ROW_GAP = 4;
    private static final int INNER = 9 * SLOT;

    private static final int FIELD_W = 170;

    private double expand;
    private float scale = 1f;
    private int panelX, panelY, panelW, panelH;
    private final InventoryEffects inventoryEffects;
    /** A click on a drawer slot went to vanilla's handling, which then needs its drag and release too. */
    private boolean drawerGesture;
    private int drawerButton;

    @Nullable
    private EditBox field;
    @Nullable
    private Consumer<String> commit;

    /** The field guide on the right, when a recipe viewer is installed. */
    final FieldGuide guide;

    public SceneScreen() {
        this(null);
    }

    /** Over the menu open now, carrying on from {@code before} when the menu changed under it. */
    private SceneScreen(@Nullable SceneScreen before) {
        super(Minecraft.getInstance().player.containerMenu, Minecraft.getInstance().player.getInventory(), Component.translatable("cinematic.scene"));
        inventoryEffects = before != null ? before.inventoryEffects : new InventoryEffects();
        guide = before != null ? before.guide : new FieldGuide();
        if (before != null) {
            expand = before.expand;
            field = before.field;
            commit = before.commit;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        // back from a recipe viewer's page: the scene decides whether the work goes on here
        Scene.consulted();
    }

    @Override
    protected void containerTick() {
        if (!Scene.active()) {
            // the scene ended while a recipe viewer's page covered it; the viewer handed back a screen with nothing under it
            Minecraft.getInstance().setScreen(null);
            return;
        }
        // the menu a container screen works on is fixed; a station that opens its menu later gets a screen over that one
        if (minecraft.player != null && minecraft.player.containerMenu != menu) {
            minecraft.setScreen(new SceneScreen(this));
            return;
        }
        inventoryEffects.tick();
        guide.tick();
    }

    /** The menu stays open under the scene: unlike a window, closing this screen never closes it. */
    @Override
    public void removed() {}

    /** Clicks off the drawer go to the station, never drop what is carried. */
    @Override
    protected boolean hasClickedOutside(double mx, double my, int left, int top, int button) {
        return false;
    }

    /** Vanilla finds a slot by its place in its own window; here the inventory's slots sit in the drawer, the station's nowhere. */
    @Override
    protected boolean isHovering(int x, int y, int w, int h, double mx, double my) {
        Slot slot = drawerSlotAt(mx, my);
        return slot != null && slot.x == x && slot.y == y;
    }

    @Nullable
    private Slot drawerSlotAt(double mx, double my) {
        LocalPlayer player = minecraft.player;
        if (player == null || !working() || FreeLook.looking() || !Scene.station.showsInventory() || !overPanel(mx, my)) {
            return null;
        }
        int drawer = slotAt(mx, my);
        int i = drawer < 0 ? -1 : menuSlot(player, drawer);
        return i >= 0 && i < menu.slots.size() ? menu.slots.get(i) : null;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {}

    @Override
    //? if >=1.21 {
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    //?} else if <1.21 {
    /*public void renderBackground(GuiGraphics graphics) {
    *///?}
        // the scene itself is the background
    }

    /** 0..1: sliding in after the hands settle, sliding away when stepping back. */
    private static float presence() {
        if (Scene.transition && Scene.phase == Scene.Phase.INTRO) {
            // moving over to a neighbouring station: the drawer stays where it is
            return 1;
        }
        if (Scene.phase == Scene.Phase.OUTRO) {
            return (float) (1 - Anim.inOutSine(Scene.time / Scene.UI_FADE_OUT));
        }
        return (float) Anim.outCubic(Scene.time / 0.45);
    }

    private static boolean working() {
        return Scene.phase == Scene.Phase.ACTIVE && Scene.station != null;
    }

    static LayoutEditor editor() {
        if (Scene.station instanceof TableStation table) return table.gridEditor;
        if (Scene.station instanceof FurnaceStation furnace) return furnace.gridEditor;
        if (Scene.station instanceof SurfaceStation<?> surface) return surface.gridEditor;
        if (Scene.station instanceof SpatialTarget spatial) return spatial.layoutEditor();
        return null;
    }

    // ---- a line of text -----------------------------------------------------------------------------

    /** Opens a line to type into over the scene; Enter (or a click elsewhere) keeps it, Esc drops it. */
    public static void edit(String initial, int maxLength, Consumer<String> onCommit) {
        if (Minecraft.getInstance().screen instanceof SceneScreen screen) {
            screen.openField(initial, maxLength, onCommit);
        }
    }

    /** The text being typed, or null when no line is open. */
    @Nullable
    public static String editing() {
        return Minecraft.getInstance().screen instanceof SceneScreen screen && screen.field != null ? screen.field.getValue() : null;
    }

    /** Keeps what was typed, as Enter would. */
    public static void commitEdit(String value) {
        if (Minecraft.getInstance().screen instanceof SceneScreen screen && screen.field != null) {
            screen.field.setValue(value);
            screen.closeField(true);
        }
    }

    private void openField(String initial, int maxLength, Consumer<String> onCommit) {
        EditBox box = new EditBox(font, 0, 0, FIELD_W, 12, Component.translatable("cinematic.scene.text"));
        box.setMaxLength(maxLength);
        box.setBordered(false);
        box.setTextColor(0xFFF2EAD8);
        box.setValue(initial);
        box.setFocused(true);
        //? if >=1.21 {
        box.moveCursorToEnd(false);
        //?} else if <1.21 {
        /*box.moveCursorToEnd();
        *///?}
        box.setHighlightPos(0);
        field = box;
        commit = onCommit;
    }

    private void closeField(boolean keep) {
        EditBox box = field;
        Consumer<String> then = commit;
        field = null;
        commit = null;
        if (keep && box != null && then != null) {
            then.accept(box.getValue());
        }
    }

    private void drawField(GuiGraphics g, float alpha) {
        if (field == null) {
            return;
        }
        int x = (width - FIELD_W) / 2;
        int y = height - 52;
        g.fill(x - 8, y - 15, x + FIELD_W + 8, y + 14, argb(0.62f * alpha, 12, 12, 16));
        g.fill(x - 8, y + 13, x + FIELD_W + 8, y + 14, argb(0.45f * alpha, 255, 224, 122));
        g.drawString(font, Component.translatable("cinematic.scene.text"), x, y - 11, argb(0.7f * alpha, 200, 192, 176), false);
        field.setX(x);
        field.setY(y + 1);
        field.render(g, 0, 0, 0);
    }

    // ---- layout -------------------------------------------------------------------------------------

    private void layout() {
        scale = Mth.clamp(height * 0.068f / SLOT, 0.6f, 1.25f);
        float appear = presence();
        panelW = Math.round((INNER + PAD * 2) * scale);
        int full = PAD * 2 + SLOT + (int) Math.round(expand * (ROW_GAP + 3 * SLOT));
        panelH = Math.round(full * scale);
        panelX = (width - panelW) / 2;
        panelY = Math.round(6 - (1 - appear) * (panelH + 10));
    }

    private boolean overPanel(double mx, double my) {
        int reach = Math.round((PAD * 2 + SLOT + (expand > 0.5 ? ROW_GAP + 3 * SLOT : 0)) * scale);
        return mx >= panelX - 4 && mx <= panelX + panelW + 4 && my >= 0 && my <= panelY + Math.max(panelH, reach) + 4;
    }

    /** Inventory-menu slot under the cursor (hotbar 36..44, main 9..35), or -1. */
    private int slotAt(double mx, double my) {
        double lx = (mx - panelX) / scale - PAD;
        double ly = (my - panelY) / scale - PAD;
        if (lx < 0 || lx >= INNER) {
            return -1;
        }
        int col = (int) (lx / SLOT);
        if (ly >= 0 && ly < SLOT) {
            return 36 + col;
        }
        double below = ly - SLOT - ROW_GAP;
        if (expand > 0.9 && below >= 0 && below < 3 * SLOT) {
            return 9 + (int) (below / SLOT) * 9 + col;
        }
        return -1;
    }

    private static int inventoryIndex(int menuSlot) {
        return menuSlot >= 36 ? menuSlot - 36 : menuSlot;
    }

    /**
     * The open menu's slot for a drawer slot: the drawer is laid out like the inventory menu (hotbar 36..44,
     * main 9..35), while the enchanting table's menu numbers the same inventory slots differently.
     */
    private static int menuSlot(LocalPlayer player, int drawerSlot) {
        int index = inventoryIndex(drawerSlot);
        var slots = player.containerMenu.slots;
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).container == player.getInventory() && slots.get(i).getContainerSlot() == index) {
                return i;
            }
        }
        return -1;
    }

    // ---- drawing ------------------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientStation station = Scene.station;
        if (player == null || station == null) {
            return;
        }
        boolean drawer = station.showsInventory();
        // looking round from behind, the panels give way to the view
        float look = (float) FreeLook.amount();
        boolean over = working() && drawer && !FreeLook.looking() && overPanel(mouseX, mouseY);
        Scene.overPanel = over || (station.recipePanel() != null && station.recipePanel().over(mouseX, mouseY))
                || (editor() != null && editor().over(mouseX, mouseY)) || guideShown() && guide.over(mouseX, mouseY)
                //? if !forge {
                || station.choices() != null && station.choices().over(mouseX, mouseY);
                //?} else if forge {
                /*|| (station.choices() != null && station.choices().over(mouseX, mouseY));
                *///?}
        expand = Anim.approach(expand, over ? 1 : 0, 13, Scene.frameDt);
        layout();
        float alpha = (working() ? (float) Anim.smoother(Scene.time / 0.4) : presence()) * (1 - look);
        int hovered = over ? slotAt(mouseX, mouseY) : -1;
        // read by vanilla's keys (1-9, F, Q, pick) and by mods working on container screens
        hoveredSlot = drawerSlotAt(mouseX, mouseY);
        inventoryEffects.beginFrame(working() && !FreeLook.looking() && field == null
                && !(editor() != null && editor().visible()));

        if (alpha > 0.01f) {
            station.renderScreen(graphics, font, alpha);
            //? if !forge {
            if (station.choices() != null) {
                station.choices().render(graphics, font, alpha);
            }
            //?} else if forge {
            /*if (station.choices() != null) station.choices().render(graphics, font, alpha);
            *///?}
            if (guideShown()) {
                guide.render(graphics, font, alpha);
            }
            if (drawer) {
                drawPanel(graphics, player, hovered, alpha);
            }
            drawHints(graphics, station.hint(), alpha);
        }
        if (look > 0.01f) {
            drawLookHint(graphics, look);
        }
        if (FreeLook.looking()) {
            return;
        }
        if (working() && Scene.neighbor != null) {
            drawMoveOver(graphics, mouseX, mouseY);
        }
        if (!working()) {
            closeField(false);
        }
        drawField(graphics, alpha);
        if (working()) {
            drawTooltip(graphics, player, station, hovered, mouseX, mouseY);
        }

        ItemStack carried = player.containerMenu.getCarried();
        inventoryEffects.endFrame(Scene.overPanel);
        // during a special move the station draws what is in hand itself
        if (working() && !carried.isEmpty() && !Hands.itemInHand() && station.handsOverride() == null
                && !(editor() != null && editor().visible())) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 300);
            inventoryEffects.carried(graphics, carried, mouseX, mouseY, (x, y) -> {
                graphics.renderItem(carried, x, y);
                graphics.renderItemDecorations(font, carried, x, y);
            });
            graphics.pose().popPose();
        }
    }

    private void drawPanel(GuiGraphics g, LocalPlayer player, int hovered, float alpha) {
        int bg = argb(0.46f * alpha, 12, 12, 16);
        int edge = argb(0.20f * alpha, 255, 255, 255);
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, bg);
        g.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, edge);
        g.fill(panelX, panelY, panelX + 1, panelY + panelH - 1, argb(0.08f * alpha, 255, 255, 255));
        g.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH - 1, argb(0.08f * alpha, 255, 255, 255));

        g.enableScissor(panelX, Math.max(0, panelY), panelX + panelW, panelY + panelH);
        g.pose().pushPose();
        g.pose().translate(panelX, panelY, 0);
        g.pose().scale(scale, scale, 1);
        int selected = player.getInventory().selected;
        for (int col = 0; col < 9; col++) {
            drawSlot(g, player, 36 + col, PAD + col * SLOT, PAD, hovered, col == selected, alpha);
        }
        if (expand > 0.02) {
            float rows = (float) Anim.smoother(expand);
            int dividerY = PAD + SLOT + ROW_GAP / 2;
            g.fill(PAD + 2, dividerY, PAD + INNER - 2, dividerY + 1, argb(0.12f * alpha * rows, 255, 255, 255));
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    drawSlot(g, player, 9 + row * 9 + col, PAD + col * SLOT, PAD + SLOT + ROW_GAP + row * SLOT, hovered, false, alpha * rows);
                }
            }
        } else {
            // a small grip under the hotbar hints that the drawer opens
            int gx = PAD + INNER / 2;
            g.fill(gx - 9, PAD + SLOT + 2, gx + 9, PAD + SLOT + 3, argb(0.35f * alpha, 255, 255, 255));
        }
        g.pose().popPose();
        g.disableScissor();
    }

    private void drawSlot(GuiGraphics g, LocalPlayer player, int menuSlot, int x, int y, int hovered, boolean selected, float alpha) {
        g.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, argb(0.13f * alpha, 255, 255, 255));
        if (selected) {
            int c = argb(0.75f * alpha, 255, 226, 150);
            g.fill(x, y, x + SLOT, y + 1, c);
            g.fill(x, y + SLOT - 1, x + SLOT, y + SLOT, c);
            g.fill(x, y, x + 1, y + SLOT, c);
            g.fill(x + SLOT - 1, y, x + SLOT, y + SLOT, c);
        }
        if (menuSlot == hovered) {
            g.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, argb(0.35f * alpha, 255, 255, 255));
        }
        if (alpha > 0.35f) {
            ItemStack stack = player.getInventory().getItem(inventoryIndex(menuSlot));
            double cx = panelX + (x + 9) * scale, cy = panelY + (y + 9) * scale;
            if (cy >= Math.max(0, panelY) && cy < panelY + panelH) {
                inventoryEffects.drawer(inventoryIndex(menuSlot), stack, cx, cy, menuSlot == hovered);
            }
            ItemStack shown = spreadPreview(player, menuSlot, stack);
            g.renderItem(shown, x + 1, y + 1);
            g.renderItemDecorations(font, shown, x + 1, y + 1);
        }
    }

    /** While a drag spreads the carried stack, a slot it passed over shows the share it will get, as vanilla's slots do. */
    private ItemStack spreadPreview(LocalPlayer player, int drawerSlot, ItemStack stack) {
        ItemStack carried = menu.getCarried();
        if (!isQuickCrafting || quickCraftSlots.size() < 2 || carried.isEmpty()) {
            return stack;
        }
        int i = menuSlot(player, drawerSlot);
        Slot slot = i >= 0 && i < menu.slots.size() ? menu.slots.get(i) : null;
        if (slot == null || !quickCraftSlots.contains(slot) || !AbstractContainerMenu.canItemQuickReplace(slot, carried, true) || !menu.canDragTo(slot)) {
            return stack;
        }
        int max = Math.min(carried.getMaxStackSize(), slot.getMaxStackSize(carried));
        // the button that began the drag is vanilla's spread kind: 0 evenly, 1 one each, 2 a full stack each (creative)
        int share = AbstractContainerMenu.getQuickCraftPlaceCount(quickCraftSlots, drawerButton, carried);
        return carried.copyWithCount(Math.min(max, stack.getCount() + share));
    }

    private void drawHints(GuiGraphics g, Component hint, float alpha) {
        if (!Settings.controlHints()) {
            return;
        }
        double fade = working() ? 1 - 0.55 * Anim.smoother((Scene.time - 7) / 2) : 0.45;
        int a = (int) (alpha * fade * 200);
        if (a < 8) {
            return;
        }
        int w = font.width(hint);
        float s = Math.min(1f, (width - 24f) / w);
        g.pose().pushPose();
        g.pose().translate(width / 2f, height - 6f - 11f * s, 0);
        g.pose().scale(s, s, 1);
        g.fill(-w / 2 - 6, -4, w / 2 + 6, 11, (int) (a * 0.45) << 24);
        g.drawString(font, hint, -w / 2, 0, (a << 24) | 0xF2EAD8, false);
        g.pose().popPose();
    }

    /** Looking round from behind: how to get back to work, small at the bottom. */
    private void drawLookHint(GuiGraphics g, float look) {
        Component hint = FreeLook.hint();
        int a = Math.round(look * 190);
        if (a < 8) {
            return;
        }
        int w = font.width(hint);
        int y = height - (int) Math.round(height * 0.085 * 0.55) - 14;
        g.fill(width / 2 - w / 2 - 6, y - 3, width / 2 + w / 2 + 6, y + 11, (int) (a * 0.45) << 24);
        g.drawString(font, hint, width / 2 - w / 2, y, (a << 24) | 0xF2EAD8, false);
    }

    /** Beside the cursor over a neighbouring station: its name and the click that moves over to it. */
    private void drawMoveOver(GuiGraphics g, int mx, int my) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || Scene.neighbor == null) {
            return;
        }
        Component name = mc.level.getBlockState(Scene.neighbor).getBlock().getName();
        Component how = Component.translatable("cinematic.move_over");
        int nameW = font.width(name), howW = font.width(how);
        int w = nameW + 9 + howW + 12, h = 13;
        int x = Math.min(mx + 10, width - w - 2), y = Math.min(my + 10, height - h - 2);
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(x, y, x + w, y + h, argb(0.62f, 12, 12, 16));
        g.fill(x, y + h - 1, x + w, y + h, argb(0.5f, 255, 226, 150));
        g.drawString(font, name, x + 6, y + 3, 0xFFFFE2A0, false);
        g.drawString(font, "·", x + 6 + nameW + 2, y + 3, 0xFF8A8478, false);
        g.drawString(font, how, x + 6 + nameW + 9, y + 3, 0xFFF2EAD8, false);
        g.pose().popPose();
    }

    private void drawTooltip(GuiGraphics g, LocalPlayer player, ClientStation station, int hovered, int mx, int my) {
        ItemStack carried = player.containerMenu.getCarried();
        List<Component> guideLines = guideShown() && carried.isEmpty() ? guide.tooltip(mx, my) : null;
        if (guideLines != null) {
            g.renderComponentTooltip(font, guideLines, mx, my);
            return;
        }
        if (hovered >= 0) {
            ItemStack stack = player.getInventory().getItem(inventoryIndex(hovered));
            if (carried.isEmpty() && !stack.isEmpty()) {
                g.renderTooltip(font, stack, mx, my);
            }
            return;
        }
        List<Component> lines = station.choices() != null && carried.isEmpty() ? station.choices().tooltip(mx, my) : null;
        if (lines == null) lines = station.tooltip(carried);
        if (lines == null || lines.isEmpty()) {
            return;
        }
        List<FormattedCharSequence> seq = new ArrayList<>(lines.size());
        for (Component line : lines) {
            if (editor() != null && editor().editing()) seq.addAll(font.split(line, Math.min(210, width / 2)));
            else seq.add(line.getVisualOrderText());
        }
        g.renderTooltip(font, seq, mx, my);
    }

    /** The guide stays out of the way while a layout editor is open. */
    private boolean guideShown() {
        return working() && !(editor() != null && editor().visible());
    }

    /** The inventory item under the cursor on the panel, for the recipe viewer's own keys. */
    @javax.annotation.Nullable
    private ItemStack hoveredPanelItem() {
        LocalPlayer player = Minecraft.getInstance().player;
        double[] mouse = Picking.mouseGui();
        if (player == null || Scene.station == null || !Scene.station.showsInventory() || !overPanel(mouse[0], mouse[1])) return null;
        int slot = slotAt(mouse[0], mouse[1]);
        return slot < 0 ? null : player.getInventory().getItem(inventoryIndex(slot));
    }

    private static int argb(float a, int r, int g, int b) {
        int alpha = Mth.clamp(Math.round(a * 255), 0, 255);
        return alpha << 24 | r << 16 | g << 8 | b;
    }

    // ---- input --------------------------------------------------------------------------------------

    /**
     * A click on a drawer slot (numbered as in the inventory menu: hotbar 36..44, the pack 9..35). A plain click picks up
     * and puts down through the open menu; with shift the stack goes into the station where it fits
     * ({@link ClientStation#quickMove}), never just shuffled between hotbar and pack.
     */
    void panelClick(int drawerSlot, int button, boolean shift) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (drawerSlot < 0 || player == null || mc.gameMode == null || Scene.station == null) {
            return;
        }
        int target = menuSlot(player, drawerSlot);
        if (shift) {
            Scene.station.quickMove(inventoryIndex(drawerSlot), target);
        } else if (target >= 0) {
            mc.gameMode.handleInventoryMouseClick(player.containerMenu.containerId, target, button, ClickType.PICKUP, player);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null || !working() || FreeLook.looking()) {
            return true;
        }
        if (button != 0 && button != 1) {
            // the middle button only means something on a drawer slot (pick a stack in creative)
            if (drawerSlotAt(mx, my) != null) {
                drawerGesture = true;
                drawerButton = 2;
                super.mouseClicked(mx, my, button);
            }
            return true;
        }
        if (editor() != null && editor().click(mx, my, button)) return true;
        if (field != null) {
            if (!field.mouseClicked(mx, my, button)) {
                closeField(true);
            }
            return true;
        }
        inventoryEffects.action(hasShiftDown() ? ClickType.QUICK_MOVE : ClickType.PICKUP);
        //? if <1.21 {
        /*RecipeChoiceStrip choices = Scene.station.choices();
        if (choices != null) {
            int picked = choices.click(mx, my);
            if (picked >= 0) Scene.station.chose(picked);
            if (picked != -2) return true;
        }
        *///?}
        if (guideShown() && guide.click(mx, my, button)) {
            return true;
        }
        if (Scene.station.recipePanel() != null && Scene.station.recipePanel().click(mx, my, button)) {
            Scene.station.release(button);
            return true;
        }
        if (Scene.station.showsInventory() && overPanel(mx, my)) {
            if (hasShiftDown()) {
                panelClick(slotAt(mx, my), button, true);
            } else if (drawerSlotAt(mx, my) != null) {
                // vanilla's own handling: double-click to gather, drag to spread
                drawerGesture = true;
                drawerButton = button;
                super.mouseClicked(mx, my, button);
            }
            return true;
        }
        //? if >=1.21 {
        RecipeChoiceStrip choices = Scene.station.choices();
        int picked = choices == null ? -2 : choices.click(mx, my);
        if (picked != -2) {
            if (picked >= 0) Scene.station.chose(picked);
            return true;
        }
        //?}
        if (button == 1 && Scene.neighbor != null) {
            Scene.moveOver();
            return true;
        }
        Scene.station.click(button, hasShiftDown());
        return true;
    }

    @Override
    //? if >=1.21 {
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
    //?} else if <1.21 {
    /*public boolean mouseScrolled(double mx, double my, double scrollY) {
    *///?}
        if (FreeLook.looking()) {
            FreeLook.scroll(scrollY);
            return true;
        }
        if (working() && editor() != null && editor().scroll(mx, my, scrollY)) return true;
        if (working() && guideShown() && guide.scroll(mx, my, scrollY)) return true;
        if (working() && Scene.station.recipePanel() != null && Scene.station.recipePanel().scroll(mx, my, scrollY)) return true;
        if (working() && !(Scene.station.showsInventory() && overPanel(mx, my))) {
            Scene.station.scroll(scrollY);
        }
        return true;
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (working() && editor() != null && editor().editing()) return true;
        if (working() && guideShown() && guide.charTyped(c, modifiers)) return true;
        if (working() && Scene.station.recipePanel() != null && Scene.station.recipePanel().charTyped(c, modifiers)) return true;
        return field != null ? field.charTyped(c, modifiers) : super.charTyped(c, modifiers);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (FreeLook.looking()) {
            return true;
        }
        if (drawerGesture) {
            return super.mouseDragged(mx, my, button, dx, dy);
        }
        if (working() && editor() != null && editor().drag(mx, my, button)) return true;
        if (working() && guideShown() && guide.over(mx, my)) {
            // over the guide: neither a drag of the station nor painting, and a left-drag still ends on release
            if (button == 1 && Scene.station instanceof TableStation table) table.release(button);
            return true;
        }
        if (working() && Scene.station.recipePanel() != null && Scene.station.recipePanel().over(mx, my)) {
            // A right-drag stops painting over the panel; a left-drag only stops gathering and still ends on release.
            if (button == 1) Scene.station.release(button);
            return true;
        }
        if (working()) {
            inventoryEffects.action(hasShiftDown() ? ClickType.QUICK_MOVE : ClickType.PICKUP);
            Scene.station.drag(button);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (drawerGesture) {
            drawerGesture = false;
            super.mouseReleased(mx, my, button);
        }
        if (editor() != null) editor().release();
        if (Scene.station != null) {
            Scene.station.release(button);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        var options = mc.options;
        if (!working()) {
            return true;
        }
        if (editor() != null && editor().editing()) {
            editor().keyPressed(key);
            return true;
        }
        if (field == null && FreeLook.keyPressed(key, scanCode)) {
            return true;
        }
        if (FreeLook.looking()) {
            // back to work first: the keys that step away do so only from the work itself
            if (key == GLFW.GLFW_KEY_ESCAPE || options.keyInventory.matches(key, scanCode) || options.keyUp.matches(key, scanCode)
                    || options.keyDown.matches(key, scanCode) || options.keyLeft.matches(key, scanCode)
                    || options.keyRight.matches(key, scanCode) || options.keyJump.matches(key, scanCode)) {
                FreeLook.stop();
            }
            return true;
        }
        if (field != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                closeField(false);
            } else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                closeField(true);
            } else {
                field.keyPressed(key, scanCode, modifiers);
            }
            return true;
        }
        inventoryEffects.action(hasShiftDown() ? ClickType.QUICK_MOVE : ClickType.PICKUP);
        if (guideShown() && guide.keyPressed(key, scanCode, modifiers)) return true;
        if (Scene.station.recipePanel() != null && Scene.station.recipePanel().keyPressed(key, scanCode, modifiers)) return true;
        if (guideShown() && guide.viewerKey(key, scanCode, hoveredPanelItem())) return true;
        if (editor() != null && editor().keyPressed(key)) return true;
        if (Scene.station.keyPressed(key)) {
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE || options.keyInventory.matches(key, scanCode) || options.keyUp.matches(key, scanCode)
                || options.keyDown.matches(key, scanCode) || options.keyLeft.matches(key, scanCode)
                || options.keyRight.matches(key, scanCode) || options.keyJump.matches(key, scanCode)) {
            Scene.leave();
            return true;
        }
        // over a drawer slot vanilla's keys work on it: 1-9 and F swap, Q drops, pick clones in creative
        if (hoveredSlot != null) {
            boolean swap = options.keySwapOffhand.matches(key, scanCode);
            for (int i = 0; i < 9; i++) swap |= options.keyHotbarSlots[i].matches(key, scanCode);
            if (swap) inventoryEffects.action(ClickType.SWAP);
            else if (options.keyDrop.matches(key, scanCode)) inventoryEffects.action(ClickType.THROW);
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int key, int scanCode, int modifiers) {
        if (FreeLook.keyReleased(key, scanCode)) {
            return true;
        }
        return super.keyReleased(key, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        Scene.leave();
    }
}
