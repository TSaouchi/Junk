# Run Python script in the background as a job
Start-Job -ScriptBlock {
    python -u myscript.py -p 12356 -f config.ini -t barg *> worker_log.log
}

# Run the app in the background as a job
Start-Job -ScriptBlock {
    & "app.exe" -listen-port=12345 -wokers=localhost:12356 *> broker_log.log
}

# Check running jobs
Get-Job

# Retrieve job output
Receive-Job -Id <JobId>
