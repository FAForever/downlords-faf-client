package com.faforever.client.ui.dialog;

import com.faforever.client.ui.converter.DialogTransitionConverter;
import com.faforever.client.ui.effects.DepthManager;
import com.faforever.client.ui.transitions.CachedTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.Transition;
import javafx.beans.DefaultProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.Effect;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Ported from JFoenix since we wanted to get rid of the JFoenix dependency */
@DefaultProperty(value = "content")
public class Dialog extends StackPane {

  private static final String DEFAULT_STYLE_CLASS = "dialog";

  private final ObjectProperty<EventHandler<? super DialogEvent>> onDialogClosedProperty = new ObjectPropertyBase<>() {
    @Override
    protected void invalidated() {
      setEventHandler(DialogEvent.CLOSED, get());
    }

    @Override
    public Object getBean() {
      return Dialog.this;
    }

    @Override
    public String getName() {
      return "onClosed";
    }
  };
  /** Whether the dialog will close when clicking on the overlay. */
  private final BooleanProperty overlayClose = new SimpleBooleanProperty(true);
  /**
   * If {@code true}, the content of dialog container will be cached and replaced with an image when displaying the
   * dialog (better performance). This is recommended if the content behind the dialog will not change during the
   * showing period
   */
  private final BooleanProperty cacheContainer = new SimpleBooleanProperty(false);
  private final StyleableObjectProperty<DialogTransition> transitionType = new SimpleStyleableObjectProperty<>(
      StyleableProperties.DIALOG_TRANSITION,
      Dialog.this,
      "dialogTransition",
      DialogTransition.CENTER);
  private final ObjectProperty<EventHandler<? super DialogEvent>> onDialogOpenedProperty = new ObjectPropertyBase<>() {
    @Override
    protected void invalidated() {
      setEventHandler(DialogEvent.OPENED, get());
    }

    @Override
    public Object getBean() {
      return Dialog.this;
    }

    @Override
    public String getName() {
      return "onOpened";
    }
  };
  private StackPane contentHolder;
  private double offsetX;
  private double offsetY;
  private StackPane dialogContainer;
  private Region content;
  private Transition animation;
  private ArrayList<Node> tempContent;
  EventHandler<? super MouseEvent> closeHandler = e -> close();

  /** Creates an empty Dialog with CENTER animation type. */
  public Dialog() {
    this(null, null, DialogTransition.CENTER);
  }

  public Dialog(StackPane dialogContainer, Region content, DialogTransition transitionType) {
    initialize();
    setContent(content);
    setDialogContainer(dialogContainer);
    this.transitionType.set(transitionType);
    initChangeListeners();
  }

  public Dialog(StackPane dialogContainer, Region content, DialogTransition transitionType, boolean overlayClose) {
    setOverlayClose(overlayClose);
    initialize();
    setContent(content);
    setDialogContainer(dialogContainer);
    this.transitionType.set(transitionType);
    initChangeListeners();
  }

  @Override
  public String getUserAgentStylesheet() {
    return getClass().getResource("/css/controls/dialog.css").toExternalForm();
  }

  public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
    return StyleableProperties.CHILD_STYLEABLES;
  }

  private void initChangeListeners() {
    overlayCloseProperty().addListener((o, oldVal, newVal) -> {
      if (newVal) {
        this.addEventHandler(MouseEvent.MOUSE_PRESSED, closeHandler);
      } else {
        this.removeEventHandler(MouseEvent.MOUSE_PRESSED, closeHandler);
      }
    });
  }

  private void initialize() {
    this.setVisible(false);
    this.getStyleClass().addAll(DEFAULT_STYLE_CLASS, "dialog-overlay-pane");

    this.transitionType.addListener((o, oldVal, newVal) -> animation = getShowAnimation(transitionType.get()));

    contentHolder = new StackPane();
    contentHolder.getStyleClass().add("dialog-content");
    DepthManager.setDepth(contentHolder, 4);
    contentHolder.setPickOnBounds(false);
    // ensure stackpane is never resized beyond it's preferred size
    contentHolder.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    this.getChildren().add(contentHolder);
    StackPane.setAlignment(contentHolder, Pos.CENTER);
    this.setBackground(new Background(new BackgroundFill(Color.rgb(0, 0, 0, 0.1), null, null)));
    // close the dialog if clicked on the overlay pane
    if (overlayClose.get()) {
      this.addEventHandler(MouseEvent.MOUSE_PRESSED, closeHandler);
    }
    // prevent propagating the events to overlay pane
    contentHolder.addEventHandler(MouseEvent.ANY, Event::consume);
  }

  public StackPane getDialogContainer() {
    return dialogContainer;
  }

  public void setDialogContainer(StackPane dialogContainer) {
    if (dialogContainer != null) {
      this.dialogContainer = dialogContainer;
      // FIXME: need to be improved to consider only the parent boundary
      offsetX = dialogContainer.getBoundsInLocal().getWidth();
      offsetY = dialogContainer.getBoundsInLocal().getHeight();
      animation = getShowAnimation(transitionType.get());
    }
  }

  public Region getContent() {
    return content;
  }

  public void setContent(Region content) {
    if (content != null) {
      this.content = content;
      this.content.setPickOnBounds(false);
      contentHolder.getChildren().setAll(content);
    }
  }

  public final BooleanProperty overlayCloseProperty() {
    return this.overlayClose;
  }

  public final boolean isOverlayClose() {
    return this.overlayCloseProperty().get();
  }

  public final void setOverlayClose(final boolean overlayClose) {
    this.overlayCloseProperty().set(overlayClose);
  }

  public boolean isCacheContainer() {
    return cacheContainer.get();
  }

  public void setCacheContainer(boolean cacheContainer) {
    this.cacheContainer.set(cacheContainer);
  }

  public BooleanProperty cacheContainerProperty() {
    return cacheContainer;
  }

  public void show(StackPane dialogContainer) {
    this.setDialogContainer(dialogContainer);
    showDialog();
  }

  public void show() {
    this.setDialogContainer(dialogContainer);
    showDialog();
  }

  private void showDialog() {
    if (dialogContainer == null) {
      throw new RuntimeException("Dialog container is not set");
    }

    if (isCacheContainer()) {
      tempContent = new ArrayList<>(dialogContainer.getChildren());

      SnapshotParameters snapshotParams = new SnapshotParameters();
      snapshotParams.setFill(Color.TRANSPARENT);
      WritableImage temp = dialogContainer.snapshot(snapshotParams,
          new WritableImage((int) dialogContainer.getWidth(),
              (int) dialogContainer.getHeight()));
      ImageView tempImage = new ImageView(temp);
      tempImage.setCache(true);
      tempImage.setCacheHint(CacheHint.SPEED);
      dialogContainer.getChildren().setAll(tempImage, this);
    } else {
      //prevent error if opening an already opened dialog
      dialogContainer.getChildren().remove(this);
      tempContent = null;
      dialogContainer.getChildren().add(this);
    }

    if (animation != null) {
      animation.play();
    } else {
      setVisible(true);
      setOpacity(1);
      Event.fireEvent(Dialog.this, new DialogEvent(DialogEvent.OPENED));
    }
  }

  public void close() {
    if (animation != null) {
      animation.setRate(-1);
      animation.play();
      animation.setOnFinished(e -> closeDialog());
    } else {
      setOpacity(0);
      setVisible(false);
      closeDialog();
    }
  }

  private void closeDialog() {
    resetProperties();
    Event.fireEvent(Dialog.this, new DialogEvent(DialogEvent.CLOSED));
    if (tempContent == null) {
      dialogContainer.getChildren().remove(this);
    } else {
      dialogContainer.getChildren().setAll(tempContent);
    }
  }

  private Transition getShowAnimation(DialogTransition transitionType) {
    Transition animation = null;
    if (contentHolder != null) {
      switch (transitionType) {
        case LEFT:
          contentHolder.setScaleX(1);
          contentHolder.setScaleY(1);
          contentHolder.setTranslateX(-offsetX);
          animation = new LeftTransition();
          break;
        case RIGHT:
          contentHolder.setScaleX(1);
          contentHolder.setScaleY(1);
          contentHolder.setTranslateX(offsetX);
          animation = new RightTransition();
          break;
        case TOP:
          contentHolder.setScaleX(1);
          contentHolder.setScaleY(1);
          contentHolder.setTranslateY(-offsetY);
          animation = new TopTransition();
          break;
        case BOTTOM:
          contentHolder.setScaleX(1);
          contentHolder.setScaleY(1);
          contentHolder.setTranslateY(offsetY);
          animation = new BottomTransition();
          break;
        case CENTER:
          contentHolder.setScaleX(0);
          contentHolder.setScaleY(0);
          animation = new CenterTransition();
          break;
        default:
          animation = null;
          contentHolder.setScaleX(1);
          contentHolder.setScaleY(1);
          contentHolder.setTranslateX(0);
          contentHolder.setTranslateY(0);
          break;
      }
    }
    if (animation != null) {
      animation.setOnFinished(finish ->
          Event.fireEvent(Dialog.this, new DialogEvent(DialogEvent.OPENED)));
    }
    return animation;
  }

  private void resetProperties() {
    this.setVisible(false);
    contentHolder.setTranslateX(0);
    contentHolder.setTranslateY(0);
    contentHolder.setScaleX(1);
    contentHolder.setScaleY(1);
  }

  public DialogTransition getTransitionType() {
    return transitionType == null ? DialogTransition.CENTER : transitionType.get();
  }

  public void setTransitionType(DialogTransition transition) {
    this.transitionType.set(transition);
  }

  public StyleableObjectProperty<DialogTransition> transitionTypeProperty() {
    return this.transitionType;
  }

  @Override
  public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
    return getClassCssMetaData();
  }

  /**
   * Defines a function to be called when the dialog is closed. Note: it will be triggered after the close animation is
   * finished.
   */
  public ObjectProperty<EventHandler<? super DialogEvent>> onDialogClosedProperty() {
    return onDialogClosedProperty;
  }

  public EventHandler<? super DialogEvent> getOnDialogClosed() {
    return onDialogClosedProperty().get();
  }

  public void setOnDialogClosed(EventHandler<? super DialogEvent> handler) {
    onDialogClosedProperty().set(handler);
  }

  /**
   * Defines a function to be called when the dialog is opened. Note: it will be triggered after the show animation is
   * finished.
   */
  public ObjectProperty<EventHandler<? super DialogEvent>> onDialogOpenedProperty() {
    return onDialogOpenedProperty;
  }

  public EventHandler<? super DialogEvent> getOnDialogOpened() {
    return onDialogOpenedProperty().get();
  }

  public void setOnDialogOpened(EventHandler<? super DialogEvent> handler) {
    onDialogOpenedProperty().set(handler);
  }

  public enum DialogTransition {
    CENTER, TOP, RIGHT, BOTTOM, LEFT, NONE
  }

  private static class StyleableProperties {
    private static final CssMetaData<Dialog, DialogTransition> DIALOG_TRANSITION =
        new CssMetaData<>("-dialog-transition",
            DialogTransitionConverter.getInstance(),
            DialogTransition.CENTER) {
          @Override
          public boolean isSettable(Dialog control) {
            return control.transitionType == null || !control.transitionType.isBound();
          }

          @Override
          public StyleableProperty<DialogTransition> getStyleableProperty(Dialog control) {
            return control.transitionTypeProperty();
          }
        };

    private static final List<CssMetaData<? extends Styleable, ?>> CHILD_STYLEABLES;

    static {
      final List<CssMetaData<? extends Styleable, ?>> styleables =
          new ArrayList<>(StackPane.getClassCssMetaData());
      Collections.addAll(styleables,
          DIALOG_TRANSITION
      );
      CHILD_STYLEABLES = Collections.unmodifiableList(styleables);
    }
  }

  public static class DialogEvent extends Event {

    public static final EventType<DialogEvent> CLOSED =
        new EventType<>(Event.ANY, "DFC_DIALOG_CLOSED");
    public static final EventType<DialogEvent> OPENED =
        new EventType<>(Event.ANY, "DFC_DIALOG_OPENED");

    public DialogEvent(EventType<? extends Event> eventType) {
      super(eventType);
    }
  }

  private class LeftTransition extends CachedTransition {
    LeftTransition() {
      super(contentHolder, new Timeline(
          new KeyFrame(Duration.ZERO,
              new KeyValue(contentHolder.translateXProperty(), -offsetX, Interpolator.EASE_BOTH),
              new KeyValue(Dialog.this.visibleProperty(), false, Interpolator.EASE_BOTH)
          ),
          new KeyFrame(Duration.millis(10),
              new KeyValue(Dialog.this.visibleProperty(), true, Interpolator.EASE_BOTH),
              new KeyValue(Dialog.this.opacityProperty(), 0, Interpolator.EASE_BOTH)
          ),
          new KeyFrame(Duration.millis(1000),
              new KeyValue(contentHolder.translateXProperty(), 0, Interpolator.EASE_BOTH),
              new KeyValue(Dialog.this.opacityProperty(), 1, Interpolator.EASE_BOTH)
          ))
      );
      // reduce the number to increase the shifting , increase number to reduce shifting
      setCycleDuration(Duration.seconds(0.4));
      setDelay(Duration.seconds(0));
    }
  }

  private class RightTransition extends CachedTransition {
    RightTransition() {
      super(contentHolder, new Timeline(
          new KeyFrame(Duration.ZERO,
              new KeyValue(contentHolder.translateXProperty(), offsetX, Interpolator.EASE_BOTH),
              new KeyValue(Dialog.this.visibleProperty(), false, Interpolator.EASE_BOTH)
          ),
          new KeyFrame(Duration.millis(10),
              new KeyValue(Dialog.this.visibleProperty(), true, Interpolator.EASE_BOTH),
              new KeyValue(Dialog.this.opacityProperty(), 0, Interpolator.EASE_BOTH)
          ),
          new KeyFrame(Duration.millis(1000),
              new KeyValue(contentHolder.translateXProperty(), 0, Interpolator.EASE_BOTH),
              new KeyValue(Dialog.this.opacityProperty(), 1, Interpolator.EASE_BOTH)))
      );
      // reduce the number to increase the shifting , increase number to reduce shifting
      setCycleDuration(Duration.seconds(0.4));
      setDelay(Duration.seconds(0));
    }
  }

  private class TopTransition extends CachedTransition {
    TopTransition() {
      super(contentHolder, new Timeline(
          new KeyFrame(Duration.ZERO,
              new KeyValue(contentHolder.translateYProperty(), -offsetY, Interpolator.EASE_BOTH),
              new KeyValue(Dialog.this.visibleProperty(), false, Interpolator.EASE_BOTH)
          ),
          new KeyFrame(Duration.millis(10),
              new KeyValue(Dialog.this.visibleProperty(), true, Interpolator.EASE_BOTH),
              new KeyValue(Dialog.this.opacityProperty(), 0, Interpolator.EASE_BOTH)
          ),
          new KeyFrame(Duration.millis(1000),
              new KeyValue(contentHolder.translateYProperty(), 0, Interpolator.EASE_BOTH),
              new KeyValue(Dialog.this.opacityProperty(), 1, Interpolator.EASE_BOTH)))
      );
      // reduce the number to increase the shifting , increase number to reduce shifting
      setCycleDuration(Duration.seconds(0.4));
      setDelay(Duration.seconds(0));
    }
  }

  private class BottomTransition extends CachedTransition {
    BottomTransition() {
      super(contentHolder, new Timeline(
          new KeyFrame(Duration.ZERO,
              new KeyValue(contentHolder.translateYProperty(), offsetY, Interpolator.EASE_BOTH),
              new KeyValue(Dialog.this.visibleProperty(), false, Interpolator.EASE_BOTH)
          ),
          new KeyFrame(Duration.millis(10),
              new KeyValue(Dialog.this.visibleProperty(), true, Interpolator.EASE_BOTH),
              new KeyValue(Dialog.this.opacityProperty(), 0, Interpolator.EASE_BOTH)
          ),
          new KeyFrame(Duration.millis(1000),
              new KeyValue(contentHolder.translateYProperty(), 0, Interpolator.EASE_BOTH),
              new KeyValue(Dialog.this.opacityProperty(), 1, Interpolator.EASE_BOTH)))
      );
      // reduce the number to increase the shifting , increase number to reduce shifting
      setCycleDuration(Duration.seconds(0.4));
      setDelay(Duration.seconds(0));
    }
  }

  private class CenterTransition extends CachedTransition {
    CenterTransition() {
      super(contentHolder, new Timeline(
          new KeyFrame(Duration.ZERO,
              new KeyValue(contentHolder.scaleXProperty(), 0, Interpolator.EASE_BOTH),
              new KeyValue(contentHolder.scaleYProperty(), 0, Interpolator.EASE_BOTH),
              new KeyValue(Dialog.this.visibleProperty(), false, Interpolator.EASE_BOTH)
          ),
          new KeyFrame(Duration.millis(10),
              new KeyValue(Dialog.this.visibleProperty(), true, Interpolator.EASE_BOTH),
              new KeyValue(Dialog.this.opacityProperty(), 0, Interpolator.EASE_BOTH)
          ),
          new KeyFrame(Duration.millis(1000),
              new KeyValue(contentHolder.scaleXProperty(), 1, Interpolator.EASE_BOTH),
              new KeyValue(contentHolder.scaleYProperty(), 1, Interpolator.EASE_BOTH),
              new KeyValue(Dialog.this.opacityProperty(), 1, Interpolator.EASE_BOTH)
          ))
      );
      setCycleDuration(Duration.seconds(0.4));
      setDelay(Duration.seconds(0));
    }
  }
}
