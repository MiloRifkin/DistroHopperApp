module com.example.distrohopper {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires com.jcraft.jsch;
    requires mysql.connector.j;
    requires java.desktop;
    requires usbdrivedetector;
    requires com.sun.jna.platform;
    requires com.sun.jna;


    opens com.example.distrohopper to javafx.fxml;
    exports com.example.distrohopper;
}