package com.faforever.client.fx;

import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.ThemeService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Popup;
import javafx.stage.PopupWindow;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.github.nocatch.NoCatch.noCatch;
import static javax.imageio.ImageIO.write;

/**
 * Utility class to fix some annoying JavaFX shortcomings.
 */
public final class JavaFxUtil {

  public static final StringConverter<Path> PATH_STRING_CONVERTER = new StringConverter<Path>() {
    @Override
    public String toString(Path object) {
      if (object == null) {
        return null;
      }
      return object.toAbsolutePath().toString();
    }

    @Override
    public Path fromString(String string) {
      if (string == null) {
        return null;
      }
      return Paths.get(string);
    }
  };

  private static final double ZOOM_STEP = 0.2d;

  private JavaFxUtil() {
    throw new AssertionError("Not instantiatable");
  }

  public static void makeSuggestionField(TextField textField,
                                         Function<String, CompletionStage<Set<Label>>> itemsFactory,
                                         Consumer<Void> onAction) {
    ListView<Label> listView = new ListView<>();
    listView.prefWidthProperty().bind(textField.widthProperty());
    listView.setFixedCellSize(24);

    Popup popupControl = new Popup();
    popupControl.setAutoHide(true);
    popupControl.setAutoFix(false);
    popupControl.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_TOP_LEFT);
    popupControl.getScene().setRoot(listView);

    BooleanProperty isUserSelecting = new SimpleBooleanProperty();

    popupControl.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
      // Don't close on space
      if (event.getCode() == KeyCode.SPACE) {
        event.consume();
      }
    });
    listView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue != null) {
        isUserSelecting.set(true);
        textField.setText(newValue.getText());
      }
    });
    listView.setOnMouseClicked(event -> popupControl.hide());
    listView.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
      if (event.getCode() == KeyCode.ENTER) {
        onAction.accept(null);
        popupControl.hide();
      }
    });
    textField.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
      if (event.getCode() == KeyCode.DOWN) {
        listView.requestFocus();
        listView.getSelectionModel().selectFirst();
        textField.positionCaret(Integer.MAX_VALUE);
      } else {
        isUserSelecting.set(false);
      }
    });
    textField.textProperty().addListener((observable, oldValue, newValue) -> {
      if (isUserSelecting.get()) {
        return;
      }
      if (newValue.isEmpty()) {
        popupControl.hide();
        return;
      }

      if (oldValue.trim().equalsIgnoreCase(newValue)) {
        return;
      }

      itemsFactory.apply(newValue).thenAccept(items -> Platform.runLater(() -> {
        listView.getItems().setAll(items);
        listView.setPrefHeight(Math.min(120, items.size() * (listView.getFixedCellSize() + 2)));
        if (listView.getItems().isEmpty()) {
          popupControl.hide();
        } else if (!popupControl.isShowing()) {
          Bounds screenBounds = textField.localToScreen(textField.getBoundsInLocal());
          popupControl.show(textField.getScene().getWindow(), screenBounds.getMinX(), screenBounds.getMaxY());
        }
      }));
    });
  }

  public static void makeNumericTextField(TextField textField, int maxLength) {
    textField.textProperty().addListener((observable, oldValue, newValue) -> {
      String value = newValue;
      if (!value.matches("\\d*")) {
        value = newValue.replaceAll("[^\\d]", "");
      }

      if (maxLength > 0 && value.length() > maxLength) {
        value = value.substring(0, maxLength);
      }

      textField.setText(value);
      if (textField.getCaretPosition() > textField.getLength()) {
        textField.positionCaret(textField.getLength());
      }
    });
  }

  /**
   * Uses reflection to change to tooltip delay/duration to some sane values.
   * <p>
   * See <a href="https://javafx-jira.kenai.com/browse/RT-19538">https://javafx-jira.kenai.com/browse/RT-19538</a>
   */
  public static void fixTooltipDuration() {
    noCatch(() -> {
      Field fieldBehavior = Tooltip.class.getDeclaredField("BEHAVIOR");
      fieldBehavior.setAccessible(true);
      Object objBehavior = fieldBehavior.get(null);

      Field activationTimerField = objBehavior.getClass().getDeclaredField("activationTimer");
      activationTimerField.setAccessible(true);
      Timeline objTimer = (Timeline) activationTimerField.get(objBehavior);

      objTimer.getKeyFrames().setAll(new KeyFrame(new Duration(500)));

      Field hideTimerField = objBehavior.getClass().getDeclaredField("hideTimer");
      hideTimerField.setAccessible(true);
      objTimer = (Timeline) hideTimerField.get(objBehavior);

      objTimer.getKeyFrames().setAll(new KeyFrame(new Duration(100000)));
    });
  }

  /**
   * Centers a window FOR REAL. https://javafx-jira.kenai.com/browse/RT-40368
   */
  public static void centerOnScreen(Stage stage) {
    double width = stage.getWidth();
    double height = stage.getHeight();

    Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
    stage.setX((screenBounds.getMaxX() - screenBounds.getMinX() - width) / 2);
    stage.setY((screenBounds.getMaxY() - screenBounds.getMinY() - height) / 2);
  }

  public static void assertApplicationThread() {
    if (!Platform.isFxApplicationThread()) {
      throw new IllegalStateException("Must run in FX Application thread");
    }
  }

  public static void assertBackgroundThread() {
    if (Platform.isFxApplicationThread()) {
      throw new IllegalStateException("Must not run in FX Application thread");
    }
  }

  public static void configureWebView(WebView webView, PreferencesService preferencesService, ThemeService themeService) {
    webView.setContextMenuEnabled(false);
    webView.setOnScroll(event -> {
      if (event.isControlDown()) {
        if (event.getDeltaY() > 0) {
          webView.setZoom(webView.getZoom() + ZOOM_STEP);
        } else {
          webView.setZoom(webView.getZoom() - ZOOM_STEP);
        }
      }
    });
    webView.setOnKeyPressed(event -> {
      if (event.isControlDown() && (event.getCode() == KeyCode.DIGIT0 || event.getCode() == KeyCode.NUMPAD0)) {
        webView.setZoom(1);
      }
    });

    WebEngine engine = webView.getEngine();
    engine.setUserDataDirectory(preferencesService.getCacheDirectory().toFile());
    themeService.registerWebView(webView);
  }

  public static boolean isVisibleRecursively(Node node) {
    if (!node.isVisible()) {
      return false;
    }

    Parent parent = node.getParent();
    if (parent == null) {
      return node.getScene() != null;
    }
    return isVisibleRecursively(parent);
  }

  public static String toRgbCode(Color color) {
    return String.format("#%02X%02X%02X",
        (int) (color.getRed() * 255),
        (int) (color.getGreen() * 255),
        (int) (color.getBlue() * 255));
  }

  /**
   * Updates the specified list with any changes made to the specified map, but not vice versa.
   */
  public static <T> void attachListToMap(ObservableList<T> list, ObservableMap<?, T> map) {
    map.addListener((MapChangeListener<Object, T>) change -> {
      if (change.wasRemoved()) {
        list.remove(change.getValueRemoved());
      } else if (change.wasAdded()) {
        list.add(change.getValueAdded());
      }
    });
  }

  public static void persistImage(Image image, Path path, String format) {
    if (image.isBackgroundLoading() && image.getProgress() < 1) {
      // Let's hope that loading doesn't finish before the listener is added
      image.progressProperty().addListener(new ChangeListener<Number>() {
        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
          if (newValue.intValue() >= 1) {
            noCatch(() -> write(SwingFXUtils.fromFXImage(image, null), format, path.toFile()));
            image.progressProperty().removeListener(this);
          }
        }
      });
    } else {
      noCatch(() -> write(SwingFXUtils.fromFXImage(image, null), format, path.toFile()));
    }
  }
}
