package com.faforever.client.news;

import com.faforever.client.preferences.PreferencesService;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

public class NewsController {

  private static final String PROPERTY_WHATS_NEW_URL = "whatsNewUrl";

  @FXML
  WebView newsRoot;

  @Autowired
  Environment environment;

  @Autowired
  PreferencesService preferencesService;

  /**
   * Loads the news web view if it hasn't already been loaded.
   */
  public void setUpIfNecessary() {
    WebEngine engine = newsRoot.getEngine();

    if (engine.getDocument() != null) {
      return;
    }

    engine.setUserDataDirectory(preferencesService.getPreferencesDirectory().toFile());

    engine.load(environment.getProperty(PROPERTY_WHATS_NEW_URL));
  }

  public Node getRoot() {
    return newsRoot;
  }
}
