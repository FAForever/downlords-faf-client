package com.faforever.client.leaderboard;

import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.StringCell;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.util.Validator;
import com.google.common.annotations.VisibleForTesting;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

import static javafx.collections.FXCollections.observableList;


@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class LeaderboardsController extends AbstractViewController<Node> {

  private final LeaderboardService leaderboardService;
  private final NotificationService notificationService;
  private final I18n i18n;
  public Pane leaderboardRoot;
  public TableColumn<LeaderboardEntry, Number> rankColumn;
  public TableColumn<LeaderboardEntry, String> nameColumn;
  public TableColumn<LeaderboardEntry, Number> winLossColumn;
  public TableColumn<LeaderboardEntry, Number> gamesPlayedColumn;
  public TableColumn<LeaderboardEntry, Number> ratingColumn;
  public TableView<LeaderboardEntry> ratingTable;
  public ComboBox<Leaderboard> leaderboardComboBox;
  public TextField searchTextField;
  public Pane connectionProgressPane;
  public Pane contentPane;

  @VisibleForTesting
  protected AutoCompletionBinding<String> usernamesAutoCompletion;

  @Override
  public void initialize() {
    super.initialize();
    leaderboardService.getLeaderboards().thenApply(leaderboards -> {
      JavaFxUtil.runLater(() -> {
        leaderboardComboBox.getItems().clear();
        leaderboardComboBox.setConverter(leaderboardStringConverter());
        leaderboardComboBox.getItems().addAll(leaderboards);
        leaderboardComboBox.getSelectionModel().selectFirst();
      });
      return null;
    });

    rankColumn.setCellValueFactory(param -> new SimpleIntegerProperty(ratingTable.getItems().indexOf(param.getValue()) + 1));
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

  @NotNull
  private StringConverter<Leaderboard> leaderboardStringConverter() {
    return new StringConverter<>() {
      @Override
      public String toString(Leaderboard leaderboard) {
        return i18n.getWithDefault(leaderboard.getTechnicalName(), leaderboard.getNameKey());
      }

      @Override
      public Leaderboard fromString(String string) {
        return null;
      }
    };
  }

  public void onLeaderboardSelected() {
    contentPane.setVisible(false);
    searchTextField.clear();
    if (usernamesAutoCompletion != null) {
      usernamesAutoCompletion.dispose();
    }
    leaderboardService.getEntries(leaderboardComboBox.getValue()).thenAccept(leaderboardEntryBeans -> {
      ratingTable.setItems(observableList(leaderboardEntryBeans));
      usernamesAutoCompletion = TextFields.bindAutoCompletion(searchTextField,
          leaderboardEntryBeans.stream().map(LeaderboardEntry::getUsername).collect(Collectors.toList()));
      usernamesAutoCompletion.setDelay(0);
      contentPane.setVisible(true);
    }).exceptionally(throwable -> {
      contentPane.setVisible(false);
      log.warn("Error while loading leaderboard entries", throwable);
      notificationService.addImmediateErrorNotification(throwable, "leaderboard.failedToLoad");
      return null;
    });
  }

  public Node getRoot() {
    return leaderboardRoot;
  }
}
