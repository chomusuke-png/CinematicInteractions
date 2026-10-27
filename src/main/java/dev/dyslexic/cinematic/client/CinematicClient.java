package dev.dyslexic.cinematic.client;

//? if neoforge {
import dev.dyslexic.cinematic.Cinematic;
//?}
import dev.dyslexic.cinematic.ClientBridge;
//? if fabric {
/*import dev.dyslexic.cinematic.client.compat.FirstPersonBodyCompat;
import dev.dyslexic.cinematic.net.AtStationPayload;
import dev.dyslexic.cinematic.net.BrewingContentsPayload;
import dev.dyslexic.cinematic.net.CampfireProgressPayload;
import dev.dyslexic.cinematic.net.EnchantDisplayPayload;
import dev.dyslexic.cinematic.net.EnchantFxPayload;
import dev.dyslexic.cinematic.net.FurnaceContentsPayload;
import dev.dyslexic.cinematic.net.FurnaceStatePayload;
import dev.dyslexic.cinematic.net.MenuDisplayPayload;
import dev.dyslexic.cinematic.net.MenuFxPayload;
import dev.dyslexic.cinematic.net.ProtocolPayload;
import dev.dyslexic.cinematic.net.SessionClosePayload;
import dev.dyslexic.cinematic.net.TableContentsPayload;
*///?} else if forge {
/*import dev.dyslexic.cinematic.client.compat.FirstPersonBodyCompat;
*///?}
import dev.dyslexic.cinematic.station.StationKind;
import java.util.HashSet;
import java.util.Set;
//? if fabric {
/*import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
*///?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
//? if !neoforge {
/*import net.minecraft.client.gui.screens.inventory.InventoryScreen;
*///?}
import net.minecraft.client.gui.screens.inventory.MenuAccess;
//? if neoforge {
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
//?}
import net.minecraft.client.player.LocalPlayer;
//? if neoforge {
import net.minecraft.world.InteractionHand;
//?} else if fabric {
/*import net.minecraft.client.renderer.entity.player.PlayerRenderer;
*///?} else if forge {
/*import net.minecraft.world.InteractionHand;
*///?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
//? if neoforge {
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
//?} else if fabric {

/*/^*
 * The client side, as NeoForge's CinematicClient: the events it listens to are Fabric API callbacks here, and where
 * Fabric API has none, a mixin calls the same handler from the same place in the game (named on each method).
 ^/
public final class CinematicClient implements ClientModInitializer {
    /^* How far a sitting player is lowered: hips just above the ground. ^/
    private static final double SITTING_DROP = 0.62;
    private static final Set<Integer> SEATED = new HashSet<>();
*///?} else if forge {
/*import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/^* The client half of the mod, set up from {@code Cinematic} only on a client. ^/
public final class CinematicClient {
    private CinematicClient() {}
*///?}

//? if neoforge {
@Mod(value = Cinematic.MODID, dist = Dist.CLIENT)
public final class CinematicClient {
    public CinematicClient(IEventBus modBus, ModContainer container) {
//?} else if fabric {
    /*@Override
    public void onInitializeClient() {
*///?} else if forge {
    /*public static void init(IEventBus modBus) {
*///?}
        ClientBridge.useStation = Scene::begin;
        ClientBridge.vanillaUse = ClientOnly::onVanillaUse;
        //? if neoforge {
        modBus.addListener((ModelEvent.RegisterAdditional event) -> event.register(HammerModel.LOCATION));
        modBus.addListener((RegisterKeyMappingsEvent event) -> {
            event.register(CampfireHint.SIT);
            event.register(FreeLook.LOOK);
        });
        modBus.addListener(BrewingBottles::hideVanillaBottles);
        modBus.addListener(ShelfBooks::emptySlots);
        modBus.addListener(PropLayer::addTo);
        modBus.addListener(KneeLayer::addTo);
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerBlockEntityRenderer(BlockEntityType.BREWING_STAND,
                BrewingBottles::new));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerBlockEntityRenderer(BlockEntityType.CHISELED_BOOKSHELF,
                ShelfLayoutBooks::new));
        container.registerConfig(ModConfig.Type.CLIENT, Settings.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new SettingsScreen(parent));
        NeoForge.EVENT_BUS.register(Events.class);
        dev.dyslexic.cinematic.client.compat.FirstPersonBodyCompat.register();
    }
        //?} else if fabric && >=1.21 {
        /*Settings.SPEC.load();
        FirstPersonBodyCompat.register();
        KeyBindingHelper.registerKeyBinding(CampfireHint.SIT);
        KeyBindingHelper.registerKeyBinding(FreeLook.LOOK);
        ModelLoadingPlugin.register(context -> {
            context.addModels(HammerModel.LOCATION);
            context.modifyModelAfterBake().register((model, ctx) -> StationModels.wrap(model, ctx.topLevelId()));
        });
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (renderer instanceof PlayerRenderer player) {
                helper.register(new PropLayer(player));
                helper.register(new KneeLayer(player));
            }
        });
        BlockEntityRendererRegistry.register(BlockEntityType.BREWING_STAND, BrewingBottles::new);
        BlockEntityRendererRegistry.register(BlockEntityType.CHISELED_BOOKSHELF, ShelfLayoutBooks::new);
        *///?} else if fabric && <1.21 {
        /*Settings.SPEC.load();
        FirstPersonBodyCompat.register();
        KeyBindingHelper.registerKeyBinding(CampfireHint.SIT);
        KeyBindingHelper.registerKeyBinding(FreeLook.LOOK);
        ModelLoadingPlugin.register(context -> {
            context.addModels(HammerModel.LOCATION);
            context.modifyModelAfterBake().register((model, ctx) -> StationModels.wrap(model, ctx.id()));
        });
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (renderer instanceof PlayerRenderer player) {
                helper.register(new PropLayer(player));
                helper.register(new KneeLayer(player));
            }
        });
        BlockEntityRendererRegistry.register(BlockEntityType.BREWING_STAND, BrewingBottles::new);
        BlockEntityRendererRegistry.register(BlockEntityType.CHISELED_BOOKSHELF, ShelfLayoutBooks::new);
        *///?} else if forge {
        /*modBus.addListener((ModelEvent.RegisterAdditional event) -> event.register(HammerModel.LOCATION));
        modBus.addListener((RegisterKeyMappingsEvent event) -> {
            event.register(CampfireHint.SIT);
            event.register(FreeLook.LOOK);
        });
        modBus.addListener(BrewingBottles::hideVanillaBottles);
        modBus.addListener(ShelfBooks::emptySlots);
        modBus.addListener(PropLayer::addTo);
        modBus.addListener(KneeLayer::addTo);
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerBlockEntityRenderer(BlockEntityType.BREWING_STAND,
                BrewingBottles::new));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerBlockEntityRenderer(BlockEntityType.CHISELED_BOOKSHELF,
                ShelfLayoutBooks::new));
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Settings.SPEC);
        modBus.addListener(Settings::onReloading);
        // the settings screen (Forge 1.20.1 has no settings screen of its own to build on)
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new SettingsScreen(parent)));
        MinecraftForge.EVENT_BUS.register(Events.class);
        FirstPersonBodyCompat.register();
    }

    public static final class Events {
        private Events() {}

        /^* How far a sitting player is lowered: hips just above the ground. ^/
        private static final double SITTING_DROP = 0.62;
        private static final Set<Integer> SEATED = new HashSet<>();

        /^* Right before the frame is drawn, after this frame's ticks (where NeoForge sends its frame event). ^/
        @SubscribeEvent
        public static void onFrame(TickEvent.RenderTickEvent event) {
            if (event.phase == TickEvent.Phase.START) {
                Scene.frame();
            }
        }
        *///?}

    //? if neoforge {
    public static final class Events {
        private Events() {}

        /** How far a sitting player is lowered: hips just above the ground. */
        private static final double SITTING_DROP = 0.62;
        private static final Set<Integer> SEATED = new HashSet<>();

        @SubscribeEvent
        public static void onFrame(RenderFrameEvent.Pre event) {
            Scene.frame();
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
    //?} else if fabric && >=1.21 {
        /*ClientPlayNetworking.registerGlobalReceiver(ProtocolPayload.TYPE, (p, c) -> {});
        ClientPlayNetworking.registerGlobalReceiver(TableContentsPayload.TYPE, (p, c) -> ClientPayloads.tableContents(p));
        ClientPlayNetworking.registerGlobalReceiver(FurnaceContentsPayload.TYPE, (p, c) -> ClientPayloads.furnaceContents(p));
        ClientPlayNetworking.registerGlobalReceiver(BrewingContentsPayload.TYPE, (p, c) -> ClientBrewing.apply(p));
        ClientPlayNetworking.registerGlobalReceiver(FurnaceStatePayload.TYPE, (p, c) -> ClientPayloads.furnaceState(p));
        ClientPlayNetworking.registerGlobalReceiver(CampfireProgressPayload.TYPE, (p, c) -> ClientPayloads.campfireProgress(p));
        ClientPlayNetworking.registerGlobalReceiver(EnchantDisplayPayload.TYPE, (p, c) -> ClientPayloads.enchantDisplay(p));
        ClientPlayNetworking.registerGlobalReceiver(EnchantFxPayload.TYPE, (p, c) -> ClientPayloads.enchantFx(p));
        ClientPlayNetworking.registerGlobalReceiver(MenuDisplayPayload.TYPE, (p, c) -> ClientPayloads.menuDisplay(p));
        ClientPlayNetworking.registerGlobalReceiver(MenuFxPayload.TYPE, (p, c) -> ClientPayloads.menuFx(p));
        ClientPlayNetworking.registerGlobalReceiver(SessionClosePayload.TYPE, (p, c) -> ClientPayloads.close(p));
        ClientPlayNetworking.registerGlobalReceiver(AtStationPayload.TYPE, (p, c) -> ClientPayloads.atStation(p));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
    *///?} else if fabric && <1.21 {
        /*ClientPlayNetworking.registerGlobalReceiver(ProtocolPayload.TYPE, (p, player, sender) -> {});
        ClientPlayNetworking.registerGlobalReceiver(TableContentsPayload.TYPE, (p, player, sender) -> ClientPayloads.tableContents(p));
        ClientPlayNetworking.registerGlobalReceiver(FurnaceContentsPayload.TYPE, (p, player, sender) -> ClientPayloads.furnaceContents(p));
        ClientPlayNetworking.registerGlobalReceiver(BrewingContentsPayload.TYPE, (p, player, sender) -> ClientBrewing.apply(p));
        ClientPlayNetworking.registerGlobalReceiver(FurnaceStatePayload.TYPE, (p, player, sender) -> ClientPayloads.furnaceState(p));
        ClientPlayNetworking.registerGlobalReceiver(CampfireProgressPayload.TYPE, (p, player, sender) -> ClientPayloads.campfireProgress(p));
        ClientPlayNetworking.registerGlobalReceiver(EnchantDisplayPayload.TYPE, (p, player, sender) -> ClientPayloads.enchantDisplay(p));
        ClientPlayNetworking.registerGlobalReceiver(EnchantFxPayload.TYPE, (p, player, sender) -> ClientPayloads.enchantFx(p));
        ClientPlayNetworking.registerGlobalReceiver(MenuDisplayPayload.TYPE, (p, player, sender) -> ClientPayloads.menuDisplay(p));
        ClientPlayNetworking.registerGlobalReceiver(MenuFxPayload.TYPE, (p, player, sender) -> ClientPayloads.menuFx(p));
        ClientPlayNetworking.registerGlobalReceiver(SessionClosePayload.TYPE, (p, player, sender) -> ClientPayloads.close(p));
        ClientPlayNetworking.registerGlobalReceiver(AtStationPayload.TYPE, (p, player, sender) -> ClientPayloads.atStation(p));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
    *///?} else if forge {
        /*@SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Settings.tick();
    *///?}
            Scene.tick();
            ClientOnlySmoke.tick();
            ServerSmoke.tick();
            //? if fabric {
            /*ServerHintSmoke.tick();
            *///?} else if forge {
            /*FurnaceBreakSmoke.tick();
            *///?}
            CampfireHint.tick();
            //? if neoforge {
            Minecraft mc = Minecraft.getInstance();
            //?} else if fabric {
            /*ClientPayloads.faceStations(mc.level);
            *///?} else if forge {
            /*Minecraft mc = Minecraft.getInstance();
            *///?}
            if (mc.level != null && mc.level.getGameTime() % 40 == 0) {
                ClientFurnaces.prune(mc.level);
            }
            Showcase.tick();
        //? if neoforge {
        }

        @SubscribeEvent
        public static void onPlayerTick(PlayerTickEvent.Post event) {
            if (event.getEntity() instanceof LocalPlayer player && player == Minecraft.getInstance().player) {
                Scene.steer(player);
        //?} else if fabric {
        /*});
        // NeoForge's RenderLevelStageEvent: the matrices of this frame, and the scene drawn after the entities
        WorldRenderEvents.START.register(Picking::capture);
        WorldRenderEvents.AFTER_ENTITIES.register(SceneRenderer::render);
        // RenderHighlightEvent.Block: no block selection box over a scene
        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, hit) -> !Scene.active());
        HudRenderCallback.EVENT.register((graphics, tick) -> Overlay.render(graphics));
        ScreenEvents.AFTER_INIT.register((mc, screen, width, height) -> {
            ServerListHint.onScreenInit(mc, screen);
            Showcase.onScreen(screen);
            ClientOnlySmoke.onScreen(screen);
            ServerHintSmoke.onScreen(screen);
        });
        ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> {
            ClientTables.unloadChunk(chunk.getPos());
            ClientFurnaces.unloadChunk(chunk.getPos());
            ClientBrewing.unloadChunk(chunk.getPos());
            ClientEnchants.unloadChunk(chunk.getPos());
            ClientMenus.unloadChunk(chunk.getPos());
            ShelfBooks.unloadChunk(chunk.getPos());
        });
        // A player out of sight is forgotten by the client, along with whatever they were doing: if they stepped away
        // from a station meanwhile, nobody tells us, and they would come back still posed at it (sitting in the ground
        // by a campfire that is not there). Coming back into sight, the server says again if they are at one.
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (entity instanceof Player) {
                ClientPayloads.AT_STATION.remove(entity.getId());
                ClientMenus.forgetEntity(entity.getId());
                ShelfBooks.forgetEntity(entity.getId());
        *///?} else if forge {
        /*}

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase == TickEvent.Phase.END && event.player instanceof LocalPlayer player && player == Minecraft.getInstance().player) {
                Scene.steer(player);
            }
        }

        /^* Someone sitting by a campfire is drawn lower, on the ground (their legs are posed by HumanoidModelMixin). ^/
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
            if (!event.isCanceled() && ClientPayloads.atStation(event.getEntity().getId()) == StationKind.CAMPFIRE) {
                event.getPoseStack().pushPose();
                event.getPoseStack().translate(0, -SITTING_DROP, 0);
                SEATED.add(event.getEntity().getId());
            }
        }

        @SubscribeEvent
        public static void onRenderPlayerDone(RenderPlayerEvent.Post event) {
            if (SEATED.remove(event.getEntity().getId())) {
                event.getPoseStack().popPose();
            }
        }

        @SubscribeEvent
        public static void onMovementInput(MovementInputUpdateEvent event) {
            Scene.movementInput(event.getInput());
        }

        @SubscribeEvent
        public static void onInteractKey(InputEvent.InteractionKeyMappingTriggered event) {
            if (Scene.active()) {
                event.setCanceled(true);
                event.setSwingHand(false);
            } else if (event.isUseItem() && event.getHand() == InteractionHand.MAIN_HAND
                    && Minecraft.getInstance().hitResult instanceof BlockHitResult hit && ClientOnly.suppressUseSwing(hit)) {
                event.setSwingHand(false);
            }
        }

        @SubscribeEvent
        public static void onScroll(InputEvent.MouseScrollingEvent event) {
            if (Scene.active() && Minecraft.getInstance().screen == null) {
                event.setCanceled(true);
        *///?}
            }
        //? if neoforge {
        }
        //?} else if fabric {
        /*});
        ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> ClientOnly.onLogin());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> onLogout());
    }
        *///?} else if forge {
        /*}
        *///?}

        //? if neoforge {
        /** Someone sitting by a campfire is drawn lower, on the ground (their legs are posed by HumanoidModelMixin). */
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
            if (!event.isCanceled() && ClientPayloads.atStation(event.getEntity().getId()) == StationKind.CAMPFIRE) {
                event.getPoseStack().pushPose();
                event.getPoseStack().translate(0, -SITTING_DROP, 0);
                SEATED.add(event.getEntity().getId());
            }
        }
        //?} else if fabric {
    /*/^*
     * The settings screen (NeoForge registers it as the mod's config screen; on Fabric, Mod Menu opens it).
     ^/
    public static Screen settingsScreen(Screen parent) {
        return new SettingsScreen(parent);
    }
        *///?} else if forge {
        /*@SubscribeEvent
        public static void onRenderLevel(RenderLevelStageEvent event) {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
                Picking.capture(event);
            } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
                SceneRenderer.render(event);
            }
        }
        *///?}

        //? if neoforge {
        @SubscribeEvent
        public static void onRenderPlayerDone(RenderPlayerEvent.Post event) {
            if (SEATED.remove(event.getEntity().getId())) {
                event.getPoseStack().popPose();
            }
        //?} else if fabric {
    /*/^* NeoForge's PlayerTickEvent.Post for the local player (LocalPlayerMixin, right after Player.tick). ^/
    public static void onPlayerTick(LocalPlayer player) {
        if (player == Minecraft.getInstance().player) {
            Scene.steer(player);
        *///?} else if forge {
        /*@SubscribeEvent
        public static void onFov(ViewportEvent.ComputeFov event) {
            if (Scene.active() && event.usedConfiguredFov()) {
                event.setFOV(CameraRig.fov(event.getFOV()));
            }
        *///?}
        }
    //? if fabric {
    /*}
    *///?}

        //? if neoforge {
        @SubscribeEvent
        public static void onMovementInput(MovementInputUpdateEvent event) {
            Scene.movementInput(event.getInput());
        //?} else if fabric {
    /*/^* Someone sitting by a campfire is drawn lower, on the ground (their legs are posed by HumanoidModelMixin). ^/
    public static boolean seat(Player player) {
        if (ClientPayloads.atStation(player.getId()) == StationKind.CAMPFIRE) {
            SEATED.add(player.getId());
            return true;
        *///?} else if forge {
        /*/^*
         * The lean's roll. In 1.20.1 the camera itself knows no roll (CameraMixin turns its own billboard vectors with it):
         * Forge rolls the view here, right after the camera was set up.
         ^/
        @SubscribeEvent
        public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
            float roll = CameraRig.roll();
            if (roll != 0) {
                event.setRoll(event.getRoll() + roll);
            }
        *///?}
        }
        //? if fabric {
        /*return false;
    }
        *///?}

        //? if neoforge {
        @SubscribeEvent
        public static void onTurn(CalculatePlayerTurnEvent event) {
            if (Scene.active()) {
                // zero mouse look while the scene drives the view
                event.setMouseSensitivity(-1.0 / 3.0);
                event.setCinematicCameraEnabled(false);
            }
        }
        //?} else if fabric {
    /*/^* True when {@link #seat} lowered this player and the pose has to be put back. ^/
    public static boolean unseat(Player player) {
        return SEATED.remove(player.getId());
    }
        *///?} else if forge {
        /*@SubscribeEvent
        public static void onRenderHand(RenderHandEvent event) {
            if (Scene.active()) {
                event.setCanceled(true);
            }
        }
        *///?}

        //? if neoforge {
        @SubscribeEvent
        public static void onInteractKey(InputEvent.InteractionKeyMappingTriggered event) {
            if (Scene.active()) {
                event.setCanceled(true);
                event.setSwingHand(false);
            } else if (event.isUseItem() && event.getHand() == InteractionHand.MAIN_HAND
                    && Minecraft.getInstance().hitResult instanceof BlockHitResult hit && ClientOnly.suppressUseSwing(hit)) {
                event.setSwingHand(false);
            }
        }
        //?} else if fabric {
    /*public static double sittingDrop() {
        return SITTING_DROP;
    }
        *///?} else if forge {
        /*@SubscribeEvent
        public static void onHighlight(RenderHighlightEvent.Block event) {
            if (Scene.active()) {
                event.setCanceled(true);
            }
        }
        *///?}

        //? if neoforge {
        @SubscribeEvent
        public static void onScroll(InputEvent.MouseScrollingEvent event) {
            if (Scene.active() && Minecraft.getInstance().screen == null) {
                event.setCanceled(true);
            }
        //?} else if fabric {
    /*/^*
     * NeoForge's ScreenEvent.Opening (MinecraftMixin, at the start of setScreen; as there, only for a screen opening, not
     * for one closing). True cancels the new screen.
     ^/
    public static boolean onScreenOpening(Screen next) {
        if (!Scene.active() && ClientOnly.onScreenOpening(next)) {
            return true;
        *///?} else if forge {
        /*@SubscribeEvent
        public static void onGuiLayer(RenderGuiOverlayEvent.Pre event) {
            if (Overlay.hides(event.getOverlay().id())) {
                event.setCanceled(true);
            }
        *///?}
        }
        //? if neoforge {

        @SubscribeEvent
        public static void onRenderLevel(RenderLevelStageEvent event) {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
                Picking.capture(event);
            } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
                SceneRenderer.render(event);
            }
        //?} else if fabric {
        /*if (!Scene.active() || next instanceof SceneScreen) {
            return false;
        *///?} else if forge {

        /*@SubscribeEvent
        public static void onGui(RenderGuiEvent.Post event) {
            Overlay.render(event.getGuiGraphics());
        *///?}
        }
        //? if neoforge {

        @SubscribeEvent
        public static void onFov(ViewportEvent.ComputeFov event) {
            if (Scene.active() && event.usedConfiguredFov()) {
                event.setFOV(CameraRig.fov(event.getFOV()));
            }
        }

        @SubscribeEvent
        public static void onRenderHand(RenderHandEvent event) {
            if (Scene.active()) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onHighlight(RenderHighlightEvent.Block event) {
            if (Scene.active()) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onGuiLayer(RenderGuiLayerEvent.Pre event) {
            if (Overlay.hides(event.getName())) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onGui(RenderGuiEvent.Post event) {
            Overlay.render(event.getGuiGraphics());
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onScreenOpening(ScreenEvent.Opening event) {
            screenOpening(event);
            if (event.isCanceled()) HiddenWindows.keptShut(event.getNewScreen());
        }

        private static void screenOpening(ScreenEvent.Opening event) {
            Screen next = event.getNewScreen();
            if (!Scene.active() && ClientOnly.onScreenOpening(next)) {
                event.setCanceled(true);
                return;
            }
            if (!Scene.active() || next instanceof SceneScreen) {
                return;
            }
            LocalPlayer player = Minecraft.getInstance().player;
            // (the lectern's window is a book screen, not a container screen, but it holds the menu all the same; while
            // the server has not opened the block's menu yet, the player's own inventory is no menu of the block's)
            if (next instanceof MenuAccess<?> window && Scene.station.kind().usesMenu() && player != null
                    && player.containerMenu != player.inventoryMenu && window.getMenu() == player.containerMenu) {
                var addon = Scene.station.kind() == dev.dyslexic.cinematic.station.StationKind.ADDON
                        ? dev.dyslexic.cinematic.api.AddonStations.forBlock(player.level().getBlockState(Scene.station.pos)) : null;
                boolean expected = addon != null ? addon.accepts(window.getMenu())
                        : dev.dyslexic.cinematic.station.MenuActions.isOpen(Scene.station.kind(), window.getMenu());
                if (expected) {
                    // the server opened the block's menu for us: keep the menu, the scene stands in for its window
                    event.setCanceled(true);
                } else {
                    // the block opened a menu the scene does not know (a mod replaced it, as Penchant does the
                    // enchanting table's): its own window must open, or the window built for it is left half-made
                    // and the mod's menu calls into it on the next slot change
                    Scene.yieldToWindow();
                }
                return;
        //?} else if fabric {
        /*LocalPlayer player = Minecraft.getInstance().player;
        // A menu the server has just opened (MenuScreens set it as the player's menu before the screen). Never the
        // player's own inventory: once stepping away, a station's menu has gone back to it, and the inventory opened
        // then is no menu of the block's.
        // (the lectern's window is a book screen, not a container screen, but it holds the menu all the same)
        if (next instanceof MenuAccess<?> window && player != null && window.getMenu() != player.inventoryMenu
                && window.getMenu() == player.containerMenu) {
            switch (Scene.answer(window.getMenu())) {
                case OURS -> {
                    // the server opened the block's menu for us: keep the menu, the scene stands in for its window
                    return true;
                }
                case LATE -> {
                    // the answer to a BEGIN of a station already let go of; the server closes that menu on the LEAVE
                    // it got after it, so no window opens and the menu goes quietly here too
                    player.containerMenu = player.inventoryMenu;
                    return true;
                }
                case FOREIGN -> {
                    // the block opened a menu the scene does not know (a mod replaced it, as Penchant does the
                    // enchanting table's on NeoForge): its own window must open, or the window built for it is left
                    // half-made and the mod's menu calls into it on the next slot change
                    Scene.yieldToWindow();
                    return false;
                }
                case UNRELATED -> {}
        *///?} else if forge {

        /*@SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onScreenOpening(ScreenEvent.Opening event) {
            screenOpening(event);
            if (event.isCanceled()) HiddenWindows.keptShut(event.getNewScreen());
        }

        private static void screenOpening(ScreenEvent.Opening event) {
            Screen next = event.getNewScreen();
            if (!Scene.active() && ClientOnly.onScreenOpening(next)) {
                event.setCanceled(true);
                return;
            }
            if (!Scene.active() || next instanceof SceneScreen) {
                return;
            }
            LocalPlayer player = Minecraft.getInstance().player;
            // (the lectern's window is a book screen, not a container screen, but it holds the menu all the same)
            if (next instanceof MenuAccess<?> window && player != null
                    && window.getMenu() == player.containerMenu && window.getMenu() != player.inventoryMenu) {
                switch (Scene.answer(window.getMenu())) {
                    case OURS -> { event.setCanceled(true); return; }
                    case LATE -> {
                        event.setCanceled(true);
                        player.containerMenu = player.inventoryMenu;
                        return;
                    }
                    case FOREIGN -> { Scene.yieldToWindow(); return; }
                    case UNRELATED -> {}
                }
            }
            if (next instanceof InventoryScreen) {
                if (Scene.phase != Scene.Phase.OUTRO) event.setCanceled(true);
            } else if (Scene.phase == Scene.Phase.ACTIVE && next != null && !next.isPauseScreen() && !Scene.consulting) {
                // something else took over the screen (a server menu, another mod): drop the scene at once
                Scene.drop();
            }
        }

        @SubscribeEvent
        public static void onScreenInit(ScreenEvent.Init.Post event) {
            Showcase.onScreen(event.getScreen());
            ClientOnlySmoke.onScreen(event.getScreen());
            ServerListHint.onScreenInit(event);
        }

        @SubscribeEvent
        public static void onChunkUnload(ChunkEvent.Unload event) {
            if (event.getLevel().isClientSide()) {
                ClientTables.unloadChunk(event.getChunk().getPos());
                ClientFurnaces.unloadChunk(event.getChunk().getPos());
                ClientBrewing.unloadChunk(event.getChunk().getPos());
                ClientEnchants.unloadChunk(event.getChunk().getPos());
                ClientMenus.unloadChunk(event.getChunk().getPos());
                ShelfBooks.unloadChunk(event.getChunk().getPos());
        *///?}
            }
            //? if neoforge {
            if (next instanceof InventoryScreen) {
                event.setCanceled(true);
            } else if (Scene.phase == Scene.Phase.ACTIVE && next != null && !next.isPauseScreen() && !Scene.consulting) {
                // something else took over the screen (a server menu, another mod): drop the scene at once
                Scene.drop();
            }
            //?}
        }
        //? if neoforge {

        @SubscribeEvent
        public static void onScreenInit(ScreenEvent.Init.Post event) {
            Showcase.onScreen(event.getScreen());
            ClientOnlySmoke.onScreen(event.getScreen());
            ServerListHint.onScreenInit(event);
        //?} else if fabric {
        /*if (next instanceof InventoryScreen) {
            // while walking up and working, E steps away instead; once stepping away, the inventory opens over it and
            // the step away goes on behind it
            return Scene.phase != Scene.Phase.OUTRO;
        *///?} else if forge {

        /*/^*
         * A player out of sight is forgotten by the client, along with whatever they were doing: if they stepped away
         * from a station meanwhile, nobody tells us, and they would come back still posed at it (sitting in the ground
         * by a campfire that is not there). Coming back into sight, the server says again if they are at one.
         ^/
        @SubscribeEvent
        public static void onEntityLeave(EntityLeaveLevelEvent event) {
            if (event.getLevel().isClientSide() && event.getEntity() instanceof Player) {
                ClientPayloads.AT_STATION.remove(event.getEntity().getId());
                ClientMenus.forgetEntity(event.getEntity().getId());
                ShelfBooks.forgetEntity(event.getEntity().getId());
            }
        *///?}
        }
        //? if neoforge {

        @SubscribeEvent
        public static void onChunkUnload(ChunkEvent.Unload event) {
            if (event.getLevel().isClientSide()) {
                ClientTables.unloadChunk(event.getChunk().getPos());
                ClientFurnaces.unloadChunk(event.getChunk().getPos());
                ClientBrewing.unloadChunk(event.getChunk().getPos());
                ClientEnchants.unloadChunk(event.getChunk().getPos());
                ClientMenus.unloadChunk(event.getChunk().getPos());
                ShelfBooks.unloadChunk(event.getChunk().getPos());
            }
        //?} else if fabric {
        /*if (Scene.phase == Scene.Phase.ACTIVE && !next.isPauseScreen() && !Scene.consulting) {
            // something else took over the screen (a server menu, another mod): drop the scene at once
            Scene.drop();
        *///?} else if forge {

        /*@SubscribeEvent
        public static void onLevelUnload(LevelEvent.Unload event) {
            if (event.getLevel().isClientSide()) {
                Scene.drop();
                ClientTables.clear();
                ClientFurnaces.clear();
                ClientBrewing.clear();
                ClientEnchants.clear();
                ClientMenus.clear();
                ShelfBooks.clear();
                Fx.clearSparks();
                ClientPayloads.clear();
            }
        *///?}
        }
        //? if fabric {
        /*return false;
    }
        *///?}

        //? if neoforge {
        /**
         * A player out of sight is forgotten by the client, along with whatever they were doing: if they stepped away
         * from a station meanwhile, nobody tells us, and they would come back still posed at it (sitting in the ground
         * by a campfire that is not there). Coming back into sight, the server says again if they are at one.
         */
        @SubscribeEvent
        public static void onEntityLeave(EntityLeaveLevelEvent event) {
            if (event.getLevel().isClientSide() && event.getEntity() instanceof Player) {
                ClientPayloads.AT_STATION.remove(event.getEntity().getId());
                ClientMenus.forgetEntity(event.getEntity().getId());
                ShelfBooks.forgetEntity(event.getEntity().getId());
            }
        }
        //?} else if fabric {
    /*/^* NeoForge's LevelEvent.Unload on the client (MinecraftMixin: a new level replaces it, or the player leaves it). ^/
    public static void onLevelUnload() {
        Scene.drop();
        ClientTables.clear();
        ClientFurnaces.clear();
        ClientBrewing.clear();
        ClientEnchants.clear();
        ClientMenus.clear();
        ShelfBooks.clear();
        Fx.clearSparks();
        ClientPayloads.clear();
    }
        *///?} else if forge {
        /*@SubscribeEvent
        public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
            ClientOnly.onLogin();
        }
        *///?}

        //? if neoforge {
        @SubscribeEvent
        public static void onLevelUnload(LevelEvent.Unload event) {
            if (event.getLevel().isClientSide()) {
                Scene.drop();
                ClientTables.clear();
                ClientFurnaces.clear();
                ClientBrewing.clear();
                ClientEnchants.clear();
                ClientMenus.clear();
                ShelfBooks.clear();
                Fx.clearSparks();
                ClientPayloads.clear();
            }
        }

        @SubscribeEvent
        public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
            ClientOnly.onLogin();
        }

        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            Scene.drop();
            Scene.forgetOwnWindows();
            ClientOnly.clear();
            ClientTables.clear();
            ClientFurnaces.clear();
            ClientBrewing.clear();
            ClientEnchants.clear();
            ClientMenus.clear();
            ShelfBooks.clear();
            ClientPayloads.clear();
        }
        //?} else if fabric {
    /*private static void onLogout() {
        Scene.drop();
        Scene.forgetOwnWindows();
        ClientOnly.clear();
        ClientTables.clear();
        ClientFurnaces.clear();
        ClientBrewing.clear();
        ClientEnchants.clear();
        ClientMenus.clear();
        ShelfBooks.clear();
        ClientPayloads.clear();
        *///?} else if forge {
        /*@SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            Scene.drop();
            Scene.forgetOwnWindows();
            ClientOnly.clear();
            ClientTables.clear();
            ClientFurnaces.clear();
            ClientBrewing.clear();
            ClientEnchants.clear();
            ClientMenus.clear();
            ShelfBooks.clear();
            ClientPayloads.clear();
        }
        *///?}
    }
}
