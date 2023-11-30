package com.faforever.client.navigation;

import com.faforever.client.domain.LeagueBean;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.NavigationItem;
import com.faforever.client.play.PlayController;
import com.faforever.client.play.PlayController.PlayContentEnum;
import com.faforever.client.vault.ReplayController;
import com.faforever.client.vault.ReplayController.ReplayContentEnum;
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
  private final ReadOnlyObjectWrapper<PlayController.PlayContentEnum> lastPlayTab = new ReadOnlyObjectWrapper<>();
  private final ReadOnlyObjectWrapper<ReplayController.ReplayContentEnum> lastReplayTab = new ReadOnlyObjectWrapper<>();
  private final ReadOnlyObjectWrapper<LeagueBean> lastLeagueTab = new ReadOnlyObjectWrapper<>();
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

  public PlayContentEnum getLastPlayTab() {
    return lastPlayTab.get();
  }

  public ReadOnlyObjectProperty<PlayContentEnum> lastPlayTabProperty() {
    return lastPlayTab.getReadOnlyProperty();
  }

  public void setLastPlayTab(PlayContentEnum lastPlayTab) {
    this.lastPlayTab.set(lastPlayTab);
  }

  public ReplayContentEnum getLastReplayTab() {
    return lastReplayTab.get();
  }

  public ReadOnlyObjectProperty<ReplayContentEnum> lastReplayTabProperty() {
    return lastReplayTab.getReadOnlyProperty();
  }

  public void setLastReplayTab(ReplayContentEnum lastReplayTab) {
    this.lastReplayTab.set(lastReplayTab);
  }

  public LeagueBean getLastLeagueTab() {
    return lastLeagueTab.get();
  }

  public ReadOnlyObjectProperty<LeagueBean> lastLeagueTabProperty() {
    return lastLeagueTab.getReadOnlyProperty();
  }

  public void setLastLeagueTab(LeagueBean lastLeagueTab) {
    this.lastLeagueTab.set(lastLeagueTab);
  }
}
