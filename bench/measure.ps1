param(
    [string]$baseUrl = "http://localhost:8080/api/duplicates/detect",
    [int]$size = 8000,
    [int]$runs = 5
)

$csvPath = "bench/resultados_benchmark.csv"
"Threads,Run,TimeMs,Count" | Out-File $csvPath -Encoding utf8

$threadsList = @(1, 2, 4, 8)
$baseCount = -1

Write-Host "Iniciando benchmark..."
Write-Host "Aquecendo a JVM (1 run com 1 thread)..."
$warmup = Invoke-RestMethod -Uri "$baseUrl?size=$size&threads=1"
Write-Host "Aquecimento concluído. Tempo: $($warmup.elapsedMs)ms"

foreach ($threads in $threadsList) {
    Write-Host "`nTestando com $threads thread(s)..."
    for ($i = 1; $i -le $runs; $i++) {
        $result = Invoke-RestMethod -Uri "$baseUrl?size=$size&threads=$threads"
        $timeMs = $result.elapsedMs
        $count = $result.duplicates.Count
        
        Write-Host "  Run $i - Tempo: ${timeMs}ms - Duplicatas: $count"
        
        if ($baseCount -eq -1) {
            $baseCount = $count
        } elseif ($count -ne $baseCount) {
            Write-Host "ERRO: Contagem de duplicatas divergente! Esperado: $baseCount, Obtido: $count" -ForegroundColor Red
        }

        "$threads,$i,$timeMs,$count" | Out-File $csvPath -Append -Encoding utf8
    }
}

Write-Host "`nAnálise dos Resultados:"
$data = Import-Csv $csvPath

$seqAvg = ($data | Where-Object Threads -eq 1 | Measure-Object -Property TimeMs -Average).Average

foreach ($threads in $threadsList) {
    $avg = ($data | Where-Object Threads -eq $threads | Measure-Object -Property TimeMs -Average).Average
    $speedup = $seqAvg / $avg
    Write-Host "Threads: $threads - Média: $([math]::Round($avg, 2))ms - Speedup: $([math]::Round($speedup, 2))x"
}
Write-Host "`nCSV salvo em $csvPath"
