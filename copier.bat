(echo select disk %~3
echo clean 
echo create partition primary 
echo active 
echo format fs=fat32 quick
echo assign letter=%~2
echo exit
) | diskpart

%~1:
CD BOOT
BOOTSECT.EXE /NT60 %~2:
XCOPY %~1:\*.* %~2:\ /E /F /H
