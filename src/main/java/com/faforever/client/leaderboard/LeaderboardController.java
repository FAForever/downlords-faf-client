package com.faforever.client.leaderboard;

import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.StringCell;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.notification.ImmediateErrorNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.remote.FafService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.util.Assert;
import com.faforever.client.util.Validator;
import com.jfoenix.controls.JFXButton;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Pagination;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;

import static java.util.stream.Collectors.toList;
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
  public TableColumn<RatingWithRank, Number> rankColumn;
  public TableColumn<RatingWithRank, String> nameColumn;
  public TableColumn<RatingWithRank, Number> meanColumn;
  public TableColumn<RatingWithRank, Number> deviationColumn;
  public TableColumn<RatingWithRank, Number> ratingColumn;
  public TableView<RatingWithRank> ratingTable;
  public TextField searchTextField;
  public Pane connectionProgressPane;
  public Pane contentPane;
  public JFXButton searchButton;
  public Pagination paginationControl;
  private KnownFeaturedMod ratingType;
  private final static int NUMBER_OF_PLAYERS_PER_PAGE = 15;
  private boolean initialized;


  @Override
  public void initialize() {
    super.initialize();
    rankColumn.setCellValueFactory(param -> param.getValue().rankProperty());
    rankColumn.setCellFactory(param -> new StringCell<>(rank -> i18n.number(rank.intValue())));

    nameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getPlayer().getLogin())) ;
    nameColumn.setCellFactory(param -> new StringCell<>(name -> name));

    meanColumn.setCellValueFactory(param -> param.getValue().meanProperty());
    meanColumn.setCellFactory(param -> new StringCell<>(number -> i18n.rounded(number.doubleValue(), 2)));

    deviationColumn.setCellValueFactory(param -> param.getValue().deviationProperty());
    deviationColumn.setCellFactory(param -> new StringCell<>(number -> i18n.rounded(number.doubleValue(), 2)));

    ratingColumn.setCellValueFactory(param -> param.getValue().ratingProperty());
    ratingColumn.setCellFactory(param -> new StringCell<>(rating -> i18n.number(rating.intValue())));

    contentPane.managedProperty().bind(contentPane.visibleProperty());
    connectionProgressPane.managedProperty().bind(connectionProgressPane.visibleProperty());
    connectionProgressPane.visibleProperty().bind(contentPane.visibleProperty().not());


  }


  @Override
  protected void onDisplay(NavigateEvent navigateEvent) {
    if (initialized) {
      return;
    }
    initialized = true;
    paginationControl.currentPageIndexProperty().setValue(0);//initialize table
    updateTable();

      paginationControl.currentPageIndexProperty().addListener(new ChangeListener<Number>() {
        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
          updateTable();
        }
      });
  }

  public Node getRoot() {
    return leaderboardRoot;
  }

  public void setRatingType(KnownFeaturedMod ratingType) {
    this.ratingType = ratingType;
  }

  public void handleSearchButtonClicked(ActionEvent event) {
    paginationControl.currentPageIndexProperty().setValue(0);
    updateTable();
  }

  private void updateTable() {
    String searchTextFieldText = searchTextField.getText();
    Assert.checkNullIllegalState(ratingType, "ratingType must not be null");

    contentPane.setVisible(false);
    leaderboardService.getSearchResultsWithMeta(ratingType, searchTextFieldText,paginationControl.getCurrentPageIndex()+1, NUMBER_OF_PLAYERS_PER_PAGE)
        .thenAccept(ratingWithRankBeans -> {
      Platform.runLater(() -> {
        ratingTable.setItems(observableList(
            ratingWithRankBeans.get()
                .parallelStream()
                .map(RatingWithRank::fromDTORatingWithRank)
                .collect(toList()))
        );
        contentPane.setVisible(true);
      });
    }).exceptionally(throwable -> {
      Platform.runLater(() -> {
        contentPane.setVisible(false);
        logger.warn("Error while loading leaderboard entries", throwable);
        notificationService.addNotification(new ImmediateErrorNotification(
            i18n.get("errorTitle"), i18n.get("leaderboard.failedToLoad"),
            throwable, i18n, reportingService
        ));
      });
      return null;

    });
  }
}
