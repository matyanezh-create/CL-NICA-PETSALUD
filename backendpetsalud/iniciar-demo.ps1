$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
if (-not $env:JWT_SECRET) {
    $jwtBytes = New-Object byte[] 32
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    $rng.GetBytes($jwtBytes)
    $rng.Dispose()
    $env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
    Write-Host 'Clave JWT temporal generada para esta ejecucion.'
}
if (-not $env:DEMO_PASSWORD) {
    $secure = Read-Host 'Clave demo de 12 a 72 caracteres ASCII imprimibles' -AsSecureString
    $env:DEMO_PASSWORD = [System.Net.NetworkCredential]::new('', $secure).Password
}
try {
    & .\mvnw.cmd '-Dspring-boot.run.profiles=demo' spring-boot:run
} finally {
    Remove-Item Env:DEMO_PASSWORD -ErrorAction SilentlyContinue
}
