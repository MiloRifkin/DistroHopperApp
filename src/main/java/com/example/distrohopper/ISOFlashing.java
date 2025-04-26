package com.example.distrohopper;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

import java.io.*;

public class ISOFlashing {

    //Java Interface to map the Windows API functions from kernel32.dll
    public interface thisKernel32 extends StdCallLibrary {

        //Load the kernel32 library
        thisKernel32 INSTANCE = Native.load("kernel32", thisKernel32.class, W32APIOptions.DEFAULT_OPTIONS);

        //Open the USB device to read/write to it
        WinNT.HANDLE CreateFile (String lpFileName, int dwDesiredAccess, int dwShareMode, Pointer lpSecurityAttributes,
                                 int dwCreationDisposition, int dwFlagsAndAttributes, WinNT.HANDLE hTemplateFile);

        //Write the raw bytes to the open device
        boolean WriteFile (WinNT.HANDLE hFile, byte[] lpBuffer, int numberOfBytesToWrite,
                           IntByReference lpNumberOfBytesWritten, Pointer lpOverlapped);

        //Close the handle
        boolean CloseHandle (WinNT.HANDLE hObject);
    }

    //Windows API Flags
    private static final int GENERIC_WRITE = 0x40000000; //Write to the device
    private static final int OPEN_EXISTING = 3; //Use an existing file
    private static final int FILE_ATTRIBUTE_NORMAL = 0x80; //No special attributes
    private static final int FILE_SHARE_READ = 0x00000001; //Allow other process to read simultaneously
    private static final int FILE_SHARE_WRITE = 0x00000002; //Allow other process to write simultaneously

    public static File getISO() {
        //TO BE IMPLEMENTED - should be located in the project root?
        return null;
    }

    public static void flashISOtoUSB (String physicalDrivePath) throws IOException {
        File isoFile = getISO();

        if (isoFile == null || !isoFile.exists()) {
            throw new FileNotFoundException("No ISO File found");
        }

        //Open the USB drive as a raw device to write to it
        WinNT.HANDLE handle = thisKernel32.INSTANCE.CreateFile(
                physicalDrivePath,
                GENERIC_WRITE,
                FILE_SHARE_READ | FILE_SHARE_WRITE,
                null,
                OPEN_EXISTING,
                FILE_ATTRIBUTE_NORMAL,
                null
        );

        //Check the opening was successful
        if (Kernel32.INVALID_HANDLE_VALUE.equals(handle)) {
            throw new IOException("Failed to open USB drive for writing.");
        }



        try (InputStream isoInput = new BufferedInputStream(new FileInputStream(isoFile))) {
            byte[] buffer = new byte[4096]; //Reading 4Kb chunks
            int bytesRead = 0;

            //Read the iso file chunk by chunk
            while ((bytesRead = isoInput.read(buffer)) != -1) {

                //If the last read is smaller than the buffer size, copy only the part it read
                byte toWrite[] = (bytesRead == buffer.length) ? buffer : copyOf(buffer, bytesRead);

                //to receive the number of bytes actually written
                IntByReference bytesWritten = new IntByReference(0);

                //Write the chunk to the open file
                boolean success = thisKernel32.INSTANCE.WriteFile(handle, toWrite, toWrite.length, bytesWritten, null);

                if (!success || bytesWritten.getValue() != toWrite.length) {
                    throw new IOException("Write operation failed or incomplete");
                }
            }
        } finally {
                //Close the file
                thisKernel32.INSTANCE.CloseHandle(handle);
            }

        System.out.println("ISO file successfully flashed to " + physicalDrivePath);
    }


    //method to create a smaller array if it reads less than the full buffer size (4kb)
    private static byte[] copyOf(byte[] source, int length) {
        byte[] dest = new byte[length];
        System.arraycopy(source, 0, dest, 0, length);
        return dest;
    }
}
