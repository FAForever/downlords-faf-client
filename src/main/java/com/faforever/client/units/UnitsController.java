package com.faforever.client.units;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.UnitDatabase;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.NodeController;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.Preferences.UnitDataBaseType;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class UnitsController extends NodeController<Node> {
  private final ClientProperties clientProperties;
  private final Preferences preferences;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  public StackPane unitsRoot;

  private WebView webView;

  @Override
  protected void onInitialize() {
    CompletableFuture.runAsync(() -> {
      webView = new WebView();
      unitsRoot.getChildren().add(webView);
      preferences.unitDataBaseTypeProperty().when(showing).subscribe(this::loadUnitDataBase);
    }, fxApplicationThreadExecutor);
  }

  private void loadUnitDataBase(UnitDataBaseType newValue) {
    UnitDatabase unitDatabase = clientProperties.getUnitDatabase();
    webView.getEngine()
           .load(newValue == UnitDataBaseType.SPOOKY ? unitDatabase.getSpookiesUrl() : unitDatabase.getRackOversUrl());
  }

  @Override
  public Node getRoot() {
    return unitsRoot;
  }

}
