package com.faforever.client.filter;

import com.faforever.client.domain.GameBean;
import com.faforever.client.filter.converter.FeaturedModConverter;
import com.faforever.client.filter.function.FeaturedModFilterFunction;
import com.faforever.client.filter.function.SimModsFilterFunction;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.mod.ModService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import javafx.beans.InvalidationListener;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CustomGamesFilterController extends AbstractFilterController<GameBean> {

  private final ModService modService;
  private final PreferencesService preferencesService;

  private final InvalidationListener propertyListener;

  private MutableListFilterController<GameBean> mapFolderNameBlackListFilter;
  private FilterCheckboxController<GameBean> privateGameFilter;
  private FilterCheckboxController<GameBean> simModsFilter;

  public CustomGamesFilterController(UiService uiService, I18n i18n, ModService modService, PreferencesService preferencesService) {
    super(uiService, i18n);
    this.modService = modService;
    this.preferencesService = preferencesService;

    this.propertyListener = observable -> preferencesService.storeInBackground();
  }

  @Override
  protected void build(FilterBuilder<GameBean> filterBuilder) {
    privateGameFilter = filterBuilder.checkbox(i18n.get("privateGames"), (selected, game) -> !selected || !game.isPasswordProtected());

    simModsFilter = filterBuilder.checkbox(i18n.get("moddedGames"), new SimModsFilterFunction());

    filterBuilder.multiCheckbox(i18n.get("featuredMod.displayName"), modService.getFeaturedMods(),
        new FeaturedModConverter(), new FeaturedModFilterFunction());

    mapFolderNameBlackListFilter = filterBuilder.mutableList(i18n.get("blacklist.mapFolderName"), i18n.get("blacklist.mapFolderName.promptText"),
        (folderNames, game) -> folderNames.isEmpty() || folderNames.stream()
            .noneMatch(name -> StringUtils.containsIgnoreCase(game.getMapFolderName(), name)));
  }

  @Override
  protected void afterBuilt() {
    Preferences preferences = preferencesService.getPreferences();

    JavaFxUtil.bindBidirectional(privateGameFilter.getObservable(), preferences.hidePrivateGamesProperty());
    JavaFxUtil.bindBidirectional(simModsFilter.getObservable(), preferences.hideModdedGamesProperty());
    JavaFxUtil.bindBidirectional(mapFolderNameBlackListFilter.getObservable(), preferences.getFilters().mapNameBlacklistProperty());

    JavaFxUtil.addListener(preferences.hideModdedGamesProperty(), propertyListener);
    JavaFxUtil.addListener(preferences.hidePrivateGamesProperty(), propertyListener);
    JavaFxUtil.addListener(preferences.getFilters().mapNameBlacklistProperty(), propertyListener);
  }
}
