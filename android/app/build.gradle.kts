//
// A PLanG tablet-alkalmazás modulja.
//
// Az értelmezőt (hu.ppke.itk.plang.prog) NEM másoltuk le kézzel: egy
// Gradle-feladat a tárológyár src/main/java könyvtárából szinkronizálja
// ide a forrásokat a Swing-only fájlok kiszűrésével, és ez a generált
// könyvtár fordul bele az APK-ba. Így a nyelvi viselkedés bitre azonos
// az asztali változattal, és a két változat ugyanazt az egy forrást
// használja.
//

plugins {
    id("com.android.application")
}

// A tárológyár megosztott Java-forrásai (az android/ könyvtár szülője).
val sharedSourceDir = rootProject.file("../src/main/java")
val generatedSharedDir = layout.buildDirectory.dir("generated/sharedJava")

// Csak a Swing-felületet szűrjük ki; a gui csomag tiszta tagjai
// (ProgramLine, ExprNode, PlangSyntax) kellenek az Android felületnek.
val swingOnlyFiles = listOf(
    "hu/ppke/itk/plang/plang.java",
    "hu/ppke/itk/plang/gui/AppPrefs.java",
    "hu/ppke/itk/plang/gui/CallStack.java",
    "hu/ppke/itk/plang/gui/ExprRenderer.java",
    "hu/ppke/itk/plang/gui/ExprTree.java",
    "hu/ppke/itk/plang/gui/MainFrame.java",
    "hu/ppke/itk/plang/gui/PrefDialog.java",
    "hu/ppke/itk/plang/gui/ProgLineRenderer.java",
    "hu/ppke/itk/plang/gui/ProgramList.java",
    "hu/ppke/itk/plang/gui/StateCellRenderer.java",
    "hu/ppke/itk/plang/gui/StateList.java",
    "hu/ppke/itk/plang/gui/StreamDocument.java",
    "hu/ppke/itk/plang/gui/StreamTabs.java",
    "hu/ppke/itk/plang/gui/Workbench.java",
    "hu/ppke/itk/plang/gui/editor/CodeEditor.java",
    "hu/ppke/itk/plang/gui/editor/LineNumberGutter.java",
    "hu/ppke/itk/plang/gui/editor/PlangUndoManager.java",
    "hu/ppke/itk/plang/gui/editor/SyntaxDocument.java",
    "hu/ppke/itk/plang/gui/theme/**",
    "hu/ppke/itk/plang/gui/widgets/**"
)

val copySharedSources = tasks.register<Copy>("copySharedSources") {
    from(sharedSourceDir) {
        exclude(swingOnlyFiles)
    }
    into(generatedSharedDir)
}

android {
    namespace = "hu.ppke.itk.plang.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "hu.ppke.itk.plang"
        minSdk = 21
        targetSdk = 34
        versionCode = 6
        versionName = "2.0"
    }

    sourceSets {
        getByName("main") {
            java.srcDir(generatedSharedDir)
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    signingConfigs {
        /* A tárolóba vett debug-kulcs: minden build (debug ÉS release)
           ugyanazzal az aláírással készül, így az "adb install -r" és a
           csomagkezelő felül tudja írni a korábbi telepítést. (Eddig minden
           CI-futás új, véletlen kulcsot generált – a frissítéshez előbb
           el kellett távolítani a régi appot.) */
        getByName("debug") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            isMinifyEnabled = false
            // telepíthető legyen a release APK is
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

// A fordítás előtt mindig frissüljön a megosztott források mása.
tasks.matching { it.name.startsWith("compile") || it.name.startsWith("merge") }
    .configureEach {
        dependsOn(copySharedSources)
    }
