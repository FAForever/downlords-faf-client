package com.faforever.client;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

import javafx.fxml.FXMLLoader;

@SpringBootApplication(exclude = {
    JmxAutoConfiguration.class,
    SecurityAutoConfiguration.class,
})
public class Main {
  public static void main(String[] args) {
    FXMLLoader.setDefaultClassLoader(Main.class.getClassLoader());
    FafClientApplication.applicationMain(args);
  }
}
