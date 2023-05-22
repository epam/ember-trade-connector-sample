$env:JAVA_HOME="@devenvDeltixHome52@\jre"

$DateNoDash = (Get-Date).ToString("yyyyMMdd")
$DateWithDash = (Get-Date).ToString("yyyy-MM-dd")

$ArchiveDate = (Get-Date).ToString("yyyyMMdd-HHmm")
$ArchiveName = "logs-$ArchiveDate"

New-Item -ItemType directory $ArchiveName

# convert journal to json
$env:EMBER_HOME="ember-home"
.\ember\bin\journal-to-json.bat $ArchiveName\journal.json

# collect all log files
$LogFiles = Get-ChildItem "." -Recurse -File -Include "*$DateWithDash.log", "*$DateNoDash.log", "*$DateNoDash.messages.log", "*$DateNoDash.event.log", "*.messages"

# copy logs to separate folder
Copy-Item -Force -Path $LogFiles -Destination $ArchiveName

# archive collected logs
Compress-Archive -Path $ArchiveName -CompressionLevel Optimal -DestinationPath "$ArchiveName.zip"
