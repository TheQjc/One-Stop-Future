param(
    [string]$BaseUrl = "http://127.0.0.1:8080",
    [string]$Endpoint = "/api/discover?tab=ALL&period=WEEK&limit=20",
    [int]$Requests = 100,
    [int]$Concurrency = 10,
    [string]$Token = ""
)

$headers = @{}
if ($Token -ne "") {
    $headers["Authorization"] = "Bearer $Token"
}

$target = "$BaseUrl$Endpoint"
$started = Get-Date
$jobs = @()
$perJob = [Math]::Ceiling($Requests / [double]$Concurrency)

Write-Host "Target: $target"
Write-Host "Requests: $Requests, Concurrency: $Concurrency"

for ($i = 0; $i -lt $Concurrency; $i++) {
    $remaining = $Requests - ($i * $perJob)
    if ($remaining -le 0) {
        break
    }
    $count = [Math]::Min($perJob, $remaining)
    $jobs += Start-Job -ArgumentList $target, $count, $headers -ScriptBlock {
        param($target, $count, $headers)
        $ok = 0
        $failed = 0
        for ($j = 0; $j -lt $count; $j++) {
            try {
                $response = Invoke-WebRequest -Uri $target -Headers $headers -UseBasicParsing -TimeoutSec 15
                if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 300) {
                    $ok++
                } else {
                    $failed++
                }
            } catch {
                $failed++
            }
        }
        [pscustomobject]@{ Ok = $ok; Failed = $failed }
    }
}

$results = $jobs | Wait-Job | Receive-Job
$jobs | Remove-Job

$elapsed = ((Get-Date) - $started).TotalSeconds
$okTotal = ($results | Measure-Object -Property Ok -Sum).Sum
$failedTotal = ($results | Measure-Object -Property Failed -Sum).Sum
$rps = if ($elapsed -eq 0) { 0 } else { [Math]::Round($Requests / $elapsed, 2) }

Write-Host "OK: $okTotal"
Write-Host "Failed: $failedTotal"
Write-Host "ElapsedSeconds: $([Math]::Round($elapsed, 2))"
Write-Host "RequestsPerSecond: $rps"
