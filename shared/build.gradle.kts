plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    android {
        namespace = "com.jean.vocabs.shared"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
        withHostTestBuilder {}.configure {}
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":contracts"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.sqldelight.coroutines.extensions)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
            implementation(libs.ktor.client.okhttp)
        }
        getByName("androidHostTest").dependencies {
            implementation(kotlin("test"))
            implementation(libs.sqldelight.sqlite.driver)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.mock)
        }
    }
}

sqldelight {
    databases {
        create("VocabsDatabase") {
            packageName.set("com.jean.vocabs.shared.db")

            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))

            // Off because the current .sqm chain cannot be replayed from empty:
            // 1.sqm opens by dropping an index and renaming a table that only a
            // long-replaced version of Vocabs.sq ever created. Verification
            // starts from an empty database, so it could never have passed here.
            // Turn on with the fresh schema, when the chain restarts at zero.
            verifyMigrations.set(false)
        }
    }
}
