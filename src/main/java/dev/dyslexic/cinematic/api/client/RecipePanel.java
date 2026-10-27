package dev.dyslexic.cinematic.api.client;

import dev.dyslexic.cinematic.client.Anim;
import dev.dyslexic.cinematic.client.Picking;
import dev.dyslexic.cinematic.client.Scene;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

/**
 * A small glass recipe reference on the left of a scene: a search box, a "craftable only" filter, pages of results and,
 * under the chosen one, an optional action button. What it lists comes from a {@link Source}; what choosing a recipe
 * does in the world (a diagram on the table, ...) is up to the station.
 *
 * <p>The station owns the panel: it returns it from {@code ClientStation.recipePanel()} (the scene screen then sends it
 * the mouse and keys), and calls {@link #tick()} from its tick and {@link #render} from its screen rendering.
 */
public final class RecipePanel {
    /** One recipe in the list. */
    public interface Entry {
        ItemStack result();

        /** Lower-case text the search box matches (the result's name, the recipe id, ...). */
        String search();

        /** Whether the player has everything for it right now; the others are dimmed. */
        boolean craftable();

        /** Lines added to the entry's tooltip under its availability. */
        default List<Component> details() {
            return List.of();
        }
    }

    /** The recipes the panel lists, and what choosing one does. */
    public interface Source {
        List<? extends Entry> entries();

        /** Changes whenever the entries or their availability change: the panel filters them again only then. */
        int revision();

        @Nullable
        Entry selected();

        /** Chooses an entry, or none with {@code null}. */
        void select(@Nullable Entry entry);

        /** Called every tick; {@code open} says whether the panel is unfolded. */
        default void tick(boolean open) {}

        /** The button under the chosen entry, or {@code null} for none. */
        @Nullable
        default Action action() {
            return null;
        }

        default Component title() {
            return text("title");
        }

        /** Drawn on the folded panel's tab. */
        default ItemStack icon() {
            return BOOK;
        }
    }

    /** A button under the chosen entry, like the crafting table's "Lay out". */
    public interface Action {
        /** Asked every frame, so it can say what pressing it would do right now. */
        Component label();

        /** Whether pressing it does anything right now; the button is dimmed otherwise. */
        boolean active();

        void onAction();

        default List<Component> tooltip() {
            return List.of();
        }
    }

    private static final int WIDTH = 128, HEIGHT = 226, CELL = 27, COLS = 4, ROWS = 4;
    /** The action button under the chosen recipe. */
    private static final int ACTION_Y = 205, ACTION_H = 15;
    private static final int GRID_X = 10, GRID_Y = 61, PAGE_SIZE = COLS * ROWS;
    private static final ItemStack BOOK = new ItemStack(Items.BOOK);
    private final Source source;
    private final EditBox search;
    private List<Entry> filtered = List.of();
    private boolean open, craftableOnly;
    private double shown;
    private int revision = -1, page;
    private float scale = 1;
    private int x, y, toggleY;

    public RecipePanel(Source source) {
        this.source = source;
        search = new EditBox(Minecraft.getInstance().font, 9, 29, WIDTH - 18, 12, text("search"));
        search.setMaxLength(96);
        search.setBordered(false);
        search.setTextColor(0xFFF2EAD8);
        search.setHint(text("search"));
        search.setResponder(value -> { page = 0; filter(); });
    }

    private static Component text(String key, Object... args) {
        return Component.translatable("cinematic.recipes." + key, args);
    }

    public Source source() {
        return source;
    }

    public void tick() {
        source.tick(open);
        if (revision != source.revision()) {
            revision = source.revision();
            filter();
        }
    }

    private void filter() {
        String query = search.getValue().strip().toLowerCase(Locale.ROOT);
        filtered = source.entries().stream().filter(e -> (!craftableOnly || e.craftable()) && e.search().contains(query))
                .map(e -> (Entry) e).toList();
        page = Mth.clamp(page, 0, pages() - 1);
    }

    private int pages() { return Math.max(1, (filtered.size() + PAGE_SIZE - 1) / PAGE_SIZE); }
    private double localX(double mx) { return (mx - x) / scale; }
    private double localY(double my) { return (my - y) / scale; }
    private boolean inPanel(double mx, double my) {
        return shown > 0.02 && localX(mx) >= 0 && localX(mx) < WIDTH && localY(my) >= 0 && localY(my) < HEIGHT;
    }
    private boolean inToggle(double mx, double my) {
        return !open && shown < 0.02 && mx >= 8 && mx < 8 + 32 * scale && my >= toggleY && my < toggleY + 28 * scale;
    }

    public boolean over(double mx, double my) { return inPanel(mx, my) || inToggle(mx, my); }

    public void fold() { open = false; search.setFocused(false); }

    /** Chooses an entry from elsewhere (the field guide, ...), as a click on it would. */
    public void select(@Nullable Entry entry) {
        source.select(entry);
    }

    private int entryAt(double mx, double my) {
        double lx = localX(mx) - GRID_X, ly = localY(my) - GRID_Y;
        if (lx < 0 || lx >= COLS * CELL || ly < 0 || ly >= ROWS * CELL) return -1;
        int index = page * PAGE_SIZE + (int) (ly / CELL) * COLS + (int) (lx / CELL);
        return index < filtered.size() ? index : -1;
    }

    public void render(GuiGraphics g, Font font, float alpha) {
        var window = Minecraft.getInstance().getWindow();
        int width = window.getGuiScaledWidth(), height = window.getGuiScaledHeight();
        // Keep the reference outside the expanded inventory and the station at narrow GUI sizes too.
        scale = Math.min(1f, Math.min(height / 300f, width / 530f));
        shown = Anim.approach(shown, open ? 1 : 0, 15, Scene.frameDt);
        x = (int) Math.round(8 - (1 - shown) * (WIDTH + 12) * scale);
        y = Math.round(height * 0.15f);
        toggleY = Math.round(height * 0.45f);
        double[] mouse = Picking.mouseGui();
        if (shown < 0.02) {
            g.pose().pushPose();
            g.pose().translate(8, toggleY, 0);
            g.pose().scale(scale, scale, 1);
            glass(g, 32, 28, alpha);
            if (inToggle(mouse[0], mouse[1])) g.fill(1, 1, 31, 27, color(alpha * 0.12f, 255, 226, 150));
            g.renderItem(source.icon(), 8, 6);
            g.pose().popPose();
            return;
        }
        float a = alpha * (float) shown;
        Entry selected = source.selected();
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        glass(g, WIDTH, HEIGHT, a);
        g.drawString(font, source.title(), 9, 9, color(a, 242, 234, 216), false);
        g.drawString(font, "−", WIDTH - 17, 8, color(a * 0.8f, 242, 234, 216), false);
        g.fill(8, 25, WIDTH - 8, 43, color(a * 0.08f, 255, 255, 255));
        search.setAlpha(a);
        search.render(g, (int) localX(mouse[0]), (int) localY(mouse[1]), 0);
        g.fill(9, 48, 18, 57, color(a * 0.15f, 255, 255, 255));
        if (craftableOnly) g.fill(11, 50, 16, 55, color(a * 0.9f, 255, 226, 150));
        g.drawString(font, text("craftable"), 23, 49, color(a * 0.8f, 242, 234, 216), false);
        int hover = entryAt(mouse[0], mouse[1]);
        for (int slot = 0; slot < PAGE_SIZE; slot++) {
            int index = page * PAGE_SIZE + slot;
            if (index >= filtered.size()) break;
            var entry = filtered.get(index);
            int sx = GRID_X + slot % COLS * CELL, sy = GRID_Y + slot / COLS * CELL;
            g.fill(sx, sy, sx + CELL - 2, sy + CELL - 2, color(a * (index == hover ? 0.19f : 0.06f), 255, 255, 255));
            if (entry == selected) rim(g, sx, sy, CELL - 2, color(a * 0.85f, 255, 226, 150));
            g.setColor(1, 1, 1, a * (entry.craftable() ? 1f : 0.6f));
            g.renderItem(entry.result(), sx + 4, sy + 3);
            g.renderItemDecorations(font, entry.result(), sx + 4, sy + 3);
            g.setColor(1, 1, 1, 1);
            if (entry.craftable()) g.fill(sx + 9, sy + CELL - 5, sx + 15, sy + CELL - 4, color(a * 0.8f, 143, 211, 176));
        }
        if (filtered.isEmpty()) {
            g.drawWordWrap(font, text("empty"), 15, GRID_Y + 35, WIDTH - 30, color(a * 0.7f, 210, 206, 198));
        }
        g.drawString(font, "‹", 12, 171, color(a * (page > 0 ? 0.85f : 0.25f), 242, 234, 216), false);
        String counter = (page + 1) + " / " + pages();
        g.drawString(font, counter, (WIDTH - font.width(counter)) / 2, 171, color(a * 0.65f, 242, 234, 216), false);
        g.drawString(font, "›", WIDTH - 18, 171, color(a * (page < pages() - 1 ? 0.85f : 0.25f), 242, 234, 216), false);
        g.fill(8, 185, WIDTH - 8, 186, color(a * 0.12f, 255, 255, 255));
        if (selected != null) {
            g.drawString(font, font.plainSubstrByWidth(selected.result().getHoverName().getString(), WIDTH - 34), 9, 190,
                    color(a * 0.85f, 242, 234, 216), false);
            g.drawString(font, "×", WIDTH - 17, 190, color(a * 0.85f, 242, 234, 216), false);
        } else {
            g.drawString(font, text("choose"), 9, 190, color(a * 0.55f, 242, 234, 216), false);
        }
        drawAction(g, font, a, mouse);
        g.pose().popPose();
    }

    @Nullable
    private Action action() {
        return source.selected() == null ? null : source.action();
    }

    private boolean inAction(double mx, double my) {
        double lx = localX(mx), ly = localY(my);
        return open && shown > 0.9 && action() != null && lx >= 8 && lx < WIDTH - 8 && ly >= ACTION_Y && ly < ACTION_Y + ACTION_H;
    }

    private void drawAction(GuiGraphics g, Font font, float a, double[] mouse) {
        Action action = action();
        if (action == null) {
            return;
        }
        boolean live = action.active();
        boolean hover = live && inAction(mouse[0], mouse[1]);
        g.fill(8, ACTION_Y, WIDTH - 8, ACTION_Y + ACTION_H, color(a * (hover ? 0.3f : live ? 0.16f : 0.06f), 255, 226, 150));
        g.fill(8, ACTION_Y + ACTION_H - 1, WIDTH - 8, ACTION_Y + ACTION_H, color(a * (live ? 0.6f : 0.15f), 255, 226, 150));
        Component label = action.label();
        int w = font.width(label);
        g.drawString(font, label, (WIDTH - w) / 2, ACTION_Y + 4, color(a * (live ? 0.95f : 0.45f), 242, 234, 216), false);
    }

    private static void glass(GuiGraphics g, int w, int h, float alpha) {
        g.fill(0, 0, w, h, color(alpha * 0.62f, 12, 12, 16));
        g.fill(0, h - 1, w, h, color(alpha * 0.22f, 255, 255, 255));
        g.fill(0, 0, 1, h - 1, color(alpha * 0.08f, 255, 255, 255));
        g.fill(w - 1, 0, w, h - 1, color(alpha * 0.08f, 255, 255, 255));
    }

    private static void rim(GuiGraphics g, int x, int y, int size, int color) {
        g.fill(x, y, x + size, y + 1, color);
        g.fill(x, y + size - 1, x + size, y + size, color);
        g.fill(x, y, x + 1, y + size, color);
        g.fill(x + size - 1, y, x + size, y + size, color);
    }

    private static int color(float alpha, int r, int g, int b) {
        return Mth.clamp(Math.round(alpha * 255), 0, 255) << 24 | r << 16 | g << 8 | b;
    }

    public boolean click(double mx, double my, int button) {
        if (inToggle(mx, my)) { open = true; return true; }
        if (!inPanel(mx, my)) { search.setFocused(false); return false; }
        if (button != 0 || !open) return true;
        double lx = localX(mx), ly = localY(my);
        search.setFocused(ly >= 25 && ly < 43 && lx >= 8 && lx < WIDTH - 8);
        if (search.isFocused()) { search.mouseClicked(lx, ly, button); return true; }
        if (inAction(mx, my)) {
            Action action = action();
            if (action.active()) action.onAction();
        } else if (ly < 23 && lx >= WIDTH - 25) open = false;
        else if (ly >= 46 && ly < 59) { craftableOnly = !craftableOnly; page = 0; filter(); }
        else if (ly >= 169 && ly < 184) {
            if (lx < 32) page = Math.max(0, page - 1);
            else if (lx > WIDTH - 32) page = Math.min(pages() - 1, page + 1);
        } else if (ly >= 186 && ly < ACTION_Y && lx >= WIDTH - 25) source.select(null);
        else {
            int index = entryAt(mx, my);
            if (index >= 0) source.select(filtered.get(index));
        }
        return true;
    }

    public boolean scroll(double mx, double my, double amount) {
        if (!over(mx, my)) return false;
        if (open) page = Mth.clamp(page - (int) Math.signum(amount), 0, pages() - 1);
        return true;
    }

    public boolean charTyped(char c, int modifiers) {
        return search.isFocused() && search.charTyped(c, modifiers);
    }

    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (!search.isFocused()) return false;
        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) search.setFocused(false);
        else search.keyPressed(key, scanCode, modifiers);
        return true;
    }

    /** The tooltip of what is under the mouse on the panel, or {@code null} off it. */
    @Nullable
    public List<Component> tooltip(double mx, double my) {
        if (inToggle(mx, my)) return List.of(source.title());
        if (!inPanel(mx, my)) return null;
        double lx = localX(mx), ly = localY(my);
        if (ly < 23 && lx >= WIDTH - 25) return List.of(text("hide"));
        if (inAction(mx, my)) return action().tooltip();
        Entry selected = source.selected();
        if (ly >= 186 && ly < ACTION_Y && selected != null) return List.of(selected.result().getHoverName(), text("clear"));
        int index = entryAt(mx, my);
        if (index < 0) return null;
        var entry = filtered.get(index);
        List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), entry.result()));
        lines.add(text(entry.craftable() ? "available" : "unavailable").copy()
                .withStyle(entry.craftable() ? ChatFormatting.GREEN : ChatFormatting.RED));
        lines.addAll(entry.details());
        return lines;
    }

    // Coordinates expose actual rendered controls to the scripted in-game walkthrough.
    public double[] togglePoint() { return new double[]{24 * scale + 8, toggleY + 14 * scale}; }
    public double[] actionPoint() { return controlPoint(WIDTH / 2, ACTION_Y + ACTION_H / 2); }
    public double[] controlPoint(int px, int py) { return new double[]{x + px * scale, y + py * scale}; }

    /** Where the first entry {@code which} accepts is drawn, turning to its page; {@code null} if none is listed. */
    @Nullable
    public double[] entryPoint(Predicate<? super Entry> which) {
        for (int i = 0; i < filtered.size(); i++) if (which.test(filtered.get(i))) {
            page = i / PAGE_SIZE;
            int slot = i % PAGE_SIZE;
            return controlPoint(GRID_X + slot % COLS * CELL + 12, GRID_Y + slot / COLS * CELL + 12);
        }
        return null;
    }
}
