package com.faforever.client.leaderboard;

import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.StringCell;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.util.Assert;
import com.faforever.client.util.Validator;
import javafx.beans.property.SimpleFloatProperty;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;

import static javafx.collections.FXCollections.observableList;


@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class LeaderboardController extends AbstractViewController<Node> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final LeaderboardService leaderboardService;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final ReportingService reportingService;
  public Pane leaderboardRoot;
  public TableColumn<LeaderboardEntry, Number> rankColumn;
  public TableColumn<LeaderboardEntry, String> nameColumn;
  public TableColumn<LeaderboardEntry, Number> winLossColumn;
  public TableColumn<LeaderboardEntry, Number> gamesPlayedColumn;
  public TableColumn<LeaderboardEntry, Number> ratingColumn;
  public TableView<LeaderboardEntry> ratingTable;
  public TextField searchTextField;
  public Pane connectionProgressPane;
  public Pane contentPane;
  private KnownFeaturedMod ratingType;

  @Override
  public void initialize() {
    super.initialize();
    rankColumn.setCellValueFactory(param -> param.getValue().rankProperty());
    rankColumn.setCellFactory(param -> new StringCell<>(rank -> i18n.number(rank.intValue())));

    nameColumn.setCellValueFactory(param -> param.getValue().usernameProperty());
    nameColumn.setCellFactory(param -> new StringCell<>(name -> name));

    winLossColumn.setCellValueFactory(param -> new SimpleFloatProperty(param.getValue().getWinLossRatio()));
    winLossColumn.setCellFactory(param -> new StringCell<>(number -> i18n.get("percentage", number.floatValue() * 100)));

    gamesPlayedColumn.setCellValueFactory(param -> param.getValue().gamesPlayedProperty());
    gamesPlayedColumn.setCellFactory(param -> new StringCell<>(count -> i18n.number(count.intValue())));

    ratingColumn.setCellValueFactory(param -> param.getValue().ratingProperty());
    ratingColumn.setCellFactory(param -> new StringCell<>(rating -> i18n.number(rating.intValue())));

    contentPane.managedProperty().bind(contentPane.visibleProperty());
    connectionProgressPane.managedProperty().bind(connectionProgressPane.visibleProperty());
    connectionProgressPane.visibleProperty().bind(contentPane.visibleProperty().not());

    searchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      if (Validator.isInt(newValue)) {
        ratingTable.scrollTo(Integer.parseInt(newValue) - 1);
      } else {
        LeaderboardEntry foundPlayer = null;
        for (LeaderboardEntry leaderboardEntry : ratingTable.getItems()) {
          if (leaderboardEntry.getUsername().toLowerCase().startsWith(newValue.toLowerCase())) {
            foundPlayer = leaderboardEntry;
            break;
          }
        }
        if (foundPlayer == null) {
          for (LeaderboardEntry leaderboardEntry : ratingTable.getItems()) {
            if (leaderboardEntry.getUsername().toLowerCase().contains(newValue.toLowerCase())) {
              foundPlayer = leaderboardEntry;
              break;
            }
          }
        }
        if (foundPlayer != null) {
          ratingTable.scrollTo(foundPlayer);
          ratingTable.getSelectionModel().select(foundPlayer);
        } else {
          ratingTable.getSelectionModel().select(null);
        }
      }
    });
  }

  @Override
  protected void onDisplay(NavigateEvent navigateEvent) {
    Assert.checkNullIllegalState(ratingType, "ratingType must not be null");

    contentPane.setVisible(false);
    leaderboardService.getEntries(ratingType).thenAccept(leaderboardEntryBeans -> {
      ratingTable.setItems(observableList(leaderboardEntryBeans));
      contentPane.setVisible(true);
    }).exceptionally(throwable -> {
      contentPane.setVisible(false);
      logger.warn("Error while loading leaderboard entries", throwable);
      notificationService.addImmediateErrorNotification(throwable, "leaderboard.failedToLoad");
      return null;
    });
  }

  public Node getRoot() {
    return leaderboardRoot;
  }

  public void setRatingType(KnownFeaturedMod ratingType) {
    this.ratingType = ratingType;
  }
}
