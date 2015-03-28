package com.faforever.client.whatsnew;

import com.faforever.client.preferences.PreferencesService;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

public class WhatsNewController {

  private static final String PROPERTY_WHATS_NEW_URL = "whatsNewUrl";

  @Autowired
  Environment environment;

  @Autowired
  PreferencesService preferencesService;

  @FXML
  private WebView whatsNewRoot;

  public void load() {
    WebEngine engine = whatsNewRoot.getEngine();
    engine.setUserDataDirectory(preferencesService.getPreferencesDirectory().toFile());

    engine.load(environment.getProperty(PROPERTY_WHATS_NEW_URL));
  }
}
