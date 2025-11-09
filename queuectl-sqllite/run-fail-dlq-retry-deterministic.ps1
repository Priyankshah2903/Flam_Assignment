# run-fail-dlq-retry-deterministic.ps1
$jar = "target\queuectl-1.0.jar"
$examples = "$PWD\examples"
New-Item -ItemType Directory -Force -Path $examples | Out-Null
$enc = New-Object System.Text.UTF8Encoding($false)

# deterministic failing job that Windows will definitely return non-zero for
$failJson = '{"command":"cmd.exe /c exit 1","maxRetries":2}'
[System.IO.File]::WriteAllText("$examples\det-fail.json",$failJson,$enc)
Write-Host "Wrote deterministic failing job to $examples\det-fail.json"

# Start worker in background (1 thread is fine)
$worker = Start-Process -FilePath "java" -ArgumentList "-jar",$jar,"worker","start","--count","1" -PassThru
Start-Sleep -Seconds 2

# Enqueue failing job
java -jar $jar enqueue --file "$examples\det-fail.json"

# Wait long enough for retries and move to DLQ:
# backoff base default usually 2 -> delays ~2 + 4 = 6s; we wait comfortable 20s
Write-Host "Waiting 20s for retries/backoff..."
Start-Sleep -Seconds 20

Write-Host "`n--- DLQ (expected: contains the failed job) ---"
java -jar $jar dlq list

# If DLQ entries exist, parse and retry them
$dlq = java -jar $jar dlq list
$ids = $dlq -split "`n" | ForEach-Object { if ($_ -match 'id=([0-9a-fA-F-]{8,})') { $matches[1] } }
if ($ids.Count -gt 0) {
  Write-Host "`nRetrying DLQ jobs..."
  foreach ($id in $ids) {
    if ($id) { java -jar $jar dlq retry $id }
  }
  Start-Sleep -Seconds 8
} else {
  Write-Host "`nNo DLQ jobs found to retry."
}

Write-Host "`n=== FINAL STATUS ==="
java -jar $jar status
java -jar $jar dlq list
java -jar $jar list --state completed

# cleanup
Remove-Item "$examples\det-fail.json" -ErrorAction SilentlyContinue
if ($worker -ne $null) { Stop-Process -Id $worker.Id -Force -ErrorAction SilentlyContinue }
Write-Host "`nDeterministic test done."
