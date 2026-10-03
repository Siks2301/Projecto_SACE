# =====================================================================
# SACE / AleLeo Tours - Prueba end-to-end del modulo de pagos
# =====================================================================
# QUE PRUEBA
#   El flujo completo contra el backend REAL, en modo pasarela simulada
#   (app.pagos.pasarela=mock), que no necesita internet ni llaves de Wompi:
#
#     1. registro de cliente y creacion de solicitud
#     2. configuracion de pagos que ve el navegador
#     3. cotizacion: el monto sale del catalogo, no del cliente
#     4. inicio de pago: nace PENDIENTE, con referencia PAGO-XXXXX
#     5. polling hasta que la pasarela resuelve la transaccion
#     6. anti-doble-cobro: el segundo intento se rechaza con 400
#     7. intento de imponer el precio: monto / amount / precio se rechazan
#     8. descarga del comprobante PDF
#     9. webhook con firma invalida: se rechaza con 401
#    10. IDOR: no se puede operar sobre la solicitud de otro cliente
#
# COMO EJECUTARLO
#   1. Con el backend arrancado en modo mock (por defecto, sin llaves).
#   2. powershell -NoProfile -ExecutionPolicy Bypass -File .\test_pagos_pasarela.ps1
#
# NOTAS
#   - Cada ejecucion registra un cliente nuevo con correo unico, porque el
#     correo es la clave primaria del login. No borra nada.
#   - Los nombres de campo de la API se tomaron de los records de
#     PagoDTOs.java: idPago, referencia, estado, comprobanteDisponible,
#     urlCheckout, yaPagado, metodos[{codigo,nombre}].
#   - Requiere PowerShell 5 o superior y el backend en el puerto 8082.
# =====================================================================

$ErrorActionPreference = 'Stop'
$base = 'http://localhost:8082/api'
$ok = 0; $fail = 0

function Check($nombre, $cond, $detalle) {
    if ($cond) {
        $script:ok++
        Write-Host "  [OK]   $nombre" -ForegroundColor Green
    } else {
        $script:fail++
        Write-Host "  [FALLA] $nombre -> $detalle" -ForegroundColor Red
    }
}

function RawPost($uri, $body, $hdr) {
    try {
        $r = Invoke-WebRequest -Uri $uri -Method Post -Headers $hdr -ContentType 'application/json' -Body $body -UseBasicParsing
        return @{ code = [int]$r.StatusCode; body = $r.Content }
    } catch {
        $resp = $_.Exception.Response
        $sr = New-Object System.IO.StreamReader($resp.GetResponseStream())
        return @{ code = [int]$resp.StatusCode; body = $sr.ReadToEnd() }
    }
}

Write-Host "`n=== 1. Registro de cliente ===" -ForegroundColor Cyan
$email = "prueba.pasarela.$([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())@test.com"
$reg = Invoke-RestMethod -Uri "$base/auth/registro" -Method Post -ContentType 'application/json' -Body (@{
    nombre='Prueba'; apellido='Pasarela'; email=$email; telefono='3001234567'; contrasenia='DemoClave2026!a'
} | ConvertTo-Json)
$hdr = @{ Authorization = "Bearer $($reg.token)" }
Check 'registro devuelve token de CLIENTE' ($reg.tipoUsuario -eq 'CLIENTE') "tipo=$($reg.tipoUsuario)"

Write-Host "`n=== 2. Crear solicitud con servicio 1 (precio 320.000) ===" -ForegroundColor Cyan
$sol = Invoke-RestMethod -Uri "$base/solicitudes" -Method Post -Headers $hdr -ContentType 'application/json' -Body (@{
    titulo='Prueba pasarela'; asunto='Pago en linea'; descripcion='Solicitud de prueba'
    prioridad='MEDIA'; categoria='PAQUETES'; servicioGeneradoId=1
} | ConvertTo-Json)
$idSol = $sol.id
Check 'solicitud creada' ($idSol -gt 0) "id=$idSol"

Write-Host "`n=== 3. GET /pagos/configuracion ===" -ForegroundColor Cyan
$cfg = Invoke-RestMethod -Uri "$base/pagos/configuracion" -Headers $hdr
Write-Host ("  pasarela=" + $cfg.pasarela + "  simulado=" + $cfg.modoSimulado + "  checkoutExterno=" + $cfg.checkoutExterno + "  metodos=" + $cfg.metodos.Count)
Check 'modo simulado activo' ($cfg.modoSimulado -eq $true) "modoSimulado=$($cfg.modoSimulado)"
Check 'NO hay checkout externo en modo simulado' ($cfg.checkoutExterno -eq $false) "checkoutExterno=$($cfg.checkoutExterno)"
Check 'lista metodos de pago' ($cfg.metodos.Count -ge 1) "ninguno"

Write-Host "`n=== 4. GET /pagos/cotizacion/$idSol ===" -ForegroundColor Cyan
$cot = Invoke-RestMethod -Uri "$base/pagos/cotizacion/$idSol" -Headers $hdr
Write-Host ("  titulo=" + $cot.titulo + "  monto=" + $cot.monto + " " + $cot.moneda + "  yaPagado=" + $cot.yaPagado)
Check 'el monto viene del catalogo (320000)' ([decimal]$cot.monto -eq 320000) "monto=$($cot.monto)"
Check 'marca que aun no esta pagada' ($cot.yaPagado -eq $false) "yaPagado=$($cot.yaPagado)"

Write-Host "`n=== 5. POST /pagos con metodo PSE ===" -ForegroundColor Cyan
$pago = Invoke-RestMethod -Uri "$base/pagos" -Method Post -Headers $hdr -ContentType 'application/json' -Body (@{
    solicitudId=$idSol; metodoPago='PSE'
} | ConvertTo-Json)
$ref = $pago.referencia
$idPago = $pago.idPago
Write-Host ("  idPago=$idPago  referencia=$ref  estado=" + $pago.estado + "  checkout=" + $pago.urlCheckout)
Check 'referencia con formato PAGO-XXXXX' ($ref -match '^PAGO-[A-Z0-9]{5}$') "ref=$ref"
Check 'nace PENDIENTE, no aprobado de una' ($pago.estado -eq 'PENDIENTE') "estado=$($pago.estado)"
Check 'el monto lo puso el servidor' ([decimal]$pago.monto -eq 320000) "monto=$($pago.monto)"
Check 'no hay URL de checkout en modo simulado' ([string]::IsNullOrWhiteSpace($pago.urlCheckout)) "url=$($pago.urlCheckout)"
Check 'aun no hay comprobante' ($pago.comprobanteDisponible -eq $false) "disponible=$($pago.comprobanteDisponible)"

Write-Host "`n=== 6. Polling del estado hasta que resuelva ===" -ForegroundColor Cyan
$final = $null
for ($i = 0; $i -lt 15; $i++) {
    Start-Sleep -Milliseconds 1000
    $est = Invoke-RestMethod -Uri "$base/pagos/estado/$ref" -Headers $hdr
    Write-Host ("  t+$($i+1)s estado=" + $est.estado)
    if ($est.estado -ne 'PENDIENTE') { $final = $est; break }
}
Check 'el pago llego a un estado final' ($null -ne $final) 'sigue PENDIENTE'
if ($final) {
    Check 'estado final APROBADO' ($final.estado -eq 'APROBADO') "estado=$($final.estado)"
    Check 'registra fecha de aprobacion' ($null -ne $final.fechaAprobacion) 'null'
    Check 'el comprobante ya esta disponible' ($final.comprobanteDisponible -eq $true) "disponible=$($final.comprobanteDisponible)"
}

Write-Host "`n=== 7. Anti-doble-cobro: pagar otra vez la misma solicitud ===" -ForegroundColor Cyan
$r7 = RawPost "$base/pagos" (@{ solicitudId=$idSol; metodoPago='TARJETA' } | ConvertTo-Json) $hdr
Write-Host "  HTTP $($r7.code): $($r7.body)"
Check 'segundo cobro rechazado con 400' ($r7.code -eq 400) "codigo=$($r7.code)"

Write-Host "`n=== 8. Intento de imponer el precio ===" -ForegroundColor Cyan
foreach ($campo in @('monto','amount','precio')) {
    $r8 = RawPost "$base/pagos" (@{ solicitudId=$idSol; metodoPago='TARJETA'; $campo = 1 } | ConvertTo-Json) $hdr
    Write-Host "  campo '$campo' -> HTTP $($r8.code)"
    Check "rechaza el campo '$campo' con 400" ($r8.code -eq 400) "codigo=$($r8.code)"
    Check "el mensaje explica que el monto no lo elige el cliente" ($r8.body -match 'monto no lo puede elegir el cliente') "body=$($r8.body)"
}

Write-Host "`n=== 9. Comprobante PDF ===" -ForegroundColor Cyan
$destino = "$env:TEMP\opencode\comprobante_prueba.pdf"
Invoke-WebRequest -Uri "$base/pagos/comprobante/$idPago" -Headers $hdr -OutFile $destino -UseBasicParsing
$bytes = (Get-Item $destino).Length
$head = [System.IO.File]::ReadAllBytes($destino)[0..3]
$esPdf = ($head[0] -eq 0x25 -and $head[1] -eq 0x50 -and $head[2] -eq 0x44 -and $head[3] -eq 0x46)
Write-Host "  archivo=$destino bytes=$bytes"
Check 'el comprobante es un PDF real' $esPdf 'firma distinta de %PDF'
Check 'el PDF tiene contenido' ($bytes -gt 1000) "bytes=$bytes"

Write-Host "`n=== 10. Webhook con checksum invalido ===" -ForegroundColor Cyan
$ev = @{
    event='transaction.status.updated'
    data=@{ transaction=@{ id='prv_falso'; status='APPROVED'; reference=$ref; amount_in_cents=32000000 } }
    sent_at='2026-01-01T00:00:00.000Z'
    signature=@{ properties=@('transaction.id','transaction.reference'); timestamp=1700000000; checksum='0000' }
} | ConvertTo-Json -Depth 8
$r10 = RawPost "$base/pagos/webhook/wompi" $ev @{}
Write-Host "  HTTP $($r10.code): $($r10.body)"
Check 'webhook con firma falsa rechazado con 401' ($r10.code -eq 401) "codigo=$($r10.code)"

Write-Host "`n=== 11. IDOR: cotizar una solicitud ajena ===" -ForegroundColor Cyan
try {
    Invoke-RestMethod -Uri "$base/pagos/cotizacion/18" -Headers $hdr | Out-Null
    $code = 200
} catch { $code = [int]$_.Exception.Response.StatusCode }
Check 'no permite operar sobre solicitud ajena' ($code -ge 400) "codigo=$code"

Write-Host "`n=== 12. GET /pagos/solicitud/$idSol ===" -ForegroundColor Cyan
$porSol = Invoke-RestMethod -Uri "$base/pagos/solicitud/$idSol" -Headers $hdr
$lista = @($porSol | Where-Object { $_ -ne $null })
Check 'la solicitud lista 1 solo pago' ($lista.Count -eq 1) "count=$($lista.Count)"
Check 'el pago listado esta APROBADO' ($lista[0].estado -eq 'APROBADO') "estado=$($lista[0].estado)"
Check 'el pago listado trae el comprobante' ($lista[0].comprobanteDisponible -eq $true) "disponible=$($lista[0].comprobanteDisponible)"

Write-Host "`n================================" -ForegroundColor Cyan
Write-Host "  OK: $ok   FALLAS: $fail" -ForegroundColor $(if ($fail -eq 0) { 'Green' } else { 'Red' })
Write-Host "================================" -ForegroundColor Cyan
if ($fail -gt 0) { exit 1 }