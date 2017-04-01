package com.faforever.client.replay;

import com.faforever.client.api.dto.Game;
import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.DismissAction;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.query.LogicalNodeController;
import com.faforever.client.query.SearchableProperties;
import com.faforever.client.query.SpecificationController;
import com.faforever.client.theme.UiService;
import com.github.rutledgepaulv.qbuilders.builders.QBuilder;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.qbuilders.visitors.RSQLVisitor;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class OnlineReplayVaultController extends AbstractViewController<Node> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int TOP_ELEMENT_COUNT = 10;
  private static final int TOP_MORE_ELEMENT_COUNT = 100;
  private static final int MAX_SEARCH_RESULTS = 100;
  private final ReplayService replayService;
  private final UiService uiService;
  private final NotificationService notificationService;
  private final I18n i18n;
  public Pane replayVaultRoot;
  public Pane newestPane;
  public Pane highestRatedPane;
  public Pane mostWatchedPane;
  public VBox searchResultGroup;
  public Pane searchResultPane;
  public Pane showroomGroup;
  public VBox loadingPane;
  public VBox contentPane;
  public TextField queryTextField;
  public Pane criteriaPane;
  public CheckBox displayQueryCheckBox;
  /**
   * The first query element.
   */
  public LogicalNodeController initialLogicalNodeController;
  public Button backButton;
  public Button searchButton;
  public ScrollPane scrollPane;
  private ReplayDetailController replayDetailController;
  private List<LogicalNodeController> queryNodes;
  private InvalidationListener queryInvalidationListener;

  @Inject
  public OnlineReplayVaultController(ReplayService replayService, UiService uiService, NotificationService notificationService, I18n i18n) {
    this.replayService = replayService;
    this.uiService = uiService;
    this.notificationService = notificationService;
    this.i18n = i18n;
    queryNodes = new ArrayList<>();
  }

  public void initialize() {
    super.initialize();
    JavaFxUtil.fixScrollSpeed(scrollPane);
    loadingPane.managedProperty().bind(loadingPane.visibleProperty());
    showroomGroup.managedProperty().bind(showroomGroup.visibleProperty());
    searchResultGroup.managedProperty().bind(searchResultGroup.visibleProperty());
    queryTextField.managedProperty().bind(queryTextField.visibleProperty());
    queryTextField.visibleProperty().bind(displayQueryCheckBox.selectedProperty());
    backButton.managedProperty().bind(backButton.visibleProperty());
    initialLogicalNodeController.logicalOperatorField.managedProperty()
        .bind(initialLogicalNodeController.logicalOperatorField.visibleProperty());
    initialLogicalNodeController.removeCriteriaButton.managedProperty()
        .bind(initialLogicalNodeController.removeCriteriaButton.visibleProperty());

    initialLogicalNodeController.logicalOperatorField.setValue(null);
    initialLogicalNodeController.logicalOperatorField.setDisable(true);
    initialLogicalNodeController.logicalOperatorField.setVisible(false);
    initialLogicalNodeController.removeCriteriaButton.setVisible(false);
    initialLogicalNodeController.specificationController.setRootType(Game.class);
    initialLogicalNodeController.specificationController.setProperties(SearchableProperties.GAME_PROPERTIES.keySet());
    queryInvalidationListener = observable -> queryTextField.setText(buildQuery(initialLogicalNodeController.specificationController, queryNodes));
    addInvalidationListener(initialLogicalNodeController);

    searchButton.disableProperty().bind(queryTextField.textProperty().isEmpty());
  }

  private void addInvalidationListener(LogicalNodeController logicalNodeController) {
    logicalNodeController.specificationController.propertyField.valueProperty().addListener(queryInvalidationListener);
    logicalNodeController.specificationController.operationField.valueProperty().addListener(queryInvalidationListener);
    logicalNodeController.specificationController.valueField.valueProperty().addListener(queryInvalidationListener);
    logicalNodeController.specificationController.valueField.getEditor().textProperty()
        .addListener(observable -> {
          if (!logicalNodeController.specificationController.valueField.valueProperty().isBound()) {
            logicalNodeController.specificationController.valueField.setValue(logicalNodeController.specificationController.valueField.getEditor().getText());
          }
        });
    logicalNodeController.specificationController.valueField.setOnKeyReleased(event -> {
      if (event.getCode() == KeyCode.ENTER) {
        searchButton.fire();
      }
    });
  }

  private void displaySearchResult(List<Replay> replays) {
    populateReplays(replays, searchResultPane);

    showroomGroup.setVisible(false);
    searchResultGroup.setVisible(true);
    loadingPane.setVisible(false);
    backButton.setVisible(true);
  }

  private void populateReplays(List<Replay> replays, Pane pane) {
    ObservableList<Node> children = pane.getChildren();
    Platform.runLater(() -> {
      children.clear();

      replays.forEach(replay -> {
        ReplayCardController controller = uiService.loadFxml("theme/vault/replay/replay_card.fxml");
        controller.setReplay(replay);
        controller.setOnOpenDetailListener(this::onShowReplayDetail);
        children.add(controller.getRoot());

        if (replays.size() == 1) {
          onShowReplayDetail(replay);
        }
      });
    });
  }

  public void onShowReplayDetail(Replay replay) {
    replayDetailController = uiService.loadFxml("theme/vault/replay/replay_detail.fxml");
    replayDetailController.setReplay(replay);

    Node replayDetailRoot = replayDetailController.getRoot();
    replayDetailRoot.setVisible(true);
    replayDetailRoot.requestFocus();

    replayVaultRoot.getChildren().add(replayDetailRoot);
    AnchorPane.setTopAnchor(replayDetailRoot, 0d);
    AnchorPane.setRightAnchor(replayDetailRoot, 0d);
    AnchorPane.setBottomAnchor(replayDetailRoot, 0d);
    AnchorPane.setLeftAnchor(replayDetailRoot, 0d);
  }

  @Override
  protected void onDisplay() {
    super.onDisplay();
    refresh();
  }

  @Override
  public Node getRoot() {
    return replayVaultRoot;
  }

  private void enterSearchingState() {
    showroomGroup.setVisible(false);
    searchResultGroup.setVisible(false);
    loadingPane.setVisible(true);
    backButton.setVisible(false);
  }

  private void enterShowroomState() {
    showroomGroup.setVisible(true);
    searchResultGroup.setVisible(false);
    loadingPane.setVisible(false);
    backButton.setVisible(false);
  }

  public void onSearchButtonClicked() {
    enterSearchingState();
    replayService.findByQuery(queryTextField.getText(), MAX_SEARCH_RESULTS)
        .thenAccept(this::displaySearchResult)
        .exceptionally(e -> {
          notificationService.addNotification(new ImmediateNotification(i18n.get("errorTitle"),
              i18n.get("vault.replays.searchError"), Severity.ERROR, e,
              Collections.singletonList(new DismissAction(i18n))));
          return null;
        });
  }

  /**
   * Builds the query string if possible, returns empty string if not. A query string can not be built if the user
   * selected no or invalid values.
   */
  private String buildQuery(SpecificationController initialSpecification, List<LogicalNodeController> queryNodes) {
    QBuilder qBuilder = new QBuilder();

    Optional<Condition> condition = initialSpecification.appendTo(qBuilder);
    if (!condition.isPresent()) {
      return "";
    }
    for (LogicalNodeController queryNode : queryNodes) {
      Optional<Condition> currentCondition = queryNode.appendTo(condition.get());
      if (!currentCondition.isPresent()) {
        break;
      }
      condition = currentCondition;
    }
    return (String) condition.get().query(new RSQLVisitor());
  }

  public void onAddCriteriaButtonClicked() {
    LogicalNodeController controller = uiService.loadFxml("theme/query/logical_node.fxml");
    controller.logicalOperatorField.valueProperty().addListener(queryInvalidationListener);
    controller.specificationController.setRootType(Game.class);
    controller.specificationController.setProperties(SearchableProperties.GAME_PROPERTIES.keySet());
    controller.setRemoveCriteriaButtonListener(() -> {
      criteriaPane.getChildren().remove(controller.getRoot());
      queryNodes.remove(controller);
      if (queryNodes.isEmpty()) {
        initialLogicalNodeController.logicalOperatorField.setVisible(false);
      }
    });
    addInvalidationListener(controller);

    criteriaPane.getChildren().add(controller.getRoot());
    queryNodes.add(controller);
    initialLogicalNodeController.logicalOperatorField.setVisible(true);
  }

  public void onBackButtonClicked() {
    enterShowroomState();
  }

  public void onResetButtonClicked() {
    new ArrayList<>(queryNodes).forEach(logicalNodeController -> logicalNodeController.removeCriteriaButton.fire());
    initialLogicalNodeController.specificationController.propertyField.getSelectionModel().select(0);
    initialLogicalNodeController.specificationController.operationField.getSelectionModel().select(0);
    initialLogicalNodeController.specificationController.valueField.setValue(null);
  }

  public void onRefreshButtonClicked() {
    refresh();
  }

  private void refresh() {
    enterSearchingState();
    replayService.getNewestReplays(TOP_ELEMENT_COUNT)
        .thenAccept(replays -> populateReplays(replays, newestPane))
        .thenCompose(aVoid -> replayService.getHighestRatedReplays(TOP_ELEMENT_COUNT).thenAccept(modInfoBeans -> populateReplays(modInfoBeans, highestRatedPane)))
        .thenCompose(aVoid -> replayService.getMostWatchedReplays(TOP_ELEMENT_COUNT).thenAccept(modInfoBeans -> populateReplays(modInfoBeans, mostWatchedPane)))
        .thenRun(this::enterShowroomState)
        .exceptionally(throwable -> {
          logger.warn("Could not populate replays", throwable);
          return null;
        });
  }

  public void onMoreNewestButtonClicked() {
    enterSearchingState();
    replayService.getNewestReplays(TOP_MORE_ELEMENT_COUNT).thenAccept(this::displaySearchResult);
  }

  public void onMoreHighestRatedButtonClicked() {
    enterSearchingState();
    replayService.getHighestRatedReplays(TOP_MORE_ELEMENT_COUNT).thenAccept(this::displaySearchResult);
  }

  public void onMoreMostWatchedButtonClicked() {
    enterSearchingState();
    replayService.getMostWatchedReplays(TOP_MORE_ELEMENT_COUNT).thenAccept(this::displaySearchResult);
  }
}
