plugins {
    id("com.android.application")
}

// Release signing: the key comes from GitHub secrets (see .github/workflows/build.yml).
val keystorePath: String? = System.getenv("SIGNING_KEYSTORE")

// The Android app plays the very same game as the website: ../index.html.
// At build time it is copied into the app with the two fonts bundled, so the app works offline.
val gameAssets = layout.buildDirectory.dir("generated/gameAssets")
val gameHtml = rootProject.file("index.html")
val fontsDir = rootProject.file("fonts")

val prepareGameAssets by tasks.registering {
    inputs.file(gameHtml)
    inputs.dir(fontsDir)
    outputs.dir(gameAssets)
    doLast {
        val out = gameAssets.get().asFile
        out.deleteRecursively()
        File(out, "fonts").mkdirs()
        val faces = listOf(400, 500, 600, 700).joinToString("") { w ->
            "@font-face{font-family:'Pixelify Sans';font-weight:$w;font-display:block;" +
                "src:url(fonts/pixelify-sans-latin-$w-normal.woff2) format('woff2')}"
        } + "@font-face{font-family:'Silkscreen';font-weight:400;font-display:block;" +
            "src:url(fonts/silkscreen-latin-400-normal.woff2) format('woff2')}"
        val html = gameHtml.readText()
            .replace(Regex("""<link rel="preconnect"[^>]*>\n?"""), "")
            .replace(Regex("""<link rel="stylesheet" href="https://fonts\.googleapis\.com[^>]*>\n?"""), "")
            .replaceFirst("<style>", "<style>$faces")
        check(!html.contains("fonts.googleapis")) { "index.html still loads fonts from the web" }
        File(out, "index.html").writeText(html)
        fontsDir.listFiles { f -> f.name.endsWith(".woff2") }!!.forEach { it.copyTo(File(out, "fonts/${it.name}")) }
    }
}

android {
    namespace = "se.jonsolberg.naughtylist"
    compileSdk = 35

    defaultConfig {
        applicationId = "se.jonsolberg.naughtylist"
        minSdk = 21
        targetSdk = 35
        versionCode = 14
        versionName = "1.13"
    }

    sourceSets["main"].assets.srcDir(gameAssets)

    signingConfigs {
        create("release") {
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (keystorePath != null) signingConfigs.getByName("release")
            else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks.named("preBuild") { dependsOn(prepareGameAssets) }
