package com.faforever.client.filter;

import com.faforever.client.builders.ChatChannelUserBuilder;
import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.builders.LeaderboardBeanBuilder;
import com.faforever.client.builders.LeaderboardRatingBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.chat.ChatChannel;
import com.faforever.client.chat.ChatListItem;
import com.faforever.client.chat.ChatUserCategory;
import com.faforever.client.domain.LeaderboardBean;
import com.faforever.client.game.PlayerGameStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.player.Country;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import com.faforever.commons.lobby.GameStatus;
import org.apache.commons.lang3.Range;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class ChatUserFilterControllerTest extends PlatformTest {

  private static final ChatChannel CHANNEL = new ChatChannel("channel");

  @Mock
  private UiService uiService;
  @Mock
  private I18n i18n;
  @Mock
  private CountryFlagService countryFlagService;
  @Mock
  private LeaderboardService leaderboardService;

  @Mock
  private FilterTextFieldController<ChatListItem> clanFilter;
  @Mock
  private FilterMultiCheckboxController<PlayerGameStatus, ChatListItem> playerStatusFilter;
  @Mock
  private RangeSliderWithChoiceFilterController<LeaderboardBean, ChatListItem> playerRatingFilter;
  @Mock
  private FilterMultiCheckboxController<Country, ChatListItem> countryFilter;

  private final LeaderboardBean ladder = LeaderboardBeanBuilder.create().defaultValues().technicalName("ladder").get();
  private final LeaderboardBean global = LeaderboardBeanBuilder.create().defaultValues().technicalName("global").get();

  @InjectMocks
  private ChatUserFilterController instance;

  @BeforeEach
  public void setUp() throws Exception {
    // Order is important
    when(uiService.loadFxml(anyString())).thenReturn(clanFilter, playerStatusFilter, countryFilter);
    when(uiService.loadFxml(anyString(), eq(RangeSliderWithChoiceFilterController.class))).thenReturn(playerRatingFilter);
    when(leaderboardService.getLeaderboards()).thenReturn(Flux.just(ladder, global));

    loadFxml("theme/filter/filter.fxml", clazz -> instance, instance);
  }

  @Test
  public void testClanFilter() {
    ArgumentCaptor<BiFunction<String, ChatListItem, Boolean>> argumentCaptor = ArgumentCaptor.forClass(BiFunction.class);
    verify(clanFilter).registerListener(argumentCaptor.capture());

    ChatListItem category = new ChatListItem(null, ChatUserCategory.FRIEND, null, null);
    ChatListItem user1 = new ChatListItem(ChatChannelUserBuilder.create("user1", CHANNEL)
        .player(PlayerBeanBuilder.create().clan("clan_lenta").get())
        .get(), ChatUserCategory.FRIEND, null, null);
    ChatListItem user2 = new ChatListItem(ChatChannelUserBuilder.create("user2", CHANNEL).get(),
                                          ChatUserCategory.FRIEND, null, null);
    BiFunction<String, ChatListItem, Boolean> filter = argumentCaptor.getValue();

    assertTrue(filter.apply("", category));
    assertTrue(filter.apply("", user1));
    assertTrue(filter.apply("", user2));

    assertTrue(filter.apply("lenta", category));
    assertTrue(filter.apply("lenta", user1));
    assertFalse(filter.apply("lenta", user2));

    assertTrue(filter.apply("no_clan", category));
    assertFalse(filter.apply("no_clan", user1));
    assertFalse(filter.apply("no_clan", user2));
  }

  @Test
  public void testGameStatusFilter() {
    ArgumentCaptor<BiFunction<List<PlayerGameStatus>, ChatListItem, Boolean>> argumentCaptor = ArgumentCaptor.forClass(
        BiFunction.class);
    verify(playerStatusFilter).registerListener(argumentCaptor.capture());

    ChatListItem category = new ChatListItem(null, ChatUserCategory.FRIEND, null, null);
    ChatListItem idleUser = new ChatListItem(ChatChannelUserBuilder.create("user1", CHANNEL).player(
        PlayerBeanBuilder.create().defaultValues().game(null).get()
    ).get(), ChatUserCategory.FRIEND, null, null);
    ChatListItem busyUser = new ChatListItem(ChatChannelUserBuilder.create("user2", CHANNEL).player(
        PlayerBeanBuilder.create()
            .defaultValues()
            .game(GameBeanBuilder.create().defaultValues().status(GameStatus.PLAYING).get())
            .get()
    ).get(), ChatUserCategory.FRIEND, null, null);
    BiFunction<List<PlayerGameStatus>, ChatListItem, Boolean> filter = argumentCaptor.getValue();

    List<PlayerGameStatus> emptyList = Collections.emptyList();
    assertTrue(filter.apply(emptyList, category));
    assertTrue(filter.apply(emptyList, idleUser));
    assertTrue(filter.apply(emptyList, busyUser));

    List<PlayerGameStatus> idle = List.of(PlayerGameStatus.IDLE);
    assertTrue(filter.apply(idle, category));
    assertTrue(filter.apply(idle, idleUser));
    assertFalse(filter.apply(idle, busyUser));

    List<PlayerGameStatus> playing = List.of(PlayerGameStatus.PLAYING, PlayerGameStatus.LOBBYING);
    assertTrue(filter.apply(playing, category));
    assertFalse(filter.apply(playing, idleUser));
    assertTrue(filter.apply(playing, busyUser));
  }

  @Test
  public void testPlayerRatingFilter() {
    ArgumentCaptor<BiFunction<ItemWithRange<LeaderboardBean, Integer>, ChatListItem, Boolean>> argumentCaptor = ArgumentCaptor.forClass(BiFunction.class);
    verify(playerRatingFilter).registerListener(argumentCaptor.capture());

    ChatListItem category = new ChatListItem(null, ChatUserCategory.FRIEND, null, null);
    ChatListItem user1 = new ChatListItem(ChatChannelUserBuilder.create("user1", CHANNEL)
        .player(PlayerBeanBuilder.create()
            .defaultValues()
            .leaderboardRatings(Map.of("ladder", LeaderboardRatingBeanBuilder.create()
                    .defaultValues()
                    .mean(2000)
                    .deviation(0)
                    .get(),
                "global", LeaderboardRatingBeanBuilder.create().defaultValues().mean(1000).deviation(0).get()))
            .get()).get(), ChatUserCategory.FRIEND, null, null);
    ChatListItem user2 = new ChatListItem(ChatChannelUserBuilder.create("user1", CHANNEL)
        .player(PlayerBeanBuilder.create()
            .defaultValues()
            .leaderboardRatings(Map.of("ladder", LeaderboardRatingBeanBuilder.create()
                    .defaultValues()
                    .mean(1500)
                    .deviation(0)
                    .get(),
                "global", LeaderboardRatingBeanBuilder.create().defaultValues().mean(1500).deviation(0).get()))
            .get()).get(), ChatUserCategory.FRIEND, null, null);
    BiFunction<ItemWithRange<LeaderboardBean, Integer>, ChatListItem, Boolean> filter = argumentCaptor.getValue();

    ItemWithRange<LeaderboardBean, Integer> noChange = new ItemWithRange<>(ladder, AbstractRangeSliderFilterController.NO_CHANGE);
    assertTrue(filter.apply(noChange, category));
    assertTrue(filter.apply(noChange, user1));
    assertTrue(filter.apply(noChange, user2));

    ItemWithRange<LeaderboardBean, Integer> ladderFilter = new ItemWithRange<>(ladder, Range.between(1700, 4000));
    assertTrue(filter.apply(ladderFilter, category));
    assertTrue(filter.apply(ladderFilter, user1));
    assertFalse(filter.apply(ladderFilter, user2));

    ItemWithRange<LeaderboardBean, Integer> godFilter = new ItemWithRange<>(ladder, Range.between(3000, 4000));
    assertTrue(filter.apply(godFilter, category));
    assertFalse(filter.apply(godFilter, user1));
    assertFalse(filter.apply(godFilter, user2));

    ItemWithRange<LeaderboardBean, Integer> globalFilter = new ItemWithRange<>(global, Range.between(800, 2000));
    assertTrue(filter.apply(globalFilter, category));
    assertTrue(filter.apply(globalFilter, user1));
    assertTrue(filter.apply(globalFilter, user2));
  }

  @Test
  public void testCountryCodeFilter() {
    ArgumentCaptor<BiFunction<List<Country>, ChatListItem, Boolean>> argumentCaptor = ArgumentCaptor.forClass(BiFunction.class);
    verify(countryFilter).registerListener(argumentCaptor.capture());

    ChatListItem category = new ChatListItem(null, ChatUserCategory.FRIEND, null, null);
    ChatListItem russiaUser = new ChatListItem(ChatChannelUserBuilder.create("user1", CHANNEL)
        .player(PlayerBeanBuilder.create().defaultValues().country("ru").get())
        .get(), ChatUserCategory.FRIEND, null, null);
    ChatListItem americanUser = new ChatListItem(ChatChannelUserBuilder.create("user1", CHANNEL)
        .player(PlayerBeanBuilder.create().defaultValues().country("us").get())
        .get(), ChatUserCategory.FRIEND, null, null);
    BiFunction<List<Country>, ChatListItem, Boolean> filter = argumentCaptor.getValue();

    List<Country> emptyList = Collections.emptyList();
    assertTrue(filter.apply(emptyList, category));
    assertTrue(filter.apply(emptyList, russiaUser));
    assertTrue(filter.apply(emptyList, americanUser));

    List<Country> ru = List.of(new Country("ru", "Russia"));
    assertTrue(filter.apply(ru, category));
    assertTrue(filter.apply(ru, russiaUser));
    assertFalse(filter.apply(ru, americanUser));

    List<Country> ruAndUs = List.of(new Country("ru", "Russia"), new Country("us", "USA"));
    assertTrue(filter.apply(ruAndUs, category));
    assertTrue(filter.apply(ruAndUs, russiaUser));
    assertTrue(filter.apply(ruAndUs, americanUser));

    List<Country> de = List.of(new Country("de", "Germany"));
    assertTrue(filter.apply(de, category));
    assertFalse(filter.apply(de, russiaUser));
    assertFalse(filter.apply(de, americanUser));
  }
}