plugins {
	id("com.google.gms.google-services")
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.android)
	alias(libs.plugins.kotlin.compose)
	alias(libs.plugins.kotlin.serialization)
	alias(libs.plugins.google.firebase.crashlytics)
}

android {
	namespace = "dev.lexip.hecate"
	compileSdk = 36
	buildToolsVersion = "36.0.0"

	defaultConfig {
		applicationId = "dev.lexip.hecate"
		minSdk = 34
		targetSdk = 35
		versionCode = 63
		versionName = "0.10.1"
		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}

	flavorDimensions += "store"
	productFlavors {
		create("play") {
			dimension = "store"
		}
		create("foss") {
			dimension = "store"
			versionNameSuffix = "-foss"
		}
	}

	buildTypes {
		release {
			isMinifyEnabled = true
			isShrinkResources = true
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
			ndk {
				debugSymbolLevel = "FULL"
			}
			manifestPlaceholders["crashlyticsEnabled"] = true
		}
		debug {
			versionNameSuffix = "-debug"
			isDebuggable = true
			ndk {
				debugSymbolLevel = "FULL"
			}
			manifestPlaceholders["crashlyticsEnabled"] = false
		}
		create("beta") {
			initWith(getByName("release"))
			versionNameSuffix = "-beta"
			isDebuggable = false
			manifestPlaceholders["crashlyticsEnabled"] = true
		}

	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}

	kotlin {
		compilerOptions {
			jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
		}
	}

	bundle {
		language {
			@Suppress("UnstableApiUsage")
			enableSplit = false
		}
	}

	buildFeatures {
		compose = true
		buildConfig = true
	}

	sourceSets {
		getByName("main") {
			resources {
				srcDirs("src/main/resources", "src/main/java/components")
			}
		}
	}
}

dependencies {
	implementation(libs.androidx.localbroadcastmanager)
	implementation(libs.androidx.core.splashscreen.v100)
	implementation(libs.androidx.activity.compose)
	implementation(libs.androidx.appcompat)
	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.datastore.preferences)
	implementation(libs.androidx.lifecycle.runtime.ktx)
	implementation(libs.androidx.lifecycle.viewmodel.compose)
	implementation(libs.androidx.material3)
	implementation(libs.androidx.navigation.compose)
	implementation(libs.kotlinx.serialization.json)
	implementation(libs.androidx.preference)
	implementation(libs.androidx.ui)
	implementation(libs.androidx.ui.graphics)
	implementation(libs.androidx.ui.tooling.preview)
	implementation(libs.material)
	implementation(platform(libs.androidx.compose.bom))
	implementation(libs.androidx.compose.material.icons.extended)
	implementation(libs.shizuku.api)
	implementation(libs.shizuku.provider)
	"playImplementation"(platform(libs.firebase.bom))
	"playImplementation"(libs.firebase.analytics)
	"playImplementation"(libs.firebase.crashlytics)
	"playImplementation"(libs.app.update.ktx)
	testImplementation(libs.junit)
	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.androidx.espresso.core)
	androidTestImplementation(platform(libs.androidx.compose.bom))
	androidTestImplementation(libs.androidx.ui.test.junit4)
	debugImplementation(libs.androidx.ui.tooling)
	debugImplementation(libs.androidx.ui.test.manifest)
}

afterEvaluate {
	tasks.matching { it.name.contains("GoogleServices") && it.name.contains("Foss") }
		.configureEach { enabled = false }
	tasks.matching { it.name.contains("Crashlytics") && it.name.contains("Foss") }
		.configureEach { enabled = false }
}

tasks.register<DefaultTask>("ensureFileCompleteness") {
	group = "build"
	description = "Ensures file completeness."
	val handlerPath = "src/main/kotlin/dev/lexip/hecate/util/DarkThemeHandler.kt"
	val handlerFile = File(projectDir, handlerPath)

	doLast {
		if (!handlerFile.exists()) {
			handlerFile.parentFile.mkdirs()
			handlerFile.writeText("package dev.lexip.hecate.util; import android.content.Context; class DarkThemeHandler(context: Context) { fun setDarkTheme(enable: Boolean) {} }".trimIndent())
		}
	}
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
	dependsOn("ensureFileCompleteness")
}
