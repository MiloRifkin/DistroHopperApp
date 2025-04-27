package com.example.distrohopper;

//region libraries
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Font;
import javafx.concurrent.Task;
import javafx.stage.Stage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
//endregion

public class FlashingMenuViewController {

    //region variables

    protected static String driveNumber;
    protected static String driveLetter;
    protected static String unusedDriveLetter;
    protected static String link;
    protected static String distroName;
    protected static String distroVersion;
    protected static Float totalImageSize = 3F;

    //endregion

    //region getters and setters

    public static void setLink(String link) {
        FlashingMenuViewController.link = link;
    }

    public static void setDistroName(String distroName) {
        FlashingMenuViewController.distroName = distroName;
    }

    public static void setDistroVersion(String distroVersion) {
        FlashingMenuViewController.distroVersion = distroVersion;
    }
    public static void setTotalImageSize(Float imageSize){
        totalImageSize = imageSize;
    }

    public static void setDriveNumber(String selectedDriveNumber) {
        driveNumber = selectedDriveNumber;
    }

    public static void setUnusedDriveLetter(String driveLetter){
        unusedDriveLetter = driveLetter;
    }

    public static void setDriveLetter(String selectedDriveLetter){
        driveLetter = selectedDriveLetter;
    }



    //endregion

    //region labels

    @FXML
    public ProgressBar flashingProgressBar;
    public ProgressBar progressBar = new ProgressBar(0);
    public Label leftLabel;
    public Label rightLabel;
    public Label arrowLabel;
    public Label errorLabel;
    public Label flashingComplete;
    public Label downloading;
    public Label flashing;

    //endRegion

    //region Images
    public ImageView leftImage;
    public ImageView rightImage;
    //endRegion

    String errorLabelText = "";



    /**
     *
     * @param progressBar Used to update the download progress
     * @return void
     */
    private Task<Void> createDownloadTask(ProgressBar progressBar, Label downloading) {


        Task<Void> ISODownload = new Task<>(){
            @Override
            public Void call() throws Exception {

                System.out.println(totalImageSize);

                if(link != null){

                    try (BufferedInputStream in = new BufferedInputStream(new URL(link).openStream()); FileOutputStream fileOutputStream = new FileOutputStream(distroVersion+ ".iso")) {
                        byte[] dataBuffer = new byte[1024];
                        int bytesRead;
                        int totalBytesRead = 0;
                        while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                            fileOutputStream.write(dataBuffer, 0, bytesRead);
                            totalBytesRead = totalBytesRead + 1;
                            updateProgress(totalBytesRead, totalImageSize* 1000000L);
                            if(isCancelled()){
                                errorLabelText = "Error: Download failed";
                                break;
                            }
                        }

                        updateProgress(100,100);
                        Task<Void> flashingTask = createFlashingTask(flashingProgressBar);
                        Thread flashingThread = new Thread(flashingTask);
                        downloading.setText("Download Complete");
                        System.out.println("Download Complete");
                        flashingThread.start();
                    }
                }
                return null;
            }
        };
        progressBar.progressProperty().bind(ISODownload.progressProperty());

        return ISODownload;
    }

    private Task<Void> createFlashingTask(ProgressBar flashingProgressBar){
        Task<Void> flashingTask = new Task<Void>() {
            @Override
            public Void call() throws Exception{
                File file = new File(driveLetter + ":\\");
                int loopCounter = 0;
                //System runs code which emulates following PowerShell call: .\diskMount.ps1 Y X Fedora_1.4 1
                System.out.println("Building process:");
                ProcessBuilder builder = new ProcessBuilder(
                        "powershell.exe",
                        "-NoProfile",
                        "-ExecutionPolicy",
                        "Bypass",
                        "-File",
                        (System.getProperty("user.dir")+"\\diskMount.PS1"),
                        unusedDriveLetter,
                        driveLetter,
                        distroVersion,
                        driveNumber
                );
                try {
                    Process p = builder.start();
                    TimeUnit.SECONDS.sleep(15);
                    double totalSpace = (double) file.getUsableSpace() / (1024 * 1024 * 1024) - 0.000000001;
                    double freeSpace = (double) file.getUsableSpace() / (1024 * 1024 * 1024) - 0.000000001;
                    while(loopCounter < 9){
                        if (freeSpace ==((double) file.getUsableSpace() / (1024 * 1024 * 1024))){ loopCounter++; }
                        freeSpace = (double) file.getUsableSpace() / (1024*1024*1024);
                        updateProgress((totalSpace-freeSpace), 1);
                        TimeUnit.SECONDS.sleep(1);
                    }
                    int result = p.waitFor();
                    updateProgress(100,100);
                    flashing.setText("Flashing Complete!");
                    System.out.println("Exited with code: " + result);
                } catch (IOException | InterruptedException e) {e.printStackTrace();}
                return null;
            }

        };
        flashingProgressBar.progressProperty().bind(flashingTask.progressProperty());
        return flashingTask;
    }


    /**
     * Function is called when the flashing menu view page is loaded
     */
    @FXML
    protected void initialize() throws Exception {
        Image leftPhoto = new Image("leftImage.png");
        Image rightPhoto = new Image("rightImage.png");
        leftImage.setImage(leftPhoto);
        rightImage.setImage(rightPhoto);
        arrowLabel.setFont(new Font(40));
        leftLabel.setText(distroName);
        rightLabel.setText("USB Drive");
        Task<Void> downloadTask = createDownloadTask(progressBar, downloading);
        Thread downloadThread = new Thread(downloadTask);
        downloadThread.start();
    }



    public void aboutDistroHopper() {
        try{

            FXMLLoader fxmlLoader = new FXMLLoader(DistroHopperApplication.class.getResource("about-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 650, 450);
            Stage stage = new Stage();
            stage.setTitle("About");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();

        }catch(Exception e){
            System.out.println("Loading new window failed");
        }
    }
}
