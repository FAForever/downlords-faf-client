package com.faforever.client.chat;

import com.faforever.client.builders.ChatChannelUserBuilder;
import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.builders.LeaderboardRatingMapBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.test.UITest;
import com.faforever.commons.lobby.GameStatus;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class ChatUserFilterControllerTest extends UITest {

  private static final String CHANNEL_NAME = "#testChannel";

  @Mock
  private I18n i18n;
  @Mock
  private CountryFlagService flagService;

  private ListItem categoryItem;
  private ListItem userLobbyingItem;
  private ListItem userHostingItem;
  private ListItem userIdleItem;
  private ListItem userPlayingItem;
  private FilteredList<ListItem> items;
  private TextField searchUsernameTextField;

  @InjectMocks
  private ChatUserFilterController instance;

  @BeforeEach
  public void setUp() throws Exception {
    categoryItem = new ChatUserCategoryItem(ChatUserCategory.OTHER, CHANNEL_NAME);
    userLobbyingItem = new ChatUserItem(
        ChatChannelUserBuilder.create("userLobbying", CHANNEL_NAME)
            .socialStatus(SocialStatus.OTHER)
            .player(PlayerBeanBuilder.create()
                .defaultValues()
                .leaderboardRatings(LeaderboardRatingMapBuilder.create()
                    .defaultValues()
                    .get())
                .game(GameBeanBuilder.create()
                    .defaultValues()
                    .host("userHosting")
                    .status(GameStatus.OPEN)
                    .get())
                .get())
            .get(),
        ChatUserCategory.OTHER
    );
    userPlayingItem = new ChatUserItem(
        ChatChannelUserBuilder.create("userPlaying", CHANNEL_NAME)
            .socialStatus(SocialStatus.OTHER)
            .player(PlayerBeanBuilder.create()
                .defaultValues()
                .leaderboardRatings(LeaderboardRatingMapBuilder.create()
                    .put("global", 1600, 100, 1000)
                    .get())
                .game(GameBeanBuilder.create()
                    .defaultValues()
                    .host("userPlaying")
                    .status(GameStatus.PLAYING)
                    .get())
                .get())
            .get(),
        ChatUserCategory.OTHER
    );
    userHostingItem = new ChatUserItem(
        ChatChannelUserBuilder.create("userHosting", CHANNEL_NAME)
            .socialStatus(SocialStatus.OTHER)
            .countryName("fr")
            .player(PlayerBeanBuilder.create()
                .defaultValues()
                .country("fr")
                .leaderboardRatings(LeaderboardRatingMapBuilder.create()
                    .put("global", 900, 100, 1000)
                    .get())
                .clan("userClan")
                .game(GameBeanBuilder.create()
                    .defaultValues()
                    .host("userHosting")
                    .status(GameStatus.OPEN)
                    .get())
                .get())
            .get(),
        ChatUserCategory.OTHER
    );
    userIdleItem = new ChatUserItem(
        ChatChannelUserBuilder.create("userIdle", CHANNEL_NAME)
            .socialStatus(SocialStatus.OTHER)
            .player(PlayerBeanBuilder.create()
                .defaultValues()
                .leaderboardRatings(LeaderboardRatingMapBuilder.create()
                    .put("global", 900, 100, 1000)
                    .get())
                .game(null)
                .get())
            .get(),
        ChatUserCategory.OTHER
    );

    items = new FilteredList<>(FXCollections.observableArrayList(categoryItem, userLobbyingItem, userHostingItem, userIdleItem, userPlayingItem));
    searchUsernameTextField = new TextField();

    when(flagService.getCountries(any())).thenReturn(List.of("us", "fr", "ru"));
    loadFxml("theme/chat/user_filter.fxml", clazz -> instance);
    instance.finalizeFiltersSettings(items, searchUsernameTextField);
  }

  @Test
  public void testIsInClan() {
    runOnFxThreadAndWait(() -> instance.clanTextField.setText("userClan"));
    assertTrue(items.contains(categoryItem));
    assertTrue(items.contains(userHostingItem));
    assertFalse(items.contains(userIdleItem));
    assertFalse(items.contains(userIdleItem));
    assertFalse(items.contains(userPlayingItem));
  }

  @Test
  public void testIsBoundedByRatingWithinBounds() {
    runOnFxThreadAndWait(() -> instance.minRatingTextField.setText("-100"));
    runOnFxThreadAndWait(() -> instance.maxRatingTextField.setText("200"));
    assertTrue(items.contains(categoryItem));
    assertTrue(items.contains(userLobbyingItem));
    assertFalse(items.contains(userIdleItem));
    assertFalse(items.contains(userHostingItem));
    assertFalse(items.contains(userPlayingItem));
  }

  @Test
  public void testOnGameStatusPlaying() {
    runOnFxThreadAndWait(() -> instance.onGameStatusPlaying());
    assertTrue(items.contains(categoryItem));
    assertTrue(items.contains(userPlayingItem));
    assertFalse(items.contains(userIdleItem));
    assertFalse(items.contains(userHostingItem));
    assertFalse(items.contains(userLobbyingItem));
  }

  @Test
  public void testOnGameStatusMatchLobby() {
    runOnFxThreadAndWait(() -> instance.onGameStatusLobby());
    assertTrue(items.contains(categoryItem));
    assertTrue(items.contains(userLobbyingItem));
    assertTrue(items.contains(userHostingItem));
    assertFalse(items.contains(userIdleItem));
    assertFalse(items.contains(userPlayingItem));
  }

  @Test
  public void testOnGameStatusNone() {
    runOnFxThreadAndWait(() -> instance.onGameStatusNone());
    assertTrue(items.contains(categoryItem));
    assertTrue(items.contains(userIdleItem));
    assertFalse(items.contains(userHostingItem));
    assertFalse(items.contains(userLobbyingItem));
    assertFalse(items.contains(userPlayingItem));
  }

  @Test
  public void testSearchUserByUsername() {
    runOnFxThreadAndWait(() -> searchUsernameTextField.setText("Playing"));
    assertTrue(items.contains(categoryItem));
    assertTrue(items.contains(userPlayingItem));
    assertFalse(items.contains(userHostingItem));
    assertFalse(items.contains(userLobbyingItem));
    assertFalse(items.contains(userIdleItem));
  }

  @Test
  public void testSearchUserByCountry() {
    runOnFxThreadAndWait(() -> instance.countryTextField.setText("fr"));
    assertTrue(items.contains(categoryItem));
    assertTrue(items.contains(userHostingItem));
    assertFalse(items.contains(userPlayingItem));
    assertFalse(items.contains(userLobbyingItem));
    assertFalse(items.contains(userIdleItem));
  }

  @Test
  public void testGetRoot() {
    assertNotNull(getRoot());
  }
}
