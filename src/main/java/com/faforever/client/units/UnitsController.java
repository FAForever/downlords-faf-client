package com.faforever.client.units;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.UnitDatabase;
import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.preferences.Preferences.UnitDataBaseType;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.ProgrammingError;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import javafx.scene.Node;
import javafx.scene.web.WebView;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Base64;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class UnitsController extends AbstractViewController<Node> {
  private final ClientProperties clientProperties;
  private final PreferencesService preferencesService;
  private final CookieService cookieService;
  private final I18n i18n;
  public WebView unitsRoot;

  @Inject
  public UnitsController(ClientProperties clientProperties, PreferencesService preferencesService, CookieService cookieService, I18n i18n) {
    this.clientProperties = clientProperties;
    this.preferencesService = preferencesService;
    this.cookieService = cookieService;
    this.i18n = i18n;
  }

  @Override
  public void onDisplay(NavigateEvent navigateEvent) {
    if (Strings.isNullOrEmpty(unitsRoot.getEngine().getLocation())) {
      cookieService.setUpCookieManger();
      loadUnitDataBase(preferencesService.getPreferences().getUnitDataBaseType());
      preferencesService.getPreferences().unitDataBaseTypeProperty().addListener((observable, oldValue, newValue) -> loadUnitDataBase(newValue));
    }
  }

  private void loadUnitDataBase(UnitDataBaseType newValue) {
    UnitDatabase unitDatabase = clientProperties.getUnitDatabase();

    switch (newValue) {
      case RACKOVER:
        String rackoverSettings = new Gson().toJson(new RackoversDBSettings(i18n.getUserSpecificLocale()));
        String rackoverSettingsEncoded = Base64.getEncoder().encodeToString(rackoverSettings.getBytes());
        unitsRoot.getEngine().load(String.format(unitDatabase.getRackOversUrl(), rackoverSettingsEncoded));
        break;

      case SPOOKY:
        unitsRoot.getEngine().load(unitDatabase.getSpookiesUrl());
        break;

      default:
        new ProgrammingError("Trying to load unimplemented database type: " + newValue.toString());
    }
  }

  public Node getRoot() {
    return unitsRoot;
  }

}
