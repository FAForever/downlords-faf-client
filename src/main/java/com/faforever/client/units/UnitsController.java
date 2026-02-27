package com.faforever.client.units;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.NodeController;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.Preferences.UnitDataBaseType;
import com.faforever.client.theme.ThemeService;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class UnitsController extends NodeController<StackPane> {
  private static final String UNIT_DB_IMAGES_PATH = "theme/images/unit_databases/";

  private final ClientProperties clientProperties;
  private final Preferences preferences;
  private final PlatformService platformService;
  private final I18n i18n;
  private final ThemeService themeService;

  private UnitDataBaseType currentType;

  public StackPane unitsRoot;
  public WebView webView;
  public StackPane externalBrowserPane;
  public VBox contentBox;
  public Label titleLabel;

  @Override
  protected void onInitialize() {
    JavaFxUtil.bindManagedToVisible(webView, externalBrowserPane);
    contentBox.translateYProperty().bind(externalBrowserPane.heightProperty().multiply(0.15));
    preferences.unitDataBaseTypeProperty().when(showing).subscribe(this::loadUnitDataBase);
  }

  private void loadUnitDataBase(UnitDataBaseType type) {
    currentType = type;
    boolean isExternal = type.isExternal();

    webView.setVisible(!isExternal);
    externalBrowserPane.setVisible(isExternal);

    if (isExternal) {
      platformService.showDocument(type.getUrl(clientProperties.getUnitDatabase()));
      String dbName = i18n.get(type.getI18nKey());
      titleLabel.setText(i18n.get("unitDatabase.external.message", dbName));
      loadBackgroundImage(type);
    } else {
      String url = type.getUrl(clientProperties.getUnitDatabase());
      webView.getEngine().load(url);
    }
  }

  private void loadBackgroundImage(UnitDataBaseType type) {
    String imagePath = UNIT_DB_IMAGES_PATH + type.name().toLowerCase() + ".png";
    Image image = themeService.getThemeImage(imagePath);

    if (image == null || image.isError()) {
      externalBrowserPane.setBackground(null);
      return;
    }

    BackgroundImage bgImage = new BackgroundImage(
        image,
        BackgroundRepeat.NO_REPEAT,
        BackgroundRepeat.NO_REPEAT,
        BackgroundPosition.CENTER,
        new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, true)
    );
    externalBrowserPane.setBackground(new Background(bgImage));
  }

  public void onOpenExternalBrowserClicked() {
    if (currentType == null || !currentType.isExternal()) {
      return;
    }
    platformService.showDocument(currentType.getUrl(clientProperties.getUnitDatabase()));
  }

  @Override
  public StackPane getRoot() {
    return unitsRoot;
  }

}
