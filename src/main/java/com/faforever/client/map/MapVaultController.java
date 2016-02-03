package com.faforever.client.map;

import com.faforever.client.game.MapInfoBean;
import com.faforever.client.game.MapSize;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;

public class MapVaultController {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @FXML
  Pane mapVaultRoot;

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

  @Resource
  MapService mapService;

  public Node getRoot() {
    return mapVaultRoot;
  }

  @FXML
  void initialize() {
    nameColumn.setCellValueFactory(param -> param.getValue().displayNameProperty());
    descriptionColumn.setCellValueFactory(param -> param.getValue().descriptionProperty());
    playsColumn.setCellValueFactory(param -> param.getValue().playsProperty());
    // creatorColumn.setCellValueFactory(param -> param.getValue().creatorProperty());
    ratingColumn.setCellValueFactory(param -> param.getValue().ratingProperty());
    downloadsColumn.setCellValueFactory(param -> param.getValue().downloadsProperty());
    sizeColumn.setCellValueFactory(param -> param.getValue().sizeProperty());
    playersColumn.setCellValueFactory(param -> param.getValue().playersProperty());
    versionColumn.setCellValueFactory(param -> param.getValue().versionProperty());
  }

  public void setUpIfNecessary() {
    // FIXME implement
  }
}
