package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.ClientBridge;
import dev.dyslexic.cinematic.client.compat.FirstPersonBodyCompat;
import dev.dyslexic.cinematic.net.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class CinematicClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CinematicPipelines.initialize();
        ClientBridge.useStation = Scene::begin;
        ClientBridge.vanillaUse = ClientOnly::onVanillaUse;
        Settings.SPEC.load();
        FirstPersonBodyCompat.register();
        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((mc, screen, width, height) -> ServerListHint.onScreenInit(mc, screen));
        KeyMappingHelper.registerKeyMapping(CampfireHint.SIT);
        KeyMappingHelper.registerKeyMapping(FreeLook.LOOK);
        ModelLoadingPlugin.register(context -> {
            context.modifyBlockModelAfterBake().register((model, ctx) -> StationModels.wrap(model, ctx.state()));
        });
        //? if >=26.2 {
        BlockEntityRendererRegistry.register(net.minecraft.world.level.block.entity.BlockEntityTypes.BREWING_STAND, BrewingBottles::new);
        BlockEntityRendererRegistry.register(net.minecraft.world.level.block.entity.BlockEntityTypes.CHISELED_BOOKSHELF, ShelfLayoutBooks::new);
        net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry.register(context -> new LoomFlags.Renderer());
        //?} else if >=26.1 <26.2 {
        /*BlockEntityRendererRegistry.register(net.minecraft.world.level.block.entity.BlockEntityType.BREWING_STAND, BrewingBottles::new);
        BlockEntityRendererRegistry.register(net.minecraft.world.level.block.entity.BlockEntityType.CHISELED_BOOKSHELF, ShelfLayoutBooks::new);
        net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry.register(context -> new LoomFlags.Renderer(context.bufferSource(), context.submitNodeCollector()));
        *///?} else if <26.1 {
        /*BlockEntityRendererRegistry.register(net.minecraft.world.level.block.entity.BlockEntityType.BREWING_STAND, BrewingBottles::new);
        BlockEntityRendererRegistry.register(net.minecraft.world.level.block.entity.BlockEntityType.CHISELED_BOOKSHELF, ShelfLayoutBooks::new);
        net.fabricmc.fabric.api.client.rendering.v1.SpecialGuiElementRegistry.register(context -> new LoomFlags.Renderer(context.vertexConsumers()));
        *///?}
        ClientPlayNetworking.registerGlobalReceiver(RecipeSyncPayload.TYPE, (p, c) -> ClientRecipes.accept(p));
        ClientPlayNetworking.registerGlobalReceiver(ProtocolPayload.TYPE, (p, c) -> {});
        ClientPlayNetworking.registerGlobalReceiver(RecipeChoicePayload.TYPE, (p, c) -> ClientPayloads.recipeChoices(p));
        ClientPlayNetworking.registerGlobalReceiver(BrewingContentsPayload.TYPE, (p, c) -> ClientBrewing.apply(p));
        ClientPlayNetworking.registerGlobalReceiver(TableContentsPayload.TYPE, (p, c) -> ClientPayloads.tableContents(p));
        ClientPlayNetworking.registerGlobalReceiver(FurnaceContentsPayload.TYPE, (p, c) -> ClientPayloads.furnaceContents(p));
        ClientPlayNetworking.registerGlobalReceiver(FurnaceStatePayload.TYPE, (p, c) -> ClientPayloads.furnaceState(p));
        ClientPlayNetworking.registerGlobalReceiver(CampfireProgressPayload.TYPE, (p, c) -> ClientPayloads.campfireProgress(p));
        ClientPlayNetworking.registerGlobalReceiver(EnchantDisplayPayload.TYPE, (p, c) -> ClientPayloads.enchantDisplay(p));
        ClientPlayNetworking.registerGlobalReceiver(EnchantFxPayload.TYPE, (p, c) -> ClientPayloads.enchantFx(p));
        ClientPlayNetworking.registerGlobalReceiver(MenuDisplayPayload.TYPE, (p, c) -> ClientPayloads.menuDisplay(p));
        ClientPlayNetworking.registerGlobalReceiver(MenuFxPayload.TYPE, (p, c) -> ClientPayloads.menuFx(p));
        ClientPlayNetworking.registerGlobalReceiver(SessionClosePayload.TYPE, (p, c) -> ClientPayloads.close(p));
        ClientPlayNetworking.registerGlobalReceiver(AtStationPayload.TYPE, (p, c) -> ClientPayloads.atStation(p));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            Scene.tick();
            ClientPayloads.faceStations(mc.level);
            CampfireHint.tick();
            if (mc.level != null && mc.level.getGameTime() % 40 == 0) ClientFurnaces.prune(mc.level);
            Showcase.tick();
            ClientOnlySmoke.tick();
        });
        // World geometry joins the vanilla feature queue; overlay extraction follows the ordinary HUD layers.
        HudElementRegistry.addLast(dev.dyslexic.cinematic.Cinematic.id("overlay"), (g, ticks) -> Overlay.render(g));
        ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> {
            ClientTables.unloadChunk(chunk.getPos());
            ClientFurnaces.unloadChunk(chunk.getPos());
            ClientBrewing.unloadChunk(chunk.getPos());
            ClientEnchants.unloadChunk(chunk.getPos());
            ClientMenus.unloadChunk(chunk.getPos());
            ShelfBooks.unloadChunk(chunk.getPos());
            ShelfLayoutBooks.unloadChunk(chunk.getPos());
        });
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof Player) {
                ClientPayloads.AT_STATION.remove(entity.getId());
                ClientMenus.forgetEntity(entity.getId());
                ShelfBooks.forgetEntity(entity.getId());
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> clear());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> ClientOnly.onLogin());
    }

    public static boolean screenOpening(Screen next) {
        if (!hides(next)) return false;
        // The window never opens, so it must not hear about its menu either: a mod's screen that listens from its
        // constructor (Scribble's lectern) would otherwise read pages it only builds when it opens.
        if (next instanceof MenuAccess<?> window && next instanceof net.minecraft.world.inventory.ContainerListener listener) {
            window.getMenu().removeSlotListener(listener);
        }
        return true;
    }

    private static boolean hides(Screen next) {
        if (!Scene.active() && ClientOnly.onScreenOpening(next)) return true;
        if (!Scene.active() || next instanceof SceneScreen) return false;
        var player = Minecraft.getInstance().player;
        if (next instanceof MenuAccess<?> window && Scene.station.kind().usesMenu() && player != null && window.getMenu() == player.containerMenu) return true;
        if (next instanceof InventoryScreen) return true;
        // a recipe viewer's page opened from the field guide holds the scene beneath it until it hands the screen back
        if (Scene.phase == Scene.Phase.ACTIVE && next != null && !next.isPauseScreen() && !Scene.consulting) Scene.drop();
        return false;
    }

    public static void clear() {
        Scene.drop();
        ClientOnly.clear();
        ClientRecipes.clear();
        ClientTables.clear();
        ClientFurnaces.clear();
        ClientBrewing.clear();
        ClientEnchants.clear();
        ClientMenus.clear();
        ShelfBooks.clear();
        ShelfLayoutBooks.clear();
        Fx.clearSparks();
        ClientPayloads.clear();
    }
}
