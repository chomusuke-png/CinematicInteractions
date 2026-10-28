plugins {
    `java-library`
    // ModDevGradle's plugin for Forge 1.17–1.20.1: Parchment names in development, SRG names (and a mixin refmap) in the jar.
    id("net.neoforged.moddev.legacyforge") version "2.0.148"
}

fun prop(name: String) = property(name).toString()
val modId = prop("mod.id")
val javaVersion = prop("mod.java").toInt()

version = prop("mod.version")
group = prop("mod.group")
base.archivesName = prop("mod.archive")
java.toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)

legacyForge {
    enable {
        forgeVersion = "${sc.current.version}-${prop("deps.forge")}"
        // On CI MDG skips recompiling Minecraft, which names every constructor parameter "o" and breaks our
        // anonymous RenderType/Button subclasses ("variable o is already defined"): always recompile.
        isDisableRecompilation = false
    }
    // this loader's access transformer is outside the default src/main/resources
    accessTransformers.from(rootProject.file("src/forge/resources/META-INF/accesstransformer.cfg"))

    parchment {
        mappingsVersion = prop("deps.parchment")
        minecraftVersion = prop("deps.parchment_minecraft")
    }

    runs {
        register("client") { client() }
        register("server") {
            server()
            programArgument("--nogui")
        }
        // Development-only scripted walkthrough of every station; screenshots go to run-showcase/screenshots.
        register("showcase") {
            client()
            gameDirectory = file(findProperty("showcaseDir")?.toString() ?: "run-showcase")
            showcaseProperties()
        }
        // Same walkthrough with Embeddium + Oculus and a shader pack (jars dropped into run-showcase-iris/mods).
        register("showcaseIris") {
            client()
            gameDirectory = file(findProperty("showcaseIrisDir")?.toString() ?: "run-showcase-iris")
            showcaseProperties()
        }
        // A real vanilla dedicated server on 127.0.0.1:25586, with no Cinematic Interactions installed.
        register("clientOnlySmoke") {
            client()
            gameDirectory = file(findProperty("clientOnlySmokeDir")?.toString() ?: "run-showcase")
            systemProperty("cinematic.clientOnlySmoke", "true")
            programArguments.addAll("--width", "1280", "--height", "720")
        }
        if (hasProperty("joinTest")) {
            // A real network join (packets are encoded, unlike singleplayer): run joinServer, then joinClient.
            register("joinServer") {
                server()
                gameDirectory = file("run-join-server")
                programArgument("--nogui")
            }
            register("joinClient") {
                client()
                gameDirectory = file(findProperty("joinClientDir")?.toString() ?: "run-join-client")
                programArguments.addAll("--quickPlayMultiplayer", findProperty("joinAddress")?.toString() ?: "127.0.0.1:25589")
                // -PserverSmoke: shift-clicks into stations over the real connection (client/ServerSmoke)
                if (hasProperty("serverSmoke")) systemProperty("cinematic.serverSmoke", "true")
                // -PserverSmokeReconnect: after a server restart, the table's contents must come back from the saved world
                if (hasProperty("serverSmokeReconnect")) {
                    systemProperty("cinematic.serverSmoke", "true")
                    systemProperty("cinematic.serverSmoke.reconnect", "true")
                }
            }
            // A Forge 1.20.1 server WITHOUT Cinematic Interactions (other mods may be added with the test-mod switches):
            // a client with the mod must join it, keep vanilla windows (or the client-only scenes) and send nothing of ours.
            register("plainServer") {
                server()
                gameDirectory = file("run-plain-server")
                // set, not add: the mixin block puts --mixin.config for our (here absent) mod on every run
                programArguments.set(listOf("--nogui"))
                loadedMods.set(emptyList())
            }
        }
        configureEach {
            logLevel = org.slf4j.event.Level.INFO
            if (hasProperty("traceMixins")) systemProperty("mixin.debug.export", "true")
        }
    }

    mods {
        register(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

fun net.neoforged.moddevgradle.dsl.RunModel.showcaseProperties() {
    systemProperty("cinematic.showcase", "true")
    systemProperty("cinematic.showcase.world", findProperty("showcaseWorld")?.toString() ?: "cinematic-showcase")
    systemProperty("cinematic.showcase.from", findProperty("from")?.toString() ?: "0")
    systemProperty("cinematic.showcase.only", findProperty("only")?.toString() ?: "false")
    systemProperty("cinematic.showcase.editorBatch", findProperty("editorBatch")?.toString() ?: "false")
    programArguments.addAll("--width", "1600", "--height", "900")
}

mixin {
    add(sourceSets.main.get(), "$modId.refmap.json")
    config("$modId.mixins.json")
}

// Forge 1.20.1 finds a mod's mixins only through this manifest attribute (MDG passes the config to its own runs alone)
tasks.named<Jar>("jar") {
    manifest.attributes(mapOf("MixinConfigs" to "$modId.mixins.json"))
}

repositories {
    maven("https://api.modrinth.com/maven") { content { includeGroup("maven.modrinth") } }
    // Local test mods for the development runs (test-mods/1.20.1-forge, not in git): none is bundled.
    flatDir { dirs(rootProject.file("test-mods/1.20.1-forge")) }
}

// Test mods for the development runs, by switch: each is remapped from SRG to the development names on the way.
val testMods = mapOf(
    "withJei" to listOf("local:jei-1.20.1-forge:15.56.0.205"),
    "withJeiFull" to listOf("local:jei-1.20.1-forge:15.56.0.205", "local:JustEnoughResources-1.20.1:1.4.0.247",
        "local:JustEnoughProfessions-forge-1.20.1:3.0.1"),
    "withInventoryEffects" to listOf("local:InventoryParticles:3.2.0+1.20.1+forge", "local:InventoryInteractions:2.2.0+1.20.1+forge",
        "local:MossyLib:1.6.0+1.20.1+forge"),
    "withInventoryParticles" to listOf("local:InventoryParticles:3.2.0+1.20.1+forge", "local:MossyLib:1.6.0+1.20.1+forge"),
    "withShaders" to listOf("local:embeddium:0.3.31+mc1.20.1", "local:oculus-mc1.20.1:1.8.0"),
    "withAmendments" to listOf("local:amendments-1.20:2.2.6", "local:moonlight-1.20:2.16.37-forge"),
    "withEasyMods" to listOf("local:EasyMagic:v8.0.1-1.20.1-Forge", "local:EasyAnvils:v8.0.2-1.20.1-Forge", "local:PuzzlesLib:v8.1.36-1.20.1-Forge"),
    "withPolymorph" to listOf("local:polymorph-forge:0.49.11+1.20.1"),
    // Quark 4.0-462 with Zeta 1.0-31: its wood crafting tables. With Every Compat 2.9.28 and Moonlight 2.16.37 too, and
    // Ecologics 2.2.7 for woods of another mod: Every Compat makes Quark's tables of those.
    "withQuark" to listOf("maven.modrinth:quark:DijQLauD", "maven.modrinth:zeta:mSER3jBI"),
    "withEveryCompat" to listOf("maven.modrinth:quark:DijQLauD", "maven.modrinth:zeta:mSER3jBI", "maven.modrinth:every-compat:gbhQFyid",
        "maven.modrinth:moonlight:b1cuadwV", "maven.modrinth:ecologics:jB0V9NeZ"),
    "withProfile" to listOf("local:entity_model_features:3.2.4-1.20.1-forge", "local:entity_texture_features_1.20.1-forge:7.1",
        "local:entityculling-forge:1.10.5-mc1.20.1", "local:ferritecore:6.0.1-forge", "local:xaeroworldmap-forge-1.20.1:1.45.0"),
)

dependencies {
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
    // Optional integrations, compiled against and never required: JEI's field guide, the inventory effects.
    for (dependency in prop("deps.compile_only").split(" ").filter { it.isNotBlank() }) "modCompileOnly"(dependency)
    val wanted = linkedSetOf<String>()
    testMods.forEach { (flag, jars) -> if (hasProperty(flag)) wanted.addAll(jars) }
    wanted.forEach { "modRuntimeOnly"(it) }
}

val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    val replaceProperties = mapOf(
        "minecraft_version" to sc.current.version,
        "minecraft_version_range" to prop("deps.minecraft_range"),
        "forge_version_range" to prop("deps.forge_range"),
        "loader_version_range" to prop("deps.loader_range"),
        "mod_id" to modId,
        "mod_name" to prop("mod.name"),
        "mod_license" to prop("mod.license"),
        "mod_version" to version.toString(),
    )
    inputs.properties(replaceProperties)
    expand(replaceProperties)
    from(rootProject.file("src/forge/templates"))
    into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}
val v1201 = rootProject.file("src/1.20.1/resources").toPath()
sourceSets.main {
    // 1.20.1 has its own translations: the shared ones are left out
    resources.srcDir(v1201)
    resources.exclude { it.relativePath.pathString.startsWith("assets/cinematic/lang/") && !it.file.toPath().startsWith(v1201) }
    resources.srcDir(rootProject.file("src/forge/resources"))
    resources.srcDir(generateModMetadata)
}
legacyForge.ideSyncTask(generateModMetadata)

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xmaxerrs", "5000"))
}
