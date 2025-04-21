package com.example.distrohopper;

//region libraries
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
//endregion

public class DistroHopperApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        try{
            FXMLLoader fxmlLoader = new FXMLLoader(DistroHopperApplication.class.getResource("main-menu-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 650, 450);
            stage.setTitle("DistroHopper");
            stage.setScene(scene);
            stage.show();

        }catch(Exception E){
            System.out.println("Loading window failed");
        }

    }

    public static void main(String[] args) {
        launch();
    }
}