package com.faforever.client.leaderboard;

import com.faforever.client.domain.LeagueEntryBean;
import com.faforever.client.domain.SubdivisionBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.StringCell;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Comparator;

import static javafx.collections.FXCollections.observableList;


@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class SubDivisionTabController implements Controller<Tab> {

  private final LeaderboardService leaderboardService;
  private final NotificationService notificationService;
  private final I18n i18n;
  public Tab subDivisionTab;
  public TableColumn<LeagueEntryBean, Number> rankColumn;
  public TableColumn<LeagueEntryBean, String> nameColumn;
  public TableColumn<LeagueEntryBean, Number> gamesPlayedColumn;
  public TableColumn<LeagueEntryBean, Number> scoreColumn;
  public TableView<LeagueEntryBean> ratingTable;

  @Override
  public Tab getRoot() {
    return subDivisionTab;
  }

  @Override
  public void initialize() {
    rankColumn.setCellValueFactory(param -> new SimpleIntegerProperty(ratingTable.getItems().indexOf(param.getValue()) + 1));
    rankColumn.setCellFactory(param -> new StringCell<>(rank -> i18n.number(rank.intValue())));

    nameColumn.setCellValueFactory(param -> param.getValue().usernameProperty());
    nameColumn.setCellFactory(param -> new StringCell<>(name -> name));

    gamesPlayedColumn.setCellValueFactory(param -> param.getValue().gamesPlayedProperty());
    gamesPlayedColumn.setCellFactory(param -> new StringCell<>(count -> i18n.number(count.intValue())));

    scoreColumn.setCellValueFactory(param -> param.getValue().scoreProperty());
    scoreColumn.setCellFactory(param -> new StringCell<>(rating -> i18n.number(rating.intValue())));
  }

  public void populate(SubdivisionBean subdivision) {
    subDivisionTab.setText(subdivision.getNameKey());

    leaderboardService.getEntries(subdivision).thenAccept(leagueEntryBeans -> {
      leagueEntryBeans.sort(Comparator.comparing(LeagueEntryBean::getScore).reversed());
      ratingTable.setItems(observableList(leagueEntryBeans));
    }).exceptionally(throwable -> {
      log.warn("Error while loading leaderboard entries for division " + subdivision, throwable);
      notificationService.addImmediateErrorNotification(throwable, "leaderboard.failedToLoad");
      return null;
    });
  }
}
