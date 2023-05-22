# stop UHF Trading Console if any
Get-Process -Name UHFTradingConsole -ErrorAction SilentlyContinue | Stop-Process | Wait-Process

# stop Ember Monitor
Get-Process | Where-Object { $_.MainWindowTitle -eq "`"Ember Monitor`"" } | Select-Object -First 1 | Stop-Process | Wait-Process
Get-Process | Where-Object { $_.MainWindowTitle -eq "`"Ember Monitor`"" } | Select-Object -First 1 | Stop-Process | Wait-Process

# stop Ember CLI
Get-Process | Where-Object { $_.MainWindowTitle -eq "`"Ember CLI`"" } | Select-Object -First 1 | Stop-Process | Wait-Process
Get-Process | Where-Object { $_.MainWindowTitle -eq "`"Ember CLI`"" } | Select-Object -First 1 | Stop-Process | Wait-Process

# stop Ember
Get-Process | Where-Object { $_.MainWindowTitle -eq "`"Ember`"" } | Select-Object -First 1 | Stop-Process | Wait-Process
Get-Process | Where-Object { $_.MainWindowTitle -eq "`"Ember`"" } | Select-Object -First 1 | Stop-Process | Wait-Process

# stop TimeBase 5.X Server
if (Test-NetConnection localhost -Port @devenvTimeBasePort@ -InformationLevel Quiet) {
	Invoke-WebRequest -Uri "http://localhost:@devenvTimeBase52Port@/shutdown%2E%2E%2E" -Method GET
}	
