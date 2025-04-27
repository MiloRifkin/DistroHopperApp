package com.example.distrohopper;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class PopUpAlertViewController {

    //region buttons and handlers

    @FXML private javafx.scene.control.Button cancelButton;

    @FXML private javafx.scene.control.Button proceedButton;

    /**
     * Handler for the cancel button, used to cancel the operation, closes the warning window
     */
    @FXML
    private void cancelOperation(){
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();

    }



    /**
     * Handler for the proceed button, proceeds to the flashing view controller and closes the warning window
     */
    @FXML
    private void beginOperation(){

        try{ // Loads the javafx scene as a new window
            FXMLLoader fxmlLoader = new FXMLLoader(DistroHopperApplication.class.getResource("flashing-menu-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 650, 450);
            Stage stage = new Stage();
            stage.setTitle("Retrieving and Flashing...");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();
        }catch(Exception e){
            System.out.println("Loading new window failed");
        }
        Stage stage = (Stage) proceedButton.getScene().getWindow();
        stage.close();

    }
    //endregion
}
