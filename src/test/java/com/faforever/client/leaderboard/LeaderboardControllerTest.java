package com.faforever.client.leaderboard;

import com.faforever.client.builders.LeaderboardBeanBuilder;
import com.faforever.client.builders.LeaderboardEntryBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.LeaderboardBean;
import com.faforever.client.domain.LeaderboardEntryBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.remote.AssetService;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LeaderboardControllerTest extends UITest {

  private LeaderboardController instance;

  @Mock
  private AssetService assetService;
  @Mock
  private I18n i18n;
  @Mock
  private LeaderboardService leaderboardService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private PlayerService playerService;
  @Mock
  private UiService uiService;

  private final LeaderboardBean leaderboardGlobal = LeaderboardBeanBuilder.create().defaultValues().id(1).technicalName("global").get();
  private final List<LeaderboardEntryBean> entriesGlobal = List.of(
      LeaderboardEntryBeanBuilder.create().defaultValues().id(0).player(PlayerBeanBuilder.create().defaultValues().username("MarcSpector").id(0).get()).get(),
      LeaderboardEntryBeanBuilder.create().defaultValues().id(1).player(PlayerBeanBuilder.create().defaultValues().username("Sheikah").id(1).get()).get(),
      LeaderboardEntryBeanBuilder.create().defaultValues().id(2).player(PlayerBeanBuilder.create().defaultValues().username("ZLO").id(2).get()).get()
  );

  @BeforeEach
  public void setUp() throws Exception {


    instance = new LeaderboardController(assetService, i18n, leaderboardService, notificationService, playerService, uiService);

    loadFxml("theme/leaderboard/leaderboards.fxml", clazz -> instance);
  }

  @Test
  public void testFilterByNamePlayerExactMatch() {

  }

  @Test
  public void testFilterByNamePlayerPartialMatch() {

  }

  @Test
  public void testAutoCompletionSuggestions() {
    settingAutoCompletionForTestEnvironment();
    setSearchText("o");
    assertSearchSuggestions("MarcSpector", "ZLO");
    setSearchText("ei");
    assertSearchSuggestions("Sheikah");
  }

  private void setSearchText(String text) {
    runOnFxThreadAndWait(() -> instance.searchTextField.setText(text));
  }

  private void settingAutoCompletionForTestEnvironment() {
    runOnFxThreadAndWait(() -> {
      getRoot().getChildren().add(instance.searchTextField);
      instance.usernamesAutoCompletion.getAutoCompletionPopup().setOpacity(0.0);
    });
  }

  private void assertSearchSuggestions(String... expectedSuggestions) {
    List<String> actualSuggestions = instance.usernamesAutoCompletion.getAutoCompletionPopup().getSuggestions();
    assertEquals(expectedSuggestions.length,  actualSuggestions.size());
    assertTrue(actualSuggestions.containsAll(Arrays.asList(expectedSuggestions)));
  }

  @Test
  public void testGetRoot() throws Exception {
    assertEquals(instance.getRoot(), instance.leaderboardRoot);
  }
}
