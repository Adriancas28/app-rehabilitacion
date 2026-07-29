// Plugins declarados a nivel raíz (sin aplicar); cada módulo los aplica según necesite.
// Nota: con Kotlin 1.9.x el compilador de Compose NO es un plugin aparte
// (eso recién existe desde Kotlin 2.0) — se activa vía `composeOptions` en
// el módulo, ver app/build.gradle.kts.
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}
