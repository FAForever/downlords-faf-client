package com.faforever.client.units;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.UnitDatabase;
import com.faforever.client.fx.NodeController;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.Preferences.UnitDataBaseType;
import javafx.scene.Node;
import javafx.scene.web.WebView;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class UnitsController extends NodeController<Node> {
  private final ClientProperties clientProperties;
  private final Preferences preferences;

  public WebView unitsRoot;

  @Override
  protected void onInitialize() {
    preferences.unitDataBaseTypeProperty().when(showing).subscribe(this::loadUnitDataBase);
  }

  private void loadUnitDataBase(UnitDataBaseType newValue) {
    UnitDatabase unitDatabase = clientProperties.getUnitDatabase();
    unitsRoot.getEngine().load(newValue == UnitDataBaseType.SPOOKY ? unitDatabase.getSpookiesUrl() : unitDatabase.getRackOversUrl());
  }

  @Override
  public Node getRoot() {
    return unitsRoot;
  }

}
