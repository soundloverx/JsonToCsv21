module org.overb.jsontocsv {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires static lombok;
    requires com.fasterxml.jackson.databind;
    requires org.apache.commons.lang3;

    opens org.overb.jsontocsv to javafx.fxml;
    opens org.overb.jsontocsv.controllers to javafx.fxml;
    opens org.overb.jsontocsv.dto to javafx.fxml;
    opens org.overb.jsontocsv.elements to javafx.fxml;
    opens org.overb.jsontocsv.enums to javafx.fxml;

    exports org.overb.jsontocsv;
    exports org.overb.jsontocsv.controllers;
    exports org.overb.jsontocsv.dto;
    exports org.overb.jsontocsv.elements;
    exports org.overb.jsontocsv.enums;
}