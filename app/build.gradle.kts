plugins {
	jacoco
	alias(libs.plugins.google.services)
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.compose)
	alias(libs.plugins.kotlin.serialization)
	alias(libs.plugins.google.firebase.crashlytics)
}

abstract class VerifyJacocoCoverageTask : DefaultTask() {
	@get:org.gradle.api.tasks.InputFile
	abstract val reportFile: org.gradle.api.file.RegularFileProperty

	@get:org.gradle.api.tasks.Input
	abstract val requiredClasses: org.gradle.api.provider.ListProperty<String>

	@org.gradle.api.tasks.TaskAction
	fun verifyCoverage() {
		val report = reportFile.get().asFile
		check(report.isFile) {
			"JaCoCo XML report was not generated at ${report.absolutePath}"
		}

		val documentFactory = javax.xml.parsers.DocumentBuilderFactory.newInstance().apply {
			setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
			setFeature("http://xml.org/sax/features/validation", false)
		}
		val document = documentFactory.newDocumentBuilder().parse(report)
		val classNodes = document.getElementsByTagName("class")

		requiredClasses.get().forEach { className ->
			var coveredInstructions: Int? = null
			for (classIndex in 0 until classNodes.length) {
				val classElement = classNodes.item(classIndex) as org.w3c.dom.Element
				if (classElement.getAttribute("name") != className) continue

				val counters = classElement.getElementsByTagName("counter")
				for (counterIndex in 0 until counters.length) {
					val counter = counters.item(counterIndex) as org.w3c.dom.Element
					if (counter.getAttribute("type") == "INSTRUCTION") {
						coveredInstructions = counter.getAttribute("covered").toInt()
						break
					}
				}
				break
			}

			check(coveredInstructions != null) {
				"Required class $className is missing from the JaCoCo report."
			}
			check(coveredInstructions > 0) {
				"Required class $className has zero covered instructions. " +
						"Robolectric coverage instrumentation is not working."
			}
		}
	}
}

jacoco {
	toolVersion = "0.8.13"
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
					sdkVersion = 34
					systemImageSource = "aosp-atd"
				}
				create("pixelApi35") {
					device = "Pixel 2"
					sdkVersion = 35
					systemImageSource = "aosp-atd"
				}
				create("pixelApi36") {
					device = "Pixel 2"
					sdkVersion = 36
					systemImageSource = "aosp"
				}
				create("pixelApi37") {
					device = "Pixel 2"
					sdkVersion = 37
					systemImageSource = "google"
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

tasks.withType<Test>().configureEach {
	extensions.configure<org.gradle.testing.jacoco.plugins.JacocoTaskExtension> {
		isIncludeNoLocationClasses = true
		excludes = listOf("jdk.internal.*")
	}
}

tasks.register<VerifyJacocoCoverageTask>("verifyFossDebugCoverage") {
	group = "verification"
	description = "Verifies that Robolectric-backed FOSS tests are represented in JaCoCo XML."
	dependsOn("createFossDebugUnitTestCoverageReport")
	reportFile.set(layout.buildDirectory.file("reports/coverage/test/foss/debug/report.xml"))
	requiredClasses.set(
		listOf(
			"dev/lexip/hecate/ui/MainViewModel",
			"dev/lexip/hecate/data/UserPreferencesRepository",
			"dev/lexip/hecate/util/WallpaperHandler",
			"dev/lexip/hecate/services/MonitoringPreferencesCoordinator"
		)
	)
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
