package com.faforever.client.fx;

import com.google.common.base.Strings;
import com.sun.javafx.stage.PopupWindowHelper;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.Property;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;
import lombok.SneakyThrows;

import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static com.github.nocatch.NoCatch.noCatch;
import static com.sun.jna.platform.win32.WinUser.GWL_STYLE;
import static java.nio.file.Files.createDirectories;
import static javax.imageio.ImageIO.write;

/**
 * Utility class to fix some annoying JavaFX shortcomings.
 */
public final class JavaFxUtil {

  public static final StringConverter<Path> PATH_STRING_CONVERTER = new StringConverter<>() {
    @Override
    public String toString(Path object) {
      if (object == null) {
        return null;
      }
      return object.toAbsolutePath().toString();
    }

    @Override
    public Path fromString(String string) {
      if (Strings.isNullOrEmpty(string)) {
        return null;
      }
      return Paths.get(string);
    }
  };

  private JavaFxUtil() {
    throw new AssertionError("Not instantiatable");
  }

  public static void makeNumericTextField(TextField textField, int maxLength) {
    JavaFxUtil.addListener(textField.textProperty(), (observable, oldValue, newValue) -> {
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
   * Better version of {@link Tooltip#setGraphic(Node)} that does not add unnecessary space.
   * Javadoc of {@link Tooltip#setGraphic(Node)} explains that this method is meant for adding icons.
   * 
   * @param content - The content of the tooltip.
   * @return New Tooltip with added content.
   */
  public static Tooltip createCustomTooltip(Node content) {
    Tooltip tooltip = new Tooltip();
    PopupWindowHelper.getContent(tooltip).setAll(content);
    return tooltip;
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
  public static <K, V> void attachListToMap(ObservableList<V> list, ObservableMap<K, V> map) {
    addListener(map, (MapChangeListener<K, V>) change -> {
      synchronized (list) {
        if (change.wasRemoved()) {
          list.remove(change.getValueRemoved());
        } else if (change.wasAdded()) {
          list.add(change.getValueAdded());
        }
      }
    });
  }

  public static void persistImage(Image image, Path path, String format) {
    if (image == null) {
      return;
    }
    if (image.isBackgroundLoading() && image.getProgress() < 1) {
      // Let's hope that loading doesn't finish before the listener is added
      JavaFxUtil.addListener(image.progressProperty(), new ChangeListener<Number>() {
        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
          if (newValue.intValue() >= 1) {
            writeImage(image, path, format);
            image.progressProperty().removeListener(this);
          }
        }
      });
    } else {
      writeImage(image, path, format);
    }
  }

  @SneakyThrows
  private static void writeImage(Image image, Path path, String format) {
    if (image == null) {
      return;
    }
    if (path.getParent() != null) {
      createDirectories(path.getParent());
    }
    BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
    if (bufferedImage == null) {
      return;
    }
    write(bufferedImage, format, path.toFile());
  }

  public static void setAnchors(Node node, double value) {
    AnchorPane.setBottomAnchor(node, value);
    AnchorPane.setLeftAnchor(node, value);
    AnchorPane.setRightAnchor(node, value);
    AnchorPane.setTopAnchor(node, value);
  }

  public static void fixScrollSpeed(ScrollPane scrollPane) {
    Node content = scrollPane.getContent();
    content.setOnScroll(event -> {
      double deltaY = event.getDeltaY() * 3;
      double height = scrollPane.getContent().getBoundsInLocal().getHeight();
      double vvalue = scrollPane.getVvalue();
      // deltaY/height to make the scrolling equally fast regardless of the actual height of the component
      scrollPane.setVvalue(vvalue + -deltaY / height);
    });
  }

  /**
   * Since the JavaFX properties API is not thread safe, adding listeners must be synchronized on the property - which
   * is what this method does.
   */
  public static <T> void addListener(ObservableValue<T> observableValue, ChangeListener<? super T> listener) {
    synchronized (observableValue) {
      observableValue.addListener(listener);
    }
  }

  /**
   * Since the JavaFX properties API is not thread safe, adding listeners must be synchronized on the property - which
   * is what this method does.
   */
  public static void addListener(Observable observable, InvalidationListener listener) {
    synchronized (observable) {
      observable.addListener(listener);
    }
  }

  /**
   * Since the JavaFX properties API is not thread safe, adding listeners must be synchronized on the property - which
   * is what this method does.
   */
  public static <K, V> void addListener(ObservableMap<K, V> observable, MapChangeListener<K, V> listener) {
    synchronized (observable) {
      observable.addListener(listener);
    }
  }

  /**
   * Since the JavaFX properties API is not thread safe, adding listeners must be synchronized on the property - which
   * is what this method does.
   */
  public static <T> void addListener(ObservableList<T> observable, ListChangeListener<T> listener) {
    synchronized (observable) {
      observable.addListener(listener);
    }
  }

  /**
   * Since the JavaFX properties API is not thread safe, adding listeners must be synchronized on the property - which
   * is what this method does.
   */
  public static <K, V> void addListener(MapProperty<K, V> mapProperty, MapChangeListener<? super K, ? super V> listener) {
    synchronized (mapProperty) {
      mapProperty.addListener(listener);
    }
  }

  /**
   * Since the JavaFX properties API is not thread safe, adding listeners must be synchronized on the property - which
   * is what this method does.
   */
  public static <T> void addListener(ObservableSet<T> set, SetChangeListener<T> listener) {
    synchronized (set) {
      set.addListener(listener);
    }
  }

  /**
   * Since the JavaFX properties API is not thread safe, removing listeners must be synchronized on the property - which
   * is what this method does.
   */
  public static <T> void removeListener(ObservableValue<T> observableValue, ChangeListener<? super T> listener) {
    synchronized (observableValue) {
      observableValue.removeListener(listener);
    }
  }

  /**
   * Since the JavaFX properties API is not thread safe, removing listeners must be synchronized on the property - which
   * is what this method does.
   */
  public static void removeListener(Observable observable, InvalidationListener listener) {
    synchronized (observable) {
      observable.removeListener(listener);
    }
  }

  /**
   * Since the JavaFX properties API is not thread safe, removing listeners must be synchronized on the property - which
   * is what this method does.
   */
  public static <T, U> void removeListener(ObservableMap<T, U> observable, MapChangeListener<T, U> listener) {
    synchronized (observable) {
      observable.removeListener(listener);
    }
  }

  /**
   * Since the JavaFX properties API is not thread safe, binding a property must be synchronized on the property - which
   * is what this method does.
   */
  public static <T> void bind(Property<T> property, ObservableValue<? extends T> observable) {
    synchronized (property) {
      property.bind(observable);
    }
  }

  /**
   * Since the JavaFX properties API is not thread safe, binding properties must be synchronized on the properties -
   * which is what this method does. Since synchronization happens on both property in order {@code property1,
   * property2}, this is prone to deadlocks. To avoid this, pass the property with the lower visibility (e.g. method- or
   * controller-only) as first and the property with higher visibility (e.g. a property from a shared object or service)
   * as second parameter.
   */
  public static void bindBidirectional(StringProperty stringProperty, IntegerProperty integerProperty, NumberStringConverter numberStringConverter) {
    synchronized (stringProperty) {
      synchronized (integerProperty) {
        stringProperty.bindBidirectional(integerProperty, numberStringConverter);
      }
    }
  }

  /**
   * Since the JavaFX properties API is not thread safe, binding properties must be synchronized on the properties -
   * which is what this method does. Since synchronization happens on both property in order {@code property1,
   * property2}, this is prone to deadlocks. To avoid this, pass the property with the lower visibility (e.g. method- or
   * controller-only) as first and the property with higher visibility (e.g. a property from a shared object or service)
   * as second parameter.
   */
  public static <T> void bindBidirectional(Property<T> property1, Property<T> property2) {
    synchronized (property1) {
      synchronized (property2) {
        property1.bindBidirectional(property2);
      }
    }
  }

  public static Pointer getNativeWindow() {

    return User32.INSTANCE.GetActiveWindow().getPointer();
  }

  public static void runLater(Runnable runnable) {
    if (Platform.isFxApplicationThread()) {
      runnable.run();
    } else {
      Platform.runLater(runnable);
    }
  }

  public static void bindManagedToVisible(Node... nodes) {
    Arrays.stream(nodes).forEach(node -> node.managedProperty().bind(node.visibleProperty()));
  }

  public static void assureRunOnMainThread(Runnable runnable) {
    if (Platform.isFxApplicationThread()) {
      runnable.run();
    } else {
      runLater(runnable);
    }
  }
}
