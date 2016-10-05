package com.faforever.client.units;

import com.google.common.base.Strings;
import javafx.scene.Node;
import javafx.scene.web.WebView;
import org.springframework.beans.factory.annotation.Value;

public class UnitsController {

  public WebView unitsRoot;

  @Value("${unitDatabase.url}")
  private String unitDatabaseUrl;

  public void setUpIfNecessary() {
    if (Strings.isNullOrEmpty(unitsRoot.getEngine().getLocation())) {
      unitsRoot.getEngine().load(unitDatabaseUrl);
    }
  }

  public Node getRoot() {
    return unitsRoot;
  }
}
