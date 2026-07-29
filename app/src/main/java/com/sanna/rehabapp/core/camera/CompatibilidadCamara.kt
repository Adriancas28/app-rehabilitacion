package com.sanna.rehabapp.core.camera

import android.content.Context
import android.content.pm.PackageManager

// RNF03-CA02/CA03: antes de intentar abrir la cámara para monitorear una
// sesión, se verifica que el dispositivo realmente tenga una — si no,
// se debe informar la incompatibilidad en vez de fallar al abrir CameraX.
fun tieneCamaraDisponible(context: Context): Boolean =
    context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
