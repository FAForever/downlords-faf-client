package com.faforever.client.tutorial;

import com.faforever.client.domain.MapBean;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.TutorialBean;
import com.faforever.client.fx.NodeController;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class TutorialDetailController extends NodeController<Node> {
  private final I18n i18n;
  private final MapService mapService;
  private final WebViewConfigurer webViewConfigurer;
  private final TutorialService tutorialServie;
  public VBox mapContainer;
  public BorderPane root;
  public ImageView mapImage;
  public Label mapNameLabel;
  public WebView descriptionWebView;
  public Label titleLabel;
  public Button launchButton;
  private TutorialBean tutorial;

  public TutorialDetailController(I18n i18n, MapService mapService, WebViewConfigurer webViewConfigurer, TutorialService tutorialService) {
    this.i18n = i18n;
    this.mapService = mapService;
    this.webViewConfigurer = webViewConfigurer;
    this.tutorialServie = tutorialService;
  }

  @Override
  protected void onInitialize() {
    mapContainer.managedProperty().bind(mapContainer.visibleProperty());
    descriptionWebView.setContextMenuEnabled(false);
    webViewConfigurer.configureWebView(descriptionWebView);
    launchButton.managedProperty().bind(launchButton.visibleProperty());
  }

  @Override
  public Node getRoot() {
    return root;
  }

  public void launchReplay() {
    if (tutorial != null) {
      tutorialServie.launchTutorial(tutorial);
    }
  }

  public TutorialBean getTutorial() {
    return tutorial;
  }

  public void setTutorial(TutorialBean tutorial) {
    this.tutorial = tutorial;
    titleLabel.textProperty().bind(tutorial.titleProperty());
    if (tutorial.getMapVersion() != null) {
      mapNameLabel.textProperty()
          .bind(tutorial.mapVersionProperty()
              .flatMap(MapVersionBean::mapProperty)
              .flatMap(MapBean::displayNameProperty)
              .map(displayName -> i18n.get("tutorial.mapName", displayName)));

      mapImage.setImage(mapService.loadPreview(tutorial.getMapVersion(), PreviewSize.LARGE));
      mapContainer.setVisible(true);
    } else {
      mapContainer.setVisible(false);
    }

    descriptionWebView.getEngine().loadContent(tutorial.getDescription());
    launchButton.visibleProperty().bind(tutorial.launchableProperty());
  }
}
