package com.faforever.client.filter;

import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.GameBean;
import com.faforever.client.filter.function.FeaturedModFilterFunction;
import com.faforever.client.filter.function.SimModsFilterFunction;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.ToStringOnlyConverter;
import com.faforever.client.i18n.I18n;
import com.faforever.client.mod.ModService;
import com.faforever.client.preferences.FiltersPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.theme.UiService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CustomGamesFilterController extends AbstractFilterController<GameBean> {

  private final ModService modService;
  private final Preferences preferences;
  private final FiltersPrefs filtersPrefs;

  private MutableListFilterController<GameBean> mapFolderNameBlackListFilter;
  private FilterCheckboxController<GameBean> privateGameFilter;
  private FilterCheckboxController<GameBean> simModsFilter;

  public CustomGamesFilterController(UiService uiService, I18n i18n, ModService modService, Preferences preferences,
                                     FiltersPrefs filtersPrefs,
                                     FxApplicationThreadExecutor fxApplicationThreadExecutor) {
    super(uiService, i18n, fxApplicationThreadExecutor);
    this.modService = modService;
    this.preferences = preferences;
    this.filtersPrefs = filtersPrefs;
  }

  @Override
  protected void build(FilterBuilder<GameBean> filterBuilder) {
    privateGameFilter = filterBuilder.checkbox(i18n.get("privateGames"), (selected, game) -> !selected || !game.isPasswordProtected());

    simModsFilter = filterBuilder.checkbox(i18n.get("moddedGames"), new SimModsFilterFunction());

    filterBuilder.multiCheckbox(i18n.get("featuredMod.displayName"), modService.getFeaturedMods(),
                                new ToStringOnlyConverter<>(FeaturedModBean::getDisplayName),
                                new FeaturedModFilterFunction());

    mapFolderNameBlackListFilter = filterBuilder.mutableList(i18n.get("blacklist.mapFolderName"), i18n.get("blacklist.mapFolderName.promptText"),
        (folderNames, game) -> folderNames.isEmpty() || folderNames.stream()
            .noneMatch(name -> StringUtils.containsIgnoreCase(game.getMapFolderName(), name)));
  }

  @Override
  protected void afterBuilt() {
    privateGameFilter.valueProperty().bindBidirectional(preferences.hidePrivateGamesProperty());
    simModsFilter.valueProperty().bindBidirectional(preferences.hideModdedGamesProperty());
    mapFolderNameBlackListFilter.valueProperty().bindBidirectional(filtersPrefs.mapNameBlacklistProperty());
  }
}
