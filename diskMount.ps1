#Check if elevated
[Security.Principal.WindowsPrincipal]$user = [Security.Principal.WindowsIdentity]::GetCurrent();
$Admin = $user.IsInRole([Security.Principal.WindowsBuiltinRole]::Administrator);

if ($Admin) 
{
$mountDriveLetter = $args[0]
$flashDriveLetter = $args[1]
$isoName = $args[2]
$directoryPath = $PSScriptRoot
$isoImg = "$directoryPath\$isoName.iso"
$driveNumber = $args[3]
$diskImg = Mount-DiskImage -ImagePath $isoImg -NoDriveLetter
$volInfo = $diskImg | Get-Volume
mountvol ($mountDriveLetter+ ":\") $volInfo.UniqueId
Start-Process -WindowStyle hidden -FilePath "$directoryPath\copier.bat" -ArgumentList $mountDriveLetter, $flashDriveLetter, $driveNumber -wait
DisMount-DiskImage -ImagePath $isoImg 
}

else
{ 
Write-Error "Must be run as admin."
exit 1;
}
