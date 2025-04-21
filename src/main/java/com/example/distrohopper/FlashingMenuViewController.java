package com.example.distrohopper;

//region libraries
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.text.Font;
import javafx.concurrent.Task;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
//endregion

public class FlashingMenuViewController {

    //region variables

    protected static String driveUUID;
    protected static String link;
    protected static String distroName;
    protected static String distroVersion;

    //endregion

    //region getters and setters
    public static String getDriveUUID() {
        return driveUUID;
    }

    public static void setDriveUUID(String driveUUID) {
        FlashingMenuViewController.driveUUID = driveUUID;
    }

    public static String getLink() {
        return link;
    }

    public static void setLink(String link) {
        FlashingMenuViewController.link = link;
    }

    public static String getDistroName() {
        return distroName;
    }

    public static void setDistroName(String distroName) {
        FlashingMenuViewController.distroName = distroName;
    }

    public static String getDistroVersion() {
        return distroVersion;
    }

    public static void setDistroVersion(String distroVersion) {
        FlashingMenuViewController.distroVersion = distroVersion;
    }
    //endregion

    //region labels

    public ProgressBar progressBar;
    public Label leftLabel;
    public Label rightLabel;
    public Label arrowLabel;

    //endRegion

    //region Images
    public ImageView leftImage;
    public ImageView rightImage;
    //endRegion


    Task<Void> ISODownload = new Task<Void>() {
        @Override
        public Void call() throws Exception {

            if(link != null){
                try (BufferedInputStream in = new BufferedInputStream(new URL(link).openStream()); FileOutputStream fileOutputStream = new FileOutputStream(distroName +" "+distroVersion+ ".iso")) {
                    byte[] dataBuffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                        fileOutputStream.write(dataBuffer, 0, bytesRead);
                    }
                }
            }
//            final int max = 1000000;
//            for (int i=1; i<=max; i++) {
//                if (isCancelled()) {
//                    break;
//                }
//                updateProgress(i, max);
//            }
            return null;
        }
    };

    /**
     * Function is called when the flashing menu view page is loaded
     */
    @FXML
    protected void initialize(){
        progressBar.setProgress(99);
        arrowLabel.setFont(new Font(40));
        leftLabel.setText(distroName);
        rightLabel.setText(driveUUID);
        System.out.println(driveUUID + link +distroName+distroVersion);
        progressBar.progressProperty().bind(ISODownload.progressProperty());
        new Thread(ISODownload).start();

    }

    /**
     * The following function downloads the iso file from variable link
     * @throws IOException: Throws exception when the link is invalid
     */
    public static void downloadISO() throws IOException {


    }

}
