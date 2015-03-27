package com.faforever.client.whatsnew;

import javafx.fxml.FXML;
import javafx.scene.web.WebView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

public class WhatsNewController {

  private static final String PROPERTY_WHATS_NEW_URL = "whatsNewUrl";

  @Autowired
  Environment environment;

  @FXML
  private WebView whatsNewRoot;

  public void load() {
    whatsNewRoot.getEngine().load(environment.getProperty(PROPERTY_WHATS_NEW_URL));
  }
}
