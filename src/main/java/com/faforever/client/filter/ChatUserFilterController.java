package com.faforever.client.filter;

import com.faforever.client.chat.ChatListItem;
import com.faforever.client.domain.LeaderboardBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.player.Country;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.RatingUtil;
import javafx.util.StringConverter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ChatUserFilterController extends AbstractFilterController<ChatListItem> {

  public final static int MIN_RATING = -1000;
  public final static int MAX_RATING = 4000;

  private final CountryFlagService countryFlagService;
  private final LeaderboardService leaderboardService;

  private final StringConverter<PlayerStatus> playerStatusConverter = new StringConverter<>() {
    @Override
    public String toString(PlayerStatus object) {
      return i18n.get(object.getI18nKey());
    }

    @Override
    public PlayerStatus fromString(String string) {
      throw new UnsupportedOperationException("Not supported");
    }
  };

  private final StringConverter<Country> countryConverter = new StringConverter<>() {
    @Override
    public String toString(Country object) {
      return String.format("%s [%s]", object.displayName(), object.code());
    }

    @Override
    public Country fromString(String string) {
      throw new UnsupportedOperationException("Not supported");
    }
  };

  private final StringConverter<LeaderboardBean> leaderboardConverter = new StringConverter<>() {
    @Override
    public String toString(LeaderboardBean object) {
      String rating = i18n.getOrDefault(object.getTechnicalName(), object.getNameKey());
      return i18n.get("leaderboard.rating", rating);
    }

    @Override
    public LeaderboardBean fromString(String string) {
      throw new UnsupportedOperationException("Not supported");
    }
  };

  public ChatUserFilterController(UiService uiService, I18n i18n, CountryFlagService countryFlagService,
                                  LeaderboardService leaderboardService,
                                  FxApplicationThreadExecutor fxApplicationThreadExecutor) {
    super(uiService, i18n, fxApplicationThreadExecutor);
    this.countryFlagService = countryFlagService;
    this.leaderboardService = leaderboardService;
  }

  @Override
  protected void build(FilterBuilder<ChatListItem> filterBuilder) {

    filterBuilder.textField(i18n.get("chat.filter.clan"),
        (text, item) -> text.isEmpty() || item.user() == null || item.user()
            .getPlayer()
            .map(PlayerBean::getClan)
            .map(clan -> StringUtils.containsIgnoreCase(clan, text))
            .orElse(false));

    filterBuilder.multiCheckbox(i18n.get("game.gameStatus"), Arrays.stream(PlayerStatus.values())
            .toList(),
        playerStatusConverter, (selectedStatus, item) -> selectedStatus.isEmpty() || item.user() == null ||
            item.user().getPlayer().map(PlayerBean::getStatus).map(selectedStatus::contains).orElse(false));

    filterBuilder.rangeSliderWithCombobox(i18n.get("game.rating"), leaderboardService.getLeaderboards(), leaderboardConverter, MIN_RATING, MAX_RATING,
        (ratingWithRange, item) -> ratingWithRange.range() == AbstractRangeSliderFilterController.NO_CHANGE || item.user() == null ||
            item.user().getPlayer()
                .map(PlayerBean::getLeaderboardRatings)
                .map(ratingMap -> ratingMap.get(ratingWithRange.item().getTechnicalName()))
                .map(RatingUtil::getRating)
                .map(rating -> ratingWithRange.range().contains(rating))
                .orElse(false));

    filterBuilder.multiCheckbox(i18n.get("country"), countryFlagService.getCountries(), countryConverter,
        (countries, item) -> countries.isEmpty() || item.user() == null ||
            item.user().getPlayer()
                .map(PlayerBean::getCountry)
                .map(countryCode -> countries.stream().map(Country::code).toList().contains(countryCode))
                .orElse(false));
  }
}
