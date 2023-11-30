package com.faforever.client.filter;

import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.filter.function.FeaturedModFilterFunction;
import com.faforever.client.filter.function.SimModsFilterFunction;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.ToStringOnlyConverter;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.mod.ModService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.social.SocialService;
import com.faforever.client.theme.UiService;
import com.faforever.commons.lobby.GameType;
import javafx.util.StringConverter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class LiveGamesFilterController extends AbstractFilterController<GameBean> {

  private final SocialService socialService;
  private final PlayerService playerService;
  private final ModService modService;
  private final MapGeneratorService mapGeneratorService;

  public LiveGamesFilterController(UiService uiService, I18n i18n, ModService modService, PlayerService playerService,
                                   MapGeneratorService mapGeneratorService,
                                   FxApplicationThreadExecutor fxApplicationThreadExecutor,
                                   SocialService socialService) {
    super(uiService, i18n, fxApplicationThreadExecutor);
    this.modService = modService;
    this.playerService = playerService;
    this.mapGeneratorService = mapGeneratorService;
    this.socialService = socialService;
  }

  @Override
  protected void build(FilterBuilder<GameBean> filterBuilder) {
    filterBuilder.checkbox(i18n.get("moddedGames"), new SimModsFilterFunction());

    filterBuilder.checkbox(i18n.get("hideSingleGames"), (selected, game) -> !selected || game.getNumActivePlayers() != 1);

    filterBuilder.checkbox(i18n.get("showGamesWithFriends"),
                           (selected, game) -> !selected || socialService.areFriendsInGame(game));

    filterBuilder.checkbox(i18n.get("showGeneratedMaps"), (selected, game) -> !selected || mapGeneratorService.isGeneratedMap(game.getMapFolderName()));

    filterBuilder.multiCheckbox(i18n.get("gameType"), List.of(GameType.CUSTOM, GameType.MATCHMAKER, GameType.COOP), gameTypeConverter,
        (selectedGameTypes, game) -> selectedGameTypes.isEmpty() || selectedGameTypes.contains(game.getGameType()));

    filterBuilder.multiCheckbox(i18n.get("featuredMod.displayName"), modService.getFeaturedMods(),
                                new ToStringOnlyConverter<>(FeaturedModBean::getDisplayName),
                                new FeaturedModFilterFunction());

    filterBuilder.textField(i18n.get("game.player.username"), (text, game) -> text.isEmpty() || game.getTeams()
        .values()
        .stream()
        .flatMap(Collection::stream)
        .map(playerService::getPlayerByIdIfOnline)
        .flatMap(Optional::stream)
        .map(PlayerBean::getUsername)
        .anyMatch(name -> StringUtils.containsIgnoreCase(name, text)));
  }

  private final StringConverter<GameType> gameTypeConverter = new StringConverter<>() {
    @Override
    public String toString(GameType object) {
      return i18n.get(switch (object) {
        case CUSTOM -> "customGame";
        case MATCHMAKER -> "matchmaker";
        case COOP -> "coopGame";
        default -> throw new IllegalArgumentException(object + " should not be used");
      });
    }

    @Override
    public GameType fromString(String string) {
      throw new UnsupportedOperationException("Not supported");
    }
  };
}
