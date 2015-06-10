package com.faforever.client.vault;

import com.faforever.client.game.MapInfoBean;
import com.faforever.client.map.MapService;
import com.faforever.client.util.Callback;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TableView;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.invoke.MethodHandles;
import java.util.List;

public class VaultController {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @FXML
  TableView<MapInfoBean> mapTableView;

  @Autowired
  MapService mapService;

  private Node root;

  public Node getRoot() {
    return new Pane();
  }

  public void setUpIfNecessary() {
    // FIXME test code so far
    mapService.getMapsFromVaultInBackground(0, 100, new Callback<List<MapInfoBean>>() {
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
}
