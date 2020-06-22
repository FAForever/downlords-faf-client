package com.faforever.client.ui.alert;

import com.faforever.client.ui.alert.animation.AlertAnimation;
import com.faforever.client.ui.effects.DepthManager;
import com.sun.javafx.event.EventHandlerManager;
import javafx.animation.Animation;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogEvent;
import javafx.scene.control.DialogPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.List;

/** Ported from JFoenix since we wanted to get rid of the JFoenix dependency */
public class Alert<R> extends Dialog<R> {

  private final StackPane contentContainer;
  private final EventHandlerManager eventHandlerManager = new EventHandlerManager(this);

  /**
   * Indicates whether the dialog will close when clicking on the overlay or not.
   */
  private final BooleanProperty overlayClose = new SimpleBooleanProperty(true);

  /**
   * Specify the animation when showing / hiding the dialog by default it's set to {@link
   * AlertAnimation#CENTER_ANIMATION}.
   */
  private final ObjectProperty<AlertAnimation> animation = new SimpleObjectProperty<>
      (AlertAnimation.CENTER_ANIMATION);
  private final BooleanProperty hideOnEscape = new SimpleBooleanProperty(this, "hideOnEscape", true);

  private InvalidationListener widthListener;
  private InvalidationListener heightListener;
  private InvalidationListener xListener;
  private InvalidationListener yListener;
  private boolean animateClosing = true;
  private Animation transition = null;

  public Alert() {
    this(null);
  }

  public Alert(Window window) {
    contentContainer = new StackPane();
    contentContainer.getStyleClass().add("alert-content-container");
    // add depth effect
    final Node materialNode = DepthManager.createMaterialNode(contentContainer, 2);
    materialNode.setPickOnBounds(false);
    materialNode.addEventHandler(MouseEvent.MOUSE_CLICKED, Event::consume);

    // create custom dialog pane (will layout children in center)
    final DialogPane dialogPane = new DialogPane() {
      private boolean performingLayout = false;

      {
        getButtonTypes().add(ButtonType.CLOSE);
        Node closeButton = this.lookupButton(ButtonType.CLOSE);
        closeButton.managedProperty().bind(closeButton.visibleProperty());
        closeButton.setVisible(false);
      }

      @Override
      protected double computePrefHeight(double width) {
        Window owner = getOwner();
        if (owner != null) {
          return owner.getHeight();
        }
        return super.computePrefHeight(width);
      }

      @Override
      protected double computePrefWidth(double height) {
        Window owner = getOwner();
        if (owner != null) {
          return owner.getWidth();
        }
        return super.computePrefWidth(height);
      }

      @Override
      public void requestLayout() {
        if (performingLayout) {
          return;
        }
        super.requestLayout();
      }

      @Override
      protected void layoutChildren() {
        performingLayout = true;
        List<Node> managed = getManagedChildren();
        final double width = getWidth();
        double height = getHeight();
        double top = getInsets().getTop();
        double right = getInsets().getRight();
        double left = getInsets().getLeft();
        double bottom = getInsets().getBottom();
        double contentWidth = width - left - right;
        double contentHeight = height - top - bottom;
        for (Node child : managed) {
          layoutInArea(child, left, top, contentWidth, contentHeight,
              0, Insets.EMPTY, HPos.CENTER, VPos.CENTER);
        }
        performingLayout = false;
      }

      public String getUserAgentStylesheet() {
        return getClass().getResource("/css/controls/alert.css").toExternalForm();
      }

      @Override
      protected Node createButtonBar() {
        return null;
      }
    };
    dialogPane.getStyleClass().add("dfc-alert-overlay");
    dialogPane.setContent(materialNode);
    setDialogPane(dialogPane);
    dialogPane.getScene().setFill(Color.TRANSPARENT);

    if (window != null) {
      // set the window to transparent
      initStyle(StageStyle.TRANSPARENT);
      initOwner(window);

      // init style for overlay
      dialogPane.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
        if (this.isOverlayClose()) {
          hide();
        }
      });
      // bind dialog position to window position
      widthListener = observable -> updateWidth();
      heightListener = observable -> updateHeight();
      xListener = observable -> updateX();
      yListener = observable -> updateY();
    }

    // handle animation / owner window layout changes
    eventHandlerManager.addEventHandler(DialogEvent.DIALOG_SHOWING, event -> {
      addLayoutListeners();
      AlertAnimation currentAnimation = getCurrentAnimation();
      currentAnimation.initAnimation(contentContainer.getParent(), dialogPane);
    });
    eventHandlerManager.addEventHandler(DialogEvent.DIALOG_SHOWN, event -> {
      if (getOwner() != null) {
        updateLayout();
      }
      animateClosing = true;
      AlertAnimation currentAnimation = getCurrentAnimation();
      Animation animation = currentAnimation.createShowingAnimation(dialogPane.getContent(), dialogPane);
      if (animation != null) {
        animation.play();
      }
    });

    eventHandlerManager.addEventHandler(DialogEvent.DIALOG_CLOSE_REQUEST, event -> {
      if (animateClosing) {
        event.consume();
        hideWithAnimation();
      }
    });
    eventHandlerManager.addEventHandler(DialogEvent.DIALOG_HIDDEN, event -> removeLayoutListeners());

    getDialogPane().getScene().getWindow().addEventFilter(KeyEvent.KEY_PRESSED, keyEvent -> {
      if (keyEvent.getCode() == KeyCode.ESCAPE) {
        if (!isHideOnEscape()) {
          keyEvent.consume();
        }
      }
    });
  }

  // this method ensure not null value for current animation
  private AlertAnimation getCurrentAnimation() {
    AlertAnimation usedAnimation = getAnimation();
    usedAnimation = usedAnimation == null ? AlertAnimation.NO_ANIMATION : usedAnimation;
    return usedAnimation;
  }

  private void removeLayoutListeners() {
    Window stage = getOwner();
    if (stage != null) {
      stage.getScene().widthProperty().removeListener(widthListener);
      stage.getScene().heightProperty().removeListener(heightListener);
      stage.xProperty().removeListener(xListener);
      stage.yProperty().removeListener(yListener);
    }
  }

  private void addLayoutListeners() {
    Window stage = getOwner();
    if (stage != null) {
      if (widthListener == null) {
        throw new RuntimeException("Owner can only be set using the constructor");
      }
      stage.getScene().widthProperty().addListener(widthListener);
      stage.getScene().heightProperty().addListener(heightListener);
      stage.xProperty().addListener(xListener);
      stage.yProperty().addListener(yListener);
    }
  }

  private void updateLayout() {
    updateX();
    updateY();
    updateWidth();
    updateHeight();
  }

  private void updateHeight() {
    Window stage = getOwner();
    setHeight(stage.getScene().getHeight());
  }

  private void updateWidth() {
    Window stage = getOwner();
    setWidth(stage.getScene().getWidth());
  }

  private void updateY() {
    Window stage = getOwner();
    setY(stage.getY() + stage.getScene().getY());
  }

  private void updateX() {
    Window stage = getOwner();
    setX(stage.getX() + stage.getScene().getX());
  }

  /**
   * play the hide animation for the dialog, as the java hide method is set to final so it can not be overridden
   */
  public void hideWithAnimation() {
    if (transition == null || transition.getStatus().equals(Animation.Status.STOPPED)) {
      AlertAnimation currentAnimation = getCurrentAnimation();
      Animation animation = currentAnimation.createHidingAnimation(getDialogPane().getContent(), getDialogPane());
      if (animation != null) {
        transition = animation;
        animation.setOnFinished(finish -> {
          animateClosing = false;
          hide();
          transition = null;
        });
        animation.play();
      } else {
        animateClosing = false;
        transition = null;
        Platform.runLater(this::hide);
      }
    }
  }

  @Override
  public EventDispatchChain buildEventDispatchChain(EventDispatchChain tail) {
    return super.buildEventDispatchChain(tail).prepend(eventHandlerManager);
  }

  public void setContent(Node... content) {
    contentContainer.getChildren().setAll(content);
  }

  public boolean isOverlayClose() {
    return overlayClose.get();
  }

  public void setOverlayClose(boolean overlayClose) {
    this.overlayClose.set(overlayClose);
  }

  public BooleanProperty overlayCloseProperty() {
    return overlayClose;
  }

  public AlertAnimation getAnimation() {
    return animation.get();
  }

  public void setAnimation(AlertAnimation animation) {
    this.animation.set(animation);
  }

  public ObjectProperty<AlertAnimation> animationProperty() {
    return animation;
  }

  public void setSize(double prefWidth, double prefHeight) {
    contentContainer.setPrefSize(prefWidth, prefHeight);
  }

  public final boolean isHideOnEscape() {
    return hideOnEscape.get();
  }

  public final void setHideOnEscape(boolean value) {
    hideOnEscape.set(value);
  }

  public final BooleanProperty hideOnEscapeProperty() {
    return hideOnEscape;
  }

}
