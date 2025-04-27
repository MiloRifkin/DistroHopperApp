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
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
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
    private Task<Void> createDownloadTask(ProgressBar progressBar) {
        Task<Void> ISODownload = new Task<Void>() {
            @Override
            public Void call() throws Exception {
                System.out.println(totalImageSize);

                if(link != null){

                    try (BufferedInputStream in = new BufferedInputStream(new URL(link).openStream()); FileOutputStream fileOutputStream = new FileOutputStream(distroName +" "+distroVersion+ ".iso")) {
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
                    }
                }
                return null;
            }
        };
        progressBar.progressProperty().bind(ISODownload.progressProperty());

        return ISODownload;
    }


    /**
     * Function is called when the flashing menu view page is loaded
     */
    @FXML
    protected void initialize(){
        Image leftPhoto = new Image("leftImage.png");
        Image rightPhoto = new Image("rightImage.png");
        leftImage.setImage(leftPhoto);
        rightImage.setImage(rightPhoto);
        arrowLabel.setFont(new Font(40));
        leftLabel.setText(distroName);
        rightLabel.setText("USB Drive");
        Task<Void> downloadTask = createDownloadTask(progressBar);
        Thread downloadThread = new Thread(downloadTask);
        downloadThread.start();



    }



    public void aboutDistroHopper(ActionEvent actionEvent) {
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


    /**
     * The following function downloads the iso file from variable link
     * @throws IOException: Throws exception when the link is invalid
     */
    public static void downloadISO() throws IOException {


    }

}
