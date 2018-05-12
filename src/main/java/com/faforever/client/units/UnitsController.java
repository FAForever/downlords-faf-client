package com.faforever.client.units;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.UnitDatabase;
import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.preferences.Preferences.UnitDataBaseType;
import com.faforever.client.preferences.PreferencesService;
import com.google.common.base.Strings;
import javafx.scene.Node;
import javafx.scene.web.WebView;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class UnitsController extends AbstractViewController<Node> {
  private final ClientProperties clientProperties;
  private final PreferencesService preferencesService;
  private final CookieService cookieService;
  public WebView unitsRoot;

  @Inject
  public UnitsController(ClientProperties clientProperties1, PreferencesService preferencesService, CookieService cookieService) {
    this.clientProperties = clientProperties1;
    this.preferencesService = preferencesService;
    this.cookieService = cookieService;
  }

  @Override
  public void onDisplay(NavigateEvent navigateEvent) {
    if (Strings.isNullOrEmpty(unitsRoot.getEngine().getLocation())) {
      cookieService.setUpCookieManger();
      loadUnitDataBase(preferencesService.getPreferences().getUnitDataBaseType());
      JavaFxUtil.addListener(preferencesService.getPreferences().unitDataBaseTypeProperty(), (observable, oldValue, newValue) -> loadUnitDataBase(newValue));
    }
  }

  private void loadUnitDataBase(UnitDataBaseType newValue) {
    UnitDatabase unitDatabase = clientProperties.getUnitDatabase();
    unitsRoot.getEngine().load(newValue == UnitDataBaseType.SPOOKY ? unitDatabase.getSpookiesUrl() : unitDatabase.getRackOversUrl());
  }

  public Node getRoot() {
    return unitsRoot;
  }

}
