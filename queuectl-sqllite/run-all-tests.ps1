# run-all-tests.ps1
$jar = "target\queuectl-1.0.jar"
$enc = New-Object System.Text.UTF8Encoding($false)
$examples = "$PWD\examples"
New-Item -ItemType Directory -Force -Path $examples | Out-Null

# create test jobs (no BOM)
[System.IO.File]::WriteAllText("$examples\auto-ok1.json", '{"command":"powershell -Command ''Write-Host \"auto-OK1\"; Start-Sleep -Seconds 1''","maxRetries":2}', $enc)
[System.IO.File]::WriteAllText("$examples\auto-ok2.json", '{"command":"powershell -Command ''Write-Host \"auto-OK2\"; Start-Sleep -Seconds 1''","maxRetries":2}', $enc)
[System.IO.File]::WriteAllText("$examples\auto-fail.json", '{"command":"powershell -Command ''exit 1''","maxRetries":2}', $enc)

# start worker in background
$worker = Start-Process -FilePath "java" -ArgumentList "-jar",$jar,"worker","start","--count","2" -PassThru
Start-Sleep -Seconds 2

# enqueue
java -jar $jar enqueue --file "$examples\auto-ok1.json"
java -jar $jar enqueue --file "$examples\auto-ok2.json"
java -jar $jar enqueue --file "$examples\auto-fail.json"

# wait for retries/backoffs to complete (adjust if you changed backoff)
Start-Sleep -Seconds 12

# report
Write-Host "`n=== STATUS ==="
java -jar $jar status
Write-Host "`n=== DLQ ==="
java -jar $jar dlq list
Write-Host "`n=== COMPLETED ==="
java -jar $jar list --state completed

# cleanup temp files
Remove-Item "$examples\auto-ok1.json","$examples\auto-ok2.json","$examples\auto-fail.json" -ErrorAction SilentlyContinue

# stop worker
Stop-Process -Id $worker.Id -Force
Write-Host "`nTest run finished."
