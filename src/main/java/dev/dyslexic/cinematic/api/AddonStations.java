package dev.dyslexic.cinematic.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Registers menu-backed blocks whose owners provide a cinematic scene. Registration must run on both physical sides
 * before a world is joined. The block's real menu remains responsible for items, recipes, costs and rewards.
 */
public final class AddonStations {
    private AddonStations() {}

    /**
     * Opens the block's menu for a player, on the server, as using the block would. Only needed when the menu takes
     * more than the vanilla {@code openMenu} sends (extra data for its client copy, a menu kept by another block of
     * a multi-block); without one, the block's {@code MenuProvider} is opened.
     */
    @FunctionalInterface
    public interface MenuOpener {
        void open(ServerPlayer player, BlockPos pos, BlockState state);
    }

    /** {@code opener} may be null: the block's own {@code MenuProvider} is opened then. */
    public record Definition(ResourceLocation id, ResourceLocation blockId,
                             Class<? extends AbstractContainerMenu> menuClass,
                             Function<BlockState, List<Direction>> sides,
                             @Nullable MenuOpener opener) {
        public Definition {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(blockId, "blockId");
            Objects.requireNonNull(menuClass, "menuClass");
            Objects.requireNonNull(sides, "sides");
        }

        public Definition(ResourceLocation id, ResourceLocation blockId, Class<? extends AbstractContainerMenu> menuClass,
                          Function<BlockState, List<Direction>> sides) {
            this(id, blockId, menuClass, sides, null);
        }

        public boolean accepts(AbstractContainerMenu menu) {
            return menuClass.isInstance(menu);
        }

        public List<Direction> usableSides(BlockState state) {
            List<Direction> result = List.copyOf(sides.apply(state));
            if (result.stream().anyMatch(side -> side == null || !side.getAxis().isHorizontal())) {
                throw new IllegalStateException("Addon station " + id + " must provide horizontal working sides");
            }
            return result;
        }
    }

    private static final Map<ResourceLocation, Definition> BY_ID = new HashMap<>();
    private static final Map<ResourceLocation, Definition> BY_BLOCK = new HashMap<>();

    /** A block may belong to only one add-on station. Call during mod setup, before joining a world. */
    public static synchronized void register(Definition definition) {
        Objects.requireNonNull(definition, "definition");
        if ("minecraft".equals(definition.blockId().getNamespace()) || BY_ID.containsKey(definition.id())
                || BY_BLOCK.containsKey(definition.blockId())) {
            throw new IllegalArgumentException("Duplicate or vanilla cinematic add-on station: " + definition.id());
        }
        BY_ID.put(definition.id(), definition);
        BY_BLOCK.put(definition.blockId(), definition);
    }

    @Nullable
    public static synchronized Definition byId(ResourceLocation id) {
        return BY_ID.get(id);
    }

    @Nullable
    public static synchronized Definition forBlock(BlockState state) {
        return BY_BLOCK.get(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }
}
