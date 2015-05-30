package com.faforever.client.news;

import com.faforever.client.preferences.PreferencesService;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.util.Collection;

public class NewsController {

  private static final String PROPERTY_WHATS_NEW_URL = "whatsNewUrl";

  @Autowired
  Environment environment;

  @Autowired
  PreferencesService preferencesService;

  @FXML
  private WebView newsRoot;

  public void setUp() {
    WebEngine engine = newsRoot.getEngine();
    engine.setUserDataDirectory(preferencesService.getPreferencesDirectory().toFile());

    engine.load(environment.getProperty(PROPERTY_WHATS_NEW_URL));
  }

  public Node getRoot() {
    return newsRoot;
  }
}
