plugins {
    // Applies the remapping Loom (both Fabric targets are obfuscated versions).
    id("dev.kikugie.loom-back-compat")
}

fun prop(name: String) = property(name).toString()
fun list(name: String) = prop(name).split(" ").filter { it.isNotBlank() }
val minecraft = sc.current.version
val javaVersion = prop("mod.java").toInt()

version = prop("mod.version")
group = prop("mod.group")
base.archivesName = prop("mod.archive")

repositories {
    maven("https://maven.parchmentmc.org/") { name = "Parchment" }
    maven("https://maven.terraformersmc.com/releases/") { name = "Terraformers" }
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
        filter { includeGroup("maven.modrinth") }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft")
    // Mojang names with Parchment parameters: exactly the names of the NeoForge sources this port follows.
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-$minecraft:${prop("deps.parchment")}@zip")
    })
    modImplementation("net.fabricmc:fabric-loader:${prop("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${prop("deps.fabric_api")}")
    // Optional: Mod Menu's settings button. It is not bundled or required.
    modCompileOnly("com.terraformersmc:modmenu:${prop("deps.modmenu")}")
    if (hasProperty("withSettings")) modRuntimeOnly("com.terraformersmc:modmenu:${prop("deps.modmenu")}")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    // Optional integrations, never bundled (see stonecutter.properties.toml).
    for (dependency in list("deps.compile_only")) compileOnly(dependency)
    for (dependency in list("deps.mod_compile_only")) modCompileOnly(dependency)
    if (hasProperty("withMouseTweaks")) modRuntimeOnly(prop("deps.mouse_tweaks"))
    // Local test mods for a run (Iris, Sodium, JEI ...): nothing in this folder goes into the release JAR.
    if (hasProperty("withTestMods")) {
        modRuntimeOnly(fileTree(rootProject.file("test-mods/${sc.current.project}/${findProperty("withTestMods")}")) { include("*.jar") })
    }
}

loom {
    accessWidenerPath = rootProject.file("src/fabric/resources/cinematic.accesswidener")
    runs {
        named("client") { runDir("run/client") }
        named("server") { runDir("run/server"); programArg("--nogui") }
        register("clientOnlySmoke") {
            client()
            runDir("run/${findProperty("clientOnlySmokeDir") ?: "client-only-smoke"}")
            vmArg("-Dcinematic.clientOnlySmoke=true")
            programArgs("--width", "1280", "--height", "720")
        }
        if (hasProperty("joinTest")) {
            // A real network join (packets are encoded, unlike singleplayer): run joinServer, then joinClient.
            register("joinServer") { server(); runDir("run/join-server"); programArg("--nogui") }
            register("joinClient") {
                client()
                runDir("run/join-client")
                programArgs("--quickPlayMultiplayer", "127.0.0.1:25589")
                if (hasProperty("serverSmoke")) vmArg("-Dcinematic.serverSmoke=true")
            }
        }
        register("showcase") {
            client()
            runDir("run/${findProperty("showcaseDir") ?: "showcase"}")
            vmArg("-Dcinematic.showcase=true")
            if (hasProperty("traceMixins")) vmArg("-Dmixin.debug.export=true")
            vmArg("-Dcinematic.showcase.world=${findProperty("showcaseWorld") ?: "cinematic-showcase"}")
            vmArg("-Dcinematic.showcase.from=${findProperty("from") ?: "0"}")
            vmArg("-Dcinematic.showcase.only=${findProperty("only") ?: "false"}")
            vmArg("-Dcinematic.showcase.editorBatch=${findProperty("editorBatch") ?: "false"}")
            programArgs("--width", "${findProperty("showcaseWidth") ?: "1600"}", "--height", "${findProperty("showcaseHeight") ?: "900"}")
        }
    }
}

val v1201 = rootProject.file("src/1.20.1/resources").toPath()
sourceSets.main {
    resources.srcDir(rootProject.file("src/fabric/resources"))
    if (sc.current.parsed < "1.21") {
        // 1.20.1 has its own translations: the shared ones are left out
        resources.srcDir(v1201)
        resources.exclude { it.relativePath.pathString.startsWith("assets/cinematic/lang/") && !it.file.toPath().startsWith(v1201) }
    }
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    val props = mapOf("version" to version.toString(), "minecraft" to minecraft, "java" to javaVersion.toString(),
        "loader" to prop("deps.fabric_loader"), "fabric_api_min" to prop("deps.fabric_api").substringBefore('+'))
    inputs.properties(props)
    filesMatching("fabric.mod.json") { expand(props) }
    // the mixin list is shared, so the mixins this version does not have are left out of it
    val excluded = list("mixins.exclude").flatMap { it.split(",") }.map { it.trim() }
    inputs.property("mixinsExcluded", excluded)
    filesMatching("cinematic.fabric.mixins.json") {
        expand("java" to javaVersion.toString())
        filter { line -> if (excluded.any { line.trim() == "\"$it\"," }) "" else line }
    }
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
    withSourcesJar()
}
tasks.named<Jar>("sourcesJar") { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }
tasks.jar { from(rootProject.file("LICENSE")) }
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = javaVersion
    options.compilerArgs.addAll(listOf("-Xmaxerrs", "1000"))
}

if (sc.current.parsed >= "1.21") {
    // The add-on example compiles against this port too (it is loader-neutral); never packaged. It is written for
    // 1.21's game code, so 1.20.1 leaves it out.
    sourceSets.register("exampleAddon") {
        java.srcDir(rootProject.file("examples/addon-template"))
        compileClasspath += sourceSets.main.get().compileClasspath + sourceSets.main.get().output
    }
    tasks.named("check") { dependsOn("compileExampleAddonJava") }
}
