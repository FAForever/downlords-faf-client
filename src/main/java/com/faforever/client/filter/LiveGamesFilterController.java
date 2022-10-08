package com.faforever.client.filter;

import com.faforever.client.domain.GameBean;
import com.faforever.client.filter.converter.FeaturedModConverter;
import com.faforever.client.filter.function.FeaturedModFilterFunction;
import com.faforever.client.filter.function.SimModsFilterFunction;
import com.faforever.client.i18n.I18n;
import com.faforever.client.mod.ModService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.theme.UiService;
import com.faforever.commons.lobby.GameType;
import javafx.util.StringConverter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class LiveGamesFilterController extends AbstractFilterController<GameBean> {

  private final PlayerService playerService;
  private final ModService modService;

  public LiveGamesFilterController(UiService uiService, I18n i18n, ModService modService, PlayerService playerService) {
    super(uiService, i18n);
    this.modService = modService;
    this.playerService = playerService;
  }

  @Override
  protected void build(FilterBuilder<GameBean> filterBuilder) {
    filterBuilder.checkbox(i18n.get("moddedGames"), new SimModsFilterFunction());

    filterBuilder.checkbox(i18n.get("hideSingleGames"), (selected, game) -> !selected || game.getNumPlayers() != 1);

    filterBuilder.checkbox(i18n.get("showGamesWithFriends"), (selected, game) -> !selected || playerService.areFriendsInGame(game));

    filterBuilder.multiCheckbox(i18n.get("gameType"), List.of(GameType.CUSTOM, GameType.MATCHMAKER, GameType.COOP), gameTypeConverter,
        (selectedGameTypes, game) -> selectedGameTypes.isEmpty() || selectedGameTypes.contains(game.getGameType()));

    filterBuilder.multiCheckbox(i18n.get("featuredMod.displayName"), modService.getFeaturedMods(),
        new FeaturedModConverter(), new FeaturedModFilterFunction());

    filterBuilder.textField(i18n.get("game.player.username"), (text, game) -> text.isEmpty() || game.getTeams()
        .values()
        .stream()
        .flatMap(Collection::stream)
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
