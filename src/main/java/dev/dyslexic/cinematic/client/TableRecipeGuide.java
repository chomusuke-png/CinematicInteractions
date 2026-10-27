package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.api.client.RecipePanel;
import dev.dyslexic.cinematic.table.TableGrid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
//? if >=1.21 {
import net.minecraft.world.item.crafting.RecipeHolder;
//?} else if <1.21 {
/*import dev.dyslexic.cinematic.RecipeRef;
*///?}
import net.minecraft.world.item.crafting.RecipeType;
//? if <1.21 {
/*import net.minecraft.world.item.crafting.RecipeManager;
*///?}
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/** Local recipe reference: never places items or asks the server to unlock/craft a recipe. */
final class TableRecipeGuide implements RecipePanel.Source {
    static final class Entry implements RecipePanel.Entry {
        //? if >=1.21 {
        final RecipeHolder<CraftingRecipe> holder;
        //?} else if <1.21 {
        /*final RecipeRef<CraftingRecipe> holder;
        *///?}
        final ItemStack result;
        final List<Ingredient> ingredients;
        final String search;
        boolean craftable;

        //? if >=1.21 {
        Entry(RecipeHolder<CraftingRecipe> holder, ItemStack result) {
        //?} else if <1.21 {
        /*Entry(RecipeRef<CraftingRecipe> holder, ItemStack result) {
        *///?}
            this.holder = holder;
            this.result = result;
            ingredients = holder.value().getIngredients().stream().filter(i -> !i.isEmpty()).toList();
            search = (result.getHoverName().getString() + " " + holder.id()).toLowerCase(Locale.ROOT);
        }

        @Override public ItemStack result() { return result; }
        @Override public String search() { return search; }
        @Override public boolean craftable() { return craftable; }

        @Override
        public List<Component> details() {
            List<Component> lines = new ArrayList<>();
            lines.add(text("preview").copy().withStyle(ChatFormatting.GRAY));
            // Recipe ids distinguish alternate recipes for the same result in large modpacks.
            if (Screen.hasShiftDown()) lines.add(Component.literal(holder.id().toString()).withStyle(ChatFormatting.DARK_GRAY));
            return lines;
        }
    }

    final List<Entry> entries = new ArrayList<>();
    @Nullable Entry selected;
    final Ingredient[] grid = new Ingredient[9];
    final ItemStack[] display = new ItemStack[9];
    final boolean[] placed = new boolean[9];
    final boolean[] missing = new boolean[9];
    int missingCount;
    int revision;
    /** When the shown recipe was chosen: its diagram fades in on the tabletop from then. */
    double selectedAt;
    private final TableStation station;
    private List<ItemStack> snapshot = List.of();
    private boolean indexed;
    //? if <1.21 {
    /*private RecipeManager indexedManager;
    *///?}

    TableRecipeGuide(TableStation station) {
        this.station = station;
        Arrays.fill(grid, Ingredient.EMPTY);
        Arrays.fill(display, ItemStack.EMPTY);
    }

    void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        //? if >=1.21 {
        if (!indexed) {
            for (var holder : mc.level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
                var recipe = holder.value();
        //?} else if <1.21 {
        /*RecipeManager manager = mc.level.getRecipeManager();
        // On a remote server the scene can open before the recipe-sync packet arrives. Do not cache
        // that initial empty manager forever; checking again stops as soon as recipes are indexed.
        if (!indexed || indexedManager != manager || (entries.isEmpty() && !manager.getAllRecipesFor(RecipeType.CRAFTING).isEmpty())) {
            var selectedId = selected == null ? null : selected.holder.id();
            entries.clear();
            selected = null;
            for (CraftingRecipe recipe : manager.getAllRecipesFor(RecipeType.CRAFTING)) {
                var holder = RecipeRef.of(recipe);
        *///?}
                // Dynamic special recipes have no fixed ingredient diagram; their vanilla GUI remains available.
                if (recipe.isSpecial() || !(recipe instanceof ShapedRecipe || recipe instanceof ShapelessRecipe)
                        || !recipe.canCraftInDimensions(3, 3) || recipe.isIncomplete()) continue;
                ItemStack result = recipe.getResultItem(mc.level.registryAccess());
                if (!result.isEmpty()) entries.add(new Entry(holder, result.copy()));
            }
            entries.sort(Comparator.comparing((Entry e) -> e.result.getHoverName().getString())
                    .thenComparing(e -> e.holder.id().toString()));
            //? if <1.21 {
            /*if (selectedId != null) selected = entry(selectedId);
            snapshot = List.of();
            *///?}
            indexed = true;
            //? if <1.21 {
            /*indexedManager = manager;
            *///?}
        }
        List<ItemStack> next = new ArrayList<>();
        for (int i = 0; i < 36; i++) next.add(mc.player.getInventory().getItem(i).copy());
        next.add(mc.player.containerMenu.getCarried().copy());
        ClientTables.Entry table = ClientTables.get(station.pos);
        for (int i = 0; i < 9; i++) next.add(table == null ? ItemStack.EMPTY : table.items[i].copy());
        boolean changed = next.size() != snapshot.size();
        for (int i = 0; !changed && i < next.size(); i++) changed = !ItemStack.matches(next.get(i), snapshot.get(i));
        if (!changed) return;
        snapshot = next;
        List<ItemStack> pool = units(next);
        for (Entry entry : entries) {
            int[] assignment = match(entry.ingredients, pool);
            entry.craftable = Arrays.stream(assignment).allMatch(i -> i >= 0);
        }
        refresh();
        revision++;
    }

    /** The reference's entry for a recipe id, if it has a diagram the table can show (shaped or shapeless). */
    @Nullable
    Entry entry(net.minecraft.resources.ResourceLocation id) {
        for (Entry e : entries) if (e.holder.id().equals(id)) return e;
        return null;
    }

    @Override public List<Entry> entries() { return entries; }
    @Override public int revision() { return revision; }
    @Override @Nullable public Entry selected() { return selected; }

    @Override
    public void tick(boolean open) {
        //? if <1.21 {
        /*// On large packs the index and inventory matching are expensive; keep them idle while the book is closed.
        if (!open && selected == null) return;
        *///?}
        tick();
    }

    private static Component text(String key, Object... args) {
        return Component.translatable("cinematic.recipes." + key, args);
    }

    /** The "Lay out" button: puts the chosen recipe's items on the table from the inventory. */
    private final RecipePanel.Action layOutAction = new RecipePanel.Action() {
        @Override
        public Component label() {
            if (Screen.hasShiftDown() && !station.busy()) {
                MaxLayout max = maxLayoutNow();
                return max.moves().isEmpty() ? text("no_more_sets") : text("lay_out_all", max.sets());
            }
            int count = layable();
            return count > 0 ? text("lay_out", count) : text(station.busy() ? "laying" : "nothing_to_lay");
        }

        @Override
        public boolean active() {
            return Screen.hasShiftDown() && !station.busy() ? !maxLayoutNow().moves().isEmpty() : layable() > 0;
        }

        @Override
        public void onAction() {
            layOut(Screen.hasShiftDown());
        }

        @Override
        public List<Component> tooltip() {
            List<Component> lines = new ArrayList<>();
            lines.add(text(station.busy() ? "laying" : layable() > 0 ? "lay_out_tip" : "nothing_to_lay_tip"));
            if (missingCount > 0) lines.add(text("lay_missing", missingCount).copy().withStyle(ChatFormatting.RED));
            //? if !forge {
            if (!station.busy() && !takeBackMoves().isEmpty()) lines.add(text("lay_take_back", takeBackMoves().size()).copy().withStyle(ChatFormatting.GRAY));
            if (!station.busy() && stuck() > 0) lines.add(text("lay_no_room", stuck()).copy().withStyle(ChatFormatting.RED));
            //?}
            if (!station.busy()) {
                MaxLayout max = maxLayoutNow();
                // Shift only promises what the items at hand and the stack limits really allow.
                lines.add((max.moves().isEmpty()
                        ? text(max.current() > 0 ? "lay_all_full" : "lay_all_none")
                        : text("lay_all_tip", max.sets())).copy().withStyle(ChatFormatting.GRAY));
            }
            return lines;
        }
    };

    @Override
    public RecipePanel.Action action() {
        return layOutAction;
    }

    /** How many items "Lay out" would put down right now (0: nothing to do, or the hands are busy). */
    private int layable() {
        if (selected == null || station.busy()) return 0;
        // the last recipe's leftovers in the way are taken back first; they may also belong in the new one
        return takeBackMoves().isEmpty() ? layoutMoves().size() : layableAfterTakingBack();
    }

    /** Shift + "Lay out": the most whole sets the inventory allows (nothing while the hands are busy). */
    private MaxLayout maxLayoutNow() {
        return selected == null || station.busy() ? new MaxLayout(0, 0, List.of()) : maxLayout();
    }

    /** Lays out the chosen recipe: one set, or with {@code all} as many whole sets as possible. */
    void layOut(boolean all) {
        List<int[]> back = selected == null || station.busy() ? List.of() : takeBackMoves();
        if (!back.isEmpty()) {
            // take back what is in the way, then lay out from the inventory as it is after that
            if (all ? maxLayoutNow().moves().isEmpty() : layable() == 0) return;
            List<int[]> moves = new ArrayList<>(back);
            moves.add(new int[]{-1, TableStation.REPLAN, all ? 1 : 0});
            station.layOut(moves);
            return;
        }
        List<int[]> moves = all ? maxLayoutNow().moves() : layable() > 0 ? layoutMoves() : List.of();
        if (!moves.isEmpty()) station.layOut(moves);
    }

    @Override
    public void select(@Nullable RecipePanel.Entry entry) {
        selected = (Entry) entry;
        selectedAt = Scene.clock();
        Arrays.fill(grid, Ingredient.EMPTY);
        refresh();
        revision++;
    }

    private ItemStack actual(int logical) {
        ClientTables.Entry table = ClientTables.get(station.pos);
        return table == null ? ItemStack.EMPTY : table.items[TableGrid.worldIndex(station.side, logical / 3, logical % 3)];
    }

    private void refresh() {
        Arrays.fill(display, ItemStack.EMPTY);
        Arrays.fill(placed, false);
        Arrays.fill(missing, false);
        missingCount = 0;
        if (selected == null) return;
        layout();
        List<ItemStack> resources = new ArrayList<>();
        for (int i = 0; i < Math.min(37, snapshot.size()); i++) resources.add(snapshot.get(i).copy());
        List<Ingredient> needed = new ArrayList<>();
        List<Integer> cells = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = actual(i).copy();
            placed[i] = !grid[i].isEmpty() && grid[i].test(stack);
            if (placed[i]) stack.shrink(1);
            resources.add(stack);
            if (!grid[i].isEmpty() && !placed[i]) {
                needed.add(grid[i]);
                cells.add(i);
            }
        }
        List<ItemStack> pool = units(resources);
        int[] assignment = match(needed, pool);
        for (int n = 0; n < cells.size(); n++) {
            int cell = cells.get(n);
            missing[cell] = assignment[n] < 0;
            if (missing[cell]) missingCount++;
            // Available tag alternatives are shown literally; unavailable alternatives rotate in the renderer.
            if (!missing[cell]) display[cell] = pool.get(assignment[n]).copyWithCount(1);
        }
    }

    private void layout() {
        if (selected.holder.value() instanceof ShapedRecipe shaped) {
            int w = shaped.getWidth(), h = shaped.getHeight();
            Ingredient[] best = shapedLayout(shaped, w == 1 ? 1 : 0, h == 1 ? 1 : 0, false);
            int score = score(best);
            if (!gridEmpty() && score(grid) >= score) { best = grid.clone(); score = score(best); }
            for (int y = 0; y <= 3 - h; y++) for (int x = 0; x <= 3 - w; x++) for (int m = 0; m < 2; m++) {
                Ingredient[] candidate = shapedLayout(shaped, x, y, m == 1);
                int value = score(candidate);
                if (value > score) { best = candidate; score = value; }
            }
            System.arraycopy(best, 0, grid, 0, 9);
        } else {
            Arrays.fill(grid, Ingredient.EMPTY);
            List<ItemStack> table = new ArrayList<>();
            for (int i = 0; i < 9; i++) table.add(actual(i));
            int[] assignment = match(selected.ingredients, table);
            for (int i = 0; i < assignment.length; i++) if (assignment[i] >= 0) grid[assignment[i]] = selected.ingredients.get(i);
            for (int i = 0; i < assignment.length; i++) if (assignment[i] < 0) {
                int cell = -1;
                for (int j = 0; j < 9; j++) if (grid[j].isEmpty() && actual(j).isEmpty()) { cell = j; break; }
                if (cell < 0) for (int j = 0; j < 9; j++) if (grid[j].isEmpty()) { cell = j; break; }
                grid[cell] = selected.ingredients.get(i);
            }
        }
    }

    private boolean gridEmpty() {
        return Arrays.stream(grid).allMatch(Ingredient::isEmpty);
    }

    private Ingredient[] shapedLayout(ShapedRecipe recipe, int x, int y, boolean mirror) {
        Ingredient[] result = new Ingredient[9];
        Arrays.fill(result, Ingredient.EMPTY);
        for (int row = 0; row < recipe.getHeight(); row++) for (int col = 0; col < recipe.getWidth(); col++) {
            result[(row + y) * 3 + col + x] = recipe.getIngredients().get(row * recipe.getWidth()
                    + (mirror ? recipe.getWidth() - col - 1 : col));
        }
        return result;
    }

    private int score(Ingredient[] candidate) {
        int score = 0;
        for (int i = 0; i < 9; i++) if (!actual(i).isEmpty()) score += candidate[i].test(actual(i)) ? 4 : -1;
        return score;
    }

    /**
     * The moves that lay out the selected recipe: for each empty cell of the diagram that the inventory (or the item in
     * hand) can fill, {world cell, inventory slot or -1}. Cells nothing can fill are left out (they stay see-through).
     */
    List<int[]> layoutMoves() {
        // worked out again only when the inventory, the table or the choice changes ({@link #revision}), not every frame
        if (movesRevision == revision && movesCache != null) {
            return movesCache;
        }
        movesRevision = revision;
        movesCache = workOutMoves();
        return movesCache;
    }

    @Nullable
    private List<int[]> movesCache;
    private int movesRevision = -1;

    private List<int[]> workOutMoves() {
        Minecraft mc = Minecraft.getInstance();
        List<int[]> moves = new ArrayList<>();
        if (selected == null || mc.player == null) {
            return moves;
        }
        List<ItemStack> units = new ArrayList<>();
        List<Integer> slots = new ArrayList<>();
        for (int slot = -1; slot < 36; slot++) {
            ItemStack stack = slot < 0 ? mc.player.containerMenu.getCarried() : mc.player.getInventory().getItem(slot);
            for (int i = 0; i < Math.min(9, stack.getCount()); i++) {
                units.add(stack);
                slots.add(slot);
            }
        }
        List<Ingredient> needed = new ArrayList<>();
        List<Integer> cells = new ArrayList<>();
        for (int logical = 0; logical < 9; logical++) {
            if (!grid[logical].isEmpty() && !placed[logical] && actual(logical).isEmpty()) {
                needed.add(grid[logical]);
                cells.add(logical);
            }
        }
        int[] assignment = match(needed, units);
        for (int n = 0; n < cells.size(); n++) {
            if (assignment[n] >= 0) {
                int logical = cells.get(n);
                moves.add(new int[]{TableGrid.worldIndex(station.side, logical / 3, logical % 3), slots.get(assignment[n]), 1});
            }
        }
        return moves;
    }

    /**
     * What lies where the chosen recipe has no place for it (a cell outside the diagram, or the wrong item in one), and
     * fits back into the inventory: {world cell, {@link TableStation#TAKE}, count}. Laying out a recipe takes these back
     * first, so the last recipe's leftovers never block the new one; what does not fit stays ({@link #stuck}).
     */
    List<int[]> takeBackMoves() {
        //? if >=1.21 {
        if (takeRevision == revision && takeCache != null) {
            return takeCache;
        }
        //?} else if <1.21 {
        /*if (takeRevision == revision && takeCache != null) return takeCache;
        *///?}
        takeRevision = revision;
        takeCache = new ArrayList<>();
        stuckCount = 0;
        Minecraft mc = Minecraft.getInstance();
        //? if >=1.21 {
        if (selected == null || mc.player == null) {
            return takeCache;
        }
        // the inventory as Inventory.add fills it: stacks of the same item first, then empty slots
        //?} else if <1.21 {
        /*if (selected == null || mc.player == null) return takeCache;
        *///?}
        ItemStack[] room = new ItemStack[36];
        for (int slot = 0; slot < 36; slot++) room[slot] = mc.player.getInventory().getItem(slot).copy();
        for (int logical = 0; logical < 9; logical++) {
            if (!wrong(logical)) continue;
            ItemStack lying = actual(logical);
            if (fits(room, lying)) {
                //? if >=1.21 {
                takeCache.add(new int[]{TableGrid.worldIndex(station.side, logical / 3, logical % 3), TableStation.TAKE, lying.getCount()});
                //?} else if <1.21 {
                /*takeCache.add(new int[] {TableGrid.worldIndex(station.side, logical / 3, logical % 3), TableStation.TAKE, lying.getCount()});
                *///?}
            } else {
                stuckCount++;
            }
        }
        return takeCache;
    }

    /** Cells holding the wrong item that the inventory has no room to take back (worked out by {@link #takeBackMoves}). */
    int stuck() {
        takeBackMoves();
        return stuckCount;
    }

    //? if >=1.21 {
    @Nullable
    private List<int[]> takeCache;
    //?} else if <1.21 {
    /*@Nullable private List<int[]> takeCache;
    *///?}
    private int takeRevision = -1;
    private int stuckCount;

    /** Whether all of {@code stack} goes into {@code room}; if it does, it is put there. */
    private static boolean fits(ItemStack[] room, ItemStack stack) {
        int left = stack.getCount();
        for (ItemStack slot : room) {
            //? if >=1.21 {
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, stack)) left -= Math.max(0, slot.getMaxStackSize() - slot.getCount());
            //?} else if <1.21 {
            /*if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, stack))
                left -= Math.max(0, slot.getMaxStackSize() - slot.getCount());
            *///?}
        }
        //? if >=1.21 {
        for (ItemStack slot : room) {
            if (left > 0 && slot.isEmpty()) left -= stack.getMaxStackSize();
        }
        //?} else if <1.21 {
        /*for (ItemStack slot : room) if (left > 0 && slot.isEmpty()) left -= stack.getMaxStackSize();
        *///?}
        if (left > 0) return false;
        int put = stack.getCount();
        //? if >=1.21 {
        for (int i = 0; i < room.length && put > 0; i++) {
            if (!room[i].isEmpty() && ItemStack.isSameItemSameComponents(room[i], stack)) {
                int move = Math.min(put, room[i].getMaxStackSize() - room[i].getCount());
                if (move > 0) { room[i].grow(move); put -= move; }
        //?} else if <1.21 {
        /*for (ItemStack slot : room) {
            if (put > 0 && !slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, stack)) {
                int move = Math.min(put, slot.getMaxStackSize() - slot.getCount());
                if (move > 0) { slot.grow(move); put -= move; }
        *///?}
            }
        }
        for (int i = 0; i < room.length && put > 0; i++) {
            if (room[i].isEmpty()) {
                int move = Math.min(put, stack.getMaxStackSize());
                room[i] = stack.copyWithCount(move);
                put -= move;
            }
        }
        return true;
    }

    /**
     * How many cells of the diagram one "Lay out" fills once what lies in the way has been taken back: the items taken
     * back count as being at hand, since they may belong in another cell of the new recipe.
     */
    int layableAfterTakingBack() {
        Minecraft mc = Minecraft.getInstance();
        if (selected == null || mc.player == null) return 0;
        //? if >=1.21 {
        List<int[]> back = takeBackMoves();
        //?}
        List<ItemStack> stacks = new ArrayList<>();
        //? if >=1.21 {
        for (int slot = -1; slot < 36; slot++) stacks.add(slot < 0 ? mc.player.containerMenu.getCarried() : mc.player.getInventory().getItem(slot));
        //?} else if <1.21 {
        /*for (int slot = -1; slot < 36; slot++)
            stacks.add(slot < 0 ? mc.player.containerMenu.getCarried() : mc.player.getInventory().getItem(slot));
        *///?}
        List<Integer> cleared = new ArrayList<>();
        //? if >=1.21 {
        for (int[] move : back) {
        //?} else if <1.21 {
        /*for (int[] move : takeBackMoves()) {
        *///?}
            int logical = logicalOf(move[0]);
            cleared.add(logical);
            stacks.add(actual(logical));
        }
        List<Ingredient> needed = new ArrayList<>();
        for (int logical = 0; logical < 9; logical++) {
            //? if >=1.21 {
            if (!grid[logical].isEmpty() && !placed[logical] && (actual(logical).isEmpty() || cleared.contains(logical))) {
            //?} else if <1.21 {
            /*if (!grid[logical].isEmpty() && !placed[logical] && (actual(logical).isEmpty() || cleared.contains(logical)))
            *///?}
                needed.add(grid[logical]);
            //? if >=1.21 {
            }
            //?}
        }
        return (int) Arrays.stream(match(needed, units(stacks))).filter(i -> i >= 0).count();
    }

    private int logicalOf(int world) {
        //? if >=1.21 {
        for (int logical = 0; logical < 9; logical++) {
        //?} else if <1.21 {
        /*for (int logical = 0; logical < 9; logical++)
        *///?}
            if (TableGrid.worldIndex(station.side, logical / 3, logical % 3) == world) return logical;
        //? if >=1.21 {
        }
        //?}
        return -1;
    }

    /**
     * Shift + "Lay out": every cell of the diagram brought up to {@code sets} items, the most whole sets the items at
     * hand allow. Each cell holds a single kind of item, so a tag ingredient picks the kinds that go furthest, and
     * no cell passes its kind's stack limit. {@code current} is how many whole sets already lie on the table; when
     * {@code sets} is not more than that, there is nothing to lay. Cells holding a wrong item are left alone.
     */
    record MaxLayout(int sets, int current, List<int[]> moves) {}

    MaxLayout maxLayout() {
        if (maxRevision == revision && maxCache != null) {
            return maxCache;
        }
        maxRevision = revision;
        maxCache = workOutMax();
        return maxCache;
    }

    @Nullable
    private MaxLayout maxCache;
    private int maxRevision = -1;
    /** Gives up on a pathological assignment search instead of stalling a frame; real recipes take a few dozen steps. */
    private static final int SEARCH_STEPS = 20_000;

    private MaxLayout workOutMax() {
        Minecraft mc = Minecraft.getInstance();
        if (selected == null || mc.player == null) {
            return new MaxLayout(0, 0, List.of());
        }
        // The kinds of item at hand (same item and components), how many of each, and which slots hold them.
        List<ItemStack> kinds = new ArrayList<>();
        List<Integer> stock = new ArrayList<>();
        List<List<Integer>> kindSlots = new ArrayList<>();
        for (int slot = -1; slot < 36; slot++) {
            ItemStack stack = slot < 0 ? mc.player.containerMenu.getCarried() : mc.player.getInventory().getItem(slot);
            if (stack.isEmpty()) continue;
            int kind = kindOf(kinds, stack);
            if (kind < 0) {
                kinds.add(stack);
                stock.add(0);
                kindSlots.add(new ArrayList<>());
                kind = kinds.size() - 1;
            }
            stock.set(kind, stock.get(kind) + stack.getCount());
            kindSlots.get(kind).add(slot);
        }
        // what will be taken back first is at hand too (where it lands is known only after: the moves re-plan then)
        List<Integer> takenBack = new ArrayList<>();
        for (int[] move : takeBackMoves()) {
            int logical = logicalOf(move[0]);
            takenBack.add(logical);
            ItemStack stack = actual(logical);
            int kind = kindOf(kinds, stack);
            if (kind < 0) {
                kinds.add(stack);
                stock.add(0);
                kindSlots.add(new ArrayList<>());
                kind = kinds.size() - 1;
            }
            stock.set(kind, stock.get(kind) + stack.getCount());
            if (!kindSlots.get(kind).contains(TableStation.REPLAN)) kindSlots.get(kind).add(TableStation.REPLAN);
        }
        // The diagram's cells: those already holding the right item keep that kind; empty ones choose a kind.
        List<Integer> cells = new ArrayList<>();
        List<Integer> fixedKind = new ArrayList<>();
        List<Integer> have = new ArrayList<>();
        List<Integer> limit = new ArrayList<>();
        List<int[]> choices = new ArrayList<>();
        int current = Integer.MAX_VALUE;
        int upper = Integer.MAX_VALUE;
        for (int logical = 0; logical < 9; logical++) {
            if (grid[logical].isEmpty()) continue;
            ItemStack lying = takenBack.contains(logical) ? ItemStack.EMPTY : actual(logical);
            if (!lying.isEmpty() && !grid[logical].test(lying)) continue;
            cells.add(logical);
            if (!lying.isEmpty()) {
                fixedKind.add(kindOf(kinds, lying));
                have.add(lying.getCount());
                limit.add(lying.getMaxStackSize());
                choices.add(new int[0]);
                current = Math.min(current, lying.getCount());
                upper = Math.min(upper, lying.getMaxStackSize());
            } else {
                List<Integer> accepted = new ArrayList<>();
                int best = 0;
                for (int k = 0; k < kinds.size(); k++) if (grid[logical].test(kinds.get(k))) {
                    accepted.add(k);
                    best = Math.max(best, kinds.get(k).getMaxStackSize());
                }
                fixedKind.add(-1);
                have.add(0);
                limit.add(best);
                choices.add(accepted.stream().mapToInt(Integer::intValue).toArray());
                current = 0;
                upper = Math.min(upper, best);
            }
        }
        if (cells.isEmpty()) {
            return new MaxLayout(0, 0, List.of());
        }
        int[] chosen = new int[cells.size()];
        for (int sets = upper; sets > current; sets--) {
            if (!assign(sets, cells.size(), fixedKind, have, limit, choices, kinds, stock, chosen)) continue;
            int[] left = new int[36 + 1];
            for (int slot = -1; slot < 36; slot++) {
                ItemStack stack = slot < 0 ? mc.player.containerMenu.getCarried() : mc.player.getInventory().getItem(slot);
                left[slot + 1] = stack.getCount();
            }
            List<int[]> moves = new ArrayList<>();
            for (int i = 0; i < cells.size(); i++) {
                int need = sets - have.get(i);
                if (need <= 0) continue;
                int logical = cells.get(i);
                int world = TableGrid.worldIndex(station.side, logical / 3, logical % 3);
                for (int slot : kindSlots.get(chosen[i])) {
                    if (need <= 0) break;
                    if (slot == TableStation.REPLAN) {
                        // a stand-in for what is taken back: counted, never sent
                        moves.add(new int[]{world, slot, need});
                        need = 0;
                        break;
                    }
                    int take = Math.min(need, left[slot + 1]);
                    if (take <= 0) continue;
                    left[slot + 1] -= take;
                    need -= take;
                    moves.add(new int[]{world, slot, take});
                }
            }
            return new MaxLayout(sets, current, moves);
        }
        return new MaxLayout(current, current, List.of());
    }

    private static int kindOf(List<ItemStack> kinds, ItemStack stack) {
        for (int k = 0; k < kinds.size(); k++) if (ItemStack.isSameItemSameComponents(kinds.get(k), stack)) return k;
        return -1;
    }

    /** Whether every cell can reach {@code sets} items; {@code chosen} receives each cell's kind. */
    private static boolean assign(int sets, int count, List<Integer> fixedKind, List<Integer> have, List<Integer> limit,
            List<int[]> choices, List<ItemStack> kinds, List<Integer> stock, int[] chosen) {
        int[] left = stock.stream().mapToInt(Integer::intValue).toArray();
        List<Integer> open = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int need = sets - have.get(i);
            if (fixedKind.get(i) >= 0 || have.get(i) > 0) {
                if (need <= 0) { chosen[i] = Math.max(0, fixedKind.get(i)); continue; }
                int kind = fixedKind.get(i);
                if (kind < 0 || sets > limit.get(i) || left[kind] < need) return false;
                left[kind] -= need;
                chosen[i] = kind;
            } else {
                if (choices.get(i).length == 0) return false;
                open.add(i);
            }
        }
        // The cells with the fewest usable kinds first: they decide fastest whether this many sets fit.
        open.sort(Comparator.comparingInt(i -> choices.get(i).length));
        int[] steps = {0};
        return place(0, open, sets, choices, kinds, left, chosen, steps);
    }

    private static boolean place(int at, List<Integer> open, int sets, List<int[]> choices, List<ItemStack> kinds, int[] left,
            int[] chosen, int[] steps) {
        if (at == open.size()) return true;
        if (++steps[0] > SEARCH_STEPS) return false;
        int cell = open.get(at);
        int[] options = choices.get(cell).clone();
        // the most plentiful kind first: it leaves the most for the cells still to come
        Integer[] order = Arrays.stream(options).boxed().sorted((a, b) -> left[b] - left[a]).toArray(Integer[]::new);
        for (int kind : order) {
            if (left[kind] < sets || kinds.get(kind).getMaxStackSize() < sets) continue;
            left[kind] -= sets;
            chosen[cell] = kind;
            if (place(at + 1, open, sets, choices, kinds, left, chosen, steps)) return true;
            left[kind] += sets;
        }
        return false;
    }

    ItemStack ghost(int logical) {
        if (!display[logical].isEmpty()) return display[logical];
        ItemStack[] choices = grid[logical].getItems();
        return choices.length == 0 ? ItemStack.EMPTY : choices[(int) (Scene.clock() / 1.8) % choices.length].copyWithCount(1);
    }

    boolean wrong(int logical) {
        return !actual(logical).isEmpty() && !placed[logical];
    }

    private static List<ItemStack> units(List<ItemStack> stacks) {
        List<ItemStack> result = new ArrayList<>();
        // No crafting recipe consumes more than nine units, even if the source stack contains thousands.
        for (ItemStack stack : stacks) for (int i = 0; i < Math.min(9, stack.getCount()); i++) result.add(stack);
        return result;
    }

    /** Maximum matching avoids spending a flexible tag alternative on an ingredient needed elsewhere. */
    static int[] match(List<Ingredient> ingredients, List<ItemStack> units) {
        int[] owner = new int[units.size()];
        Arrays.fill(owner, -1);
        for (int i = 0; i < ingredients.size(); i++) augment(i, ingredients, units, owner, new boolean[units.size()]);
        int[] result = new int[ingredients.size()];
        Arrays.fill(result, -1);
        for (int i = 0; i < owner.length; i++) if (owner[i] >= 0) result[owner[i]] = i;
        return result;
    }

    private static boolean augment(int ingredient, List<Ingredient> ingredients, List<ItemStack> units, int[] owner, boolean[] seen) {
        for (int i = 0; i < units.size(); i++) if (!seen[i] && ingredients.get(ingredient).test(units.get(i))) {
            seen[i] = true;
            if (owner[i] < 0 || augment(owner[i], ingredients, units, owner, seen)) {
                owner[i] = ingredient;
                return true;
            }
        }
        return false;
    }
}
