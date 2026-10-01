param([ValidateSet('sistema_odontologo','sistema_odontologo_test')][string]$Database='sistema_odontologo')
$ErrorActionPreference='Stop'
$backendRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$backupDirectory=Join-Path $backendRoot '.runtime/backups'
$pgTools='C:/Program Files/PostgreSQL/18/bin'
$previousPassword=$env:PGPASSWORD
try {
 $env:PGPASSWORD='admin'
 $actual=& (Join-Path $pgTools 'psql.exe') -h localhost -U postgres -d $Database -Atc 'select current_database()'
 if($LASTEXITCODE -ne 0 -or $actual.Trim() -ne $Database){throw 'No se pudo verificar la base local.'}
 New-Item -ItemType Directory -Force -Path $backupDirectory | Out-Null
 $destination=Join-Path $backupDirectory ($Database+'-'+(Get-Date -Format 'yyyyMMdd-HHmmss-fff')+'.dump')
 & (Join-Path $pgTools 'pg_dump.exe') -h localhost -U postgres -d $Database --format=custom --no-owner --no-acl --file=$destination
 if($LASTEXITCODE -ne 0){throw 'Falló la generación del respaldo.'}
 $archive=& (Join-Path $pgTools 'pg_restore.exe') --list $destination
 if($LASTEXITCODE -ne 0 -or -not($archive -match 'TABLE DATA public document_content') -or -not($archive -match 'TABLE DATA public encounter_revision') -or -not($archive -match 'TABLE DATA public treatment_plan') -or -not($archive -match 'TABLE DATA public charge_entry')){throw 'El respaldo no contiene las tablas clínicas, documentales y financieras.'}
 $hash=Get-FileHash -LiteralPath $destination -Algorithm SHA256
 [pscustomobject]@{Database=$Database;Archive=$destination;Bytes=(Get-Item -LiteralPath $destination).Length;SHA256=$hash.Hash;IncludesDocumentContent=$true}
} finally {
 if($null -eq $previousPassword){Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue}else{$env:PGPASSWORD=$previousPassword}
}
