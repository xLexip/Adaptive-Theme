plugins {
	id("com.google.gms.google-services")
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.android)
	alias(libs.plugins.kotlin.compose)
	alias(libs.plugins.google.firebase.crashlytics)
}

android {
	namespace = "dev.lexip.hecate"
	compileSdk = 36

	defaultConfig {
		applicationId = "dev.lexip.hecate"
		minSdk = 31
		targetSdk = 36
		versionCode = 7
		versionName = "0.3.0"
		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
				debugSymbolLevel = "SYMBOL_TABLE"
			}
		}
		debug {
			versionNameSuffix = "-beta"
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
	buildFeatures {
		compose = true
		buildConfig = true
	}
	buildToolsVersion = "36.0.0"
	sourceSets {
		getByName("main") {
			resources {
				srcDirs("src/main/resources", "src/main/java/components")
			}
		}
	}
}

dependencies {
	implementation(platform(libs.firebase.bom))
	implementation(libs.firebase.analytics)
	implementation(libs.firebase.crashlytics)
	implementation(libs.androidx.localbroadcastmanager)
	implementation(libs.androidx.core.splashscreen.v100)
	implementation(libs.androidx.activity.compose)
	implementation(libs.androidx.appcompat)
	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.datastore.preferences)
	implementation(libs.androidx.lifecycle.runtime.ktx)
	implementation(libs.androidx.lifecycle.viewmodel.compose)
	implementation(libs.androidx.material3)
	implementation(libs.androidx.preference)
	implementation(libs.androidx.ui)
	implementation(libs.androidx.ui.graphics)
	implementation(libs.androidx.ui.tooling.preview)
	implementation(libs.material)
	implementation(platform(libs.androidx.compose.bom))
	implementation(libs.androidx.compose.material.icons.extended)
	testImplementation(libs.junit)
	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.androidx.espresso.core)
	androidTestImplementation(platform(libs.androidx.compose.bom))
	androidTestImplementation(libs.androidx.ui.test.junit4)
	debugImplementation(libs.androidx.ui.tooling)
	debugImplementation(libs.androidx.ui.test.manifest)
}