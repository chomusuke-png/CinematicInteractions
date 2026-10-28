plugins {
    `java-library`
    id("net.neoforged.moddev") version "2.0.148"
    idea
}

fun prop(name: String) = property(name).toString()
val modId = prop("mod.id")
val javaVersion = prop("mod.java").toInt()

version = prop("mod.version")
group = prop("mod.group")
base.archivesName = prop("mod.archive")
java.toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)

// The run folders stay at the top of the repository, where they were before the projects were merged.
fun runDir(name: String) = rootProject.file(name)

neoForge {
    version = prop("deps.neoforge")
    // this loader's access transformer is outside the default src/main/resources
    accessTransformers.from(rootProject.file("src/neoforge/resources/META-INF/accesstransformer.cfg"))

    parchment {
        mappingsVersion = prop("deps.parchment")
        minecraftVersion = prop("deps.parchment_minecraft")
    }

    runs {
        register("client") {
            client()
            gameDirectory = runDir("run")
        }
        register("server") {
            server()
            gameDirectory = runDir("run")
            programArgument("--nogui")
        }
        // Development-only scripted walkthrough of every station (tools/showcase.sh); screenshots go to run-showcase/screenshots.
        register("showcase") {
            client()
            gameDirectory = runDir(findProperty("showcaseDir")?.toString() ?: "run-showcase")
            showcaseProperties()
        }
        // Same walkthrough with Sodium + Iris and a shader pack (jars dropped into run-showcase-iris/mods).
        register("showcaseIris") {
            client()
            gameDirectory = runDir(findProperty("showcaseIrisDir")?.toString() ?: "run-showcase-iris")
            showcaseProperties()
        }
        // A real vanilla dedicated server on 127.0.0.1:25586, with no Cinematic Interactions installed.
        register("clientOnlySmoke") {
            client()
            gameDirectory = runDir(findProperty("clientOnlySmokeDir")?.toString() ?: "run-showcase")
            systemProperty("cinematic.clientOnlySmoke", "true")
            programArguments.addAll("--width", "1280", "--height", "720")
        }
        if (hasProperty("joinTest")) {
            // A real network join (packets are encoded, unlike singleplayer): run joinServer, then joinClient.
            register("joinServer") {
                server()
                gameDirectory = runDir("run-join-server")
                programArgument("--nogui")
            }
            register("joinClient") {
                client()
                gameDirectory = runDir("run-join-client")
                programArguments.addAll("--quickPlayMultiplayer", "127.0.0.1:25589")
                // -PserverSmoke: shift-clicks into stations over the real connection (client/ServerSmoke)
                if (hasProperty("serverSmoke")) systemProperty("cinematic.serverSmoke", "true")
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

repositories {
    maven("https://api.modrinth.com/maven")
}

dependencies {
    // Optional integrations: these jars are never bundled or required to run Cinematic Interactions.
    for (dependency in prop("deps.compile_only").split(" ").filter { it.isNotBlank() }) compileOnly(dependency)
    if (hasProperty("withMouseTweaks")) runtimeOnly(prop("deps.mouse_tweaks"))
}

val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    val replaceProperties = mapOf(
        "minecraft_version" to sc.current.version,
        "minecraft_version_range" to prop("deps.minecraft_range"),
        "neo_version" to prop("deps.neoforge"),
        "loader_version_range" to prop("deps.loader_range"),
        "mod_id" to modId,
        "mod_name" to prop("mod.name"),
        "mod_license" to prop("mod.license"),
        "mod_version" to version.toString(),
    )
    inputs.properties(replaceProperties)
    expand(replaceProperties)
    from(rootProject.file("src/neoforge/templates"))
    into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}
sourceSets.main {
    resources.srcDir(rootProject.file("src/neoforge/resources"))
    resources.srcDir(generateModMetadata)
}
neoForge.ideSyncTask(generateModMetadata)

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
