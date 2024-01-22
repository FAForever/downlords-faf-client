package com.faforever.client.filter;

import com.faforever.client.chat.ChatListItem;
import com.faforever.client.domain.LeaderboardBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.game.PlayerGameStatus;
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

  private final StringConverter<PlayerGameStatus> playerStatusConverter = new StringConverter<>() {
    @Override
    public String toString(PlayerGameStatus object) {
      return i18n.get(object.getI18nKey());
    }

    @Override
    public PlayerGameStatus fromString(String string) {
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

    filterBuilder.multiCheckbox(i18n.get("game.gameStatus"), Arrays.stream(PlayerGameStatus.values())
            .toList(),
        playerStatusConverter, (selectedStatus, item) -> selectedStatus.isEmpty() || item.user() == null || item.user()
                                                                                                                .getPlayer()
                                                                                                                .map(
                                                                                                                    PlayerBean::getGameStatus)
                                                                                                                .map(
                                                                                                                    selectedStatus::contains)
                                                                                                                .orElse(
                                                                                                                    false));

    RangeSliderWithChoiceFilterController<LeaderboardBean, ChatListItem> ratingFilter = filterBuilder.rangeSliderWithCombobox(
        i18n.get("game.rating"), leaderboardConverter, MIN_RATING, MAX_RATING, (ratingWithRange, item) -> {
          if (ratingWithRange.range() == AbstractRangeSliderFilterController.NO_CHANGE) {
            return true;
          }

          if (item.user() == null) {
            return true;
          }

          return item.user()
                     .getPlayer()
                     .map(PlayerBean::getLeaderboardRatings)
                     .map(ratingMap -> ratingMap.get(ratingWithRange.item().getTechnicalName()))
                     .map(RatingUtil::getRating)
                     .map(rating -> ratingWithRange.range().contains(rating))
                     .orElse(false);
        });

    leaderboardService.getLeaderboards().collectList().subscribe(ratingFilter::setItems);

    filterBuilder.multiCheckbox(i18n.get("country"), countryFlagService.getCountries(), countryConverter,
        (countries, item) -> countries.isEmpty() || item.user() == null ||
            item.user().getPlayer()
                .map(PlayerBean::getCountry)
                .map(countryCode -> countries.stream().map(Country::code).toList().contains(countryCode))
                .orElse(false));
  }
}
