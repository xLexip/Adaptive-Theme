// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
	alias(libs.plugins.android.application) apply false
	alias(libs.plugins.kotlin.android) apply false
	alias(libs.plugins.kotlin.compose) apply false
	id("org.sonarqube") version "7.1.0.6387"
	id("com.google.gms.google-services") version "4.4.4" apply false
	alias(libs.plugins.google.firebase.crashlytics) apply false
}

sonar {
	properties {
		property("sonar.projectKey", "xLexip_Hecate")
		property("sonar.projectVersion", "0.6.0")
		property("sonar.organization", "xlexip")
		property("sonar.host.url", "https://sonarcloud.io")
		property("sonar.androidLint.reportPaths", "app/build/reports/lint-results-debug.html")
	}
}