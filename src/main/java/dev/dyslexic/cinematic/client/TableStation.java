package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
//? if fabric && <1.21 {
/*import dev.dyslexic.cinematic.net.PacketDistributor;
*///?}
import dev.dyslexic.cinematic.api.client.RecipePanel;
import dev.dyslexic.cinematic.net.StationActionPayload;
import dev.dyslexic.cinematic.station.StationKind;
import dev.dyslexic.cinematic.table.TableCrafting;
import dev.dyslexic.cinematic.table.TableGrid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
//? if neoforge {
import net.neoforged.neoforge.network.PacketDistributor;
//?} else if fabric && >=1.21 {
/*import dev.dyslexic.cinematic.net.PacketDistributor;
*///?}
import org.joml.Matrix4f;

/**
 * The crafting table scene: lean over the top, lay items into the painted 3x3 grid, click the result that
 * floats over the far edge to craft (the hands press the ingredients together while the server crafts).
 */
public final class TableStation extends ClientStation {
    static final double HAND_Y = 0.062;
    static final double REST_BACK = 0.4;
    static final double REST_SIDE = 0.24;
    /** How far in front of the fist a held item hangs. */
    static final double ITEM_REACH = 0.075;

    public static final double LEAN_BACK = 0.86;
    public static final double LEAN_UP = 0.72;
    /** The point looked at, measured from the top centre towards the player (negative: beyond the centre). */
    public static final double LOOK_BACK = -0.04;

    public static final double CRAFT_LENGTH = 0.5;
    public static final double CRAFT_MID = 0.2;
    private static final float RESULT_SIZE = 0.2f;
    private static final float OCCLUDED_OPACITY = 0.28f;
    private static final double FADE_RATE = 14;

    public final Vec3 top;
    final TableRecipeGuide guide;
    /** The recipe reference on the left; choosing a recipe shows its diagram on the tabletop ({@link #drawDiagram}). */
    final RecipePanel recipeBook;
    final TableGridEditor gridEditor;
    public int hoverCell = -1;
    public boolean hoverResult;

    public ItemStack result = ItemStack.EMPTY;
    public double resultChangedAt = -10;
    private int resultVersion = -1;
    //? if <1.21 {
    /*private final RecipeChoiceStrip choiceStrip = new RecipeChoiceStrip();
    private List<net.minecraft.world.item.crafting.CraftingRecipe> competing = List.of();
    @Nullable private net.minecraft.resources.ResourceLocation chosenRecipe;
    *///?}

    /** The ingredients as they lay when crafting was clicked, gathered while the server crafts. */
    public double craftAt = -10;
    @Nullable
    public ItemStack[] craftItems;
    @Nullable
    public byte[] craftFacing;

    private boolean painting;
    private final boolean[] painted = new boolean[TableGrid.CELLS];
    private final float[] itemOpacity = new float[TableGrid.CELLS];

    public TableStation(BlockPos pos, Direction side) {
        super(pos, side);
        top = new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        Arrays.fill(itemOpacity, 1f);
        guide = new TableRecipeGuide(this);
        recipeBook = new RecipePanel(guide);
        gridEditor = new TableGridEditor(this);
    }

    @Override
    public StationKind kind() {
        return StationKind.TABLE;
    }

    @Override
    public Vec3 center() {
        return top;
    }

    @Override
    public List<Vec3> approach(LocalPlayer player) {
        Vec3 stand = new Vec3(top.x + side.getStepX() * 0.86, player.getY(), top.z + side.getStepZ() * 0.86);
        boolean walk = Math.hypot(stand.x - player.getX(), stand.z - player.getZ()) > 0.06;
        return walk ? List.of(stand) : List.of();
    }

    @Override
    public boolean busy() {
        return craftProgress() >= 0 || laying != null || gridEditor.editing();
    }

    @Override
    public boolean showsInventory() { return !gridEditor.visible(); }

    @Override
    public Vec3 workPoint() { return TableLayouts.current().center(pos); }

    @Override
    public List<Vec3> workSamples() {
        TableLayout layout = TableLayouts.current();
        if (layout.equals(TableLayout.VANILLA)) return super.workSamples();
        double low = TableGrid.GRID_MIN, high = TableGrid.GRID_MAX;
        return List.of(layout.center(pos).add(0, 0.04, 0), layout.point(pos, low, low).add(0, 0.04, 0),
                layout.point(pos, high, low).add(0, 0.04, 0), layout.point(pos, low, high).add(0, 0.04, 0),
                layout.point(pos, high, high).add(0, 0.04, 0));
    }

    @Override
    public void onEnd() {
        gridEditor.cancel();
        cancelSpread();
    }

    double surfaceY() { return TableLayouts.current().surfaceY(pos); }

    // ---- laying out a recipe -------------------------------------------------------------------------

    /** Seconds one item takes: from the side, over the cell, down onto it, and the hand lifts. */
    static final double LAY_MOVE = 0.36;
    /** When in a move the item is let go of (the server is asked to put it down). */
    private static final double LAY_DROP = 0.64;

    /** A move's "slot" for taking back what lies on the cell into the inventory, rather than laying onto it. */
    static final int TAKE = -2;
    /**
     * A move's "slot" for working out the rest of the layout once the cells taken back are clear ({cell -1, REPLAN,
     * 1 for every whole set}): what came back to the inventory may be laid out again, in its new slot.
     */
    static final int REPLAN = -3;
    /** How long a re-plan waits for the server to clear the cells taken back (seconds) before it lays what it can. */
    private static final double REPLAN_WAIT = 1.5;

    /**
     * The moves still to make, {world cell, inventory slot or -1, how many}; the one under way is {@link #laying}. A
     * move carries a whole handful, so laying out many sets takes no longer than laying out one.
     */
    private final java.util.ArrayDeque<int[]> layQueue = new java.util.ArrayDeque<>();
    @Nullable
    private int[] laying;
    private ItemStack layItem = ItemStack.EMPTY;
    private double layAt;
    private boolean layDropped;
    /** The cells taken back for a re-plan, which the server is to empty first. */
    private final List<Integer> takenBack = new java.util.ArrayList<>();

    /** Lays out the selected recipe from the inventory, one cell after another. */
    void layOut(List<int[]> moves) {
        if (busy() || moves.isEmpty()) {
            return;
        }
        layQueue.addAll(moves);
        nextLay();
    }

    /**
     * Shift-click from the drawer: the stack goes onto the table, carried by the hand like a laid-out recipe, by a rule a
     * player can see coming: a cell of the chosen recipe's diagram that wants it, else a cell already holding the same
     * item with room, else the first free cell, reading the grid as a recipe is read from where the player stands (far
     * row first, left to right). As much of the stack as the cell takes; the rest stays in its slot.
     */
    @Override
    public void quickMove(int inventorySlot, int menuSlot) {
        Minecraft mc = Minecraft.getInstance();
        if (busy() || mc.player == null) {
            return;
        }
        ItemStack stack = mc.player.getInventory().getItem(inventorySlot);
        if (stack.isEmpty()) {
            return;
        }
        ClientTables.Entry entry = ClientTables.get(pos);
        int target = -1;
        for (int logical = 0; logical < TableGrid.CELLS && target < 0 && guide.selected != null; logical++) {
            int world = TableGrid.worldIndex(side, logical / 3, logical % 3);
            if (!guide.grid[logical].isEmpty() && !guide.placed[logical] && onCell(entry, world).isEmpty() && guide.grid[logical].test(stack)) {
                target = world;
            }
        }
        for (int logical = 0; logical < TableGrid.CELLS && target < 0; logical++) {
            int world = TableGrid.worldIndex(side, logical / 3, logical % 3);
            ItemStack there = onCell(entry, world);
            if (!there.isEmpty() && ItemStack.isSameItemSameComponents(there, stack) && there.getCount() < there.getMaxStackSize()) {
                target = world;
            }
        }
        for (int logical = 0; logical < TableGrid.CELLS && target < 0; logical++) {
            int world = TableGrid.worldIndex(side, logical / 3, logical % 3);
            if (onCell(entry, world).isEmpty()) {
                target = world;
            }
        }
        if (target < 0) {
            // a full table: the stack stays where it is
            Scene.sound(net.minecraft.sounds.SoundEvents.WOOD_HIT, 0.25f, 0.7f);
            return;
        }
        ItemStack there = onCell(entry, target);
        int count = Math.min(stack.getCount(), there.isEmpty() ? stack.getMaxStackSize() : there.getMaxStackSize() - there.getCount());
        layOut(List.of(new int[]{target, inventorySlot, count}));
    }

    private static ItemStack onCell(@Nullable ClientTables.Entry entry, int world) {
        return entry == null ? ItemStack.EMPTY : entry.items[world];
    }

    private void nextLay() {
        laying = layQueue.poll();
        layDropped = false;
        layAt = Scene.clock();
        //? if >=1.21 {
        if (laying == null) {
            takenBack.clear();
        }
        //?} else if <1.21 {
        /*if (laying == null) takenBack.clear();
        *///?}
        if (laying != null && laying[1] == REPLAN) {
            layItem = ItemStack.EMPTY;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ItemStack source = laying == null || mc.player == null ? ItemStack.EMPTY
                : laying[1] == TAKE ? onCell(ClientTables.get(pos), laying[0])
                : laying[1] < 0 ? mc.player.containerMenu.getCarried() : mc.player.getInventory().getItem(laying[1]);
        layItem = laying == null ? ItemStack.EMPTY : source.copyWithCount(Math.min(source.getCount(), laying[2]));
        if (laying != null && layItem.isEmpty()) {
            // the inventory changed meanwhile: skip it
            nextLay();
        }
    }

    /** 0..1 through the move under way. */
    private double layProgress() {
        return Anim.clamp01((Scene.clock() - layAt) / LAY_MOVE);
    }

    /** Where the hand is on the laying path: taking back runs the same path the other way, from the cell to the hand. */
    private double layPath() {
        return laying != null && laying[1] == TAKE ? 1 - layProgress() : layProgress();
    }

    /** The rest of the layout, once the server has cleared the cells taken back (or it took too long). */
    private void replan() {
        boolean clear = true;
        ClientTables.Entry entry = ClientTables.get(pos);
        for (int cell : takenBack) clear &= onCell(entry, cell).isEmpty();
        double waited = Scene.clock() - layAt;
        //? if >=1.21 {
        if (waited < 0.1 || !clear && waited < REPLAN_WAIT) {
            return;
        }
        //?} else if <1.21 {
        /*if (waited < 0.1 || !clear && waited < REPLAN_WAIT) return;
        *///?}
        boolean all = laying[2] == 1;
        guide.tick();
        for (int[] move : all ? guide.maxLayout().moves() : guide.layoutMoves()) {
            if (move[1] >= -1) layQueue.add(move);
        }
        nextLay();
    }

    @Override
    public void beforeFrame() {
        gridEditor.frame();
        if (laying == null || Scene.phase != Scene.Phase.ACTIVE) {
            if (Scene.phase != Scene.Phase.ACTIVE) {
                layQueue.clear();
                laying = null;
            }
            return;
        }
        if (laying[1] == REPLAN) {
            replan();
            return;
        }
        double t = layProgress();
        if (laying[1] == TAKE) {
            if (!layDropped && t >= 1 - LAY_DROP) {
                layDropped = true;
                Hands.tap();
                takenBack.add(laying[0]);
                // the server's own shift-click: into the inventory as far as it has room, the rest stays on the cell
                if (Scene.clientOnly()) clickVanilla(laying[0], 0, true);
                //? if !forge {
                else PacketDistributor.sendToServer(new StationActionPayload(pos, StationKind.TABLE, StationActionPayload.Action.CLICK,
                //?} else if forge {
                /*else ClientNet.sendToServer(new StationActionPayload(pos, StationKind.TABLE, StationActionPayload.Action.CLICK,
                *///?}
                        laying[0], 0, true, 0));
            }
            //? if >=1.21 {
            if (t >= 1) {
                nextLay();
            }
            //?} else if <1.21 {
            /*if (t >= 1) nextLay();
            *///?}
            return;
        }
        if (!layDropped && t >= LAY_DROP) {
            layDropped = true;
            Hands.tap();
            if (Scene.clientOnly()) placeVanilla(laying[0], laying[1], layItem.getCount());
            //? if !forge {
            else PacketDistributor.sendToServer(new StationActionPayload(pos, StationKind.TABLE, StationActionPayload.Action.PLACE,
            //?} else if forge {
            /*else ClientNet.sendToServer(new StationActionPayload(pos, StationKind.TABLE, StationActionPayload.Action.PLACE,
            *///?}
                    laying[0], laying[1], false, layItem.getCount()));
        }
        if (t >= 1) {
            nextLay();
        }
    }

    /** The middle of a cell on the top. */
    Vec3 cellPoint(int cell) {
        return TableLayouts.current().cell(pos, cell);
    }

    /**
     * The hand on the cell's side: back a little towards the player (taking the item), over the cell, down onto it, and
     * up again; the other hand rests.
     */
    @Nullable
    private Vec3[] layHands() {
        if (laying == null || laying[1] == REPLAN) {
            return null;
        }
        double t = layPath();
        Vec3 cell = cellPoint(laying[0]);
        boolean right = lateral(cell) >= 0;
        Vec3 rest = rest(right);
        Vec3 take = rest.add(side().scale(0.14)).add(0, 0.03, 0);
        Vec3 toward = new Vec3(cell.x - rest.x, 0, cell.z - rest.z).normalize();
        Vec3 over = cell.add(0, HAND_Y + 0.07, 0).subtract(toward.scale(ITEM_REACH));
        Vec3 down = over.add(0, -0.06, 0);
        Vec3 hand;
        if (t < 0.28) {
            hand = Anim.lerp(rest, take, Anim.smoother(t / 0.28));
        } else if (t < LAY_DROP - 0.06) {
            double k = Anim.inOutCubic((t - 0.28) / (LAY_DROP - 0.06 - 0.28));
            hand = Anim.lerp(take, over, k).add(0, 0.05 * Anim.hump(k), 0);
        } else if (t < LAY_DROP + 0.06) {
            hand = Anim.lerp(over, down, Anim.hump((t - (LAY_DROP - 0.06)) / 0.12));
        } else {
            hand = Anim.lerp(over, rest, Anim.smoother((t - LAY_DROP - 0.06) / (1 - LAY_DROP - 0.06)));
        }
        Vec3 other = rest(!right);
        return right ? new Vec3[]{hand, other} : new Vec3[]{other, hand};
    }

    /** The item in the hand while it is carried over (until the table has it). */
    private void drawLayItem(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int light) {
        //? if >=1.21 {
        if (laying == null || layItem.isEmpty() || laying[1] == REPLAN) {
        //?} else if <1.21 {
        /*if (laying == null || layItem.isEmpty()) {
        *///?}
            return;
        }
        //? if >=1.21 {
        double t = layPath();
        //?} else if <1.21 {
        /*double t = layProgress();
        *///?}
        if (t < 0.24 || t > LAY_DROP + 0.04) {
            return;
        }
        Vec3 cell = cellPoint(laying[0]);
        boolean right = lateral(cell) >= 0;
        Vec3 hand = Hands.position(right);
        Vec3 toward = new Vec3(cell.x - hand.x, 0, cell.z - hand.z);
        toward = toward.lengthSqr() < 1e-6 ? forward() : toward.normalize();
        Vec3 at = hand.add(toward.scale(ITEM_REACH)).add(0, -HAND_Y + 0.012, 0);
        float grow = (float) Anim.smoother((t - 0.24) / 0.08);
        TableLayouts.current().renderItem(pose, buffers, camera, layItem, at, side, 91L, grow, 14f, light, 1, true);
    }

    @Override
    public boolean worksAtAnAngle() {
        return true;
    }

    @Override
    public CameraRig.Pose lean() {
        Vec3 at = top.add(side().scale(LEAN_BACK)).add(0, LEAN_UP, 0);
        float pitch = (float) Math.toDegrees(Math.atan2(LEAN_UP, LEAN_BACK - LOOK_BACK));
        return new CameraRig.Pose(at, side.getOpposite().toYRot(), pitch, 0);
    }

    @Override
    public Vec3 rest(boolean right) {
        return top.add(side().scale(REST_BACK)).add(right().scale(right ? REST_SIDE : -REST_SIDE))
                .add(0, HAND_Y + TableLayouts.current().height() / 16, 0);
    }

    // ---- cursor ----------------------------------------------------------------------------------------

    @Override
    public void updateCursor(boolean overPanel) {
        double[] mouse = Picking.mouseGui();
        overPanel |= recipeBook.over(mouse[0], mouse[1]);
        overPanel |= gridEditor.over(mouse[0], mouse[1]);
        hoverResult = false;
        if (!result.isEmpty() && !overPanel) {
            double[] at = Picking.toGui(resultPosition());
            if (at != null) {
                double radius = resultRadiusGui();
                hoverResult = (mouse[0] - at[0]) * (mouse[0] - at[0]) + (mouse[1] - at[1]) * (mouse[1] - at[1]) < radius * radius;
            }
        }
        Vec3 hit = overPanel || hoverResult ? null : Picking.cursorOnPlane(surfaceY());
        hoverCell = -1;
        aim = null;
        if (hit == null) {
            return;
        }
        double lx = hit.x - pos.getX();
        double lz = hit.z - pos.getZ();
        if (lx > -0.02 && lx < 1.02 && lz > -0.02 && lz < 1.02) {
            hoverCell = TableLayouts.current().cellAt(pos, hit);
            aim = new Vec3(Math.max(pos.getX() + 0.07, Math.min(pos.getX() + 0.93, hit.x)), surfaceY(),
                    Math.max(pos.getZ() + 0.07, Math.min(pos.getZ() + 0.93, hit.z)));
        }
    }

    @Override
    public void clearCursor() {
        super.clearCursor();
        hoverCell = -1;
        hoverResult = false;
    }

    /** How far the camera must really move (blocks) before the items in the way are worked out again. */
    private static final double OCCLUSION_MOVE = 0.06;
    /** Points of the hovered cell the camera must see (fractions of a cell from its middle): its middle and four inside it. */
    private static final double[][] CELL_SAMPLES = {{0, 0}, {-0.3, -0.3}, {0.3, -0.3}, {-0.3, 0.3}, {0.3, 0.3}};
    private final boolean[] inTheWay = new boolean[TableGrid.CELLS];
    private int occlusionCell = Integer.MIN_VALUE;
    private int occlusionVersion = Integer.MIN_VALUE;
    //? if >=1.21 {
    @Nullable
    private Vec3 occlusionCamera;

    /**
     * The tabletop remains pickable through ingredients; only the ones in front of the hovered cell fade. Which ones is
     * worked out for the cell itself (rays to its middle and four points in it), and kept while the same cell stays under
     * the cursor: from the point under the cursor it changed with every small move of the mouse and every breath of the
     * camera, and the items at the edge of that line faded in and out.
     */
    //?} else if fabric && <1.21 {
    /*@Nullable private Vec3 occlusionCamera;

    boolean[] inTheWayCopy() {
        return inTheWay.clone();
    }

    /^* Keep one stable occlusion decision for the hovered cell until the camera or table changes. ^/
    *///?} else if forge {
    /*@Nullable private Vec3 occlusionCamera;

    /^* Keep one stable occlusion decision for the hovered cell until the camera or table changes. ^/
    *///?}
    void updateOcclusion(Vec3 camera) {
        ClientTables.Entry entry = ClientTables.get(pos);
        int cell = Scene.phase == Scene.Phase.ACTIVE && craftProgress() < 0 ? hoverCell : -1;
        int version = entry == null ? -1 : entry.version;
        if (cell != occlusionCell || version != occlusionVersion || occlusionCamera == null
                || camera.distanceToSqr(occlusionCamera) > OCCLUSION_MOVE * OCCLUSION_MOVE) {
            occlusionCell = cell;
            occlusionVersion = version;
            occlusionCamera = camera;
            TableLayout layout = TableLayouts.current();
            for (int i = 0; i < TableGrid.CELLS; i++) {
                ItemStack stack = entry == null ? ItemStack.EMPTY : entry.items[i];
                inTheWay[i] = false;
                if (cell < 0 || i == cell || stack.isEmpty()) continue;
                Vec3 base = cellPoint(i);
                //? if >=1.21 {
                net.minecraft.world.phys.AABB bounds = (layout.equals(TableLayout.VANILLA) ? ItemPlacer.restingBounds(stack, base) : layout.bounds(stack, base)).inflate(0.003);
                //?} else if <1.21 {
                /*var bounds = (layout.equals(TableLayout.VANILLA) ? ItemPlacer.restingBounds(stack, base) : layout.bounds(stack, base)).inflate(0.003);
                *///?}
                Vec3 target = cellPoint(cell);
                for (double[] at : CELL_SAMPLES) {
                    Vec3 point = new Vec3(target.x + at[0] * TableGrid.CELL, surfaceY(), target.z + at[1] * TableGrid.CELL);
                    if (bounds.clip(camera, point).isPresent()) {
                        inTheWay[i] = true;
                        break;
                    }
                }
            }
        }
        for (int i = 0; i < TableGrid.CELLS; i++) {
            ItemStack stack = entry == null ? ItemStack.EMPTY : entry.items[i];
            float target = inTheWay[i] ? OCCLUDED_OPACITY : 1f;
            itemOpacity[i] = stack.isEmpty() ? 1f : (float) Anim.approach(itemOpacity[i], target, FADE_RATE, Scene.frameDt);
            if (Math.abs(itemOpacity[i] - target) < 0.001f) {
                itemOpacity[i] = target;
            }
        }
    }

    //? if >=1.21 {
    boolean[] inTheWayCopy() {
        return inTheWay.clone();
    }

    //?}
    public float itemOpacity(int cell) {
        return cell >= 0 && cell < itemOpacity.length ? itemOpacity[cell] : 1f;
    }

    boolean hasFadedItems() {
        for (float opacity : itemOpacity) {
            if (opacity < 0.999f) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Vec3 handTarget(Vec3 aim, Vec3 shoulder, boolean carrying, double tap) {
        Vec3 reach = new Vec3(aim.x - shoulder.x, 0, aim.z - shoulder.z).normalize();
        double hover = carrying ? 0.075 : 0.045;
        return new Vec3(aim.x, surfaceY() + HAND_Y + hover - 0.06 * tap, aim.z).subtract(reach.scale(ITEM_REACH));
    }

    @Override
    public Vec3 heldItemPoint(Vec3 hand, Vec3 shoulder) {
        Vec3 reach = new Vec3(hand.x - shoulder.x, 0, hand.z - shoulder.z).normalize();
        return hand.add(reach.scale(ITEM_REACH)).add(0, -HAND_Y + 0.012, 0);
    }

    @Nullable
    @Override
    public Vec3[] handsOverride() {
        Vec3[] lay = layHands();
        if (lay != null) {
            return lay;
        }
        double craft = craftProgress();
        if (craft < 0) {
            return null;
        }
        Vec3 middle = TableLayouts.current().center(pos).add(side().scale(0.04));
        double y = HAND_Y + 0.05 - 0.07 * Anim.hump(Anim.window(craft, 0.08, 0.62));
        return new Vec3[]{middle.add(right().scale(0.085)).add(0, y, 0), middle.add(right().scale(-0.085)).add(0, y, 0)};
    }

    // ---- actions ---------------------------------------------------------------------------------------

    @Override
    public void click(int button, boolean shift) {
        if (laying != null) {
            return;
        }
        if (hoverResult && button == 0) {
            craft(shift);
            return;
        }
        if (hoverCell < 0) {
            return;
        }
        boolean carrying = !Hands.carried().isEmpty();
        if (button == 0 && !shift && carrying) {
            // As over vanilla slots, a held stack is put down on release: a drag over more cells shares it out.
            spreadStart = hoverCell;
            spreadMask = spreadable(hoverCell) ? 1 << hoverCell : 0;
            spreadStack = Hands.carried().copy();
            spreadSince = Scene.clock();
            return;
        }
        send(StationActionPayload.Action.CLICK, hoverCell, button, shift);
        if (button == 1 && carrying) {
            painting = true;
            Arrays.fill(painted, false);
            painted[hoverCell] = true;
        }
    }

    /** Right-drag across cells drops one item into each; a left-drag gathers the cells to share the stack between. */
    @Override
    public void drag(int button) {
        if (painting && button == 1) {
            Scene.updateCursor();
            if (hoverCell >= 0 && !painted[hoverCell]) {
                painted[hoverCell] = true;
                send(StationActionPayload.Action.PAINT, hoverCell, 1, false);
            }
        }
        if (spreadStart >= 0 && button == 0) {
            Scene.updateCursor();
            if (hoverCell >= 0 && (spreadMask & 1 << hoverCell) == 0 && spreadable(hoverCell)
                    && spreadStack.getCount() > Integer.bitCount(spreadMask)) {
                spreadMask |= 1 << hoverCell;
                Scene.sound(net.minecraft.sounds.SoundEvents.WOOD_HIT, 0.12f, 1.6f + 0.05f * Integer.bitCount(spreadMask));
            }
        }
    }

    @Override
    public void release(int button) {
        if (button == 1) {
            painting = false;
        }
        if (button == 0 && spreadStart >= 0) {
            int start = spreadStart, mask = spreadMask;
            spreadStart = -1;
            spreadMask = 0;
            if (Integer.bitCount(mask) > 1) spread(mask);
            else send(StationActionPayload.Action.CLICK, start, 0, false);
        }
    }

    /** Stops gathering cells without putting anything down (the drag left for a panel, or the scene ends). */
    void cancelSpread() {
        spreadStart = -1;
        spreadMask = 0;
    }

    /** The cell where a left-drag began, or -1; the cells it gathered so far. */
    private int spreadStart = -1;
    private int spreadMask;
    private ItemStack spreadStack = ItemStack.EMPTY;
    private double spreadSince;

    /** A cell that can take part in sharing the held stack: empty, or holding the very same item. */
    private boolean spreadable(int cell) {
        ClientTables.Entry entry = ClientTables.get(pos);
        ItemStack onCell = entry == null ? ItemStack.EMPTY : entry.items[cell];
        ItemStack held = Hands.carried();
        return !held.isEmpty() && (onCell.isEmpty() || ItemStack.isSameItemSameComponents(held, onCell));
    }

    /** The cells a left-drag will share the held stack between (at least two), or 0 while it is a plain click. */
    int spreadCells() {
        return spreadStart >= 0 && Integer.bitCount(spreadMask) > 1 ? spreadMask : 0;
    }

    /** How many items each gathered cell gets, by vanilla's left-drag rule (the remainder stays in hand). */
    int spreadShare() {
        int cells = Integer.bitCount(spreadCells());
        return cells == 0 ? 0 : Hands.carried().getCount() / cells;
    }

    private void spread(int mask) {
        Hands.tap();
        if (!Scene.clientOnly()) {
            //? if !forge {
            PacketDistributor.sendToServer(new StationActionPayload(pos, StationKind.TABLE, StationActionPayload.Action.SPREAD,
            //?} else if forge {
            /*ClientNet.sendToServer(new StationActionPayload(pos, StationKind.TABLE, StationActionPayload.Action.SPREAD,
            *///?}
                    mask, 0, false, 0));
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        CraftingMenu menu = vanillaMenu();
        if (menu == null || mc.gameMode == null || mc.player == null) return;
        // The vanilla drag over slots: begin, each slot, end; the server repeats and checks it.
        int type = 0;
        mc.gameMode.handleInventoryMouseClick(menu.containerId, -999,
                net.minecraft.world.inventory.AbstractContainerMenu.getQuickcraftMask(0, type), ClickType.QUICK_CRAFT, mc.player);
        for (int cell = 0; cell < TableGrid.CELLS; cell++) {
            if ((mask & 1 << cell) != 0 && menuCell(cell) > 0) {
                mc.gameMode.handleInventoryMouseClick(menu.containerId, menuCell(cell),
                        net.minecraft.world.inventory.AbstractContainerMenu.getQuickcraftMask(1, type), ClickType.QUICK_CRAFT, mc.player);
            }
        }
        mc.gameMode.handleInventoryMouseClick(menu.containerId, -999,
                net.minecraft.world.inventory.AbstractContainerMenu.getQuickcraftMask(2, type), ClickType.QUICK_CRAFT, mc.player);
    }

    private void send(StationActionPayload.Action action, int cell, int button, boolean shift) {
        Hands.tap();
        if (Scene.clientOnly()) {
            clickVanilla(cell, button, shift);
        } else {
            //? if !forge {
            PacketDistributor.sendToServer(new StationActionPayload(pos, StationKind.TABLE, action, cell, button, shift, 0));
            //?} else if forge {
            /*ClientNet.sendToServer(new StationActionPayload(pos, StationKind.TABLE, action, cell, button, shift, 0));
            *///?}
        }
    }

    @Nullable
    private CraftingMenu vanillaMenu() {
        var player = Minecraft.getInstance().player;
        return player != null && player.containerMenu instanceof CraftingMenu menu ? menu : null;
    }

    private int menuCell(int world) {
        for (int logical = 0; logical < TableGrid.CELLS; logical++)
            if (TableGrid.worldIndex(side, logical / 3, logical % 3) == world) return logical + 1;
        return -1;
    }

    private void clickVanilla(int world, int button, boolean shift) {
        Minecraft mc = Minecraft.getInstance();
        CraftingMenu menu = vanillaMenu();
        int slot = menuCell(world);
        if (menu != null && slot > 0 && mc.gameMode != null && mc.player != null)
            mc.gameMode.handleInventoryMouseClick(menu.containerId, slot, button,
                    shift ? ClickType.QUICK_MOVE : ClickType.PICKUP, mc.player);
    }

    private void placeVanilla(int world, int source, int count) {
        Minecraft mc = Minecraft.getInstance();
        CraftingMenu menu = vanillaMenu();
        if (menu == null || mc.gameMode == null || mc.player == null || menuCell(world) < 0) return;
        if (source < 0) {
            for (int i = 0; i < count && !menu.getCarried().isEmpty(); i++) clickVanilla(world, 1, false);
            return;
        }
        // A vanilla pickup, a right-click for each item, and return. The server validates every click and recipe.
        if (!menu.getCarried().isEmpty() || source >= 36) return;
        int inventorySlot = source < 9 ? 37 + source : 10 + source - 9;
        if (menu.getSlot(inventorySlot).getItem().isEmpty()) return;
        mc.gameMode.handleInventoryMouseClick(menu.containerId, inventorySlot, 0, ClickType.PICKUP, mc.player);
        if (menu.getCarried().getCount() <= count) clickVanilla(world, 0, false);
        else for (int i = 0; i < count && !menu.getCarried().isEmpty(); i++) clickVanilla(world, 1, false);
        if (!menu.getCarried().isEmpty())
            mc.gameMode.handleInventoryMouseClick(menu.containerId, inventorySlot, 0, ClickType.PICKUP, mc.player);
    }

    public void craft(boolean all) {
        if (result.isEmpty()) {
            return;
        }
        ClientTables.Entry entry = ClientTables.get(pos);
        if (entry != null) {
            craftItems = new ItemStack[TableGrid.CELLS];
            for (int i = 0; i < TableGrid.CELLS; i++) {
                craftItems[i] = entry.items[i].copy();
            }
            craftFacing = entry.facing.clone();
        }
        craftAt = Scene.clock();
        if (Scene.clientOnly()) {
            Minecraft mc = Minecraft.getInstance();
            CraftingMenu menu = vanillaMenu();
            if (menu != null && mc.gameMode != null && mc.player != null)
                mc.gameMode.handleInventoryMouseClick(menu.containerId, CraftingMenu.RESULT_SLOT, 0,
                        all ? ClickType.QUICK_MOVE : ClickType.PICKUP, mc.player);
        } else {
            //? if !forge {
            PacketDistributor.sendToServer(new StationActionPayload(pos, StationKind.TABLE, StationActionPayload.Action.CRAFT, 0, 0, all, 0));
            //?} else if forge {
            /*ClientNet.sendToServer(new StationActionPayload(pos, StationKind.TABLE, StationActionPayload.Action.CRAFT, 0, 0, all, 0));
            *///?}
        }
    }

    /** 0..1 progress of the craft flourish, or -1 when idle. */
    public double craftProgress() {
        double t = (Scene.clock() - craftAt) / CRAFT_LENGTH;
        return t >= 0 && t <= 1 ? t : -1;
    }

    // ---- competing recipes (Polymorph) -------------------------------------------------------------------
    //? if <1.21 {
    /*@Override RecipeChoiceStrip choices() { return choiceStrip; }
    *///?}

    //? if >=1.21 {
    private final RecipeChoiceStrip choiceStrip = new RecipeChoiceStrip();
    /** The recipe in use when several compete for what lies on the table; the server crafts the same one. */
    @Nullable
    private net.minecraft.resources.ResourceLocation chosenRecipe;
    private List<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> competing = List.of();

    @Override
    RecipeChoiceStrip choices() {
        return choiceStrip;
    }

    @Override
    void chose(int index) {
        if (index >= competing.size()) return;
        chosenRecipe = competing.get(index).id();
    //?} else if <1.21 {
    /*@Override void chose(int index) {
        if (index < 0 || index >= competing.size()) return;
        chosenRecipe = competing.get(index).getId();
    *///?}
        super.chose(index);
        // the preview follows at once
        resultVersion = Integer.MIN_VALUE;
    }

    private void updateChoices(@Nullable ClientTables.Entry entry) {
        Minecraft mc = Minecraft.getInstance();
        //? if >=1.21 {
        if (!dev.dyslexic.cinematic.station.RecipeChoices.available() || Scene.clientOnly() || entry == null || mc.level == null || mc.player == null) {
        //?} else if <1.21 {
        /*if (!dev.dyslexic.cinematic.station.RecipeChoices.available() || Scene.clientOnly()
                || entry == null || mc.level == null || mc.player == null) {
        *///?}
            choiceStrip.clear();
            competing = List.of();
            return;
        }
        //? if >=1.21 {
        var input = net.minecraft.world.item.crafting.CraftingInput.ofPositioned(3, 3, TableGrid.oriented(entry.list(), side)).input();
        competing = input.isEmpty() ? List.of() : dev.dyslexic.cinematic.station.RecipeChoices.matching(mc.level,
                net.minecraft.world.item.crafting.RecipeType.CRAFTING, input);
        //?} else if <1.21 {
        /*var input = new dev.dyslexic.cinematic.table.TableInput(TableGrid.oriented(entry.list(), side));
        competing = input.isEmpty() ? List.of() : dev.dyslexic.cinematic.station.RecipeChoices.matching(
                mc.level, net.minecraft.world.item.crafting.RecipeType.CRAFTING, input);
        *///?}
        if (competing.size() < 2) {
            choiceStrip.clear();
            return;
        }
        int index = -1;
        //? if >=1.21 {
        for (int i = 0; i < competing.size(); i++) if (competing.get(i).id().equals(chosenRecipe)) index = i;
        //?} else if <1.21 {
        /*for (int i = 0; i < competing.size(); i++) if (competing.get(i).getId().equals(chosenRecipe)) index = i;
        *///?}
        if (index < 0) {
            // not chosen in this scene yet: Polymorph's own record if the client has it, else the first; the server is
            // told, so both show and make the same thing
            var known = dev.dyslexic.cinematic.station.RecipeChoices.chosen(mc.player);
            //? if >=1.21 {
            for (int i = 0; i < competing.size(); i++) if (competing.get(i).id().equals(known)) index = i;
            //?} else if <1.21 {
            /*for (int i = 0; i < competing.size(); i++) if (competing.get(i).getId().equals(known)) index = i;
            *///?}
            index = Math.max(0, index);
            //? if >=1.21 {
            chosenRecipe = competing.get(index).id();
            //?} else if <1.21 {
            /*chosenRecipe = competing.get(index).getId();
            *///?}
            RecipeChoiceStrip.send(pos, kind(), index);
        }
        //? if >=1.21 {
        List<ItemStack> outputs = new java.util.ArrayList<>();
        for (var holder : competing) outputs.add(holder.value().assemble(input, mc.level.registryAccess()));
        //?} else if <1.21 {
        /*List<ItemStack> outputs = new ArrayList<>();
        for (var recipe : competing) outputs.add(recipe.assemble(input, mc.level.registryAccess()));
        *///?}
        choiceStrip.set(outputs, index);
    }

    @Override
    public void tick() {
        recipeBook.tick();
        Minecraft mc = Minecraft.getInstance();
        ClientTables.Entry entry = ClientTables.get(pos);
        int version = entry == null ? -1 : entry.version;
        if (version == resultVersion && !Scene.clientOnly()) {
            return;
        }
        resultVersion = version;
        updateChoices(entry);
        ItemStack next = Scene.clientOnly() ? vanillaMenu() == null ? ItemStack.EMPTY : vanillaMenu().getSlot(0).getItem()
                : entry == null || mc.level == null ? ItemStack.EMPTY
                : TableCrafting.match(mc.level, entry.list(), side, chosenRecipe).map(TableCrafting.Match::result).orElse(ItemStack.EMPTY);
        if (!ItemStack.isSameItemSameComponents(next, result) || next.getCount() != result.getCount()) {
            resultChangedAt = Scene.clock();
        }
        result = next;
    }

    // ---- drawing ---------------------------------------------------------------------------------------

    @Override
    public void renderWorld(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int light) {
        gridEditor.renderWorld(pose, buffers, camera);
        if (gridEditor.visible()) return;
        drawResult(pose, buffers, camera, light);
        drawLayItem(pose, buffers, camera, light);
        drawSpreadPreview(pose, buffers, camera, light);
        drawDiagram(pose, buffers, camera, light);
    }

    /** While a left-drag gathers cells, the empty ones show a faint copy of what will land there. */
    private void drawSpreadPreview(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int light) {
        int spread = spreadCells();
        ItemStack held = Hands.carried();
        if (spread == 0 || held.isEmpty() || spreadShare() <= 0) {
            return;
        }
        ClientTables.Entry entry = ClientTables.get(pos);
        float appear = (float) Anim.smoother((Scene.clock() - spreadSince) / 0.2);
        if (buffers instanceof MultiBufferSource.BufferSource source) source.endBatch();
        for (int cell = 0; cell < TableGrid.CELLS; cell++) {
            if ((spread & 1 << cell) == 0 || entry != null && !entry.items[cell].isEmpty()) continue;
            TableLayout layout = TableLayouts.current();
            layout.renderItem(pose, buffers, camera, held.copyWithCount(spreadShare()), layout.cell(pos, cell).add(0, 0.002, 0),
                    side, cell, 1, 0, light, appear * 0.4f, false);
        }
        if (buffers instanceof MultiBufferSource.BufferSource source) source.endBatch();
    }

    @Override
    public void renderScreen(GuiGraphics graphics, Font font, float alpha) {
        if (!gridEditor.visible()) recipeBook.render(graphics, font, alpha);
        gridEditor.render(graphics, font, alpha);
    }

    void drawHighlight(PoseStack pose, MultiBufferSource buffers, Vec3 cam) {
        if (craftProgress() >= 0) {
            return;
        }
        int spread = spreadCells();
        for (int cell = 0; cell < TableGrid.CELLS; cell++) {
            if (cell == hoverCell || (spread & 1 << cell) != 0) {
                drawHighlight(pose, buffers, cam, cell);
            }
        }
    }

    private void drawHighlight(PoseStack pose, MultiBufferSource buffers, Vec3 cam, int cell) {
        double half = TableGrid.CELL / 2 - 0.006;
        pose.pushPose();
        TableLayouts.current().cellPose(pose, pos, cell, cam);
        double cx = 0, cz = 0, y = 0;
        // one even, steady glow: the rim blends over the fill instead of fighting it for depth
        int alpha = 47;
        Matrix4f m = pose.last().pose();
        VertexConsumer quads = buffers.getBuffer(Fx.HIGHLIGHT);
        //? if >=1.21 {
        quads.addVertex(m, (float) (cx - half), (float) y, (float) (cz - half)).setColor(255, 244, 214, alpha);
        quads.addVertex(m, (float) (cx - half), (float) y, (float) (cz + half)).setColor(255, 244, 214, alpha);
        quads.addVertex(m, (float) (cx + half), (float) y, (float) (cz + half)).setColor(255, 244, 214, alpha);
        quads.addVertex(m, (float) (cx + half), (float) y, (float) (cz - half)).setColor(255, 244, 214, alpha);
        //?} else if <1.21 {
        /*quads.addVertex(m, (float) (cx - half), (float) y, (float) (cz - half)).color(255, 244, 214, alpha).endVertex();
        quads.addVertex(m, (float) (cx - half), (float) y, (float) (cz + half)).color(255, 244, 214, alpha).endVertex();
        quads.addVertex(m, (float) (cx + half), (float) y, (float) (cz + half)).color(255, 244, 214, alpha).endVertex();
        quads.addVertex(m, (float) (cx + half), (float) y, (float) (cz - half)).color(255, 244, 214, alpha).endVertex();
        *///?}
        double width = 0.004;
        // The rim makes a partly hidden destination readable through the faded ingredient.
        highlightStrip(quads, m, cx - half, cz - half, cx + half, cz - half + width, y, 150);
        highlightStrip(quads, m, cx - half, cz + half - width, cx + half, cz + half, y, 150);
        highlightStrip(quads, m, cx - half, cz - half, cx - half + width, cz + half, y, 150);
        highlightStrip(quads, m, cx + half - width, cz - half, cx + half, cz + half, y, 150);
        pose.popPose();
    }

    private static void highlightStrip(VertexConsumer quads, Matrix4f m, double x0, double z0, double x1, double z1, double y, int alpha) {
        //? if >=1.21 {
        quads.addVertex(m, (float) x0, (float) y, (float) z0).setColor(255, 235, 185, alpha);
        quads.addVertex(m, (float) x0, (float) y, (float) z1).setColor(255, 235, 185, alpha);
        quads.addVertex(m, (float) x1, (float) y, (float) z1).setColor(255, 235, 185, alpha);
        quads.addVertex(m, (float) x1, (float) y, (float) z0).setColor(255, 235, 185, alpha);
        //?} else if <1.21 {
        /*quads.addVertex(m, (float) x0, (float) y, (float) z0).color(255, 235, 185, alpha).endVertex();
        quads.addVertex(m, (float) x0, (float) y, (float) z1).color(255, 235, 185, alpha).endVertex();
        quads.addVertex(m, (float) x1, (float) y, (float) z1).color(255, 235, 185, alpha).endVertex();
        quads.addVertex(m, (float) x1, (float) y, (float) z0).color(255, 235, 185, alpha).endVertex();
        *///?}
    }

    /** The result floats over the far edge of the table, turning slowly. */
    public Vec3 resultPosition() {
        double bob = Math.sin(Scene.clock() * 2.1) * 0.012;
        return top.add(forward().scale(0.42)).add(0, 0.17 + bob, 0);
    }

    /** Click radius of the result, in GUI pixels. */
    private double resultRadiusGui() {
        Vec3 at = resultPosition();
        double[] a = Picking.toGui(at);
        double[] b = Picking.toGui(at.add(0, RESULT_SIZE * 0.5, 0));
        if (a == null || b == null) {
            return 12;
        }
        return Math.max(10, Math.hypot(a[0] - b[0], a[1] - b[1]) * 1.25);
    }

    private void drawResult(PoseStack pose, MultiBufferSource buffers, Vec3 cam, int light) {
        if (result.isEmpty()) {
            return;
        }
        double craft = craftProgress();
        double appear = Anim.outBack(Anim.window(Scene.clock() - resultChangedAt, 0, 0.35), 1.8);
        double hover = hoverResult ? 1.14 : 1.0;
        double pop = craft >= 0 ? 1 - 0.6 * Anim.hump(Anim.window(craft, 0.15, 0.55)) : 1;
        float size = (float) (RESULT_SIZE * appear * hover * pop);
        if (size <= 0.002f) {
            return;
        }
        Vec3 at = resultPosition();
        float spin = (float) (Scene.clock() * (hoverResult ? 70 : 32) % 360);
        Minecraft mc = Minecraft.getInstance();
        pose.pushPose();
        pose.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
        pose.mulPose(Axis.YP.rotationDegrees(spin));
        boolean block = mc.getItemRenderer().getModel(result, mc.level, null, 0).isGui3d();
        float scale = block ? size * 1.6f : size;
        pose.scale(scale, scale, scale);
        int glow = LightTexture.pack(Math.max(LightTexture.block(light), 12), LightTexture.sky(light));
        mc.getItemRenderer().renderStatic(result, ItemDisplayContext.FIXED, glow, OverlayTexture.NO_OVERLAY, pose, buffers, mc.level, 0);
        pose.popPose();
    }

    @Nullable
    @Override
    public List<Component> tooltip(ItemStack carried) {
        double[] mouse = Picking.mouseGui();
        List<Component> editorTip = gridEditor.tooltip(mouse[0], mouse[1]);
        if (editorTip != null || gridEditor.editing()) return editorTip;
        List<Component> panelTip = recipeBook.tooltip(mouse[0], mouse[1]);
        if (panelTip != null || recipeBook.over(mouse[0], mouse[1])) return panelTip;
        List<Component> diagramTip = diagramTooltip();
        if (diagramTip != null) return diagramTip;
        int spread = Integer.bitCount(spreadCells());
        if (spread > 0) {
            int share = spreadShare();
            int rest = carried.getCount() - share * spread;
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("cinematic.hint.spread", share, spread));
            if (rest > 0) lines.add(Component.translatable("cinematic.hint.spread_rest", rest).withStyle(ChatFormatting.GRAY));
            return lines;
        }
        if (!carried.isEmpty()) {
            return null;
        }
        if (hoverResult && !result.isEmpty()) {
            List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), result));
            lines.add(Component.translatable("cinematic.hint.result").withStyle(ChatFormatting.GRAY));
            return lines;
        }
        ClientTables.Entry entry = ClientTables.get(pos);
        if (hoverCell >= 0 && entry != null && !entry.items[hoverCell].isEmpty()) {
            ItemStack stack = entry.items[hoverCell];
            List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), stack));
            lines.add(Component.translatable("cinematic.hint.table_count", stack.getCount()).withStyle(ChatFormatting.GRAY));
            return lines;
        }
        return null;
    }

    @Override
    public Component hint() {
        if (gridEditor.editing()) return TableGridEditor.text("hint");
        return Component.translatable("cinematic.hint.table");
    }

    @Override
    public RecipePanel recipePanel() {
        return recipeBook;
    }

    /** For the scripted walkthrough: where a recipe is listed in the reference, or {@code null} if it is not. */
    @Nullable
    double[] recipePoint(net.minecraft.resources.ResourceLocation id) {
        return recipeBook.entryPoint(e -> ((TableRecipeGuide.Entry) e).holder.id().equals(id));
    }

    /** The chosen recipe's cell under the mouse: what goes there, or that its item is wrong. */
    @Nullable
    private List<Component> diagramTooltip() {
        if (guide.selected == null || hoverCell < 0) return null;
        int cell = logicalCell(hoverCell);
        if (guide.grid[cell].isEmpty()) return guide.wrong(cell) ? List.of(recipeText("extra").copy().withStyle(ChatFormatting.RED)) : null;
        ItemStack expected = guide.placed[cell] ? ItemStack.EMPTY : guide.ghost(cell);
        if (expected.isEmpty()) return null;
        return List.of(expected.getHoverName(), recipeText(guide.wrong(cell) ? "wrong" : guide.missing[cell] ? "missing" : "place")
                .copy().withStyle(guide.wrong(cell) || guide.missing[cell] ? ChatFormatting.RED : ChatFormatting.GRAY));
    }

    private static Component recipeText(String key) {
        return Component.translatable("cinematic.recipes." + key);
    }

    private int logicalCell(int world) {
        for (int i = 0; i < 9; i++) if (TableGrid.worldIndex(side, i / 3, i % 3) == world) return i;
        return 0;
    }

    /** The chosen recipe's see-through diagram on the tabletop: a rim on each of its cells and a ghost of what goes there. */
    private void drawDiagram(PoseStack pose, MultiBufferSource buffers, Vec3 cam, int light) {
        if (guide.selected == null || craftProgress() >= 0) return;
        if (buffers instanceof MultiBufferSource.BufferSource source) source.endBatch();
        float appear = (float) Anim.smoother((Scene.clock() - guide.selectedAt) / 0.28);
        for (int logical = 0; logical < 9; logical++) {
            boolean wrong = guide.wrong(logical);
            if (guide.grid[logical].isEmpty() && !wrong) continue;
            int world = TableGrid.worldIndex(side, logical / 3, logical % 3);
            TableLayout layout = TableLayouts.current();
            Vec3 base = layout.cell(pos, world).add(0, 0.002, 0);
            boolean red = guide.missing[logical] || wrong;
            int r = red ? 255 : guide.placed[logical] ? 143 : 166;
            int g = red ? 106 : guide.placed[logical] ? 211 : 211;
            int b = red ? 98 : guide.placed[logical] ? 176 : 235;
            pose.pushPose();
            layout.cellPose(pose, pos, world, cam);
            drawRim(pose, buffers, Vec3.ZERO, r, g, b, appear * (guide.placed[logical] ? 0.3f : 0.7f));
            pose.popPose();
            if (!guide.placed[logical] && !wrong) {
                layout.renderItem(pose, buffers, cam, guide.ghost(logical), base, side, world, 1, 0, light, appear * 0.28f, false);
            }
        }
        if (buffers instanceof MultiBufferSource.BufferSource source) source.endBatch();
    }

    private static void drawRim(PoseStack pose, MultiBufferSource buffers, Vec3 at, int r, int g, int b, float alpha) {
        double h = TableGrid.CELL / 2 - 0.009, t = 0.004;
        VertexConsumer out = buffers.getBuffer(RenderType.lightning());
        Matrix4f m = pose.last().pose();
        strip(out, m, at, -h, -h, h, -h + t, r, g, b, alpha);
        strip(out, m, at, -h, h - t, h, h, r, g, b, alpha);
        strip(out, m, at, -h, -h, -h + t, h, r, g, b, alpha);
        strip(out, m, at, h - t, -h, h, h, r, g, b, alpha);
    }

    private static void strip(VertexConsumer out, Matrix4f m, Vec3 at, double x0, double z0, double x1, double z1, int r, int g, int b, float a) {
        int alpha = Math.round(a * 255);
        //? if >=1.21 {
        out.addVertex(m, (float) (at.x + x0), (float) at.y, (float) (at.z + z0)).setColor(r, g, b, alpha);
        out.addVertex(m, (float) (at.x + x0), (float) at.y, (float) (at.z + z1)).setColor(r, g, b, alpha);
        out.addVertex(m, (float) (at.x + x1), (float) at.y, (float) (at.z + z1)).setColor(r, g, b, alpha);
        out.addVertex(m, (float) (at.x + x1), (float) at.y, (float) (at.z + z0)).setColor(r, g, b, alpha);
        //?} else if <1.21 {
        /*out.addVertex(m, (float) (at.x + x0), (float) at.y, (float) (at.z + z0)).color(r, g, b, alpha).endVertex();
        out.addVertex(m, (float) (at.x + x0), (float) at.y, (float) (at.z + z1)).color(r, g, b, alpha).endVertex();
        out.addVertex(m, (float) (at.x + x1), (float) at.y, (float) (at.z + z1)).color(r, g, b, alpha).endVertex();
        out.addVertex(m, (float) (at.x + x1), (float) at.y, (float) (at.z + z0)).color(r, g, b, alpha).endVertex();
        *///?}
    }
}
