package com.example.distrohopper;

//region libraries

//SSH libraries
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

//UI Libraries
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.concurrent.Task;

//SQL
import java.sql.*;

//Others
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

//endregion

public class MainMenuViewController {

    //region SSH connection parameters

    String SSHHost = "lxfarm08.csc.liv.ac.uk"; //lxfarm08 allows connections from the outside to be established
    String SSHUserName = "sgsho2";
    String privateKey = "id_rsa";
    int SSHPort = 22;



    //endregion

    //region USB Detector
    W32Window window = new W32Window();
    //endregion

    //region database connection parameters
    String databaseHost = "studdb";
    int databasePort = 3306;
    String databaseUsername = "sgsho2";
    String databasePassword = "";
    //endregion

    //region variables
    String selectedDistro = null;
    String selectedVersion = null;
    List<String> usbDevicesList = null;
    String selectedDistroDescription = null;
    String selectedISOSize = null;
    String selectedUSBDriveName = null;
    String selectedDriveSize = null;
    String selectedISODownloadLink = null;
    String selectedDriveUUID = null;

    String warningLabelText = "";
    //endregion

    //region Labels and Strings

    @FXML
    private Label infoAndDescriptions;

    @FXML
    private Label warningLabel; //Not yet implemented

    //endregion

    //region Getters and Setters

    private ArrayList<String> getDistros(){

        String databaseQuery = "select distinct distro from distro_information;";
        return executeQuery(databaseQuery, "distro");
    }

    private ArrayList<String> getDescription(){
        String databaseQuery = "select distinct description, distro from distro_information where distro =\"" + selectedDistro + "\";";
        return executeQuery(databaseQuery, "description");
    }

    private ArrayList<String> getVersionNumbers(){

        //Have to put "version, distro" or else it cannot find version (could fix later)
        String databaseQuery = "select version, distro from distro_information where distro=\"" + selectedDistro +"\";";
        return executeQuery(databaseQuery, "version");
    }

    private String getISOSize(){
        String databaseQuery = "select size, distro, version from distro_information where distro =\"" + selectedDistro + "\" and  version = \"" + selectedVersion +"\";";
        return executeQuery(databaseQuery, "size").getFirst();
    }

    private String getISOLink(){
        String databaseQuery = "select distro, version, link from distro_information where distro = \"" + selectedDistro + "\" and version = \"" + selectedVersion + "\";";
        return executeQuery(databaseQuery, "link").getFirst();
    }

    private String getDriveSize(){
        return "16GB";
    }



    //endregion

    //region Combo box and handlers
    @FXML
    private ComboBox<String> linuxComboBox = new ComboBox<>();

    @FXML
    private ComboBox<String> versionComboBox = new ComboBox<>();

    @FXML
    private ComboBox<String> usbComboBox = new ComboBox<>();

    /**
     * When an option is selected from linuxComboBox:
     * - update the selected distro variable
     * - fetch and update the infos and descriptions
     * - fetch the available version numbers
     */
    @FXML
    public void selectedLinux(){

        selectedVersion = null;
        selectedDistroDescription = null;
        selectedDistro = linuxComboBox.getValue();
        versionComboBox.getItems().addAll(getVersionNumbers());
        selectedDistroDescription = getDescription().getFirst();
        reloadDescription();

    }

    /**
     * When an option is selected from linuxComboBox:
     * - Update the selected version
     */
    public void selectedVersion() {

        selectedVersion = versionComboBox.getValue();
        selectedISOSize = getISOSize();
        reloadDescription();

    }

    /**
     * When a USB drive is selected:
     * Update the selected USB Drive
     * Update the selected Drive UUID
     * Update the drive size
     */
    public void selectedUSBDrive() {

        selectedUSBDriveName = null;
        selectedDriveSize = null;

        selectedUSBDriveName = usbComboBox.getValue();

        for(String usbDevice: usbDevicesList){
            if(Objects.equals(selectedUSBDriveName, usbDevice.toString())){
                selectedDriveUUID = window.getDriveUUIDs(usbDevice);
            }
        }

        reloadDescription();

    }

    //endregion

    //region USB Device Detection Multithreading

    Task<Void> usbDetection = new Task<Void>() {
        @Override
        protected Void call() throws Exception {

            String currentSystem = System.getProperty("os.name");

            if(!Objects.equals(currentSystem, "Windows 10") && !Objects.equals(currentSystem, "Windows 11")){

                ArrayList<String> localUSBDeviceList= new ArrayList<>();
                warningLabelText = warningLabelText + "\nWarning: this program is only compatible with windows 10 or above.";
                warningLabel.setText(warningLabelText);
                window.getDriveUUIDs("D");


            }else{
                window.run();
                if(!window.getListOfDrives().isEmpty()){
                    usbDevicesList = window.getListOfDrives();
                }
            }
            return null;
        }
    };

    //endregion

    /**
     * Loads the warning popup which leads to the Flashing View
     */
    public void retrieveAndFlash(){

        boolean proceed = ProceedToInstallCheck();

        if(proceed){

            selectedISODownloadLink = getISOLink();
            FlashingMenuViewController.setDistroName(selectedDistro);
            FlashingMenuViewController.setDistroVersion(selectedVersion);
            FlashingMenuViewController.setDriveUUID(selectedDriveUUID);
            FlashingMenuViewController.setLink(selectedISODownloadLink);

            try{

                FXMLLoader fxmlLoader = new FXMLLoader(DistroHopperApplication.class.getResource("pop-up-alert-view.fxml"));
                Scene scene = new Scene(fxmlLoader.load(), 650, 450);
                Stage stage = new Stage();
                stage.setTitle("Warning");
                stage.setScene(scene);
                stage.show();

            }catch(Exception e){
                System.out.println("Loading new window failed");
            }

        }
    }

    /**
     * Checks if all the parameters required for installation has been filled
     * @return true if the conditions have been met
     */
    private boolean ProceedToInstallCheck() {
        boolean proceed = true;
        String warningLabel = "";

        if(selectedDistro == null){
            warningLabel = warningLabel + "Please select the distro\n";
            proceed = false;
        }
        if(selectedVersion == null){
            proceed = false;
            warningLabel = warningLabel + "Please select the version\n";
        }
        if(selectedUSBDriveName == null){
            proceed = false;
            warningLabel = warningLabel + "Please select the drive to create the installer on\n";
        }
        return proceed;
    }


    /**
     * This function is called when this window is loaded
     */
    @FXML
    protected void initialize(){

        new Thread(usbDetection).start();


        linuxComboBox.getItems().addAll(getDistros());

        System.out.println(usbDevicesList);

        //reloadDescription();

    }

    /**
     * This function connects to the remote database via SSH and executes a query. It then returns the column of the output as an array list.
     * @param databaseQuery String      the query which is to be executed
     * @param target        String      the column which the array should return
     * @return              ArrayList   all the elements found in the targeted column
     */
    private ArrayList<String> executeQuery(String databaseQuery, String target){

        ArrayList<String> databaseReturn = new ArrayList<>(8);

        try{


            JSch jSch = new JSch();

            jSch.addIdentity(privateKey);
            Session session = jSch.getSession(SSHUserName, SSHHost, SSHPort);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            try{
                Class.forName("com.mysql.cj.jdbc.Driver");
            }catch(ClassNotFoundException E){
                return databaseReturn;
            }

            try{
                //Set up local port to connect to the mysql port 3306
                int port = session.setPortForwardingL(0, databaseHost, databasePort);
                String databaseUrl = "jdbc:mysql://" + "localhost" + ":" + port + "/sgsho2";


                Connection connection = DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword);
                Statement statement = connection.createStatement();
                ResultSet databaseResult = statement.executeQuery(databaseQuery);
                while(databaseResult.next()){
                    databaseReturn.add(databaseResult.getString(target));
                }

            }catch (SQLException E){
                warningLabelText = "SQL error";
                warningLabel.setText(warningLabelText);
                return null;
            }
        }catch(JSchException E) {
            warningLabelText = "Cannot establish SSH Connection to the Server.";
            warningLabel.setText(warningLabelText);
            databaseReturn.add("-1");
            return null;
        }

        return databaseReturn;



    }

    /**
     * updates the infosAndDescriptions label with up-to-date information
     */
    private void reloadDescription(){

        String infoAndDescriptionsContent = "";

        //Distro Name
        if (selectedDistro != null){infoAndDescriptionsContent = infoAndDescriptionsContent +  "Selected distro: "+selectedDistro+"\n";
        }else{infoAndDescriptionsContent = infoAndDescriptionsContent + "Selected distro: "+"\n";}

        //Distro Description
        if (selectedDistroDescription != null){infoAndDescriptionsContent = "\n"+ infoAndDescriptionsContent + selectedDistroDescription + "\n";}

        //Distro Version
        if(selectedVersion != null){infoAndDescriptionsContent = infoAndDescriptionsContent +  "Selected Version: "+selectedVersion+"\n";
        }else{infoAndDescriptionsContent = infoAndDescriptionsContent +  "Selected Version: "+"\n";}

        //Distro Download Size
        if(selectedISOSize != null) {infoAndDescriptionsContent = infoAndDescriptionsContent + "Installer File size: " + selectedISOSize + "\n";
        }else{infoAndDescriptionsContent = infoAndDescriptionsContent + "Installer File Size: " + "\n";}

        //Update label
        infoAndDescriptions.setText(infoAndDescriptionsContent);

        usbComboBox.getItems().removeAll();

        if (!usbDevicesList.isEmpty()){
            for (String usbStorageDevice : usbDevicesList) {
                usbComboBox.getItems().add(usbStorageDevice);
            }

        } else {
            warningLabel.setText("\nNo USB Drive detected!"+ warningLabel.getText());
        }
    }

}

