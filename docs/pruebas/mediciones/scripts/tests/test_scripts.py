"""Pruebas de los scripts de medición con datos SINTÉTICOS construidos aquí.

Sirven solo para comprobar que los cálculos son correctos. Los números de estas pruebas no son
resultados de medición y no se escriben en las tablas de docs/pruebas/mediciones/.
Ejecutar:  python -m unittest discover -s tests -v   (desde la carpeta scripts)
"""
import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
import analizar_logcat as al  # noqa: E402
import calcular_error_angular as ea  # noqa: E402

MS = 1_000_000
PERIODO = 33_333_333  # 30 fotogramas por segundo


def registro_sintetico():
    """3 s de monitoreo a 30 FPS. Se pierden los fotogramas 40, 41 y 42 (hueco de ~133 ms entre
    resultados) y el fotograma 10 se dibuja con 600 ms de latencia; el resto, con 25 ms."""
    L = ["11-05 10:00:00.000  1  1 I MEDICION: evento=sesion dispositivo=Prueba_Modelo android=14 sdk=34 abi=arm64-v8a "
         "compilacion=release depurable=false ejercicio=Abduccion repeticiones_asignadas=3 t=0",
         "11-05 10:00:00.001  1  1 I MEDICION: evento=fase valor=preparacion t=0",
         "11-05 10:00:10.000  1  1 I MEDICION: evento=fase valor=monitoreo t=10000000000"]
    inicio = 10_000_000_000
    for i in range(90):
        ts = 1000 + i
        llegada = inicio + i * PERIODO
        if i in (40, 41, 42):
            continue
        resultado = llegada + 20 * MS
        dibujo = llegada + (600 * MS if i == 10 else 25 * MS)
        L.append("11-05 10:00:10.0  1  1 I MEDICION: evento=resultado ts={} llegada={} resultado={}".format(ts, llegada, resultado))
        L.append("11-05 10:00:10.0  1  1 I MEDICION: evento=dibujo ts={} dibujo={}".format(ts, dibujo))
    for k in (1, 2, 3):
        L.append("11-05 10:00:13.0  1  1 I MEDICION: evento=repeticion n={} t={}".format(k, inicio + k * 1_000_000_000))
    L.append("11-05 10:00:13.000  1  1 I MEDICION: evento=fase valor=fin t={}".format(inicio + 3_000_000_000))
    L.append("11-05 10:00:13.001  1  1 I MEDICION: evento=sin_resultado ts=1040 llegada={}".format(inicio + 40 * PERIODO))
    L.append("linea de otra etiqueta sin evento")
    return L


class PruebasLogcat(unittest.TestCase):
    def setUp(self):
        self.r = al.analizar(al.leer_eventos(registro_sintetico()), umbral_lento_ms=50.0)

    def test_cuenta_los_cuadros_con_guia_dibujada(self):
        self.assertEqual(self.r["lat"]["n"], 87)

    def test_latencia_promedio_maximo_minimo(self):
        lat = self.r["lat"]
        esperado = (86 * 25 + 600) / 87
        self.assertAlmostEqual(lat["promedio"], esperado, places=6)
        self.assertAlmostEqual(lat["maximo"], 600.0, places=6)
        self.assertAlmostEqual(lat["minimo"], 25.0, places=6)

    def test_porcentaje_sobre_500_ms(self):
        self.assertEqual(self.r["lat"]["sobre_limite"], 1)
        self.assertAlmostEqual(self.r["lat"]["pct_sobre_limite"], 100.0 / 87, places=6)

    def test_separa_inferencia_y_dibujo(self):
        self.assertAlmostEqual(self.r["lat"]["inferencia_prom"], 20.0, places=6)

    def test_fps_promedio_sobre_el_tramo_de_monitoreo(self):
        # 87 resultados dentro de una ventana de 3,0 s
        self.assertAlmostEqual(self.r["fps"]["duracion_s"], 3.0, places=6)
        self.assertAlmostEqual(self.r["fps"]["promedio"], 87 / 3.0, places=6)

    def test_cuadros_lentos_segun_el_umbral(self):
        lentos = self.r["fps"]["lentos_por_umbral"]
        self.assertAlmostEqual(lentos[50.0], 100.0 / 86, places=6)  # 1 intervalo de 133 ms entre 86
        self.assertAlmostEqual(lentos[100.0], 100.0 / 86, places=6)

    def test_descartados_y_repeticiones(self):
        self.assertEqual(self.r["sin_resultado"], 1)
        self.assertEqual(self.r["repeticiones"], 3)

    def test_el_dispositivo_sale_del_registro(self):
        texto = al.describir_dispositivos(self.r["sesiones"], None)
        self.assertIn("Prueba Modelo", texto)
        self.assertIn("no depurable", texto)

    def test_sin_umbral_lento_el_informe_lo_deja_por_definir(self):
        r = al.analizar(al.leer_eventos(registro_sintetico()), umbral_lento_ms=None)
        texto = al.tabla_fps(r, "x.log", "disp", None)
        self.assertIn("| Umbral de cuadro lento | Por definir |", texto)

    def test_un_registro_sin_eventos_no_rompe(self):
        r = al.analizar([])
        self.assertIsNone(r["lat"])
        self.assertIsNone(r["fps"])


class PruebasErrorAngular(unittest.TestCase):
    def csv_temporal(self, filas):
        f = tempfile.NamedTemporaryFile("w", suffix=".csv", delete=False, encoding="utf-8", newline="")
        f.write("postura,angulo_referencia_grados,angulo_calculado_app_grados,error_absoluto_grados,observaciones\n")
        for fila in filas:
            f.write(fila + "\n")
        f.close()
        self.addCleanup(os.remove, f.name)
        return f.name

    def test_mae_desviacion_y_sesgo(self):
        # errores con signo: +2, -4, +6 -> absolutos 2, 4, 6
        ruta = self.csv_temporal(["a,30,32,,", "b,60,56,,", "c,90,96,,", ",,,,"])
        r = ea.calcular(ea.leer(ruta))
        self.assertEqual(r["n"], 3)
        self.assertAlmostEqual(r["mae"], 4.0)
        self.assertAlmostEqual(r["sd_abs"], 2.0)
        self.assertAlmostEqual(r["sesgo"], 4.0 / 3)
        self.assertAlmostEqual(r["max_abs"], 6.0)

    def test_acepta_coma_decimal(self):
        ruta = self.csv_temporal(['a,"30,5","32,5",,'])
        filas = ea.leer(ruta)
        self.assertAlmostEqual(filas[0]["ref"], 30.5)

    def test_avisa_si_hay_pocas_observaciones(self):
        ruta = self.csv_temporal(["a,30,32,,"])
        texto = ea.tabla(ea.calcular(ea.leer(ruta)), None, ruta)
        self.assertIn("menor que el mínimo propuesto", texto)
        self.assertIn("| Referencia empleada | Por definir |", texto)

    def test_una_fila_incompleta_se_rechaza(self):
        ruta = self.csv_temporal(["a,30,,,"])
        with self.assertRaises(SystemExit):
            ea.leer(ruta)

    def test_csv_vacio_no_rompe(self):
        r = ea.calcular(ea.leer(self.csv_temporal([])))
        self.assertEqual(r["n"], 0)


if __name__ == "__main__":
    unittest.main()
