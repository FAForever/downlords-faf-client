package com.faforever.client.fx;

import javafx.beans.binding.BooleanExpression;
import javafx.util.Subscription;

import java.util.ArrayList;
import java.util.List;

public abstract class Controller<ROOT> {

  protected BooleanExpression attached;
  protected BooleanExpression showing;

  private final List<Subscription> shownSubscriptions = new ArrayList<>();
  private final List<Subscription> attachedSubscriptions = new ArrayList<>();

  public abstract ROOT getRoot();

  protected abstract BooleanExpression createAttachedExpression();

  protected abstract BooleanExpression createVisibleExpression();

  /** Magic method called by JavaFX after FXML has been loaded. */
  public final void initialize() {
    attached = createAttachedExpression();
    showing = createVisibleExpression().and(attached);
    onInitialize();

    attached.subscribe(isAttached -> {
      if (isAttached) {
        onAttached();
      } else {
        attachedSubscriptions.forEach(Subscription::unsubscribe);
        attachedSubscriptions.clear();
        onDetached();
      }
    });
    showing.subscribe(isShowing -> {
      if (isShowing) {
        onShow();
      } else {
        shownSubscriptions.forEach(Subscription::unsubscribe);
        shownSubscriptions.clear();
        onHide();
      }
    });
  }

  protected void addShownSubscription(Subscription subscription) {
    shownSubscriptions.add(subscription);
  }

  protected void addAttachedSubscription(Subscription subscription) {
    attachedSubscriptions.add(subscription);
  }

  /**
   * Subclasses may override in order to perform actions when the controller is being initialized.
   */
  protected void onInitialize() {
    // To be overridden by subclass
  }

  /**
   * Subclasses may override in order to perform actions when the controller is no longer being displayed.
   */
  protected void onHide() {
    // To be overridden by subclass
  }

  /**
   * Subclasses may override in order to perform actions when the controller is being displayed.
   */
  protected void onShow() {
    // To be overridden by subclass
  }

  /**
   * Subclasses may override in order to perform actions when the controller is being removed from a menu.
   */
  protected void onDetached() {
    // To be overridden by subclass
  }

  /**
   * Subclasses may override in order to perform actions when the controller is being added to a menu.
   */
  protected void onAttached() {
    // To be overridden by subclass
  }
}
