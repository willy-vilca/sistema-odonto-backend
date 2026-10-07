$ErrorActionPreference='Stop'
$backendRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$privateFile=Join-Path $backendRoot 'config/kapso.local.properties'
$keyLine=Get-Content -LiteralPath $privateFile | Where-Object {$_ -match '^odontocare\.kapso\.api-key='}
$kapsoKey=($keyLine -split '=',2)[1].Trim()
if(-not $kapsoKey -or $kapsoKey.StartsWith('REEMPLAZAR')) {throw 'Guarda primero la API key en config/kapso.local.properties.'}
try {
 $result=Invoke-RestMethod -Uri 'https://api.kapso.ai/platform/v1/whatsapp/phone_numbers' -Headers @{'X-API-Key'=$kapsoKey} -Method Get -TimeoutSec 15
 $result.data | Select-Object id,phone_number_id,name,display_phone_number,is_sandbox | Format-List
} catch {throw 'No se pudieron consultar los números de Kapso. Revisa la clave del proyecto y la conexión; la clave no se muestra.'}
