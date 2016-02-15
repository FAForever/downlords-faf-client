package playground;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class WebViewTest extends Application {

  @Override
  public void start(Stage primaryStage) throws Exception {
// Create a webview
    WebView webView = new WebView();

// Build a scene
    Scene scene = new Scene(webView, 200, 200, Color.TRANSPARENT);

// Show in a transparent stage
    primaryStage.initStyle(StageStyle.TRANSPARENT);
    primaryStage.setScene(scene);
    primaryStage.show();

// Fill the view
    StringBuilder buffer = new StringBuilder("<html>");
    buffer.append("<head><style> body { background-color: rgba(0, 0, 255, 0.5); }</style></head>");
    buffer.append("<body>");
    for (int i = 0; i < 100; i++) {
      buffer.append("</br>a line");
    }
    buffer.append("</body></html>");
    webView.getEngine().loadContent(buffer.toString());
  }

  public static void main(String[] args) {
    launch(args);
  }
}
