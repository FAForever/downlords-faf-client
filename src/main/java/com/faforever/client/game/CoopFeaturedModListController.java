package com.faforever.client.game;

import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.StringListCell;
import com.faforever.client.mod.ModService;
import javafx.collections.FXCollections;
import javafx.scene.control.ListView;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class CoopFeaturedModListController implements Controller<ListView<FeaturedModBean>> {

  private final ModService modService;

  public ListView<FeaturedModBean> root;

  @Override
  public void initialize() {
    root.setCellFactory(param -> new StringListCell<>(FeaturedModBean::getDisplayName));
    modService.getFeaturedMod(KnownFeaturedMod.COOP.getTechnicalName()).thenAccept(mod -> JavaFxUtil.runLater(() -> {
      root.setItems(FXCollections.observableArrayList(mod));
      root.getSelectionModel().selectFirst();
    }));
  }

  public FeaturedModBean getSelectedFeaturedMod() {
    return root.getSelectionModel().getSelectedItem();
  }

  @Override
  public ListView<FeaturedModBean> getRoot() {
    return root;
  }
}
