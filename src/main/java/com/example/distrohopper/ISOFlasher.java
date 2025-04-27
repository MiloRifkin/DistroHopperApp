package com.example.distrohopper;

import java.io.IOException;

public class ISOFlasher {

    ISOFlasher(){
    }

    public boolean flashISO (String mountLetter, String driveLetter, String isoName, String diskNumber) throws Exception
    {
        //System runs code which emulates following PowerShell call: .\diskMount.ps1 Y X Fedora_1.4 1
        System.out.println("Building process:");
        ProcessBuilder builder = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-ExecutionPolicy",
                "Bypass",
                "-File",
                (System.getProperty("user.dir")+"\\diskMount.PS1"),
                mountLetter,
                driveLetter,
                isoName,
                diskNumber
        );
        try {
            Process p = builder.start();
            int result = p.waitFor();
            System.out.println("Exited with code: " + result);
            if (result==0){ return true; }
            else { return false; }
        } catch (IOException | InterruptedException e) {e.printStackTrace();}
        return false;
    }
}
