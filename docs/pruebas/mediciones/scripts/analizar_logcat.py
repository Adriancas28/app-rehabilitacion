#!/usr/bin/env python3
"""Calcula la latencia de la retroalimentación (EDT 1.8.6.2) y los fotogramas por segundo (EDT 1.8.6.3)
a partir del registro de logcat con la etiqueta MEDICION.

Captura (antes de iniciar la sesión, en otra terminal; detener con Ctrl+C al terminar):
    adb logcat -c
    adb logcat -v threadtime -s MEDICION:I > latencia_<modelo>.log

Uso:
    python analizar_logcat.py latencia_<modelo>.log [--umbral-lento-ms 50] [--salida DIR]
    python analizar_logcat.py --adb                     # lee el buffer actual (puede estar incompleto)

El umbral de "cuadro lento" no tiene valor por defecto: si no se indica, el informe muestra el
porcentaje para varios umbrales candidatos y deja el umbral "Por definir".
Solo usa la biblioteca estándar de Python.
"""
import argparse
import csv
import os
import re
import statistics
import subprocess
import sys
from collections import Counter, defaultdict

LIMITE_LATENCIA_MS = 500.0  # HU10-CA11
UMBRALES_CANDIDATOS_MS = (33.3, 50.0, 66.7, 100.0)
NS = 1e9
MS = 1e6


# --------------------------------------------------------------------------- lectura
def leer_eventos(lineas):
    """Devuelve la lista de eventos (dict) de las líneas de logcat con la etiqueta MEDICION."""
    eventos = []
    for linea in lineas:
        if "MEDICION" not in linea:
            continue
        i = linea.find("evento=")
        if i < 0:
            continue
        d = {}
        for par in linea[i:].split():
            if "=" in par:
                k, v = par.split("=", 1)
                d[k] = v
        eventos.append(d)
    return eventos


def entero(d, clave, defecto=None):
    try:
        return int(d[clave])
    except (KeyError, ValueError):
        return defecto


# --------------------------------------------------------------------------- análisis
def analizar(eventos, umbral_lento_ms=None, limite_latencia_ms=LIMITE_LATENCIA_MS):
    sesiones = [e for e in eventos if e.get("evento") == "sesion"]
    fases = sorted(
        ((entero(e, "t"), e.get("valor")) for e in eventos if e.get("evento") == "fase" and entero(e, "t") is not None)
    )
    resultados = {}
    for e in eventos:
        if e.get("evento") == "resultado":
            ts, ll, r = entero(e, "ts"), entero(e, "llegada"), entero(e, "resultado")
            if ts is not None and r is not None:
                resultados[ts] = (ll if ll is not None else -1, r)
    dibujos = {}
    for e in eventos:
        if e.get("evento") == "dibujo":
            ts, d = entero(e, "ts"), entero(e, "dibujo")
            if ts is not None and d is not None:
                dibujos[ts] = d
    sin_resultado = sum(1 for e in eventos if e.get("evento") == "sin_resultado")
    repeticiones = sum(1 for e in eventos if e.get("evento") == "repeticion")

    # --- latencia: llegada del fotograma -> dibujo de la guía con ese resultado
    cuadros = []
    sin_llegada = 0
    for ts, (llegada, resultado) in sorted(resultados.items()):
        if ts not in dibujos:
            continue
        if llegada < 0:
            sin_llegada += 1
            continue
        cuadros.append((ts, (resultado - llegada) / MS, (dibujos[ts] - resultado) / MS, (dibujos[ts] - llegada) / MS))
    latencias = [c[3] for c in cuadros]
    lat = None
    if latencias:
        ordenadas = sorted(latencias)
        p95 = ordenadas[min(len(ordenadas) - 1, int(round(0.95 * (len(ordenadas) - 1))))]
        lat = {
            "n": len(latencias),
            "promedio": statistics.fmean(latencias),
            "mediana": statistics.median(latencias),
            "minimo": ordenadas[0],
            "maximo": ordenadas[-1],
            "p95": p95,
            "sobre_limite": sum(1 for x in latencias if x > limite_latencia_ms),
            "pct_sobre_limite": 100.0 * sum(1 for x in latencias if x > limite_latencia_ms) / len(latencias),
            "inferencia_prom": statistics.fmean(c[1] for c in cuadros),
            "dibujo_prom": statistics.fmean(c[2] for c in cuadros),
            "negativos": sum(1 for x in latencias if x < 0),
        }

    # --- ventanas de monitoreo (entre la fase "monitoreo" y la siguiente fase distinta)
    ventanas = []
    for k, (t, valor) in enumerate(fases):
        if valor != "monitoreo":
            continue
        fin = next((t2 for t2, v2 in fases[k + 1:] if v2 != "monitoreo"), None)
        ventanas.append([t, fin])
    tiempos_resultado = sorted(r for (_, r) in resultados.values())
    fps = None
    intervalos_ms = []
    por_segundo = []
    duracion_total_s = 0.0
    frames_total = 0
    for inicio, fin in ventanas:
        dentro = [t for t in tiempos_resultado if t >= inicio and (fin is None or t <= fin)]
        if not dentro:
            continue
        fin_real = fin if fin is not None else dentro[-1]
        dur = (fin_real - inicio) / NS
        if dur <= 0:
            continue
        duracion_total_s += dur
        frames_total += len(dentro)
        intervalos_ms += [(b - a) / MS for a, b in zip(dentro, dentro[1:])]
        cubos = Counter(int((t - inicio) // NS) for t in dentro)
        for s in range(int(dur)):  # solo segundos completos
            por_segundo.append(cubos.get(s, 0))
    if duracion_total_s > 0:
        fps = {
            "ventanas": len([1 for v in ventanas]),
            "duracion_s": duracion_total_s,
            "cuadros": frames_total,
            "promedio": frames_total / duracion_total_s,
            "seg_completos": len(por_segundo),
            "min_por_s": min(por_segundo) if por_segundo else None,
            "max_por_s": max(por_segundo) if por_segundo else None,
            "intervalos": len(intervalos_ms),
            "intervalo_prom_ms": statistics.fmean(intervalos_ms) if intervalos_ms else None,
            "intervalo_max_ms": max(intervalos_ms) if intervalos_ms else None,
            "lentos_por_umbral": {
                u: (100.0 * sum(1 for x in intervalos_ms if x > u) / len(intervalos_ms)) if intervalos_ms else None
                for u in sorted(set(UMBRALES_CANDIDATOS_MS) | ({umbral_lento_ms} if umbral_lento_ms else set()))
            },
            "pct_descartados": 100.0 * sin_resultado / (len(resultados) + sin_resultado) if (resultados or sin_resultado) else None,
        }
    return {
        "sesiones": sesiones, "repeticiones": repeticiones, "cuadros": cuadros, "lat": lat, "fps": fps,
        "sin_llegada": sin_llegada, "sin_resultado": sin_resultado, "resultados": len(resultados), "dibujos": len(dibujos),
    }


# --------------------------------------------------------------------------- salida
def describir_dispositivos(sesiones, forzado):
    if forzado:
        return forzado
    vistos = []
    for s in sesiones:
        texto = "{} (Android {}, API {}, {}, compilación {}{})".format(
            s.get("dispositivo", "?").replace("_", " "), s.get("android", "?"), s.get("sdk", "?"), s.get("abi", "?"),
            s.get("compilacion", "?"), ", depurable" if s.get("depurable") == "true" else ", no depurable",
        )
        if texto not in vistos:
            vistos.append(texto)
    return "; ".join(vistos) if vistos else "Por definir (el registro no contiene la línea de sesión)"


def f1(x):
    return "—" if x is None else "{:.1f}".format(x)


def tabla_latencia(r, archivo, dispositivo, limite):
    lat = r["lat"]
    metodo = ("Instrumentación en la app (logcat, etiqueta MEDICION): se resta el instante en que el fotograma llega al "
              "analizador de la cámara del instante en que se dibuja la guía calculada con ese fotograma. "
              "Reloj monotónico del dispositivo; no incluye el retardo del sensor ni de la pantalla.")
    salida = ["# 1.8.6.2 Latencia de la retroalimentación", "",
              "Generado por `scripts/analizar_logcat.py` a partir de `{}`.".format(os.path.basename(archivo)), ""]
    salida += ["| Campo | Valor |", "|---|---|",
               "| Método | {} |".format(metodo),
               "| Dispositivo | {} |".format(dispositivo),
               "| Repeticiones completadas en el registro | {} |".format(r["repeticiones"]),
               "| Fotogramas con guía dibujada (n) | {} |".format(lat["n"] if lat else 0)]
    if lat:
        salida += ["| Promedio (ms) | {} |".format(f1(lat["promedio"])),
                   "| Mediana (ms) | {} |".format(f1(lat["mediana"])),
                   "| Mínimo (ms) | {} |".format(f1(lat["minimo"])),
                   "| Máximo (ms) | {} |".format(f1(lat["maximo"])),
                   "| Percentil 95 (ms) | {} |".format(f1(lat["p95"])),
                   "| Fotogramas sobre {:.0f} ms | {} de {} ({:.2f} %) |".format(limite, lat["sobre_limite"], lat["n"], lat["pct_sobre_limite"]),
                   "| Comparación con el límite de {:.0f} ms (HU10-CA11) | Promedio {}; máximo {} |".format(
                       limite, "dentro del límite" if lat["promedio"] <= limite else "SOBRE el límite",
                       "dentro del límite" if lat["maximo"] <= limite else "SOBRE el límite"),
                   "| Promedio de la parte de inferencia (llegada a resultado, ms) | {} |".format(f1(lat["inferencia_prom"])),
                   "| Promedio de la parte de dibujo (resultado a dibujo, ms) | {} |".format(f1(lat["dibujo_prom"]))]
    else:
        salida.append("| Resultados | Sin fotogramas con guía dibujada en el registro (la guía solo se dibuja con una pose confiable) |")
    notas = []
    if r["sin_llegada"]:
        notas.append("{} fotogramas con guía dibujada no tenían instante de llegada y se excluyeron.".format(r["sin_llegada"]))
    if lat and lat["negativos"]:
        notas.append("{} latencias negativas: revisar el registro.".format(lat["negativos"]))
    if notas:
        salida += ["", "Notas: " + " ".join(notas)]
    return "\n".join(salida) + "\n"


def tabla_fps(r, archivo, dispositivo, umbral):
    f = r["fps"]
    metodo = ("Instrumentación en la app (logcat, etiqueta MEDICION): se cuentan los fotogramas que MediaPipe procesa por segundo "
              "durante las fases de monitoreo (sin cuenta regresiva ni descansos). Un cuadro es lento cuando el intervalo "
              "hasta el cuadro procesado anterior supera el umbral indicado.")
    salida = ["# 1.8.6.3 Fotogramas por segundo", "",
              "Generado por `scripts/analizar_logcat.py` a partir de `{}`.".format(os.path.basename(archivo)), ""]
    salida += ["| Campo | Valor |", "|---|---|", "| Método | {} |".format(metodo), "| Dispositivo | {} |".format(dispositivo)]
    if not f:
        salida.append("| Resultados | Sin fases de monitoreo con fotogramas en el registro |")
        return "\n".join(salida) + "\n"
    salida += ["| Tiempo de monitoreo analizado | {:.1f} s en {} tramo(s) |".format(f["duracion_s"], f["ventanas"]),
               "| Fotogramas procesados | {} |".format(f["cuadros"]),
               "| FPS promedio | {:.2f} |".format(f["promedio"]),
               "| FPS por segundo completo (mín. y máx.) | {} y {} (en {} segundos) |".format(f["min_por_s"], f["max_por_s"], f["seg_completos"]),
               "| Intervalo entre fotogramas (promedio y máximo, ms) | {} y {} |".format(f1(f["intervalo_prom_ms"]), f1(f["intervalo_max_ms"])),
               "| Fotogramas descartados (llegaron y no produjeron resultado) | {} |".format(
                   "—" if f["pct_descartados"] is None else "{:.2f} %".format(f["pct_descartados"]))]
    if umbral:
        salida.append("| Umbral de cuadro lento | {} ms |".format(f1(umbral)))
        salida.append("| Porcentaje de cuadros lentos | {:.2f} % |".format(f["lentos_por_umbral"][umbral]))
    else:
        salida.append("| Umbral de cuadro lento | Por definir |")
        salida.append("| Porcentaje de cuadros lentos | Por definir (ver los porcentajes por umbral candidato) |")
    salida += ["", "Porcentaje de intervalos que superan cada umbral candidato (referencia; no es el resultado oficial):", "",
               "| Umbral (ms) | Equivale a (FPS) | Cuadros lentos |", "|---|---|---|"]
    for u, p in f["lentos_por_umbral"].items():
        salida.append("| {} | {:.1f} | {} |".format(f1(u), 1000.0 / u, "—" if p is None else "{:.2f} %".format(p)))
    return "\n".join(salida) + "\n"


def escribir(ruta, texto):
    with open(ruta, "w", encoding="utf-8", newline="\n") as f:
        f.write(texto)
    print("escrito:", ruta)


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("archivo", nargs="?", help="registro de logcat (texto)")
    ap.add_argument("--adb", action="store_true", help="leer el buffer actual con adb logcat -d")
    ap.add_argument("--salida", default=os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."),
                    help="carpeta donde se escriben las tablas (por defecto docs/pruebas/mediciones)")
    ap.add_argument("--dispositivo", help="descripción del dispositivo; si se omite se toma del registro")
    ap.add_argument("--umbral-latencia-ms", type=float, default=LIMITE_LATENCIA_MS)
    ap.add_argument("--umbral-lento-ms", type=float, help="umbral de cuadro lento; sin valor por defecto")
    a = ap.parse_args(argv)
    if a.adb:
        txt = subprocess.run(["adb", "logcat", "-d", "-v", "threadtime", "-s", "MEDICION:I"], capture_output=True, text=True, check=True).stdout
        lineas, origen = txt.splitlines(), "adb_logcat"
    elif a.archivo:
        with open(a.archivo, encoding="utf-8", errors="replace") as f:
            lineas, origen = f.read().splitlines(), a.archivo
    else:
        ap.error("indique un archivo de logcat o --adb")
    ev = leer_eventos(lineas)
    if not ev:
        sys.exit("El registro no contiene líneas MEDICION. ¿La app se compiló con -PmedirRendimiento=true?")
    r = analizar(ev, a.umbral_lento_ms, a.umbral_latencia_ms)
    disp = describir_dispositivos(r["sesiones"], a.dispositivo)
    os.makedirs(a.salida, exist_ok=True)
    escribir(os.path.join(a.salida, "1.8.6.2_latencia.md"), tabla_latencia(r, origen, disp, a.umbral_latencia_ms))
    escribir(os.path.join(a.salida, "1.8.6.3_fps.md"), tabla_fps(r, origen, disp, a.umbral_lento_ms))
    if r["cuadros"]:
        ruta = os.path.join(a.salida, "latencia_cuadros.csv")
        with open(ruta, "w", encoding="utf-8", newline="") as f:
            w = csv.writer(f)
            w.writerow(["ts_ms", "inferencia_ms", "dibujo_ms", "latencia_total_ms"])
            for c in r["cuadros"]:
                w.writerow([c[0]] + ["{:.3f}".format(x) for x in c[1:]])
        print("escrito:", ruta)


if __name__ == "__main__":
    main()
