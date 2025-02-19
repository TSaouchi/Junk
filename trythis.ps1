# Define your variables
$remoteHost = "host@remote"
$password = "YourPassword"
$secondaryCommand = "sudo su - otheraccount"

# Path to plink.exe (update if needed)
$plinkPath = "C:\Path\To\plink.exe"

# Automatically connect and execute the command
$command = "`"$secondaryCommand`""

# Run plink with SSH, automatically handle password and command execution
Start-Process -NoNewWindow -Wait -FilePath $plinkPath -ArgumentList "-ssh $remoteHost -pw $password $command"
