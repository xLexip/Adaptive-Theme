plugins {
	alias(libs.plugins.google.services)
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.compose)
	alias(libs.plugins.kotlin.serialization)
	alias(libs.plugins.google.firebase.crashlytics)
}

android {
	namespace = "dev.lexip.hecate"
	compileSdk = 37
	buildToolsVersion = "36.0.0"

	defaultConfig {
		applicationId = "dev.lexip.hecate"
		minSdk = 34
		targetSdk = 37
		versionCode = 122
		versionName = "2.2.0"
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
			dependenciesInfo {
				includeInApk = false
				includeInBundle = false
			}
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
			enableUnitTestCoverage = true
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
	testOptions {
		animationsDisabled = true
		unitTests {
			isIncludeAndroidResources = true
			all {
				it.jvmArgs(
					"--add-opens=java.base/java.lang=ALL-UNNAMED",
					"--add-opens=java.base/java.util=ALL-UNNAMED",
					"--add-opens=java.base/java.io=ALL-UNNAMED",
					"--add-opens=java.base/java.net=ALL-UNNAMED",
					"--add-opens=java.base/java.security=ALL-UNNAMED",
					"--add-opens=java.base/java.text=ALL-UNNAMED",
					"--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
					"--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
					"--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED"
				)
			}
		}
		managedDevices {
			localDevices {
				create("pixelApi34") {
					device = "Pixel 2"
					apiLevel = 34
					systemImageSource = "aosp-atd"
				}
				create("pixelApi35") {
					device = "Pixel 2"
					apiLevel = 35
					systemImageSource = "aosp-atd"
				}
			}
		}
	}

	sourceSets {
		getByName("main") {
			resources {
				srcDirs("src/main/resources", "src/main/kotlin/components")
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
	"playImplementation"(libs.review)
	"playImplementation"(libs.review.ktx)
	testImplementation(libs.junit)
	testImplementation(libs.kotlinx.coroutines.test)
	testImplementation(libs.robolectric)
	testImplementation(libs.androidx.test.core.ktx)
	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.androidx.espresso.core)
	androidTestImplementation(platform(libs.androidx.compose.bom))
	androidTestImplementation(libs.androidx.ui.test.junit4)
	androidTestImplementation(libs.androidx.ui.test.junit4.accessibility)
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
			handlerFile.writeText("package dev.lexip.hecate.util; import android.content.Context; class DarkThemeHandler(context: Context) { fun setDarkTheme(enable: Boolean) = DarkThemeChangeResult(succeeded = false, changed = false) }")
		}
	}
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
	dependsOn("ensureFileCompleteness")
}
