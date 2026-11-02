open module io.angistrem.employeemanagement{
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.materialdesign2;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires javafx.graphics;
    requires java.sql;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires kernel;
    requires layout;
    requires io;
    requires svg;
    requires java.desktop;
    requires javafx.swing;
    requires org.apache.pdfbox;
    requires org.kordamp.ikonli.fontawesome;
    requires org.slf4j;

    exports io.angistrem.employeemanagement.controller;
    exports io.angistrem.employeemanagement.dialog;
    exports io.angistrem.employeemanagement.model;
    exports io.angistrem.employeemanagement.controller.menu;
    exports io.angistrem.employeemanagement;
}
