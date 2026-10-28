plugins {
    // Applies the right Loom for the version: remapping below 26.1, plain from 26.1 on.
    id("dev.kikugie.loom-back-compat")
}

val minecraft = sc.current.version
val java = property("mod.java").toString().toInt()
fun dep(name: String) = property("deps.$name").toString()

version = property("mod.version").toString()
group = "dev.dyslexic.cinematic"
base.archivesName = "Cinematic Interaction-Fabric-$minecraft"

repositories {
    maven("https://maven.terraformersmc.com/releases/") { name = "Terraformers" }
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
        filter { includeGroup("maven.modrinth") }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft")
    loomx.applyMojangMappings()
    modImplementation("net.fabricmc:fabric-loader:${dep("fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${dep("fabric_api")}")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    // Optional native APIs; none of these mods are packaged in the release JAR.
    modCompileOnly(dep("modmenu"))
    modCompileOnly(dep("inventory_particles"))
    modCompileOnly(dep("inventory_interactions"))
    modCompileOnly(dep("polymorph"))
    modCompileOnly(dep("jei"))
    if (hasProperty("withPunchy")) modRuntimeOnly(dep("punchy"))
    if (hasProperty("withPolymorph")) modRuntimeOnly(dep("polymorph"))
    if (hasProperty("withJei")) modRuntimeOnly(dep("jei"))
    if (hasProperty("withMouseTweaks")) modRuntimeOnly(dep("mouse_tweaks"))
    if (hasProperty("withInventoryParticles") || hasProperty("withInventoryEffects")) {
        modRuntimeOnly(dep("inventory_particles"))
        modRuntimeOnly(dep("mossylib"))
    }
    if (hasProperty("withInventoryEffects")) modRuntimeOnly(dep("inventory_interactions"))
    if (hasProperty("withNoXpAnvils")) modRuntimeOnly(fileTree("test-mods/no-xp") { include("*.jar") })
    if (hasProperty("withSettings")) modRuntimeOnly(dep("modmenu"))
    if (hasProperty("withShaders")) {
        modRuntimeOnly(dep("iris"))
        modRuntimeOnly(dep("sodium"))
        runtimeOnly(files("test-mods/libraries/antlr4-runtime-4.13.1.jar",
                "test-mods/libraries/glsl-transformer-3.0.0-pre3.jar", "test-mods/libraries/jcpp-1.4.14.jar"))
    }
}

// Development launches do not expose Iris's embedded parser libraries as ordinary Java libraries.
val prepareIrisRuntime by tasks.registering {
    onlyIf { hasProperty("withShaders") }
    doLast {
        val iris = configurations.runtimeClasspath.get().resolvedConfiguration.resolvedArtifacts.find {
            it.moduleVersion.id.group == "maven.modrinth" && it.name == "iris"
        } ?: throw GradleException("Iris runtime artifact is missing")
        copy {
            from(zipTree(iris.file))
            include("META-INF/jars/antlr4*.jar", "META-INF/jars/glsl-transformer*.jar", "META-INF/jars/jcpp*.jar")
            eachFile { path = name }
            includeEmptyDirs = false
            into("test-mods/libraries")
        }
    }
}
tasks.matching { it.name in setOf("configureLaunch", "generateDLIConfig") }.configureEach { dependsOn(prepareIrisRuntime) }

loom {
    // Loom reads the access widener while configuring, so it gets this version's processed copy.
    accessWidenerPath = sc.process(rootProject.file("src/main/resources/cinematic.accesswidener"), "build/processed.accesswidener")
    runs {
        named("client") { runDir("run/client") }
        named("server") { runDir("run/server"); programArg("--nogui") }
        register("clientOnlySmoke") { client(); runDir("run/client-only-smoke"); vmArg("-Dcinematic.clientOnlySmoke=true") }
        if (hasProperty("joinTest")) {
            // A real network join (packets are encoded, unlike singleplayer): run joinServer, then joinClient.
            register("joinServer") { server(); runDir("run/join-server"); programArg("--nogui") }
            register("joinClient") { client(); runDir("run/join-client"); programArgs("--quickPlayMultiplayer", "127.0.0.1:25589") }
        }
        register("showcase") {
            client()
            runDir("run/${findProperty("showcaseDir") ?: if (hasProperty("withShaders")) "showcase-iris" else "showcase"}")
            vmArg("-Dcinematic.showcase=true")
            if (hasProperty("traceMixins")) vmArg("-Dmixin.debug.export=true")
            vmArg("-Dcinematic.showcase.world=cinematic-showcase-${System.currentTimeMillis()}")
            vmArg("-Dcinematic.showcase.from=${findProperty("from") ?: "0"}")
            vmArg("-Dcinematic.showcase.only=${findProperty("only") ?: "false"}")
            vmArg("-Dcinematic.showcase.editorBatch=${findProperty("editorBatch") ?: "false"}")
            programArgs("--width", "${findProperty("showcaseWidth") ?: "1600"}", "--height", "${findProperty("showcaseHeight") ?: "900"}")
        }
    }
}

sourceSets.main {
    // textures, translations and models are shared with the root project
    resources.srcDir(rootProject.file("../src/main/resources"))
    resources.exclude("META-INF/accesstransformer.cfg", "META-INF/neoforge.mods.toml", "cinematic.mixins.json")
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    val props = mapOf("version" to version.toString(), "minecraft" to minecraft, "fabric_api" to dep("fabric_api"),
            "loader" to dep("fabric_loader"), "java" to java.toString())
    inputs.properties(props)
    filesMatching("fabric.mod.json") { expand(props) }
    // the mixin list is shared, so the mixins this version does not have are left out of it
    val excluded = findProperty("mixins.exclude")?.toString()?.split(",")?.map { it.trim() }.orEmpty()
    inputs.property("mixinsExcluded", excluded)
    filesMatching("cinematic.fabric.mixins.json") {
        expand("java" to java.toString())
        filter { line -> if (excluded.any { line.trim() == "\"$it\"," }) "" else line }
    }
    // the version conditions and the variants they comment out stay out of the released file
    filesMatching("cinematic.accesswidener") { filter { line -> if (line.startsWith("#")) "" else line } }
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(java)
    withSourcesJar()
}
tasks.named<Jar>("sourcesJar") { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }
tasks.jar { from(rootProject.file("../LICENSE")) }
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = java
    options.compilerArgs.addAll(listOf("-Xmaxerrs", "1000"))
}

tasks.register("extractFabricReferences") {
    doLast {
        configurations.compileClasspath.get().resolvedConfiguration.resolvedArtifacts.filter {
            it.moduleVersion.id.group == "net.fabricmc.fabric-api"
        }.forEach { artifact ->
            val id = artifact.moduleVersion.id
            val source = configurations.detachedConfiguration(dependencies.create("${id.group}:${id.name}:${id.version}:sources"))
            source.isTransitive = false
            source.files.forEach { jar -> copy { from(zipTree(jar)); into("refs/fabric") } }
        }
    }
}
