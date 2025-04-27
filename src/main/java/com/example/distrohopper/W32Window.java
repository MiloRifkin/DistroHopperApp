package com.example.distrohopper;

//Using the Java Native Interface to make surface level Windows calls
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;
//import com.sun.tools.javac.Main;


import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class W32Window implements Runnable{
    //Defining Windows event code constants
    private static final int WM_DEVICECHANGE = 537; // a device has changed
    private static final int DBT_DEVICEARRIVAL = 32768; //device has been inserted
    private static final int DBT_DEVICEREMOVECOMPLETE = 32772; //device has been removed
    private static final int DBT_DEVTYP_VOLUME = 2; //Device type: Logical volume (drive)
    private static final int DBT_DEVTYP_DEVICEINTERFACE = 0x00000005; //Device type: Interface (e.g. USB drive)
    private static final int DEVICE_NOTIFY_WINDOW_HANDLE = 0x00000000; //Notification type: window handle

    private static List<String> listOfDrives;
    //The drive letters of currently connected drives is stored in this arraylist

    private HashMap<String, String> driveUUIDs = new HashMap<>();
    //The Drive UUID's (needed for flashing) are stored in this Hashmap. Use the getter getDriveUUID's & pass the drive letter to access.

    private HashMap<Object, String> driveDescription = new HashMap<Object, String>();
    //The Drive description, e.g. 'USB Drive' is stored in this hashmap. Use the getter getDriveDescription() & pass the drive letter to access.

    private static HashMap<Object, Float> driveCapacity = new HashMap<Object, Float>();
    //The Drive capacity, stored in MB. Use the getter getDriveCapacity to access, & pass the drive letter to access.

    private HashMap<String, String> driveVIDs = new HashMap<>();
    //The driveVIDs are stored in here. Use the getter getDriveVid() to access
    private HashMap<String, String> drivePIDs = new HashMap<>();
    //The drivePIDs are stored in here. Use the getter getDrivePid() to access

    private HashMap<String, String> driveNumbers = new HashMap<>();


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

        DBT.DEV_BROADCAST_DEVICEINTERFACE notificationFilter = new DBT.DEV_BROADCAST_DEVICEINTERFACE();
        notificationFilter.dbcc_reserved = notificationFilter.size();
        notificationFilter.dbcc_devicetype = DBT_DEVTYP_DEVICEINTERFACE;
        notificationFilter.dbcc_classguid = new Guid.GUID("{A5DCBF10-6530-11D2-901F-00C04FB951ED}"); //Guid for USB devices

//        Pointer notificationHandle = user.RegisterDeviceNotification(hwnd, notificationFilter, DEVICE_NOTIFY_WINDOW_HANDLE).getPointer();
//
//        if (notificationHandle == null) {
//            System.err.println("Failed to register for USB events");
//        } else {
//            System.err.println("Listening for USB events");
//        }

        WinUser.MSG msg = new WinUser.MSG();
        while (user.GetMessage(msg, hwnd, 0, 0) > 0) {
            user.TranslateMessage(msg);
            user.DispatchMessage(msg);
        }
    }

    //Sub-routine to check if the device change is a Insertion or Removal, & print to console
    private void deviceChange(int eventType, WinDef.LPARAM lparam) {
        DBT.DEV_BROADCAST_HDR hdr = new DBT.DEV_BROADCAST_HDR(lparam.longValue());

        if (eventType == DBT_DEVICEARRIVAL) {
            System.err.println("Device connected.");

            if (hdr.dbch_devicetype == DBT_DEVTYP_VOLUME) {
                handleVolumeArrival(lparam);
            } else if (hdr.dbch_devicetype == DBT_DEVTYP_DEVICEINTERFACE) {
                handleDeviceInterfaceArrival(lparam);
            }

        } else if (eventType == DBT_DEVICEREMOVECOMPLETE) {
            System.err.println("Device removed.");
        }
    }

    private void handleVolumeArrival(WinDef.LPARAM lparam) {
        DBT.DEV_BROADCAST_VOLUME vol = new DBT.DEV_BROADCAST_VOLUME(new Pointer(lparam.longValue()));
        char driveLetter = getDriveLetter(vol.dbcv_unitmask);
//        MainMenuViewController.setUsbDevicesList(listOfDrives);
        System.out.println("Drive letter: " + driveLetter + "://");
        listOfDrives.add(String.valueOf(driveLetter));

        String serial = getVolumeSerial(String.valueOf(driveLetter));
        driveUUIDs.put(String.valueOf(driveLetter), serial);

        try {
            File driveRoot = new File(driveLetter + "://");
            if (driveRoot.exists() && driveRoot.canRead()) {
                FileStore store = Files.getFileStore(driveRoot.toPath());
                float spaceinMB = store.getTotalSpace() / (1024f * 1024f);
                driveCapacity.put(driveLetter, spaceinMB);
                System.out.println(spaceinMB + " MB");

                FileSystemView fsv = FileSystemView.getFileSystemView();
                String description = fsv.getSystemTypeDescription(driveRoot);
                driveDescription.put(driveLetter, description);
                System.out.println(description);
            }
        } catch (Exception e) {
            System.err.println("Failed to read drive information for drive " + driveLetter + "://. " + e.getMessage());
        }
    }

    private void handleDeviceInterfaceArrival(WinDef.LPARAM lparam) {
        DBT.DEV_BROADCAST_DEVICEINTERFACE deviceInterface = new DBT.DEV_BROADCAST_DEVICEINTERFACE(new Pointer(lparam.longValue()));
        String devicePath = deviceInterface.getDbcc_name();

        System.out.println("Device Path: " + devicePath);

        // Extract Vendor ID and Product ID from device path
        Pattern p = Pattern.compile("vid_([0-9a-fA-F]+)&pid_([0-9a-fA-F]+)");
        Matcher m = p.matcher(devicePath.toLowerCase());
        if (m.find()) {
            String vid = m.group(1);
            String pid = m.group(2);
            System.out.println("Vendor ID: " + vid);
            System.out.println("Product ID: " + pid);

            // Attempting to use the device path to get the device number
            Pattern deviceNumberPattern = Pattern.compile("#(\\d+)$");
            Matcher deviceNumberMatcher = deviceNumberPattern.matcher(devicePath.replaceAll("\\\\", "/"));

            if (deviceNumberMatcher.find()) {
                String deviceNumber = deviceNumberMatcher.group(1);
                System.out.println("Device Number (quick parse): " + deviceNumber);
                if (!listOfDrives.isEmpty()) {
                    String lastDrive = listOfDrives.get(listOfDrives.size() -1);
                    driveNumbers.put(lastDrive, deviceNumber);
                }
            } else {
                System.out.println("Device Number not found in device path.");
            }

            if (!listOfDrives.isEmpty()) {
                String lastDrive = listOfDrives.get(listOfDrives.size() - 1);
                driveVIDs.put(lastDrive, vid);
                drivePIDs.put(lastDrive, pid);

            }
        } else {
            System.out.println("VID/PID not found in device path.");
        }
    }

    //Using the Windows LParam to get the drive letter, add to the ArrayList
    //Afterwards, get the capacity and description to add to Hashmap
    private void printDriveLetter(WinDef.LPARAM lparam) {
        DBT.DEV_BROADCAST_HDR hdr = new DBT.DEV_BROADCAST_HDR(lparam.longValue());
        if (hdr.dbch_devicetype == DBT_DEVTYP_VOLUME) {
            DBT.DEV_BROADCAST_VOLUME vol = new DBT.DEV_BROADCAST_VOLUME(hdr.getPointer());
            String driveLetter = String.valueOf(this.getDriveLetter(vol.dbcv_unitmask));
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
                if (driveLetter != 'C') {

                    listOfDrives.add(String.valueOf(driveLetter));
                    driveDescription.put(String.valueOf(driveLetter), Description);
                    System.out.println(driveLetter + "://. " + Description);


                    String serial = getVolumeSerial(String.valueOf(driveLetter));
                    driveUUIDs.put(String.valueOf(driveLetter), serial);

                    try {
                        FileStore store = Files.getFileStore(root.toPath());
                        long totalSpace = store.getTotalSpace();
                        float spaceinMB = totalSpace / (1024f * 1024f);
                        driveCapacity.put(String.valueOf(driveLetter), spaceinMB);
                        System.out.println(driveLetter + "://.  " + spaceinMB);
                    } catch (IOException e) {
                        System.err.println("Could not get Filestore for drive " + driveLetter + "://. " + e.getMessage());
                    }
                }
            }
        }
        //MainMenuViewController.setUsbDevicesList(listOfDrives);
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
    public String getDrive(int i) {
        return listOfDrives.get(i);
    }

    public String getDriveDescription(char c) {
        return driveDescription.get(c);
    }

    public float getDriveCapacity(String c){
        return driveCapacity.get(c);
    }

    public List<String> getListOfDrives() {
        return listOfDrives;
    }

    public String getDriveUUIDs(String c) {
        return driveUUIDs.get(c);
    }

    public String getDriveVID(String c) {
        return driveVIDs.get(c);
    }

    public String getDrivePID(String c) {
        return drivePIDs.get(c);
    }

    public String getDriveNumber(String c) {
        return driveNumbers.get(c);
    }
}
