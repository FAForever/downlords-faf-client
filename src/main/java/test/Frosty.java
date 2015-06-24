package test;

import javafx.animation.*;
import javafx.application.Application;
import javafx.beans.property.*;
import javafx.geometry.Rectangle2D;
import javafx.scene.*;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.*;
import javafx.scene.image.*;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

// slides a frost pane in on scroll or swipe up; slides it out on scroll or swipe down.
public class Frosty extends Application {

  private static final double W = 330;
  private static final double H = 590;

  private static final double BLUR_AMOUNT = 60;
  private static final Duration SLIDE_DURATION = Duration.seconds(0.4);

  private static final Effect frostEffect =
      new BoxBlur(BLUR_AMOUNT, BLUR_AMOUNT, 3);

  @Override
  public void start(Stage stage) {
    DoubleProperty y = new SimpleDoubleProperty(H);

    Node background = createBackground();
    Node frost = freeze(background, y);
    Node content = createContent();
    content.setVisible(false);

    Scene scene = new Scene(
        new StackPane(
            background,
            new VBox(frost),
            content
        )
    );

    stage.setScene(scene);
    stage.show();

    addSlideHandlers(y, content, scene);
  }

  // create a background node to be frozen over.
  private Node createBackground() {
    Image backgroundImage = new Image(
        "http://rack.1.mshcdn.com/media/ZgkyMDEzLzA2LzEyLzVhL2lvczczLmIxNGYzLnBuZwpwCXRodW1iCTg1MHg1OTA-CmUJanBn/2c3f63af/e4e/ios7-3.jpg"
    );
    ImageView background = new ImageView(backgroundImage);
    Rectangle2D viewport = new Rectangle2D(0, 0, W, H);
    background.setViewport(viewport);

    return background;
  }

  // create some content to be displayed on top of the frozen glass panel.
  private Label createContent() {
    Label label = new Label("Create a new question for drop shadow effects.");

    label.setStyle("-fx-font-size: 25px; -fx-text-fill: midnightblue;");
    label.setEffect(new Glow());
    label.setMaxWidth(250);
    label.setWrapText(true);

    return label;
  }

  // add handlers to slide the glass panel in and out.
  private void addSlideHandlers(DoubleProperty y, Node content, Scene scene) {
    Timeline slideIn = new Timeline(
        new KeyFrame(
            SLIDE_DURATION,
            new KeyValue(
                y,
                100
            )
        )
    );

    slideIn.setOnFinished(e -> content.setVisible(true));

    Timeline slideOut = new Timeline(
        new KeyFrame(
            SLIDE_DURATION,
            new KeyValue(
                y,
                H
            )
        )
    );

    scene.setOnSwipeUp(e -> {
      slideOut.stop();
      slideIn.play();
    });

    scene.setOnSwipeDown(e -> {
      slideIn.stop();
      slideOut.play();
      content.setVisible(false);
    });

    // scroll handler isn't necessary if you have a touch screen.
    scene.setOnScroll((ScrollEvent e) -> {
      if (e.getDeltaY() < 0) {
        slideOut.stop();
        slideIn.play();
      } else {
        slideIn.stop();
        slideOut.play();
        content.setVisible(false);
      }
    });
  }

  // create a frosty pane from a background node.
  private StackPane freeze(Node background, DoubleProperty y) {
    Image frostImage = background.snapshot(
        new SnapshotParameters(),
        null
    );
    ImageView frost = new ImageView(frostImage);

    Rectangle filler = new Rectangle(0, 0, W, H);
    filler.setFill(Color.AZURE);

    Pane frostPane = new Pane(frost);
    frostPane.setEffect(frostEffect);

    StackPane frostView = new StackPane(
        filler,
        new StackPane(frostPane)
    );

    Rectangle clipShape = new Rectangle(0, y.get(), W, H);
    frostView.setClip(clipShape);

    clipShape.yProperty().bind(y);

    return frostView;
  }

  public static void main(String[] args) {
    launch(args);
  }
}
