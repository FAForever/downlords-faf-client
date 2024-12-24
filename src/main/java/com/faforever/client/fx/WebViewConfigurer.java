package com.faforever.client.fx;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.theme.ThemeService;
import javafx.concurrent.Worker.State;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import lombok.RequiredArgsConstructor;
import netscape.javascript.JSObject;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class WebViewConfigurer {

  /**
   * This is the member name within the JavaScript code that provides access to the Java callback instance.
   */
  private static final String JAVA_REFERENCE_IN_JAVASCRIPT = "java";
  private static final double ZOOM_STEP = 0.2d;

  private final ThemeService themeService;
  private final ClientProperties clientProperties;
  private final ObjectFactory<BrowserCallback> browserCallbackFactory;

  public void configureWebView(WebView webView) {
    WebEngine engine = webView.getEngine();
    webView.setPageFill(Color.TRANSPARENT);
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

    BrowserCallback browserCallback = browserCallbackFactory.getObject();

    engine.setUserAgent(clientProperties.getUserAgent()); // removes faforever.com header and footer
    themeService.registerWebView(webView);
    JavaFxUtil.addListener(engine.getLoadWorker().stateProperty(), (observable, oldValue, newValue) -> {
      if (newValue != State.SUCCEEDED) {
        return;
      }
      themeService.registerWebView(webView);

      ((JSObject) engine.executeScript("window")).setMember(JAVA_REFERENCE_IN_JAVASCRIPT, browserCallback);
      Document document = webView.getEngine().getDocument();
      if (document == null) {
        return;
      }

      engine.executeScript("""
          let obs = new MutationObserver((mutations, observer) => {
            const addedNodes = mutations.flatMap(mut => Array.from(mut.addedNodes));
            const links = [];
            for (const node of addedNodes) {
              if (node?.querySelectorAll) {
                links.push(...document.querySelectorAll("a"));
              }
            }
            for (const elt of links) {
              if (!elt.href.includes("javascript:java.openUrl")) {
                elt.setAttribute("href", "javascript:java.openUrl('" + elt.href + "')");
              }
            }
          });
          obs.observe(document.body, {subtree:true, childList:true});
          """);

      NodeList nodeList = document.getElementsByTagName("a");
      for (int i = 0; i < nodeList.getLength(); i++) {
        Element link = (Element) nodeList.item(i);
        String href = link.getAttribute("href");
        if (!href.contains("javascript:java.openUrl")) {
          link.setAttribute("href", "javascript:java.openUrl('" + href + "');");
        }
      }
    });
  }
}
