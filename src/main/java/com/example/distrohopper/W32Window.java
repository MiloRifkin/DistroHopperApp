package com.example.distrohopper;

//Using the Java Native Interface to make surface level Windows calls
import com.sun.jna.platform.win32.DBT;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.ptr.IntByReference;



import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class W32Window implements Runnable{
    //Defining Windows event code constants
    private static final int WM_DEVICECHANGE = 537;
    private static final int DBT_DEVICEARRIVAL = 32768;
    private static final int DBT_DEVICEREMOVECOMPLETE = 32772;
    private static final int DBT_DEVTYP_VOLUME = 2;

    private static List<Character> listOfDrives;
    //The drive letters of currently connected drives is stored in this arraylist

    private HashMap<Character, String> driveUUIDs = new HashMap<>();
    //The Drive UUID's (needed for flashing) are stored in this Hashmap. Use the getter getDriveUUID's & pass the drive letter to access.

    private HashMap <Character, String> driveDescription = new HashMap<Character, String>();
    //The Drive description, e.g. 'USB Drive' is stored in this hashmap. Use the getter getDriveDescription() & pass the drive letter to access.

    private static HashMap <Character, Float> driveCapacity = new HashMap<Character, Float>();
    //The Drive capacity, stored in MB. Use the getter getDriveCapacity to access, & pass the drive letter to access.

    public W32Window() {
        listOfDrives = new ArrayList<>();
    }

    public void run() {
        listInsertedDrives();
        User32 user = User32.INSTANCE;

        //Defining a Window class to receive messages
        WinUser.WNDCLASSEX wndclassex = new WinUser.WNDCLASSEX();
        wndclassex.lpszClassName = "USBListenerWindow";

        //Setting the callback to process Windows Messages, check against the event code constants
        wndclassex.lpfnWndProc = new WinUser.WindowProc() {
            public WinDef.LRESULT callback(WinDef.HWND hwnd, int uMsg, WinDef.WPARAM wparam, WinDef.LPARAM lparam) {
                if (uMsg == WM_DEVICECHANGE) {
                    deviceChange(wparam.intValue(), lparam);
                }
                return User32.INSTANCE.DefWindowProc(hwnd, uMsg, wparam, lparam);
            }
        };

        //Registering the window class
        WinDef.ATOM atom = user.RegisterClassEx(wndclassex);
        if (atom.intValue() == 0) {
            System.err.println("Error creating window");
            return;
        }

        //Creating the hidden window, final step in the process, with messages printed to console in case of success/failure
        WinDef.HWND hwnd = user.CreateWindowEx(0, wndclassex.lpszClassName, "USBListener", 0,0,0,0,0, null, null, null, null);
        if (hwnd == null) {
            System.err.println("Error creating window");
            return;
        }

        System.err.println("Listening for USB events");

        WinUser.MSG msg = new WinUser.MSG();
        while (user.GetMessage(msg, hwnd, 0, 0) > 0) {
            user.TranslateMessage(msg);
            user.DispatchMessage(msg);
        }
    }

    //Sub-routine to check if the device change is a Insertion or Removal, & print to console
    private void deviceChange(int eventType, WinDef.LPARAM lparam) {
        if (eventType == 32768) {
            System.err.println("USB detected");
            this.printDriveLetter(lparam);
        } else if (eventType == 32772) {
            System.err.println("USB removal");
        }

    }

    //Using the Windows LParam to get the drive letter, add to the ArrayList
    //Afterwards, get the capacity and description to add to Hashmap
    private void printDriveLetter(WinDef.LPARAM lparam) {
        DBT.DEV_BROADCAST_HDR hdr = new DBT.DEV_BROADCAST_HDR(lparam.longValue());
        if (hdr.dbch_devicetype == 2) {
            DBT.DEV_BROADCAST_VOLUME vol = new DBT.DEV_BROADCAST_VOLUME(hdr.getPointer());
            char driveLetter = this.getDriveLetter(vol.dbcv_unitmask);
            System.out.println("Drive letter: " + driveLetter + "://");
            listOfDrives.add(driveLetter);

            String serial = getVolumeSerial(String.valueOf(driveLetter));
            driveUUIDs.put(driveLetter, serial);

            try {
                File driveRoot = new File(driveLetter + "://");
                if (driveRoot.exists() && driveRoot.canRead()) {
                    FileStore store = Files.getFileStore(driveRoot.toPath());
                    float spaceinMB = store.getTotalSpace() / (1024f * 1024f);
                    driveCapacity.put(driveLetter, spaceinMB);
                    System.out.println(spaceinMB);

                    FileSystemView fsv = FileSystemView.getFileSystemView();
                    String Description = fsv.getSystemTypeDescription(driveRoot);
                    driveDescription.put(driveLetter, Description);
                    System.out.println(Description);
                }
            } catch (Exception e) {
                System.err.println("Failed to read drive information for drive " + driveLetter + "://. " + e.getMessage());
            }
        }
    }

    private char getDriveLetter(int unitmask) {
        for(int i = 0; i < 26; ++i) {
            if ((unitmask & 1 << i) != 0) {
                return (char)(65 + i);
            }
        }
        return '?';
    }

    //Subroutine run at the start of execution, to return the currently inserted drives to the Arraylist & the details to the Hashmaps
    public void listInsertedDrives() {
        FileSystemView fsv = FileSystemView.getFileSystemView();
        File[] roots = File.listRoots();

        for (File root : roots) {
            if (fsv.isDrive(root) && fsv.isTraversable(root) != null && root.canRead()) {
                String Description = fsv.getSystemTypeDescription(root);
                char driveLetter = root.getPath().charAt(0);

                listOfDrives.add(driveLetter);
                driveDescription.put(driveLetter, Description);
                System.out.println(driveLetter + "://. " + Description);

                String serial = getVolumeSerial(String.valueOf(driveLetter));
                driveUUIDs.put(driveLetter, serial);

                try {
                    FileStore store = Files.getFileStore(root.toPath());
                    long totalSpace = store.getTotalSpace();
                    float spaceinMB = totalSpace / (1024f * 1024f);
                    driveCapacity.put(driveLetter, spaceinMB);
                    System.out.println(driveLetter + "://.  " + spaceinMB);
                } catch (IOException e) {
                    System.err.println("Could not get Filestore for drive " + driveLetter + "://. " + e.getMessage());
                }
            }
        }
    }

    private String getVolumeSerial(String driveLetter) {
        char[] volumeNameBuffer = new char[256];
        char[] fileSystemNameBuffer = new char[256];
        IntByReference serialNumber = new IntByReference();
        IntByReference maxComponentLength = new IntByReference();
        IntByReference fileSystemFlags = new IntByReference();

        boolean success = Kernel32.INSTANCE.GetVolumeInformation(
                driveLetter + ":\\",
                volumeNameBuffer,
                volumeNameBuffer.length,
                serialNumber,
                maxComponentLength,
                fileSystemFlags,
                fileSystemNameBuffer,
                fileSystemNameBuffer.length
        );

        if (success) {
            return String.format("%08X", serialNumber.getValue());
        } else {
            return "Unavailable";
        }
    }


    //Various public getters for the ArrayList & Hashmaps, used for wider program function
    public char getDrive(int i) {
        return listOfDrives.get(i);
    }

    public String getDriveDescription(char c) {
        return driveDescription.get(c);
    }

    public float getDriveCapacity(char c){
        return driveCapacity.get(c);
    }

    public List<Character> getListOfDrives() {
        return listOfDrives;
    }

    public String getDriveUUIDs(char c) {
        return driveUUIDs.get(c);
    }
}
