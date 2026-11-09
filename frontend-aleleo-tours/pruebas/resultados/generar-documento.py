# -*- coding: utf-8 -*-
"""
Genera el documento Word de la Actividad 1:
Plan de pruebas del Frontend con Jest (SACE - AleLeo Tours).
"""
import json
import os
import re
from datetime import datetime

from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_BREAK
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor

BASE = os.path.dirname(os.path.abspath(__file__))
PRUEBAS = os.path.dirname(BASE)
FRONTEND = os.path.dirname(PRUEBAS)
DOCS = os.path.join(os.path.dirname(FRONTEND), "docs")
EVIDENCIA = os.path.join(PRUEBAS, "evidencia")

AZUL = RGBColor(0x0E, 0x74, 0x90)
AZUL_OSCURO = RGBColor(0x0F, 0x2A, 0x3A)
GRIS = RGBColor(0x47, 0x55, 0x69)
VERDE = RGBColor(0x16, 0x63, 0x34)

with open(os.path.join(BASE, "resultados.json"), encoding="utf-8") as f:
    RESULTADOS = json.load(f)

TOTAL = RESULTADOS["numTotalTests"]
SUITES = RESULTADOS["numTotalTestSuites"]


# --------------------------------------------------------------------- utilidades
def sombrear(celda, color_hex):
    tcPr = celda._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:val"), "clear")
    shd.set(qn("w:color"), "auto")
    shd.set(qn("w:fill"), color_hex)
    tcPr.append(shd)


def agregar_campo(parrafo, campo):
    inicio = parrafo.add_run()
    fld = OxmlElement("w:fldChar")
    fld.set(qn("w:fldCharType"), "begin")
    inicio._r.append(fld)

    instr_run = parrafo.add_run()
    instruccion = OxmlElement("w:instrText")
    instruccion.set(qn("xml:space"), "preserve")
    instruccion.text = " %s " % campo
    instr_run._r.append(instruccion)

    separador = parrafo.add_run()
    fld_sep = OxmlElement("w:fldChar")
    fld_sep.set(qn("w:fldCharType"), "separate")
    separador._r.append(fld_sep)

    valor = parrafo.add_run("1")

    fin_run = parrafo.add_run()
    fld_fin = OxmlElement("w:fldChar")
    fld_fin.set(qn("w:fldCharType"), "end")
    fin_run._r.append(fld_fin)
    return valor


def configurar_estilos(doc):
    normal = doc.styles["Normal"]
    normal.font.name = "Calibri"
    normal.font.size = Pt(11)
    normal.font.color.rgb = RGBColor(0x1F, 0x29, 0x37)
    normal.paragraph_format.space_after = Pt(6)
    normal.paragraph_format.line_spacing = 1.12

    for nivel, tam in ((1, 16), (2, 13), (3, 11.5)):
        estilo = doc.styles[f"Heading {nivel}"]
        estilo.font.name = "Calibri"
        estilo.font.size = Pt(tam)
        estilo.font.bold = True
        estilo.font.color.rgb = AZUL if nivel < 3 else AZUL_OSCURO
        estilo.paragraph_format.space_before = Pt(14 if nivel == 1 else 10)
        estilo.paragraph_format.space_after = Pt(6)
        estilo.paragraph_format.keep_with_next = True


def encabezado_pie(seccion, mostrar_encabezado=True):
    seccion.different_first_page_header_footer = True

    if mostrar_encabezado:
        p = seccion.header.paragraphs[0]
        p.text = "SACE · Actividad 1 — Plan de pruebas del Frontend con Jest"
        p.alignment = WD_ALIGN_PARAGRAPH.RIGHT
        for run in p.runs:
            run.font.size = Pt(8.5)
            run.font.color.rgb = GRIS

    pie = seccion.footer.paragraphs[0]
    pie.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = pie.add_run("Página ")
    r.font.size = Pt(8.5)
    r.font.color.rgb = GRIS
    agregar_campo(pie, "PAGE").font.size = Pt(8.5)
    r = pie.add_run(" de ")
    r.font.size = Pt(8.5)
    r.font.color.rgb = GRIS
    agregar_campo(pie, "NUMPAGES").font.size = Pt(8.5)


def titulo(doc, texto, nivel=1):
    return doc.add_heading(texto, level=nivel)


def parrafo(doc, texto=None, negrita=False, cursiva=False, tam=None, color=None, alineacion=None, espacio=6):
    p = doc.add_paragraph()
    if texto:
        run = p.add_run(texto)
        run.bold = negrita
        run.italic = cursiva
        if tam:
            run.font.size = Pt(tam)
        if color:
            run.font.color.rgb = color
    if alineacion is not None:
        p.alignment = alineacion
    p.paragraph_format.space_after = Pt(espacio)
    return p


def vinetas(doc, items, estilo="List Bullet"):
    for item in items:
        p = doc.add_paragraph(style=estilo)
        p.paragraph_format.space_after = Pt(3)
        partes = item.split("**")
        for i, parte in enumerate(partes):
            run = p.add_run(parte)
            run.bold = (i % 2 == 1)


def codigo(doc, texto):
    for linea in texto.split("\n"):
        p = doc.add_paragraph()
        p.paragraph_format.space_after = Pt(0)
        p.paragraph_format.left_indent = Cm(0.4)
        pPr = p._p.get_or_add_pPr()
        shd = OxmlElement("w:shd")
        shd.set(qn("w:val"), "clear")
        shd.set(qn("w:color"), "auto")
        shd.set(qn("w:fill"), "F1F5F9")
        pPr.append(shd)
        run = p.add_run(linea if linea else " ")
        run.font.name = "Consolas"
        run.font.size = Pt(9)
        run.font.color.rgb = RGBColor(0x1E, 0x29, 0x3B)
    doc.add_paragraph().paragraph_format.space_after = Pt(2)


def tabla(doc, encabezados, filas, anchos=None, tam=9.5, alineaciones=None):
    t = doc.add_table(rows=1, cols=len(encabezados))
    t.style = "Table Grid"
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    t.autofit = False

    for i, texto in enumerate(encabezados):
        celda = t.rows[0].cells[i]
        celda.text = ""
        p = celda.paragraphs[0]
        run = p.add_run(texto)
        run.bold = True
        run.font.size = Pt(tam)
        run.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)
        p.paragraph_format.space_after = Pt(2)
        sombrear(celda, "0E7490")
        if anchos:
            celda.width = Cm(anchos[i])

    for numero, fila in enumerate(filas):
        celdas = t.add_row().cells
        for i, valor in enumerate(fila):
            celdas[i].text = ""
            p = celdas[i].paragraphs[0]
            run = p.add_run(str(valor))
            run.font.size = Pt(tam)
            p.paragraph_format.space_after = Pt(1)
            if alineaciones and alineaciones[i] == "centro":
                p.alignment = WD_ALIGN_PARAGRAPH.CENTER
            if anchos:
                celdas[i].width = Cm(anchos[i])
        if numero % 2 == 1:
            for celda in celdas:
                sombrear(celda, "F8FAFC")

    # Ninguna fila se parte entre dos paginas y el encabezado se repite.
    for fila_tabla in t.rows:
        trPr = fila_tabla._tr.get_or_add_trPr()
        trPr.append(OxmlElement("w:cantSplit"))

    trPr = t.rows[0]._tr.get_or_add_trPr()
    cabecera_repetida = OxmlElement("w:tblHeader")
    cabecera_repetida.set(qn("w:val"), "true")
    trPr.append(cabecera_repetida)

    return t


def figura(doc, nombre_archivo, caption, ancho_cm):
    ruta = os.path.join(EVIDENCIA, nombre_archivo)
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(6)
    p.paragraph_format.space_after = Pt(3)
    p.add_run().add_picture(ruta, width=Cm(ancho_cm))
    pie = doc.add_paragraph()
    pie.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = pie.add_run(caption)
    run.italic = True
    run.font.size = Pt(8.5)
    run.font.color.rgb = GRIS
    pie.paragraph_format.space_after = Pt(10)


def caja_destacada(doc, titulo_texto, cuerpo, color_fondo="ECFDF5", color_borde="16A34A"):
    t = doc.add_table(rows=1, cols=1)
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    celda = t.rows[0].cells[0]
    celda.text = ""
    sombrear(celda, color_fondo)
    tcPr = celda._tc.get_or_add_tcPr()
    bordes = OxmlElement("w:tcBorders")
    for lado in ("top", "left", "bottom", "right"):
        borde = OxmlElement(f"w:{lado}")
        borde.set(qn("w:val"), "single")
        borde.set(qn("w:sz"), "6")
        borde.set(qn("w:color"), color_borde)
        bordes.append(borde)
    tcPr.append(bordes)

    p = celda.paragraphs[0]
    run = p.add_run(titulo_texto)
    run.bold = True
    run.font.size = Pt(10.5)
    run.font.color.rgb = RGBColor(0x16, 0x63, 0x34)
    p.paragraph_format.space_after = Pt(2)

    for linea in cuerpo.split("\n"):
        p2 = celda.add_paragraph()
        run2 = p2.add_run(linea)
        run2.font.size = Pt(10)
        p2.paragraph_format.space_after = Pt(1)
    doc.add_paragraph().paragraph_format.space_after = Pt(2)


# --------------------------------------------------------------------- documento
doc = Document()
configurar_estilos(doc)

seccion = doc.sections[0]
seccion.page_width = Cm(21.59)
seccion.page_height = Cm(27.94)
seccion.top_margin = Cm(2.2)
seccion.bottom_margin = Cm(2.0)
seccion.left_margin = Cm(2.3)
seccion.right_margin = Cm(2.3)
encabezado_pie(seccion)

# ===================================================================== PORTADA
p = doc.add_paragraph()
p.paragraph_format.space_before = Pt(10)
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
run = p.add_run("SERVICIO NACIONAL DE APRENDIZAJE — SENA")
run.bold = True
run.font.size = Pt(11)
run.font.color.rgb = AZUL

p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
run = p.add_run("Programa de Formación · Análisis y Desarrollo de Software\nFicha 3171149-B")
run.font.size = Pt(10.5)
run.font.color.rgb = GRIS

for _ in range(2):
    doc.add_paragraph()

p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
run = p.add_run("SACE")
run.bold = True
run.font.size = Pt(34)
run.font.color.rgb = AZUL

p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
run = p.add_run("Sistema de Atención al Cliente — AleLeo Tours")
run.font.size = Pt(15)
run.font.color.rgb = AZUL_OSCURO

for _ in range(3):
    doc.add_paragraph()

p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
run = p.add_run("ACTIVIDAD 1")
run.bold = True
run.font.size = Pt(13)
run.font.color.rgb = GRIS

p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
run = p.add_run("Plan de pruebas del Frontend con Jest")
run.bold = True
run.font.size = Pt(24)
run.font.color.rgb = AZUL_OSCURO

p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
run = p.add_run("Pruebas unitarias y de integración ligera sobre el sitio estático")
run.italic = True
run.font.size = Pt(11)
run.font.color.rgb = GRIS

for _ in range(3):
    doc.add_paragraph()

t = doc.add_table(rows=0, cols=2)
t.alignment = WD_TABLE_ALIGNMENT.CENTER
t.autofit = False
datos_portada = [
    ("Integrantes", "Cesar Leonardo Ramírez Montejo\nYerson Alexei Torres García\nFelipe Gonzales Quitero"),
    ("Responsable de la actividad", "Cesar Leonardo Ramírez Montejo"),
    ("Módulo evaluado", "Frontend web (HTML, CSS y JavaScript vanilla)"),
    ("Herramienta", "Jest 29.7.0 + jest-environment-jsdom 29.7.0"),
    ("Fecha de ejecución", "7 de noviembre de 2026"),
    ("Repositorio del proyecto", "1_Proyecto_Git / Projecto_SACE / frontend-aleleo-tours"),
]
for etiqueta, valor in datos_portada:
    fila = t.add_row().cells
    fila[0].width = Cm(5.2)
    fila[1].width = Cm(10.5)
    p0 = fila[0].paragraphs[0]
    r0 = p0.add_run(etiqueta)
    r0.bold = True
    r0.font.size = Pt(10)
    r0.font.color.rgb = AZUL
    p1 = fila[1].paragraphs[0]
    r1 = p1.add_run(valor)
    r1.font.size = Pt(10)

doc.add_paragraph()
p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
run = p.add_run("Resultado de la ejecución: %d suites · %d casos · %d aprobados · 0 fallidos"
                % (SUITES, TOTAL, RESULTADOS["numPassedTests"]))
run.bold = True
run.font.size = Pt(10)
run.font.color.rgb = VERDE

# ===================================================================== CONTENIDO
doc.add_page_break()
titulo(doc, "Contenido de la actividad", 1)
parrafo(doc, "El documento desarrolla los nueve puntos solicitados en la guía de la actividad:", color=GRIS)

tabla(
    doc,
    ["#", "Punto", "Qué contiene"],
    [
        ["1", "Portada", "Identificación del proyecto, integrantes, herramienta y fecha."],
        ["2", "Introducción", "Propósito, alcance y resultado general de la ejecución."],
        ["3", "Estrategia y tipos de prueba", "Enfoque, técnicas de diseño, niveles y matriz de riesgo."],
        ["4", "Descripción del recurso o herramienta", "Jest, jsdom, Node.js y estructura del proyecto de pruebas."],
        ["5", "Casos de prueba", "40 casos (48 ejecuciones) con tabla de resultados y capturas de pantalla."],
        ["6", "Responsable", "Roles del equipo y responsabilidad de la ejecución."],
        ["7", "Criterios de entrada y salida", "Condiciones para iniciar, suspender y cerrar la ejecución."],
        ["8", "Errores encontrados", "Observación OBS-01, evidencia, impacto y recomendación."],
        ["9", "Video corto", "Guion escena por escena, comandos y checklist de grabación."],
    ],
    anchos=[1.2, 5.6, 9.9],
    alineaciones=["centro", None, None],
)

parrafo(doc, "")
caja_destacada(
    doc,
    "Resumen ejecutivo",
    "Se ejecutaron 48 pruebas automatizadas sobre 7 módulos del frontend: todas aprobaron en 6,3 segundos.\n"
    "La suite corre con un solo comando (npx jest) y no requiere backend ni base de datos, por lo que puede\n"
    "repetirse en cada cambio del sitio. Se documentó 1 observación de severidad baja (OBS-01) sobre la\n"
    "clasificación de categorías del catálogo.",
)

# ===================================================================== 2. INTRODUCCIÓN
doc.add_page_break()
titulo(doc, "2. Introducción", 1)

parrafo(doc, "SACE (Sistema de Atención al Cliente) es el proyecto formativo de AleLeo Tours. El sistema se compone de un "
             "backend REST en Spring Boot y de un frontend web 100 % estático construido con HTML, CSS, Bootstrap y "
             "JavaScript vanilla. Este documento presenta el plan de pruebas del frontend y la ejecución real de ese plan "
             "con Jest.")

parrafo(doc, "El frontend concentra reglas que el usuario percibe de forma directa: la resolución de la URL del API, el "
             "escapado de datos antes de pintarlos (prevención de XSS), la validación de contraseñas, el manejo de la "
             "sesión y del token, el catálogo de destinos con sus precios y categorías, los filtros de reportes, los "
             "estados de las solicitudes y el control de acceso del panel del asesor. Un error en cualquiera de esas "
             "reglas no rompe el servidor, pero sí produce pantallas vacías, precios equivocados, filtros que no "
             "encuentran destinos o mensajes incomprensibles para el cliente.")

parrafo(doc, "La actividad tiene cuatro objetivos concretos:")
vinetas(doc, [
    "**Verificar** el comportamiento de las funciones críticas del frontend con datos reales y con datos límite.",
    "**Automatizar** la verificación para que pueda repetirse en cada entrega, sin depender de pruebas manuales.",
    "**Documentar** los casos de prueba, los criterios de entrada/salida y los errores encontrados.",
    "**Dejar evidencia** de la ejecución (salida de consola y capturas) para la sustentación del proyecto.",
])

parrafo(doc, "El alcance es el frontend. No se prueban aquí los endpoints del backend ni la base de datos: esas pruebas "
             "corresponden al plan de pruebas de la API. Tampoco se hacen pruebas de carga ni de usabilidad con usuarios "
             "reales; el documento indica esas limitaciones en el punto 3.")

parrafo(doc, "Resultado general: se diseñaron 40 casos de prueba que, por sus variantes de borde, se ejecutan como 48 "
             "pruebas automatizadas. Las 48 aprobaron. La ejecución completa tarda alrededor de 6 segundos y se "
             "encontró una observación menor que se documenta en el punto 8.")

caja_destacada(
    doc,
    "Dato de la ejecución",
    "Comando: npx jest --verbose   ·   Suites: 7/7 aprobadas   ·   Pruebas: 48/48 aprobadas   ·   Fallos: 0   ·   Tiempo: 6,279 s",
)

# ===================================================================== 3. ESTRATEGIA
doc.add_page_break()
titulo(doc, "3. Estrategia y tipos de prueba", 1)

titulo(doc, "3.1 Enfoque general", 2)
parrafo(doc, "Se aplicó una estrategia de pruebas unitarias de caja blanca, complementadas con pruebas de integración "
             "ligera sobre el DOM. La diferencia con una prueba de extremo a extremo (E2E) es deliberada: en lugar de "
             "levantar el backend, la base de datos y un navegador, se cargan los archivos JavaScript reales del "
             "frontend en un DOM simulado (jsdom) y se invocan directamente las funciones que usa la interfaz. Así la "
             "retroalimentación es inmediata y el resultado no depende de la red ni de datos de la base.")

parrafo(doc, "El principio rector fue probar el código de producción sin copiarlo ni reescribirlo: el helper "
             "helpers/cargar-frontend.js lee los mismos archivos que carga el navegador, los evalúa en el ámbito global "
             "del DOM y expone únicamente las funciones que cada prueba necesita.")

titulo(doc, "3.2 Tipos de prueba aplicados", 2)
tabla(
    doc,
    ["Tipo de prueba", "Qué se verificó", "Ejemplo"],
    [
        ["Unitaria de lógica pura", "Entradas y salidas de funciones sin efectos secundarios.",
         "normalizarBusqueda, formatearPrecio, calcularKPIs."],
        ["Integración con DOM (jsdom)", "Funciones que crean o leen elementos de la página.",
         "Toast.mostrar, Sesion.actualizar, leerPasajeros."],
        ["Seguridad del cliente", "Saneamiento de HTML y manejo del token de sesión.",
         "escaperHtml (XSS), protegerFetch (Bearer y 401)."],
        ["Contrato de datos", "Tolerancia a datos incompletos o mal formados del backend.",
         "mapearServicio con precio no numérico o sin destino."],
        ["Valores límite", "Extremos de los rangos permitidos.",
         "pasajeros 0, -5, 1, 10, 99 y texto no numérico."],
        ["Regresión", "Repetición automática de los 48 casos en cada cambio.",
         "npx jest antes de cada entrega."],
    ],
    anchos=[4.2, 6.6, 5.9],
)

titulo(doc, "3.3 Técnicas de diseño de casos", 2)
vinetas(doc, [
    "**Partición de equivalencia:** se agruparon entradas que deben producir el mismo resultado (por ejemplo, estados "
    "válidos frente a estados desconocidos) y se probó un representante de cada grupo.",
    "**Análisis de valores límite:** se probaron los extremos de los rangos (0, 1, 10 y 99 pasajeros; fechas de inicio "
    "y fin de mes; cadenas vacías).",
    "**Casos negativos:** null, undefined, cadena vacía, JSON corrupto, precio no numérico y respuestas HTTP 401.",
    "**Dobles de prueba:** se reemplazó window.fetch por un espía de Jest para verificar cabeceras y códigos de "
    "respuesta sin llamar a la red.",
    "**Temporizadores falsos:** se simuló el paso del tiempo para comprobar que el aviso Toast desaparece a los 3,5 "
    "segundos y para fijar la fecha del 15 de marzo de 2026 en los filtros de reportes.",
])

titulo(doc, "3.4 Niveles y alcance", 2)
tabla(
    doc,
    ["Nivel", "Incluido", "Fuera del alcance"],
    [
        ["Unitario", "7 módulos de js/ (núcleo, cliente, administrador y empleado).", "Funciones de renderizado completas."],
        ["Integración", "Interacción con el DOM simulado y con sessionStorage.", "Navegador real, CSS y accesibilidad visual."],
        ["Sistema", "No aplica en esta actividad.", "Backend, base de datos y pasarela de pagos."],
        ["Aceptación", "Validación del documento y de las evidencias.", "Pruebas con usuarios finales."],
    ],
    anchos=[2.8, 7.3, 6.6],
)

titulo(doc, "3.5 Matriz de riesgo", 2)
tabla(
    doc,
    ["Riesgo", "Probabilidad", "Impacto", "Cobertura en el plan"],
    [
        ["XSS por datos del usuario", "Media", "Alto", "CP-05 y CP-06 (escapado de HTML)."],
        ["Token enviado a un dominio ajeno", "Baja", "Alto", "CP-12, CP-12b y CP-12c."],
        ["Precio o categoría incorrectos en el catálogo", "Media", "Medio", "CP-15 a CP-20 y CP-23."],
        ["Filtros de fecha mal calculados", "Media", "Medio", "CP-24 a CP-28."],
        ["Acceso de un asesor inactivo", "Baja", "Alto", "CP-36 y CP-37."],
        ["Pantalla en blanco por datos incompletos", "Media", "Medio", "CP-06, CP-18 y CP-38b."],
    ],
    anchos=[5.6, 2.6, 2.6, 5.9],
    alineaciones=[None, "centro", "centro", None],
)

# ===================================================================== 4. HERRAMIENTA
doc.add_page_break()
titulo(doc, "4. Descripción del recurso o herramienta", 1)

titulo(doc, "4.1 Jest", 2)
parrafo(doc, "Jest es el marco de pruebas de JavaScript mantenido por Meta. Se eligió porque integra en una sola "
             "herramienta el ejecutor de pruebas, las aserciones (expect), los dobles de prueba (jest.fn), los "
             "temporizadores simulados y los informes de resultados, sin necesidad de configurar un navegador ni un "
             "servidor. En este proyecto se usó la versión 29.7.0, la última de la rama 29, junto con "
             "jest-environment-jsdom 29.7.0.")

tabla(
    doc,
    ["Característica", "Uso en este plan"],
    [
        ["Ejecutor de pruebas", "Descubre y ejecuta los archivos *.test.js de la carpeta tests/."],
        ["Aserciones (expect)", "Compara el resultado real con el esperado en cada caso (toBe, toEqual, toMatch)."],
        ["Espías y mocks", "Reemplaza window.fetch para verificar el token y el manejo del 401."],
        ["Temporizadores falsos", "Simula 3,5 s del Toast y fija la fecha de los reportes."],
        ["Entorno jsdom", "Proporciona document, window, sessionStorage y eventos del navegador."],
        ["Salida JSON", "Permite generar la tabla de evidencias a partir de la ejecución real."],
    ],
    anchos=[5.0, 11.7],
)

titulo(doc, "4.2 jsdom y Node.js", 2)
parrafo(doc, "jsdom es una implementación en JavaScript de los estándares del DOM. Jest la usa como entorno de pruebas "
             "para que el código del frontend encuentre document, window y sessionStorage como en un navegador. Node.js "
             "24.21.0 ejecuta tanto a Jest como a jsdom; npm 11.19.0 administra las dos dependencias de desarrollo.")

titulo(doc, "4.3 Por qué no se usaron otras herramientas", 2)
tabla(
    doc,
    ["Alternativa", "Motivo para descartarla"],
    [
        ["Vitest", "Está pensado para proyectos con Vite y módulos ES; el frontend es estático y sin build."],
        ["Cypress / Playwright (E2E)", "Requieren navegador y backend levantados; son más lentos y no aíslan la lógica pura."],
        ["Karma + Jasmine", "Configuración más extensa y enfoque heredado; Jest ya incluye espías y temporizadores."],
        ["Mocha + Chai + Sinon", "Obliga a integrar tres paquetes y un reportero; Jest cubre todo en uno."],
    ],
    anchos=[4.4, 12.3],
)

titulo(doc, "4.4 Estructura del proyecto de pruebas", 2)
codigo(doc, """frontend-aleleo-tours/
├── js/                         <- código de producción (no se modifica)
└── pruebas/
    ├── package.json            <- Jest 29.7.0 + jest-environment-jsdom 29.7.0
    ├── jest.config.js          <- entorno jsdom, URL local y carpeta tests/
    ├── helpers/
    │   └── cargar-frontend.js  <- carga los scripts reales dentro del DOM
    ├── tests/
    │   ├── config.test.js
    │   ├── app.test.js
    │   ├── destinos.test.js
    │   ├── reportes.test.js
    │   ├── mis-solicitudes.test.js
    │   ├── panel-empleado.test.js
    │   └── gestion-empleados.test.js
    ├── resultados/             <- salida de consola, JSON y generador de evidencias
    └── evidencia/              <- capturas usadas en este documento""")

parrafo(doc, "El helper es la pieza que permite probar el código real. Los archivos del frontend no son módulos: se "
             "declaran funciones globales y unos archivos usan lo que definen otros (por ejemplo, destinos.js usa "
             "normalizarBusqueda de destinos-datos.js). El helper concatena los archivos en el mismo orden en que los "
             "carga el HTML, los evalúa en el ámbito global y publica las funciones necesarias, sin copiar ni "
             "reformatear el código de producción.")

titulo(doc, "4.5 Ejecución", 2)
codigo(doc, """# 1. Instalar dependencias (solo la primera vez)
cd frontend-aleleo-tours/pruebas
npm install

# 2. Ejecutar todas las pruebas
npx jest --verbose

# 3. Ejecutar un solo módulo
npx jest tests/destinos.test.js --verbose

# 4. Generar el JSON con los resultados
npx jest --json --outputFile=resultados/resultados.json""")

parrafo(doc, "La salida de la ejecución se guarda en resultados/salida-jest.txt y el JSON en "
             "resultados/resultados.json. Con esos dos archivos se generan las capturas y las tablas de este documento.")

titulo(doc, "4.6 Limitaciones", 2)
vinetas(doc, [
    "**No mide cobertura de líneas:** el código se evalúa con window.eval, por lo que Istanbul no puede instrumentarlo. "
    "La cobertura se expresa en términos de módulos y reglas cubiertas (7 de 13 archivos de js/, los que contienen "
    "lógica pura y de presentación crítica).",
    "**No reemplaza las pruebas E2E:** no valida la integración real con el backend ni el aspecto visual.",
    "**Depende del DOM simulado:** jsdom no implementa navegación ni todos los APIs del navegador; por eso las pruebas "
    "evitan las rutas que redirigen y se concentran en la lógica.",
])

# ===================================================================== 5. CASOS
doc.add_page_break()
titulo(doc, "5. Casos de prueba", 1)

parrafo(doc, "Se diseñaron 40 casos identificados como CP-01 a CP-40. Ocho de ellos tienen una variante de borde "
             "(sufijo b), de modo que la suite ejecuta 48 pruebas. Cada caso vive en un archivo de prueba y se "
             "identifica con el mismo código en la salida de Jest, lo que permite rastrear el resultado hasta el "
             "código fuente.")

tabla(
    doc,
    ["Suite", "Módulo", "Casos", "Estado"],
    [
        ["config.test.js", "js/core/config.js", "4", "Aprobada"],
        ["app.test.js", "js/core/app.js", "12", "Aprobada"],
        ["destinos.test.js", "js/cliente/destinos-datos.js + destinos.js", "13", "Aprobada"],
        ["reportes.test.js", "js/admin/reportes.js", "7", "Aprobada"],
        ["mis-solicitudes.test.js", "js/cliente/mis-solicitudes.js", "5", "Aprobada"],
        ["panel-empleado.test.js", "js/empleado/panel-empleado.js", "5", "Aprobada"],
        ["gestion-empleados.test.js", "js/admin/gestion-empleados.js", "2", "Aprobada"],
        ["Total", "7 suites", "48", "48/48"],
    ],
    anchos=[4.4, 7.7, 1.8, 2.8],
    alineaciones=[None, None, "centro", "centro"],
)

# ----- tabla detallada
doc.add_page_break()
titulo(doc, "5.1 Detalle de los casos ejecutados", 2)

ESPERADOS = {
    "CP-01": "localhost y 127.0.0.1 resuelven http://localhost:8082/api.",
    "CP-02": "www.aleleotours.com resuelve https://api.aleleotours.com/api.",
    "CP-03": "El override gana y se eliminan las barras finales.",
    "CP-04": "Sin host cae en el valor local por defecto.",
    "CP-05": "Los seis caracteres peligrosos se convierten en entidades HTML.",
    "CP-06": "null/undefined devuelven cadena vacía; los números se convierten.",
    "CP-07": "Cliente y desconocido devuelven true; equipo devuelve false.",
    "CP-08": "Una contraseña completa es válida y no genera mensaje.",
    "CP-09": "Cada requisito incumplido produce su mensaje específico.",
    "CP-10": "Guardar, leer y cerrar la sesión funciona sobre sessionStorage.",
    "CP-10b": "Un JSON corrupto no lanza excepción: devuelve null.",
    "CP-11": "Crea el contenedor, pinta el aviso y lo retira a los 3500 ms.",
    "CP-11b": "Un solo contenedor y tipo 'info' por defecto.",
    "CP-12": "El token solo se agrega a las llamadas del API propia.",
    "CP-12b": "Se respeta el Authorization que ya traía la petición.",
    "CP-12c": "Una respuesta 401 borra la sesión del usuario.",
    "CP-13": "'  San   ANDRÉS ' se normaliza a 'san andres'.",
    "CP-14": "Une los campos no vacíos separados por un espacio.",
    "CP-15": "Imagen por destino; un destino desconocido usa Cartagena.",
    "CP-16": "Categorías por palabra clave; sin coincidencia usa playa y ciudad.",
    "CP-16b": "OBS-01: el catálogo del backend pierde categorías frente al local.",
    "CP-17": "El DTO se traduce a la tarjeta con imagen y categorías.",
    "CP-18": "Precio no numérico = 0 y se aplican los valores por defecto.",
    "CP-19": "Solo los tipos aéreos anuncian vuelos.",
    "CP-20": "Devuelve '$ 320.000 COP' con el formato colombiano.",
    "CP-20b": "Une como máximo dos categorías ('Playa y Ciudad').",
    "CP-21": "Acota a 1-10 y usa 1 cuando el valor no es numérico.",
    "CP-22": "Coincide por nombre, título o palabras, sin acentos.",
    "CP-23": "Los 7 destinos locales tienen precio, imagen y categorías.",
    "CP-24": "HOY devuelve el mismo día en desde y hasta.",
    "CP-25": "SEMANA cubre 7 días (6 de diferencia).",
    "CP-26": "MES_ACTUAL inicia el día 1; MES_ANTERIOR cierra el mes.",
    "CP-27": "HISTÓRICO y presets desconocidos van sin fechas.",
    "CP-28": "El query string solo incluye los extremos informados.",
    "CP-29": "Traduce los estados y conserva los desconocidos.",
    "CP-30": "Traduce las categorías; null devuelve 'Consulta General'.",
    "CP-31": "Pasa a mayúsculas y usa PENDIENTE por defecto.",
    "CP-32": "Reserva habla de pago; las demás categorías, de atención.",
    "CP-33": "Traduce los estados finales y conserva los desconocidos.",
    "CP-34": "APROBADO, APPROVED y CONFIRMADO cuentan como pago aprobado.",
    "CP-35": "Formatea 320.000 y 1.234,5; un valor inválido da '0'.",
    "CP-36": "Sin sesión o con la cuenta inactiva no hay acceso.",
    "CP-37": "Solo ACTIVA + DISPONIBLE habilita el panel.",
    "CP-37b": "Sin estados declarados se asume ACTIVA y DISPONIBLE.",
    "CP-38": "Los KPI cuentan 6 asignadas, 2 pendientes, 1 en proceso y 2 resueltas.",
    "CP-38b": "Sin solicitudes los contadores quedan en cero.",
    "CP-39": "'Diana.Lopez@…' se reduce a 'diana.lopez'.",
    "CP-40": "Los valores vacíos no rompen el formulario.",
}

filas = []
ORDEN_SUITES = [
    "config.test.js",
    "app.test.js",
    "destinos.test.js",
    "reportes.test.js",
    "mis-solicitudes.test.js",
    "panel-empleado.test.js",
    "gestion-empleados.test.js",
]

for nombre in ORDEN_SUITES:
    suite = next(s for s in RESULTADOS["testResults"] if s["name"].endswith(nombre))
    for caso in suite["assertionResults"]:
        coincidencia = re.match(r"^(CP-\d+[a-z]?)\s*(?:\([^)]*\))?\s*:\s*(.*)$", caso["title"])
        if coincidencia:
            codigo_caso, descripcion = coincidencia.group(1), coincidencia.group(2)
        else:
            codigo_caso, descripcion = caso["title"], caso["title"]
        filas.append([
            codigo_caso,
            descripcion,
            ESPERADOS.get(codigo_caso, "Conforme a lo definido en el caso."),
            "PASS",
        ])

tabla(
    doc,
    ["ID", "Caso de prueba", "Resultado esperado", "Estado"],
    filas,
    anchos=[1.7, 7.2, 6.3, 1.5],
    tam=8.3,
    alineaciones=["centro", None, None, "centro"],
)

parrafo(doc, "")
parrafo(doc, "Los 48 casos finalizaron en estado PASS. La columna 'Estado' resume el veredicto de Jest: el valor real "
             "coincidió con el esperado en todas las aserciones.", cursiva=True, tam=9.5, color=GRIS)

# ----- evidencias
doc.add_page_break()
titulo(doc, "5.2 Evidencias de la ejecución", 2)
parrafo(doc, "Las capturas se generaron a partir de la salida real de la consola y del archivo JSON que produce Jest. "
             "No son maquetas: reflejan exactamente los 7 PASS, los 48 casos y el tiempo de ejecución de la corrida "
             "del 7 de noviembre de 2026.")

figura(doc, "01-resumen.png",
       "Figura 1. Resumen de la ejecución: 7 suites, 48 casos, 48 aprobados y 0 fallidos.",
       ancho_cm=16.5)

doc.add_page_break()
figura(doc, "02-consola-1.png",
       "Figura 2. Salida real de 'npx jest --verbose' (parte 1 de 2): módulos de destinos y núcleo.",
       ancho_cm=16.4)

doc.add_page_break()
figura(doc, "02-consola-2.png",
       "Figura 3. Salida real de 'npx jest --verbose' (parte 2 de 2): resto de módulos y resumen final.",
       ancho_cm=16.4)

doc.add_page_break()
figura(doc, "03-casos-1.png",
       "Figura 4. Detalle de los casos 1 a 24: identificador, módulo, descripción, tiempo y estado.",
       ancho_cm=16.4)

doc.add_page_break()
figura(doc, "03-casos-2.png",
       "Figura 5. Detalle de los casos 25 a 48 de la suite.",
       ancho_cm=16.4)

# ===================================================================== 6. RESPONSABLE
doc.add_page_break()
titulo(doc, "6. Responsable", 1)
parrafo(doc, "La actividad fue ejecutada por Cesar Leonardo Ramírez Montejo, integrante encargado del backend, la API y "
             "el despliegue. Para esta entrega asumió además la redacción del plan de pruebas del frontend y la "
             "ejecución de la suite con Jest.")

tabla(
    doc,
    ["Rol", "Nombre", "Responsabilidad en esta actividad"],
    [
        ["Responsable de la ejecución", "Cesar Leonardo Ramírez Montejo",
         "Diseño de los casos, configuración de Jest, ejecución, capturas y documento."],
        ["Revisión de la lógica del frontend", "Felipe Gonzales Quitero",
         "Validación de que los casos cubren las funciones reales del sitio."],
        ["Revisión de datos y evidencias", "Yerson Alexei Torres García",
         "Verificación de los datos del catálogo y de la coherencia con la base de datos."],
        ["Docente / instructor", "________________________",
         "Revisión, retroalimentación y aprobación de la entrega."],
    ],
    anchos=[4.6, 5.4, 6.7],
)

parrafo(doc, "")
parrafo(doc, "Responsable técnico del frontend en el proyecto: Felipe Gonzales Quitero. Responsable del repositorio y "
             "de la entrega: Cesar Leonardo Ramírez Montejo. La ejecución se realizó en el equipo de trabajo con "
             "Windows 10, Node.js 24.21.0 y npm 11.19.0.", cursiva=True, tam=9.5, color=GRIS)

# ===================================================================== 7. CRITERIOS
doc.add_page_break()
titulo(doc, "7. Criterios de entrada y salida", 1)

titulo(doc, "7.1 Criterios de entrada", 2)
parrafo(doc, "La ejecución se puede iniciar cuando se cumplen todas estas condiciones:")
vinetas(doc, [
    "El código del frontend está disponible y sin cambios pendientes por guardar.",
    "Node.js y npm están instalados (probado con Node 24.21.0 y npm 11.19.0).",
    "La carpeta pruebas/ contiene package.json, jest.config.js, el helper y los siete archivos de prueba.",
    "Las dependencias están instaladas (node_modules con Jest y jest-environment-jsdom).",
    "El entorno de ejecución tiene al menos 200 MB libres y permisos de escritura en pruebas/resultados.",
    "Los casos de prueba están revisados y aprobados por el equipo.",
])
parrafo(doc, "No se requiere backend en ejecución, base de datos, conexión a internet ni cuentas de usuario: las "
             "pruebas son autocontenidas.")

titulo(doc, "7.2 Criterios de salida", 2)
vinetas(doc, [
    "**Cobertura de ejecución:** los 48 casos se ejecutaron; ninguno quedó omitido (skipped) ni pendiente.",
    "**Resultado:** 48/48 en estado PASS y 0 fallos.",
    "**Trazabilidad:** cada caso tiene identificador CP, módulo, resultado esperado y evidencia en la consola.",
    "**Evidencias:** se generaron y almacenaron las capturas del resumen, la consola y el detalle de casos.",
    "**Defectos:** todo comportamiento no conforme quedó registrado en el punto 8 con severidad e impacto.",
    "**Documento:** el plan está completo, con portada, introducción, estrategia, herramienta, casos, responsable, "
    "criterios, errores y guion del video.",
])

titulo(doc, "7.3 Criterios de suspensión y reanudación", 2)
tabla(
    doc,
    ["Situación", "Acción"],
    [
        ["Falla la instalación de dependencias", "Se detiene la ejecución y se corrige el entorno antes de continuar."],
        ["Más del 20 % de los casos falla", "Se suspende, se analiza la causa raíz y se corrige el código o el caso."],
        ["Un caso depende de datos externos", "Se reemplaza por un doble de prueba o se marca fuera de alcance."],
        ["El código del frontend cambia durante la corrida", "Se repite la suite completa para evitar resultados obsoletos."],
        ["Un fallo no se puede reproducir", "Se documenta como intermitente y se adjunta la salida de consola."],
    ],
    anchos=[7.0, 9.7],
)

# ===================================================================== 8. ERRORES
doc.add_page_break()
titulo(doc, "8. Errores encontrados", 1)
parrafo(doc, "Durante la ejecución no se presentaron fallos bloqueantes: los 48 casos aprobaron. Sí se identificó una "
             "inconsistencia funcional que no rompe la aplicación, pero afecta los filtros del catálogo. Se registra a "
             "continuación con su evidencia y recomendación.")

tabla(
    doc,
    ["ID", "Severidad", "Módulo", "Descripción", "Estado"],
    [
        ["OBS-01", "Baja", "js/cliente/destinos-datos.js",
         "El catálogo local y el que llega del backend asignan categorías distintas a los mismos destinos.", "Abierto"],
    ],
    anchos=[1.8, 2.0, 4.6, 6.6, 1.7],
    tam=9,
    alineaciones=["centro", "centro", None, None, "centro"],
)

titulo(doc, "8.1 OBS-01 — Categorías inconsistentes entre la copia local y el backend", 2)
parrafo(doc, "La lista DESTINOS_DEFAULT declara Cartagena como 'playa' y 'ciudad', y Tayrona como 'naturaleza' y "
             "'playa'. Sin embargo, cuando el catálogo se pinta desde GET /api/servicios, la función "
             "resolverCategorias() clasifica los mismos destinos solo con las palabras clave de su tabla interna: "
             "Cartagena queda únicamente como 'playa' y Tayrona únicamente como 'naturaleza'.")

parrafo(doc, "Consecuencia: al usar los filtros del catálogo, una búsqueda por 'Ciudad' no muestra Cartagena y una "
             "búsqueda por 'Playa' no muestra Tayrona, aunque la copia local del mismo plan sí las mostraría. El "
             "usuario percibe que el filtro 'pierde' destinos según el origen de los datos.")

parrafo(doc, "Evidencia automatizada (caso CP-16b):")
codigo(doc, """const local = DESTINOS_DEFAULT.find(d => d.nombre === 'Cartagena');
local.categorias                          // ['playa', 'ciudad']

const backend = mapearServicio({ nombre: 'Paquete Vacacional Cartagena',
                                 destino: 'Cartagena', ... });
backend.categorias                        // ['playa']   <- falta 'ciudad'

// Lo mismo ocurre con Tayrona: local ['naturaleza', 'playa'] vs backend ['naturaleza']""")

parrafo(doc, "Causa raíz: existen dos fuentes de verdad para las categorías. La copia local las escribe a mano y "
             "resolverCategorias() las deduce de una tabla de palabras clave que no incluye 'cartagena' en 'ciudad' ni "
             "'tayrona' en 'playa'.")

parrafo(doc, "Recomendación (no aplicada en esta actividad porque el alcance es el plan de pruebas):")
vinetas(doc, [
    "Unificar la clasificación en una sola fuente: que DESTINOS_DEFAULT también se construya con resolverCategorias().",
    "Ampliar la tabla de palabras clave para que la deducción coincida con la intención del catálogo.",
    "Agregar una prueba de regresión que compare, destino por destino, las categorías del catálogo local con las del "
    "backend.",
])

caja_destacada(
    doc,
    "Impacto y prioridad",
    "Severidad baja: no impide reservar ni genera errores de datos; afecta la experiencia de búsqueda y los filtros.\n"
    "Prioridad sugerida: incluir la corrección en el próximo sprint del frontend y repetir la suite para verificar.\n"
    "Estado: abierto y documentado; la prueba CP-16b quedará en rojo cuando se corrija, avisando del cambio.",
    color_fondo="FFF7ED",
    color_borde="EA580C",
)

titulo(doc, "8.2 Comportamientos verificados que no son defectos", 2)
parrafo(doc, "Varios comportamientos podrían parecer errores en una lectura rápida, pero son decisiones "
             "deliberadas del frontend. Se dejaron cubiertos por pruebas para que un cambio futuro no las "
             "rompa sin aviso.")
tabla(
    doc,
    ["Comportamiento observado", "Por qué es correcto", "Caso que lo cubre"],
    [
        ["Un destino sin imagen conocida usa la foto de Cartagena.",
         "Es un respaldo intencional: evita que la tarjeta quede rota o vacía.", "CP-15"],
        ["Un estado o una categoría desconocidos se muestran tal cual.",
         "Evita ocultar información nueva que el backend ya reconozca.", "CP-29 y CP-30"],
        ["Una sesión sin estados declarados se asume ACTIVA y DISPONIBLE.",
         "Es el valor por defecto histórico del sistema para cuentas antiguas.", "CP-37b"],
        ["Un precio no numérico se muestra como 0 en lugar de NaN.",
         "Protege la interfaz ante datos corruptos o incompletos del backend.", "CP-18 y CP-35"],
        ["Una cadena vacía en el campo de estado vuelve a PENDIENTE.",
         "Mantiene una etiqueta comprensible aunque el dato llegue sin diligenciar.", "CP-31"],
        ["El token solo se agrega cuando la URL pertenece al API propia.",
         "Impide filtrar credenciales hacia dominios externos.", "CP-12 y CP-12b"],
    ],
    anchos=[6.4, 7.6, 2.7],
    tam=9,
    alineaciones=[None, None, "centro"],
)

parrafo(doc, "En conjunto, los 48 casos no solo confirman el camino feliz: también fijan el comportamiento "
             "esperado ante datos límite, datos corruptos y accesos no autorizados. Esa es la base para que la "
             "suite funcione como red de seguridad en las próximas entregas.", cursiva=True, tam=9.5, color=GRIS)

# ===================================================================== 9. VIDEO
doc.add_page_break()
titulo(doc, "9. Video corto con la ejecución de las pruebas", 1)
parrafo(doc, "El video debe mostrar la ejecución real de la suite, no una presentación de diapositivas. La duración "
             "objetivo es de 4 a 5 minutos y se graba en una sola toma continua (se puede editar solo para recortar "
             "silencios). A continuación se entrega el guion escena por escena, los comandos exactos y una lista de "
             "verificación antes de grabar.")

titulo(doc, "9.1 Preparación previa", 2)
vinetas(doc, [
    "Abrir PowerShell en frontend-aleleo-tours/pruebas y ejecutar npm install una vez.",
    "Ampliar la fuente de la terminal a 16-18 pt para que el texto se lea en el video.",
    "Cerrar notificaciones y ventanas que puedan distraer; dejar solo el editor y la terminal.",
    "Grabar a 1080p, con micrófono, y comprobar el audio con una prueba de 10 segundos.",
    "Tener a mano este documento para seguir el guion.",
])

titulo(doc, "9.2 Guion escena por escena", 2)
tabla(
    doc,
    ["Escena", "Tiempo", "Qué se muestra", "Qué se dice (resumen)"],
    [
        ["1. Presentación", "0:00-0:25",
         "Pantalla del proyecto y portada del documento.",
         "Nombre, ficha, proyecto SACE y objetivo: probar el frontend con Jest."],
        ["2. Contexto", "0:25-0:55",
         "Árbol de la carpeta pruebas/ (package.json, jest.config.js, tests/).",
         "Explicar que el frontend es estático y que se prueban los scripts reales en jsdom."],
        ["3. Ejecución", "0:55-1:45",
         "npx jest --verbose y la salida avanzando.",
         "Narrar los módulos que aparecen: config, app, destinos, reportes, solicitudes, panel y empleados."],
        ["4. Resultado", "1:45-2:20",
         "Final de la salida: 7 passed y 48 passed.",
         "Destacar 0 fallos y el tiempo de ejecución; relacionarlo con los criterios de salida."],
        ["5. Un caso", "2:20-3:10",
         "Abrir tests/destinos.test.js y señalar CP-21 (límites de pasajeros).",
         "Explicar la técnica de valores límite: 0, 1, 10, 99 y texto no numérico."],
        ["6. Regresión (opcional)", "3:10-3:50",
         "Cambiar temporalmente un valor esperado, ejecutar y ver el FAIL; deshacer y ver el PASS.",
         "Demostrar que la suite detecta un cambio y protege el comportamiento."],
        ["7. Cierre", "3:50-4:30",
         "Resumen de la ejecución y documento final.",
         "Conclusiones: 48/48, una observación registrada y el comando para repetir la suite."],
    ],
    anchos=[3.1, 2.7, 5.6, 5.3],
    tam=8.8,
    alineaciones=[None, "centro", None, None],
)

titulo(doc, "9.3 Comandos que deben aparecer en el video", 2)
codigo(doc, """# Situarse en la carpeta de pruebas
cd "C:\\Users\\cesar\\OneDrive\\Escritorio\\SACE_Entrega_Final\\1_Proyecto_Git\\Projecto_SACE\\frontend-aleleo-tours\\pruebas"

# Ejecutar toda la suite
npx jest --verbose

# (Opcional) Ejecutar solo el módulo del catálogo
npx jest tests/destinos.test.js --verbose

# (Opcional) Generar el JSON de resultados
npx jest --json --outputFile=resultados/resultados.json""")

titulo(doc, "9.4 Lista de verificación antes de entregar el video", 2)
vinetas(doc, [
    "Se ve el comando npx jest --verbose escrito completo.",
    "La salida muestra los 7 archivos con PASS y el resumen 'Tests: 48 passed, 48 total'.",
    "Se explica al menos un caso de prueba con su código y su resultado esperado.",
    "Se menciona la observación OBS-01 y por qué no bloquea la entrega.",
    "El audio es claro, sin ruido y sincronizado con la pantalla.",
    "El video dura entre 4 y 5 minutos y está en formato MP4 a 1080p.",
    "El archivo se nombra Actividad1_PlanPruebas_Frontend_Jest_Apellidos.mp4.",
])

# ===================================================================== ANEXOS
doc.add_page_break()
titulo(doc, "Anexo A. Cómo reproducir la entrega", 1)
parrafo(doc, "La entrega se compone de los siguientes archivos y carpetas:")
tabla(
    doc,
    ["Elemento", "Ruta"],
    [
        ["Documento Word", "docs/Actividad-1-Plan-de-Pruebas-Frontend-Jest.docx"],
        ["Proyecto de pruebas", "frontend-aleleo-tours/pruebas/"],
        ["Casos automatizados", "frontend-aleleo-tours/pruebas/tests/ (7 archivos, 48 casos)"],
        ["Helper de carga", "frontend-aleleo-tours/pruebas/helpers/cargar-frontend.js"],
        ["Salida de consola", "frontend-aleleo-tours/pruebas/resultados/salida-jest.txt"],
        ["Resultados JSON", "frontend-aleleo-tours/pruebas/resultados/resultados.json"],
        ["Capturas", "frontend-aleleo-tours/pruebas/evidencia/ (01-resumen, 02-consola-1, 02-consola-2, 03-casos-1 y 03-casos-2)"],
    ],
    anchos=[4.6, 12.1],
)

parrafo(doc, "")
parrafo(doc, "Para repetir la ejecución desde cero: npm install dentro de pruebas/ y luego npx jest --verbose. Para "
             "regenerar las capturas: node resultados/generar-evidencia.js y abrir los HTML de evidencia/ en el "
             "navegador.")

titulo(doc, "Anexo B. Glosario breve", 1)
tabla(
    doc,
    ["Término", "Definición"],
    [
        ["Jest", "Marco de pruebas de JavaScript con aserciones, mocks y temporizadores."],
        ["jsdom", "Implementación del DOM del navegador en JavaScript, usada como entorno de prueba."],
        ["Caso de prueba", "Conjunto de datos de entrada, pasos y resultado esperado que se verifica."],
        ["PASS / FAIL", "Resultado de una prueba: coincidió o no con lo esperado."],
        ["Mock / espía", "Objeto simulado que registra cómo se usó y evita llamadas reales."],
        ["Valor límite", "Extremo de un rango válido, donde suelen aparecer los errores."],
        ["Regresión", "Repetición de las pruebas para asegurar que un cambio no rompió lo anterior."],
    ],
    anchos=[3.6, 13.1],
)

salida = os.path.join(DOCS, "Actividad-1-Plan-de-Pruebas-Frontend-Jest.docx")
os.makedirs(DOCS, exist_ok=True)
doc.save(salida)
print("Documento generado:", salida)
print("Tablas de casos:", len(filas), "| suites:", SUITES, "| total:", TOTAL)
