package playground;

import javafx.application.Application;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

public class Play extends Application {


  public class JavaApplication {

    public void callJavascript(String msg) {
      System.out.println("JS>> " + msg);
    }
  }

  @Override
  public void start(Stage stage) {
    final WebView webView = new WebView();
    final WebEngine webEngine = webView.getEngine();

    // Early call of executeScript to get a JavaScript object, a proxy for the
    // Java object to be accessed on the JavaScript environment

    webEngine.getLoadWorker().stateProperty().addListener((ov, oldState, newState) -> {
      if (newState == Worker.State.SCHEDULED) {
        System.out.println("state: scheduled");
      } else if (newState == Worker.State.RUNNING) {
        JSObject window = (JSObject) webEngine.executeScript("window");
        window.setMember("app", new JavaApplication());
        System.out.println("state: running");
      } else if (newState == Worker.State.SUCCEEDED) {
        System.out.println("state: succeeded");
      }
    });

    Button button = new Button("Load Content");
    button.setOnAction(e -> webEngine.load("file://S://tmp/test.html"));

    VBox vbox = new VBox(10, button, webView);
    Scene scene = new Scene(vbox, 400, 300);

    stage.setScene(scene);
    stage.show();

  }

  public static void main(String[] args) {
    launch(args);
  }
}
