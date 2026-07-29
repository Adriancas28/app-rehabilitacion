package com.sanna.rehabapp.feature.paciente

private val EXTENSIONES_VIDEO = listOf(".mp4", ".webm", ".mkv", ".mov", ".3gp")

// HU05-CA02: decide si el material se reproduce como video o se muestra
// como imagen, a partir de la extensión preservada al subir el archivo
// (ver EjercicioRepositoryImpl.subirMaterial).
fun esMaterialVideo(url: String): Boolean {
    val ruta = url.substringBefore("?").lowercase()
    return EXTENSIONES_VIDEO.any { ruta.endsWith(it) }
}
