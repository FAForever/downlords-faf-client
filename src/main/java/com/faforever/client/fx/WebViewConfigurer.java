package com.faforever.client.fx;

import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import javafx.concurrent.Worker.State;
import javafx.scene.input.KeyCode;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import javax.inject.Inject;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class WebViewConfigurer {

  private static final double ZOOM_STEP = 0.2d;
  private static final String EVENT_TYPE_CLICK = "click";

  private final PreferencesService preferencesService;
  private final UiService uiService;
  private final PlatformService platformService;

  @Inject
  public WebViewConfigurer(PreferencesService preferencesService, UiService uiService, PlatformService platformService) {
    this.preferencesService = preferencesService;
    this.uiService = uiService;
    this.platformService = platformService;
  }

  public void configureWebView(WebView webView) {
    webView.setContextMenuEnabled(false);
    webView.setOnScroll(event -> {
      if (event.isControlDown()) {
        webView.setZoom(webView.getZoom() + ZOOM_STEP * Math.signum(event.getDeltaY()));
      }
    });
    webView.setOnKeyPressed(event -> {
      if (event.isControlDown() && (event.getCode() == KeyCode.DIGIT0 || event.getCode() == KeyCode.NUMPAD0)) {
        webView.setZoom(1);
      }
    });

    WebEngine engine = webView.getEngine();
    engine.setUserDataDirectory(preferencesService.getCacheDirectory().toFile());
    uiService.registerWebView(webView);
    JavaFxUtil.addListener(webView.getEngine().getLoadWorker().stateProperty(), (observable, oldValue, newValue) -> {
      if (newValue == State.SUCCEEDED) {
        EventListener listener = ev -> {
          ev.preventDefault();
          String domEventType = ev.getType();
          if (domEventType.equals(EVENT_TYPE_CLICK)) {
            platformService.showDocument(((Element) ev.getTarget()).getAttribute("href"));
            ev.stopPropagation();
          }
        };

        Document doc = webView.getEngine().getDocument();
        if (doc == null) {
          return;
        }
        NodeList nodeList = doc.getElementsByTagName("a");
        for (int i = 0; i < nodeList.getLength(); i++) {
          ((EventTarget) nodeList.item(i)).addEventListener(EVENT_TYPE_CLICK, listener, true);
        }
      }
    });
  }
}
