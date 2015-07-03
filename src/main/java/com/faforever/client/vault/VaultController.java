package com.faforever.client.vault;

import com.faforever.client.game.MapInfoBean;
import com.faforever.client.game.MapSize;
import com.faforever.client.map.MapService;
import com.faforever.client.replay.ReplayVaultController;
import com.faforever.client.util.Callback;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import java.util.List;

public class VaultController {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @FXML
  TabPane vaultRoot;

  @FXML
  Pane replayVaultContainer;

  @FXML
  TableView<MapInfoBean> mapTableView;

  @FXML
  TableColumn<MapInfoBean, String> nameColumn;

  @FXML
  TableColumn<MapInfoBean, String> descriptionColumn;

  @FXML
  TableColumn<MapInfoBean, Number> playsColumn;

  @FXML
  TableColumn<MapInfoBean, MapSize> sizeColumn;

  @FXML
  TableColumn<MapInfoBean, String> creatorColumn;

  @FXML
  TableColumn<MapInfoBean, Number> ratingColumn;

  @FXML
  TableColumn<MapInfoBean, Number> downloadsColumn;

  @FXML
  TableColumn<MapInfoBean, Number> playersColumn;

  @FXML
  TableColumn<MapInfoBean, Number> versionColumn;

  @Autowired
  ReplayVaultController replayVaultController;

  @Autowired
  MapService mapService;

  public Node getRoot() {
    return vaultRoot;
  }

  @FXML
  void initialize() {
    nameColumn.setCellValueFactory(param -> param.getValue().nameProperty());
    descriptionColumn.setCellValueFactory(param -> param.getValue().descriptionProperty());
    playsColumn.setCellValueFactory(param -> param.getValue().playsProperty());
    // creatorColumn.setCellValueFactory(param -> param.getValue().creatorProperty());
    ratingColumn.setCellValueFactory(param -> param.getValue().ratingProperty());
    downloadsColumn.setCellValueFactory(param -> param.getValue().downloadsProperty());
    sizeColumn.setCellValueFactory(param -> param.getValue().sizeProperty());
    playersColumn.setCellValueFactory(param -> param.getValue().playersProperty());
    versionColumn.setCellValueFactory(param -> param.getValue().versionProperty());
  }

  @PostConstruct
  void postConstruct() {
    replayVaultContainer.getChildren().setAll(replayVaultController.getRoot());
    placeInContainer(replayVaultController.getRoot(), replayVaultContainer);

    replayVaultController.loadLocalReplaysInBackground();
    replayVaultController.loadOnlineReplaysInBackground();
  }

  public void setUpIfNecessary() {
    // FIXME test code so far
    mapService.readMapVaultInBackground(0, 100, new Callback<List<MapInfoBean>>() {
      @Override
      public void success(List<MapInfoBean> result) {
        mapTableView.setItems(FXCollections.observableList(result));
      }

      @Override
      public void error(Throwable e) {
        logger.warn("Failed", e);
      }
    });
  }

  private static void placeInContainer(Node node, Node container) {
    AnchorPane.setTopAnchor(node, 0d);
    AnchorPane.setRightAnchor(node, 0d);
    AnchorPane.setBottomAnchor(node, 0d);
    AnchorPane.setLeftAnchor(node, 0d);
  }
}
