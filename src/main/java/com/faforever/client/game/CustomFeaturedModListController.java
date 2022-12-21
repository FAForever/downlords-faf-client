package com.faforever.client.game;

import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.DualStringListCell;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.mod.ModService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class CustomFeaturedModListController implements Controller<ListView<FeaturedModBean>> {

  private static final String STYLE_CLASS_DUAL_LIST_CELL = "create-game-dual-list-cell";

  private final ModService modService;
  private final I18n i18n;
  private final UiService uiService;
  private final PreferencesService preferencesService;

  public ListView<FeaturedModBean> root;

  @Override
  public void initialize() {
    root.setCellFactory(featuredModCellFactory());
    modService.getFeaturedMods().thenAccept(mods -> JavaFxUtil.runLater(() -> {
      root.setItems(FXCollections.observableList(mods));
      selectLastOrDefaultGameType();
    }));
    addFeaturedModSelectionListener();
  }

  private Callback<ListView<FeaturedModBean>, ListCell<FeaturedModBean>> featuredModCellFactory() {
    return param -> new DualStringListCell<>(
        FeaturedModBean::getDisplayName,
        featuredMod -> StringUtils.equals(featuredMod.getTechnicalName(), KnownFeaturedMod.DEFAULT.getTechnicalName())
            ? " " + i18n.get("game.create.defaultGameTypeMarker")
            : null,
        FeaturedModBean::getDescription,
        STYLE_CLASS_DUAL_LIST_CELL, uiService
    );
  }

  private void selectLastOrDefaultGameType() {
    String lastGameMod = preferencesService.getPreferences().getLastGame().getLastGameType();
    if (lastGameMod == null) {
      lastGameMod = KnownFeaturedMod.DEFAULT.getTechnicalName();
    }

    for (FeaturedModBean mod : root.getItems()) {
      if (Objects.equals(mod.getTechnicalName(), lastGameMod)) {
        root.getSelectionModel().select(mod);
        root.scrollTo(mod);
        break;
      }
    }
  }

  private void addFeaturedModSelectionListener() {
    JavaFxUtil.addListener(selectedFeaturedModProperty(), (observable, oldValue, newValue) -> {
      preferencesService.getPreferences().getLastGame().setLastGameType(newValue.getTechnicalName());
      preferencesService.storeInBackground();
    });
  }

  public ReadOnlyObjectProperty<FeaturedModBean> selectedFeaturedModProperty() {
    return root.getSelectionModel().selectedItemProperty();
  }

  public FeaturedModBean getSelectedFeaturedMod() {
    return root.getSelectionModel().getSelectedItem();
  }

  @Override
  public ListView<FeaturedModBean> getRoot() {
    return root;
  }
}
