# run-fail-and-retry.ps1
# Purpose: enqueue 1 success job + 1 failing job, wait for processing, show DLQ,
# retry DLQ jobs, wait, verify final state, cleanup, stop worker.
#
# Usage: from project root (D:\Project\queuectl-sqllite):
#   .\run-fail-and-retry.ps1

$jar = "target\queuectl-1.0.jar"
$examples = "$PWD\examples"
New-Item -ItemType Directory -Force -Path $examples | Out-Null

# UTF-8 encoder without BOM
$enc = New-Object System.Text.UTF8Encoding($false)

# Create two test jobs:
# - auto-ok: prints a message and exits 0
# - auto-fail: exits 1 (will be retried and go to DLQ)
$okJson = '{"command":"powershell -Command ''Write-Host \"auto-OK\"; Start-Sleep -Seconds 1''","maxRetries":2}'
$failJson = '{"command":"powershell -Command ''exit 1''","maxRetries":2}'

[System.IO.File]::WriteAllText("$examples\auto-ok.json", $okJson, $enc)
[System.IO.File]::WriteAllText("$examples\auto-fail.json", $failJson, $enc)

Write-Host "Created test job files in $examples"

# Start worker in background (2 threads)
Write-Host "Starting worker (2 threads) in background..."
$worker = Start-Process -FilePath "java" -ArgumentList "-jar",$jar,"worker","start","--count","2" -PassThru
Start-Sleep -Seconds 2

# Enqueue both jobs
Write-Host "Enqueuing success job..."
java -jar $jar enqueue --file "$examples\auto-ok.json" | ForEach-Object { Write-Host $_ }
Write-Host "Enqueuing failing job..."
java -jar $jar enqueue --file "$examples\auto-fail.json" | ForEach-Object { Write-Host $_ }

# Wait for processing and retries (timing depends on backoff_base)
# With default backoff_base=2 and maxRetries=2, wait ~ (2^1 + 2^2)+buffer = 7-8s. Use 15s to be safe.
Write-Host "Waiting for jobs to process and failing job to reach DLQ (sleeping 15s)..."
Start-Sleep -Seconds 15

# Show status and DLQ
Write-Host "`n=== STATUS AFTER INITIAL PROCESSING ==="
java -jar $jar status
Write-Host "`n=== DLQ LIST (should show failing job) ==="
java -jar $jar dlq list

# Parse DLQ, retry if any
$dlqOut = java -jar $jar dlq list
$ids = $dlqOut -split "`n" | ForEach-Object {
    if ($_ -match 'id=([0-9a-fA-F-]{8,})') { $matches[1] }
}
if ($ids.Count -gt 0) {
    Write-Host "`nRetrying DLQ job(s):"
    foreach ($id in $ids) {
        if ($id) {
            Write-Host " -> retrying $id"
            java -jar $jar dlq retry $id | ForEach-Object { Write-Host $_ }
        }
    }
    # Wait a bit for retried jobs to be processed
    Write-Host "Waiting for retried jobs to be processed (sleeping 8s)..."
    Start-Sleep -Seconds 8
} else {
    Write-Host "`nNo DLQ jobs found to retry."
}

# Final status
Write-Host "`n=== FINAL STATUS ==="
java -jar $jar status
Write-Host "`n=== FINAL DLQ ==="
java -jar $jar dlq list
Write-Host "`n=== COMPLETED JOBS ==="
java -jar $jar list --state completed

# Cleanup test files
Write-Host "`nCleaning up test JSON files..."
Remove-Item "$examples\auto-ok.json","$examples\auto-fail.json" -ErrorAction SilentlyContinue

# Stop worker
if ($worker -ne $null) {
    try {
        Write-Host "`nStopping background worker (pid $($worker.Id))..."
        Stop-Process -Id $worker.Id -Force -ErrorAction SilentlyContinue
    } catch {
        Write-Host "Could not stop worker process cleanly: $_"
    }
}

Write-Host "`nDone. Inspect outputs above to confirm DLQ retry behavior."
