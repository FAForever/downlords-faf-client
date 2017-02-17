package com.faforever.client.replay;

import com.faforever.client.api.dto.FeaturedMod;
import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.theme.UiService;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.util.List;

public class OnlineReplayVaultController extends AbstractViewController<Node> {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int TOP_ELEMENT_COUNT = 10;
  private final ReplayService replayService;
  private final UiService uiService;
  public Pane replayVaultRoot;
  public FlowPane newestPane;
  public FlowPane highestRatedPane;
  public FlowPane mostWatchedPane;
  public VBox searchResultGroup;
  public FlowPane searchResultPane;
  public GridPane showroomGroup;
  public TextField mapTextField;
  public TextField playerTextField;
  public ComboBox<FeaturedMod> featuredModListView;
  public VBox loadingPane;
  public VBox contentPane;
  private ReplayDetailController replayDetailController;

  @Inject
  public OnlineReplayVaultController(ReplayService replayService, UiService uiService) {
    this.replayService = replayService;
    this.uiService = uiService;
  }

  public void initialize() {
    loadingPane.managedProperty().bind(loadingPane.visibleProperty());
    showroomGroup.managedProperty().bind(showroomGroup.visibleProperty());
    searchResultGroup.managedProperty().bind(searchResultGroup.visibleProperty());
    replayDetailController = uiService.loadFxml("theme/vault/replay/replay_detail.fxml");
    Node replayDetailRoot = replayDetailController.getRoot();
    replayVaultRoot.getChildren().add(replayDetailRoot);
    AnchorPane.setTopAnchor(replayDetailRoot, 0d);
    AnchorPane.setRightAnchor(replayDetailRoot, 0d);
    AnchorPane.setBottomAnchor(replayDetailRoot, 0d);
    AnchorPane.setLeftAnchor(replayDetailRoot, 0d);
    replayDetailRoot.setVisible(false);
  }

  public void onSearchByMap() {
    replayService.searchByMap(mapTextField.getText()).thenAccept(this::displaySearchResult);
  }

  public void onSearchByPlayer() {
    replayService.searchByPlayer(playerTextField.getText()).thenAccept(this::displaySearchResult);
  }

  public void onSearchByMod() {
    replayService.searchByMod(featuredModListView.getSelectionModel().getSelectedItem()).thenAccept(this::displaySearchResult);
  }

  private void displaySearchResult(List<Replay> replays) {
    showroomGroup.setVisible(false);
    searchResultGroup.setVisible(true);

    populateReplays(replays, searchResultPane);
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
      });
    });
  }

  public void onShowReplayDetail(Replay replay) {
    replayDetailController.setReplay(replay);
    replayDetailController.getRoot().setVisible(true);
    replayDetailController.getRoot().requestFocus();
  }

  @Override
  protected void onDisplay() {
    super.onDisplay();
    replayService.getNewestReplays(TOP_ELEMENT_COUNT).thenAccept(replays -> populateReplays(replays, newestPane))
        .thenCompose(aVoid -> replayService.getHighestRatedReplays(TOP_ELEMENT_COUNT).thenAccept(modInfoBeans -> populateReplays(modInfoBeans, highestRatedPane)))
        .thenCompose(aVoid -> replayService.getMostWatchedReplays(TOP_ELEMENT_COUNT).thenAccept(modInfoBeans -> populateReplays(modInfoBeans, mostWatchedPane)))
        .thenRun(this::enterShowroomState)
        .exceptionally(throwable -> {
          logger.warn("Could not populate replays", throwable);
          return null;
        });
  }

  @Override
  public Node getRoot() {
    return replayVaultRoot;
  }

  private void enterShowroomState() {
    showroomGroup.setVisible(true);
    searchResultGroup.setVisible(false);
    loadingPane.setVisible(false);
  }
}
