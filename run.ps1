param([switch]$Interativo)
# Escolha worst, circular, buddy, paginacao ou personalizado.
$Caso = 'worst'
$PassoAPasso = $false

# Para usar outro arquivo ou outros tamanhos, edite a linha personalizado.
$Casos = @{
    worst         = @('tests/example-1.txt', '--mode', '1', '--mem', '32', '--policy', 'worst')
    circular      = @('tests/example-1.txt', '--mode', '1', '--mem', '32', '--policy', 'circular')
    buddy         = @('tests/example-3.txt', '--mode', '2', '--mem', '64')
    paginacao     = @('tests/example-4.txt', '--mode', '3', '--mem', '64', '--page', '8')
    personalizado = @('tests/teste3.txt', '--mode', '3', '--mem', '64', '--page', '8')
}

$ErrorActionPreference = 'Stop'

$argumentos = @()
if (-not $Interativo) {
    if (-not $Casos.ContainsKey($Caso)) {
        throw "Caso desconhecido '$Caso'. Escolha um destes: $($Casos.Keys -join ', ')."
    }

    $execucao = if ($PassoAPasso) { '--step' } else { '--continuous' }
    $argumentos = $Casos[$Caso] + $execucao
}
$jdkBin = Join-Path $PSScriptRoot 'oracleJdk-27/bin'
$javac = Join-Path $jdkBin 'javac.exe'
$java = Join-Path $jdkBin 'java.exe'
if (-not ((Test-Path -LiteralPath $javac -PathType Leaf) -and (Test-Path -LiteralPath $java -PathType Leaf))) {
    $compilador = Get-Command javac.exe -CommandType Application -ErrorAction SilentlyContinue
    if (-not $compilador) {
        throw 'JDK 17 ou superior ausente. Use o JDK local ou coloque a pasta bin do JDK no PATH.'
    }
    $javac = $compilador.Source
    $java = Join-Path (Split-Path -Parent $javac) 'java.exe'
}

Push-Location -LiteralPath $PSScriptRoot
try {
    $fontes = @(Get-ChildItem -LiteralPath src -Recurse -File -Filter '*.java' | Select-Object -ExpandProperty FullName)
    & $javac -encoding UTF-8 --release 17 -d out @fontes
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }

    & $java -cp out memory.Main @argumentos
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
