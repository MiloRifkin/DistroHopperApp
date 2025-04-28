package com.example.distrohopper;

//region libraries
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
//endregion

public class AboutViewController{


    //region Images

    public ImageView distroHopperIcon;
    //endRegion

    String errorLabelText = "";

    @FXML
    protected void initialize() throws Exception {
        Image distroHopperIconImage = new Image("helpPageIcon.jpeg");
        distroHopperIcon.setImage(distroHopperIconImage);

    }



}
