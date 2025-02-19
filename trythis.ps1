$remoteHost = "host@remote"
$password = "YourPassword"
$secondaryCommand = "sudo su - otheraccount -c 'bash --noprofile --norc'"

$plinkPath = "C:\Path\To\plink.exe"

Start-Process -NoNewWindow -Wait -FilePath $plinkPath -ArgumentList "-batch -ssh $remoteHost -pw $password `"$secondaryCommand`""
