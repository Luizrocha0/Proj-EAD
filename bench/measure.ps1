param(
    [string]$baseUrl = "http://localhost:8080/api/duplicates/detect",
    [int[]]$sizes = @(8000, 16000),
    [ValidateRange(1, 2147483647)]
    [int]$runs = 5
)

$ErrorActionPreference = "Stop"

$scriptDir = $PSScriptRoot
if (-not $scriptDir) { $scriptDir = "." }

$rawCsv = Join-Path $scriptDir "measurements.csv"
$aggCsv = Join-Path $scriptDir "measurements_agg.csv"
$rawTmp = "${rawCsv}.tmp"
$aggTmp = "${aggCsv}.tmp"
$csvCulture = [System.Globalization.CultureInfo]::InvariantCulture

# Publicar os resultados somente após concluir toda a coleta com sucesso.
"size,mode,threads,run,responseTimeMs,processingTimeMs,duplicatesFound,status" | Out-File $rawTmp -Encoding utf8
"size,mode,threads,avgResponseTimeMs,speedup" | Out-File $aggTmp -Encoding utf8

$threadsList = @(1, 2, 4, 8)
$mode = "platform"

Write-Host "Iniciando benchmark..."
Write-Host "Aquecendo a JVM (1 run com 1 thread)..."
$warmupUrl = "${baseUrl}?size=8000&threads=1&mode=$mode"
try {
    $warmup = Invoke-RestMethod -Uri $warmupUrl
    if ($warmup.status -ne "SUCCESS") { throw "Warmup falhou com status $($warmup.status)" }
    Write-Host "Aquecimento concluído. Tempo interno: $($warmup.processingTimeMs)ms"
} catch {
    Write-Host "ERRO no aquecimento: $_" -ForegroundColor Red
    exit 1
}

$aggregatedData = @()

foreach ($size in $sizes) {
    Write-Host "`nTestando tamanho $size..."
    $baseCount = -1
    $seqAvgTime = -1

    foreach ($threads in $threadsList) {
        Write-Host "  Threads: $threads"
        $times = @()

        for ($i = 1; $i -le $runs; $i++) {
            $url = "${baseUrl}?size=${size}&threads=${threads}&mode=${mode}"
            
            $sw = [System.Diagnostics.Stopwatch]::StartNew()
            try {
                $result = Invoke-RestMethod -Uri $url
            } catch {
                Write-Host "ERRO: Falha na requisição HTTP: $_" -ForegroundColor Red
                exit 1
            }
            $sw.Stop()
            $responseTimeMs = $sw.ElapsedMilliseconds

            if (-not $result.PSObject.Properties.Match('status')) {
                Write-Host "ERRO: Campo 'status' ausente na resposta!" -ForegroundColor Red
                exit 1
            }
            if ($result.status -ne "SUCCESS") {
                Write-Host "ERRO: Status não é SUCCESS. Mensagem: $($result.message)" -ForegroundColor Red
                exit 1
            }
            if (-not $result.PSObject.Properties.Match('processingTimeMs') -or -not $result.PSObject.Properties.Match('duplicatesFound')) {
                Write-Host "ERRO: Campos obrigatórios ausentes!" -ForegroundColor Red
                exit 1
            }

            $procTime = $result.processingTimeMs
            $count = $result.duplicatesFound

            Write-Host "    Run $i - Response: ${responseTimeMs}ms, Process: ${procTime}ms, Duplicatas: $count"

            if ($baseCount -eq -1) {
                $baseCount = $count
            } elseif ($count -ne $baseCount) {
                Write-Host "ERRO: Contagem de duplicatas divergente! Esperado: $baseCount, Obtido: $count" -ForegroundColor Red
                exit 1
            }

            "${size},${mode},${threads},${i},${responseTimeMs},${procTime},${count},$($result.status)" | Out-File $rawTmp -Append -Encoding utf8
            
            $times += $responseTimeMs
        }

        # Calcular média
        $sum = 0
        foreach ($t in $times) { $sum += $t }
        $avg = $sum / $runs

        if ($threads -eq 1) {
            $seqAvgTime = $avg
            $speedup = 1.0
        } else {
            $speedup = $seqAvgTime / $avg
        }

        $aggregatedData += [PSCustomObject]@{
            Size = $size
            Mode = $mode
            Threads = $threads
            Avg = $avg
            Speedup = $speedup
        }

        $avgText = $avg.ToString("F2", $csvCulture)
        $speedupText = $speedup.ToString("F6", $csvCulture)
        "${size},${mode},${threads},${avgText},${speedupText}" | Out-File $aggTmp -Append -Encoding utf8
    }
}

$rawExisted = Test-Path -LiteralPath $rawCsv
$previousRaw = if ($rawExisted) { [System.IO.File]::ReadAllBytes($rawCsv) } else { $null }
$rawPublished = $false
try {
    Move-Item -LiteralPath $rawTmp -Destination $rawCsv -Force
    $rawPublished = $true
    Move-Item -LiteralPath $aggTmp -Destination $aggCsv -Force
} catch {
    # Se o segundo arquivo estiver bloqueado, restaurar o bruto anterior.
    if ($rawPublished) {
        if ($rawExisted) {
            [System.IO.File]::WriteAllBytes($rawCsv, $previousRaw)
        } else {
            Remove-Item -LiteralPath $rawCsv
        }
    }
    throw
}

Write-Host "`nAnálise Agregada:"
$aggregatedData | Format-Table -Property Size, Mode, Threads, @{Name="AvgResponse(ms)";Expression={[math]::Round($_.Avg, 2)}}, @{Name="Speedup";Expression={[math]::Round($_.Speedup, 2)}}

Write-Host "Dados brutos salvos em $rawCsv"
Write-Host "Dados agregados salvos em $aggCsv"

