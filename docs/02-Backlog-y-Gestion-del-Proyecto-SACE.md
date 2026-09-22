# Backlog y gestión del proyecto

_Proyecto académico AleLeo Tours (SENA) · Ficha / Grupo: 3171149-B_
_Cesar Leonardo Ramírez Montejo · Yerson Alexei Torres Garcia · Felipe Gonzales Quitero_
_21 de septiembre de 2026_

## 2. Backlog del proyecto y gestión
### 2.1 Metodología de gestión
Se aplica una adaptación ágil tipo Scrum a la medida del contexto académico: historias de usuario, backlog priorizado, planificación por sprints semanales, revisión de avances y evidencias fechadas (informes, modelo de datos, diagramas y resultados de pruebas).

### 2.2 Backlog del producto
El backlog se deriva de las 11 historias de usuario documentadas en 'Historias_de_usuario.docx' y se organiza por épica funcional (módulo). El estado refleja la entrega verificada al 21/09/2026.

| Épica / Módulo | Historias vinculadas | Tareas principales | Prioridad | Estado |
|---|---|---|---|---|
| Autenticación | HU-01, HU-02 | Formulario de registro; login con token; cifrado BCrypt | Alta | Entregado |
| Solicitudes | HU-03, HU-08 | CRUD por categorías; protección de propiedad; reglas de estado | Alta | Entregado |
| Chatbot | HU-04 | Motor de intenciones; FAQs; catálogo desde API con respaldo local | Alta | Entregado |
| Asignación y atención | HU-05, HU-06, HU-07, HU-09 | Panel asesor; mensajes por solicitud; adjuntos; generación de servicio | Media | Entregado |
| Administración | HU-10, HU-11 | CRUD clientes/empleados/servicios/solicitudes; FAQs | Media | Entregado |
| Pagos y comprobantes | RF-11, RF-12 | Pago con precio real; anti-doble-cobro; PDF con sello e IVA | Alta | Entregado |
| Reportes y KPIs | RF-13 | Consolidados y exportación PDF restringidos a administradores | Media | Entregado |
| Pruebas y calidad | — | Suites E2E API + frontend; corrección de hallazgos de seguridad | Alta | Entregado |

### 2.3 Planificación por sprints
| Sprint | Periodo (2026) | Objetivo | Entregable / evidencia |
|---|---|---|---|
| S1 | 4 – 21 ago | Análisis y levantamiento de requerimientos | Historia_necesidad.docx; historias de usuario (borrador) |
| S2 | 22 ago – 4 sep | Diseño del sistema y de la base de datos | Diagramas de casos de uso, clases, MER y modelo relacional; diccionario de datos; Historias_de_usuario.docx |
| S3 | 5 – 12 sep | Construcción del backend y base de datos | Informe-01-Backend-SACE.docx; API REST funcional en /api |
| S4 | 13 – 19 sep | Construcción del frontend e integración | Informe-02-Frontend-AleLeo-Tours.docx; páginas y chatbot integrados |
| S5 | 20 – 21 sep | Pruebas, seguridad, documentación y publicación | Plan de Pruebas; 62/62 pruebas automáticas; README; plan de despliegue; repositorio GitHub |

### 2.4 Seguimiento y cumplimiento
| Métrica | Valor |
|---|---|
| Sprints planificados | 5 |
| Sprints completados | 5 (100 %) |
| Historias de usuario documentadas | 11 (HU-01 … HU-11) |
| Requerimientos funcionales identificados | 14 (RF-01 … RF-14) |
| Requerimientos no funcionales | 8 (RNF-01 … RNF-08) |
| Casos de prueba ejecutados | 62 PASS / 0 FAIL (21/09/2026) |
| Pruebas de regresión del backend | 37 |
| Pruebas de correcciones de seguridad | 12 |
| Pruebas del frontend (DOM simulado) | 13 |
| Repositorio publicado | github.com/Siks2301/Projecto_SACE |

### 2.5 Distribución del trabajo por integrante
Distribución propuesta del equipo (ajustable según la participación real de cada integrante):

| Integrante | Rol / aporte principal |
|---|---|
| Cesar Leonardo Ramírez Montejo | Backend y seguridad: API REST, token HMAC, interceptor de autorización, pagos y comprobante PDF, pruebas E2E del backend. |
| Yerson Alexei Torres Garcia | Base de datos: modelo entidad-relación, modelo relacional, diccionario de datos y consultas. |
| Felipe Gonzales Quitero | Frontend: páginas web, componentes (chatbot, widgets) e integración con la API. |

### 2.6 Seguimiento de tareas
- T1 Registro y autenticación — completado y probado (A1–A9).
- T2 Solicitudes con protección de propiedad — completado y probado (S1–S9).
- T3 Mensajería por solicitud — completado y probado (M1–M4).
- T4 Pagos, anti-doble-cobro y comprobante PDF — completado y probado (P1–P8, T1–T5).
- T5 Reportes/KPIs admin — completado y probado (R1–R5).
- T6 Catálogo público — completado y probado (G1).
- T7 Correcciones de seguridad (IDOR, doble cobro, XSS, precios estáticos) — completado y probado (fixes 1–15).
- T8 Documentación y despliegue — completado (README, planes, docs/).