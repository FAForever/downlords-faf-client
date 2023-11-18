package com.faforever.client.navigation;

import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.NavigationItem;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class NavigationHandler {

  private final ReadOnlyObjectWrapper<NavigateEvent> navigationEvent = new ReadOnlyObjectWrapper<>();
  private final ObservableSet<NavigationItem> highlightedItems = FXCollections.observableSet();

  public void navigateTo(NavigateEvent navigationEvent) {
    this.navigationEvent.set(navigationEvent);
  }

  public void addHighlight(NavigationItem navigationItem) {
    this.highlightedItems.add(navigationItem);
  }

  public void removeHighlight(NavigationItem navigationItem) {
    this.highlightedItems.remove(navigationItem);
  }

  public ReadOnlyObjectProperty<NavigateEvent> navigationEventProperty() {
    return navigationEvent.getReadOnlyProperty();
  }

  public ObservableSet<NavigationItem> getHighlightedItems() {
    return FXCollections.unmodifiableObservableSet(highlightedItems);
  }

}
