package com.faforever.client.tutorial;

import com.faforever.client.domain.MapBean;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.TutorialBean;
import com.faforever.client.fx.FxApplicationThreadExecutor;
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
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class TutorialDetailController extends NodeController<Node> {
  private final I18n i18n;
  private final MapService mapService;
  private final WebViewConfigurer webViewConfigurer;
  private final TutorialService tutorialService;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  public VBox mapContainer;
  public BorderPane root;
  public ImageView mapImage;
  public Label mapNameLabel;
  public VBox webViewContainer;
  public Label titleLabel;
  public Button launchButton;
  private TutorialBean tutorial;

  private CompletableFuture<WebView> initializeWebViewFuture;

  @Override
  protected void onInitialize() {
    mapContainer.managedProperty().bind(mapContainer.visibleProperty());
    launchButton.managedProperty().bind(launchButton.visibleProperty());

    initializeWebViewFuture = CompletableFuture.supplyAsync(() -> {
      WebView webView = new WebView();
      webView.setContextMenuEnabled(false);
      webViewConfigurer.configureWebView(webView);
      webViewContainer.getChildren().add(webView);
      VBox.setVgrow(webView, Priority.ALWAYS);
      return webView;
    }, fxApplicationThreadExecutor);
  }

  @Override
  public Node getRoot() {
    return root;
  }

  public void launchReplay() {
    if (tutorial != null) {
      tutorialService.launchTutorial(tutorial);
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

    initializeWebViewFuture.thenAcceptAsync(webView -> webView.getEngine().loadContent(tutorial.getDescription()),
                                            fxApplicationThreadExecutor);
    launchButton.visibleProperty().bind(tutorial.launchableProperty());
  }
}
