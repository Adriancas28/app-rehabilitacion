#!/usr/bin/env python3
"""Calcula el error absoluto medio (MAE) del ángulo que calcula la app frente a una referencia (EDT 1.8.6.1).

Uso:
    python calcular_error_angular.py ../1.8.6.1_posturas.csv [--actualizar-csv] [--salida DIR] [--referencia "texto"]

El CSV tiene las columnas: postura, angulo_referencia_grados, angulo_calculado_app_grados,
error_absoluto_grados (la calcula este script) y observaciones (opcional). Las filas vacías se ignoran.
Solo usa la biblioteca estándar de Python.
"""
import argparse
import csv
import math
import os
import statistics
import sys

MINIMO_OBSERVACIONES = 30  # criterio mínimo propuesto; ver README
MINIMO_ANGULOS_DISTINTOS = 6
# Valores críticos de t de Student, 95 % bilateral, por grados de libertad (1 a 30).
T95 = [12.706, 4.303, 3.182, 2.776, 2.571, 2.447, 2.365, 2.306, 2.262, 2.228, 2.201, 2.179, 2.160, 2.145, 2.131,
       2.120, 2.110, 2.101, 2.093, 2.086, 2.080, 2.074, 2.069, 2.064, 2.060, 2.056, 2.052, 2.048, 2.045, 2.042]
COLUMNAS = ["postura", "angulo_referencia_grados", "angulo_calculado_app_grados", "error_absoluto_grados", "observaciones"]


def numero(texto):
    t = (texto or "").strip().replace(",", ".")
    return float(t) if t else None


def leer(ruta):
    filas = []
    with open(ruta, encoding="utf-8-sig", newline="") as f:
        for i, f_ in enumerate(csv.DictReader(f), start=2):
            ref, app = numero(f_.get("angulo_referencia_grados")), numero(f_.get("angulo_calculado_app_grados"))
            if ref is None and app is None:
                continue
            if ref is None or app is None:
                sys.exit("Fila {}: falta el ángulo de referencia o el calculado por la app.".format(i))
            filas.append({"postura": (f_.get("postura") or "").strip(), "ref": ref, "app": app,
                          "obs": (f_.get("observaciones") or "").strip(), "linea": i})
    return filas


def calcular(filas):
    errores = [f["app"] - f["ref"] for f in filas]  # con signo: positivo = la app mide de más
    absolutos = [abs(e) for e in errores]
    n = len(filas)
    r = {"n": n, "angulos_distintos": len({f["ref"] for f in filas})}
    if n == 0:
        return r
    r.update({
        "mae": statistics.fmean(absolutos),
        "sd_abs": statistics.stdev(absolutos) if n > 1 else None,
        "sesgo": statistics.fmean(errores),
        "sd_sesgo": statistics.stdev(errores) if n > 1 else None,
        "max_abs": max(absolutos),
        "min_abs": min(absolutos),
        "rmse": math.sqrt(statistics.fmean(e * e for e in errores)),
    })
    if n > 1:
        t = T95[n - 2] if n - 1 <= len(T95) else 1.96
        mitad = t * r["sd_abs"] / math.sqrt(n)
        r["ic95"] = (r["mae"] - mitad, r["mae"] + mitad)
    else:
        r["ic95"] = None
    return r


def fmt(x, d=2):
    return "—" if x is None else "{:.{d}f}".format(x, d=d)


def tabla(r, referencia, archivo):
    n = r["n"]
    salida = ["# 1.8.6.1 Error angular", "",
              "Generado por `scripts/calcular_error_angular.py` a partir de `{}`.".format(os.path.basename(archivo)), "",
              "| Campo | Valor |", "|---|---|",
              "| Referencia empleada | {} |".format(referencia or "Por definir"),
              "| Posturas medidas | {} observaciones en {} ángulos de referencia distintos |".format(n, r["angulos_distintos"])]
    if n:
        salida += ["| Error absoluto medio, MAE (grados) | {} |".format(fmt(r["mae"])),
                   "| Desviación estándar del error absoluto (grados) | {} |".format(fmt(r["sd_abs"])),
                   "| Intervalo de confianza del 95 % del MAE (grados) | {} |".format(
                       "—" if not r["ic95"] else "{} a {}".format(fmt(r["ic95"][0]), fmt(r["ic95"][1]))),
                   "| Sesgo, error con signo promedio (grados) | {} (positivo: la app mide de más) |".format(fmt(r["sesgo"])),
                   "| Desviación estándar del error con signo (grados) | {} |".format(fmt(r["sd_sesgo"])),
                   "| Error absoluto mínimo y máximo (grados) | {} y {} |".format(fmt(r["min_abs"]), fmt(r["max_abs"])),
                   "| Raíz del error cuadrático medio (grados) | {} |".format(fmt(r["rmse"]))]
    salida.append("| Umbral de aceptación | Por definir |")
    avisos = []
    if n < MINIMO_OBSERVACIONES:
        avisos.append("n = {} es menor que el mínimo propuesto de {} observaciones.".format(n, MINIMO_OBSERVACIONES))
    if r["angulos_distintos"] < MINIMO_ANGULOS_DISTINTOS:
        avisos.append("Hay {} ángulos de referencia distintos; se proponen al menos {}.".format(r["angulos_distintos"], MINIMO_ANGULOS_DISTINTOS))
    if avisos:
        salida += ["", "Avisos: " + " ".join(avisos)]
    salida += ["", "El error describe la diferencia con la referencia utilizada; no constituye una validación clínica."]
    return "\n".join(salida) + "\n"


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("csv")
    ap.add_argument("--actualizar-csv", action="store_true", help="escribe la columna error_absoluto_grados en el CSV")
    ap.add_argument("--referencia", help="método de referencia usado (goniómetro, transportador, etc.)")
    ap.add_argument("--salida", default=os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
    a = ap.parse_args(argv)
    filas = leer(a.csv)
    r = calcular(filas)
    os.makedirs(a.salida, exist_ok=True)
    ruta = os.path.join(a.salida, "1.8.6.1_error_angular.md")
    with open(ruta, "w", encoding="utf-8", newline="\n") as f:
        f.write(tabla(r, a.referencia, a.csv))
    print("escrito:", ruta)
    if a.actualizar_csv:
        with open(a.csv, "w", encoding="utf-8", newline="") as f:
            w = csv.writer(f)
            w.writerow(COLUMNAS)
            for fila in filas:
                w.writerow([fila["postura"], fila["ref"], fila["app"], "{:.2f}".format(abs(fila["app"] - fila["ref"])), fila["obs"]])
        print("actualizado:", a.csv)
    if not filas:
        print("El CSV no tiene mediciones todavía.")


if __name__ == "__main__":
    main()
