package com.faforever.client.filter;

import com.faforever.client.domain.GameBean;
import com.faforever.client.filter.converter.FeaturedModConverter;
import com.faforever.client.filter.function.FeaturedModFilterFunction;
import com.faforever.client.filter.function.SimModsFilterFunction;
import com.faforever.client.i18n.I18n;
import com.faforever.client.mod.ModService;
import com.faforever.client.theme.UiService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CustomGamesFilterController extends AbstractFilterController<GameBean> {

  private final ModService modService;

  private MutableListFilterController<GameBean> mapFolderNameBlackListFilter;
  private FilterCheckboxController<GameBean> privateGameFilter;
  private FilterCheckboxController<GameBean> simModsFilter;

  public CustomGamesFilterController(UiService uiService, I18n i18n, ModService modService) {
    super(uiService, i18n);
    this.modService = modService;
  }

  @Override
  protected void build(FilterBuilder<GameBean> filterBuilder) {
    privateGameFilter = filterBuilder.checkbox(i18n.get("privateGames"), (selected, game) -> !selected || !game.isPasswordProtected());

    simModsFilter = filterBuilder.checkbox(i18n.get("moddedGames"), new SimModsFilterFunction());

    filterBuilder.multiCheckbox(i18n.get("featuredMod.displayName"), modService.getFeaturedMods(),
        new FeaturedModConverter(), new FeaturedModFilterFunction());

    mapFolderNameBlackListFilter = filterBuilder.mutableList(i18n.get("blacklist.mapFolderName"), i18n.get("blacklist.mapFolderName.promptText"),
        (folderNames, game) -> folderNames.isEmpty() || folderNames.stream()
            .noneMatch(name -> game.getMapFolderName().contains(name)));
  }

  public BooleanProperty getPrivateGamesProperty() {
    return privateGameFilter.getObservable();
  }

  public BooleanProperty getSimsModsProperty() {
    return simModsFilter.getObservable();
  }

  public ListProperty<String> getMapFolderNameBlackListProperty() {
    return mapFolderNameBlackListFilter.getObservable();
  }
}
