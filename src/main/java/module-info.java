module com.example.distrohopper {
    requires javafx.fxml;
    requires java.sql;
    requires com.jcraft.jsch;
    requires mysql.connector.j;
    requires java.desktop;
    requires com.sun.jna.platform;
    requires com.sun.jna;
    requires atlantafx.base;


    opens com.example.distrohopper to javafx.fxml;
    exports com.example.distrohopper;
}