package com.example.distrohopper;

//region libraries
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
//endregion

public class DistroHopperApplication extends Application {

    @Override
    public void start(Stage stage){
        try{
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
            FXMLLoader fxmlLoader = new FXMLLoader(DistroHopperApplication.class.getResource("main-menu-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 650, 450);
            stage.setTitle("DistroHopper");
            stage.setScene(scene);
            stage.setWidth(640);
            stage.setHeight(640);
            stage.setResizable(false);
            stage.show();

        }catch(Exception E){
            System.out.println("Loading window failed");
        }

    }

    public static void main(String[] args) {
        launch();
    }
}