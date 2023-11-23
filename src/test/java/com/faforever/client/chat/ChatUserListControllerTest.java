package com.faforever.client.chat;

import com.faforever.client.builders.ChatChannelUserBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.filter.ChatUserFilterController;
import com.faforever.client.game.GameTooltipController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.Pane;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.beans.factory.ObjectFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.faforever.client.player.SocialStatus.FRIEND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
public class ChatUserListControllerTest extends PlatformTest {

  private static final String CHANNEL_NAME = "#testChannel";


  @Mock
  private UiService uiService;
  @Mock
  private I18n i18n;
  @Mock
  private ChatUserFilterController chatUserFilterController;
  @Mock
  private GameTooltipController gameInfoController;
  @Mock
  private ObjectFactory<ChatListItemCell> chatListItemCellFactory;
  @Spy
  private ChatPrefs chatPrefs;

  private ChatChannel chatChannel;

  @InjectMocks
  private ChatUserListController instance;

  @BeforeEach
  public void setUp() throws Exception {
    chatChannel = new ChatChannel(CHANNEL_NAME);

    when(uiService.loadFxml("theme/filter/filter.fxml", ChatUserFilterController.class)).thenReturn(chatUserFilterController);
    when(uiService.loadFxml("theme/play/game_tooltip.fxml")).thenReturn(gameInfoController);
    when(gameInfoController.getRoot()).thenReturn(new Pane());
    when(chatListItemCellFactory.getObject()).thenAnswer(invocation -> {
      ChatListItemCell mockCell = mock(ChatListItemCell.class);
      when(mockCell.getNode()).thenReturn(new Pane());
      return mockCell;
    });
    when(chatUserFilterController.filterActiveProperty()).thenReturn(new SimpleBooleanProperty());
    when(chatUserFilterController.predicateProperty()).thenReturn(new SimpleObjectProperty<>(item -> true));
    when(chatUserFilterController.getPredicate()).thenReturn(item -> true);
    when(chatUserFilterController.getRoot()).thenReturn(new SplitPane());
    loadFxml("theme/chat/user_list.fxml", clazz -> instance);
  }

  @Test
  public void testOnOneUserJoinedForHiddenCategory() throws Exception {
    setHiddenCategoriesToChatPrefs(ChatUserCategory.OTHER);
    runOnFxThreadAndWait(() -> instance.setChatChannel(chatChannel));

    ChatChannelUser user = generateUser(SocialStatus.OTHER);
    assertNoUsersInCategory(ChatUserCategory.OTHER);
    addUsersToChannel(user);
    assertNotContainUsersInCategory(ChatUserCategory.OTHER, user);
    assertContainUsersInSource(user);
  }

  @Test
  public void testOnManyUsersJoinedForHiddenCategory() throws Exception {
    setHiddenCategoriesToChatPrefs(ChatUserCategory.OTHER);
    runOnFxThreadAndWait(() -> instance.setChatChannel(chatChannel));

    List<ChatChannelUser> userList = generateUsers(SocialStatus.OTHER, 10);
    assertNoUsersInCategory(ChatUserCategory.OTHER);
    addUsersToChannel(userList);
    assertNotContainUsersInCategory(ChatUserCategory.OTHER, userList);
    assertContainUsersInSource(userList);
  }

  @Test
  public void testOnOneUserLeftForHiddenCategory() throws Exception {
    setHiddenCategoriesToChatPrefs(ChatUserCategory.OTHER);

    ChatChannelUser user = generateUser(SocialStatus.OTHER);
    addUsersToChannel(user);

    runOnFxThreadAndWait(() -> instance.setChatChannel(chatChannel));

    assertNoUsersInCategory(ChatUserCategory.OTHER);
    assertContainUsersInSource(user);

    removeUsersFromChannel(user);

    assertNoUsersInCategory(ChatUserCategory.OTHER);
    assertNotContainUsersInSource(user);
  }

  @Test
  public void testOnManyUsersLeftForHiddenCategory() throws Exception {
    setHiddenCategoriesToChatPrefs(ChatUserCategory.OTHER);

    List<ChatChannelUser> userList = generateUsers(SocialStatus.OTHER, 10);
    addUsersToChannel(userList);

    runOnFxThreadAndWait(() -> instance.setChatChannel(chatChannel));

    assertNoUsersInCategory(ChatUserCategory.OTHER);
    assertContainUsersInSource(userList);

    removeUsersFromChannel(userList);

    assertNoUsersInCategory(ChatUserCategory.OTHER);
    assertNotContainUsersInSource(userList);
  }

  @Test
  public void testOnOneUserJoinedForVisibleCategory() throws Exception {
    runOnFxThreadAndWait(() -> instance.setChatChannel(chatChannel));

    ChatChannelUser user = generateUser(SocialStatus.OTHER);
    assertNoUsersInCategory(ChatUserCategory.OTHER);
    addUsersToChannel(user);
    assertContainUsersInCategory(ChatUserCategory.OTHER, user);
    assertContainUsersInSource(user);
  }

  @Test
  public void testOnManyUsersJoinedForVisibleCategory() throws Exception {
    runOnFxThreadAndWait(() -> instance.setChatChannel(chatChannel));

    List<ChatChannelUser> userList = generateUsers(SocialStatus.OTHER, 10);
    assertNoUsersInCategory(ChatUserCategory.OTHER);
    addUsersToChannel(userList);
    assertContainUsersInCategory(ChatUserCategory.OTHER, userList);
    assertContainUsersInSource(userList);
  }

  @Test
  public void testOnOneUserLeftForVisibleCategory() throws Exception {
    ChatChannelUser user = generateUser(SocialStatus.OTHER);
    addUsersToChannel(user);

    runOnFxThreadAndWait(() -> instance.setChatChannel(chatChannel));

    assertContainUsersInCategory(ChatUserCategory.OTHER, user);
    assertContainUsersInSource(user);

    removeUsersFromChannel(user);

    assertNotContainUsersInCategory(ChatUserCategory.OTHER, user);
    assertNotContainUsersInSource(user);
  }

  @Test
  public void testOnManyUsersLeftForVisibleCategory() throws Exception {
    List<ChatChannelUser> userList = generateUsers(SocialStatus.OTHER, 10);
    addUsersToChannel(userList);

    runOnFxThreadAndWait(() -> instance.setChatChannel(chatChannel));

    assertContainUsersInCategory(ChatUserCategory.OTHER, userList);
    assertContainUsersInSource(userList);

    removeUsersFromChannel(userList);

    assertNotContainUsersInCategory(ChatUserCategory.OTHER, userList);
    assertNotContainUsersInSource(userList);
  }

  @Test
  public void testOnChangeUserCategory() throws Exception {
    ChatChannelUser user = generateUser(SocialStatus.OTHER);
    ChatChannelUser updatedUser = ChatChannelUserBuilder.create(user.getUsername(), CHANNEL_NAME)
        .player(PlayerBeanBuilder.create().socialStatus(FRIEND).get())
        .get();
    addUsersToChannel(user);

    runOnFxThreadAndWait(() -> instance.setChatChannel(chatChannel));

    assertContainUsersInCategory(ChatUserCategory.OTHER, user);
    assertNotContainUsersInCategory(ChatUserCategory.FRIEND, updatedUser);
    assertContainUsersInSource(user);

    runOnFxThreadAndWait(() -> user.getPlayer().ifPresent(playerBean -> playerBean.setSocialStatus(FRIEND)));

    assertNotContainUsersInCategory(ChatUserCategory.OTHER, user);
    assertContainUsersInCategory(ChatUserCategory.FRIEND, updatedUser);
    assertContainUsersInSource(updatedUser);
  }

  @Test
  public void testFriendlyUserIsModerator() throws Exception {
    ChatChannelUser user = generateUser(SocialStatus.FRIEND, true);
    addUsersToChannel(user);

    runOnFxThreadAndWait(() -> instance.setChatChannel(chatChannel));

    assertContainUsersInCategory(ChatUserCategory.FRIEND, user);
    assertContainUsersInCategory(ChatUserCategory.MODERATOR, user);
  }

  @Test
  public void testEnemyUserIsModerator() throws Exception {
    ChatChannelUser user = generateUser(SocialStatus.FOE, true);
    addUsersToChannel(user);

    runOnFxThreadAndWait(() -> instance.setChatChannel(chatChannel));

    assertContainUsersInCategory(ChatUserCategory.FOE, user);
    assertContainUsersInCategory(ChatUserCategory.MODERATOR, user);
  }

  @Test
  public void testChatOnlyUserIsModerator() throws Exception {
    ChatChannelUser user = generateUser(null, true);
    addUsersToChannel(user);

    runOnFxThreadAndWait(() -> instance.setChatChannel(chatChannel));

    assertContainUsersInCategory(ChatUserCategory.CHAT_ONLY, user);
    assertContainUsersInCategory(ChatUserCategory.MODERATOR, user);
  }

  @Test
  public void testExistingChannelUsers() throws Exception {
    ChatChannelUser selfUser = generateUser(SocialStatus.SELF);
    ChatChannelUser moderatorUser = generateUser(SocialStatus.OTHER, true);
    ChatChannelUser friendUser = generateUser(SocialStatus.FRIEND);
    ChatChannelUser otherUser = generateUser(SocialStatus.OTHER);
    ChatChannelUser foeUser = generateUser(SocialStatus.FOE);
    ChatChannelUser chatOnlyUser = generateUser(null);

    addUsersToChannel(selfUser, moderatorUser, friendUser, otherUser, foeUser, chatOnlyUser);
    runOnFxThreadAndWait(() -> instance.setChatChannel(chatChannel));

    assertContainUsersInCategory(ChatUserCategory.SELF, selfUser);
    assertContainUsersInCategory(ChatUserCategory.MODERATOR, moderatorUser);
    assertContainUsersInCategory(ChatUserCategory.FRIEND, friendUser);
    assertContainUsersInCategory(ChatUserCategory.OTHER, moderatorUser);
    assertContainUsersInCategory(ChatUserCategory.OTHER, otherUser);
    assertContainUsersInCategory(ChatUserCategory.FOE, foeUser);
    assertContainUsersInCategory(ChatUserCategory.CHAT_ONLY, chatOnlyUser);

    assertContainUsersInSource(selfUser, moderatorUser, friendUser, otherUser, foeUser, chatOnlyUser);
  }

  @Test
  public void testHideCategory() throws Exception {
    List<ChatChannelUser> userList = generateUsers(SocialStatus.OTHER, 10);
    addUsersToChannel(userList);
    runOnFxThreadAndWait(() -> instance.setChatChannel(chatChannel));

    assertContainUsersInCategory(ChatUserCategory.OTHER, userList);
    assertContainUsersInSource(userList);

    hideCategory(ChatUserCategory.OTHER);

    assertNoUsersInCategory(ChatUserCategory.OTHER);
    assertContainUsersInSource(userList);
  }

  @Test
  public void testHideCategories() throws Exception {
    List<ChatChannelUser> userList1 = generateUsers(SocialStatus.OTHER, 10);
    List<ChatChannelUser> userList2 = generateUsers(SocialStatus.FRIEND, 10);
    addUsersToChannel(userList1);
    addUsersToChannel(userList2);
    runOnFxThreadAndWait(() -> instance.setChatChannel(chatChannel));

    assertContainUsersInCategory(ChatUserCategory.OTHER, userList1);
    assertContainUsersInCategory(ChatUserCategory.FRIEND, userList2);
    assertContainUsersInSource(userList1);
    assertContainUsersInSource(userList2);

    hideCategory(ChatUserCategory.OTHER);
    hideCategory(ChatUserCategory.FRIEND);

    assertNoUsersInCategory(ChatUserCategory.OTHER);
    assertNoUsersInCategory(ChatUserCategory.FRIEND);
    assertContainUsersInSource(userList1);
    assertContainUsersInSource(userList2);
  }

  @Test
  public void testShowCategory() throws Exception {
    setHiddenCategoriesToChatPrefs(ChatUserCategory.OTHER);

    List<ChatChannelUser> userList = generateUsers(SocialStatus.OTHER, 10);
    addUsersToChannel(userList);
    runOnFxThreadAndWait(() -> instance.setChatChannel(chatChannel));

    assertNoUsersInCategory(ChatUserCategory.OTHER);
    assertContainUsersInSource(userList);

    showCategory(ChatUserCategory.OTHER);

    assertContainUsersInCategory(ChatUserCategory.OTHER, userList);
    assertContainUsersInSource(userList);
  }

  @Test
  public void testShowCategories() throws Exception {
    setHiddenCategoriesToChatPrefs(ChatUserCategory.OTHER, ChatUserCategory.FRIEND);

    List<ChatChannelUser> userList1 = generateUsers(SocialStatus.OTHER, 10);
    List<ChatChannelUser> userList2 = generateUsers(SocialStatus.FRIEND, 10);
    addUsersToChannel(userList1);
    addUsersToChannel(userList2);
    runOnFxThreadAndWait(() -> instance.setChatChannel(chatChannel));

    assertNoUsersInCategory(ChatUserCategory.OTHER);
    assertNoUsersInCategory(ChatUserCategory.FRIEND);
    assertContainUsersInSource(userList1);
    assertContainUsersInSource(userList2);

    showCategory(ChatUserCategory.OTHER);
    showCategory(ChatUserCategory.FRIEND);

    assertContainUsersInCategory(ChatUserCategory.OTHER, userList1);
    assertContainUsersInCategory(ChatUserCategory.FRIEND, userList2);
    assertContainUsersInSource(userList1);
    assertContainUsersInSource(userList2);
  }

  @Test
  public void testInitializeListView() throws Exception {
    addUsersToChannel(generateUsers(SocialStatus.OTHER, 10));
    runOnFxThreadAndWait(() -> instance.setChatChannel(chatChannel));

    assertTrue(instance.userListContainer.getChildren().stream()
        .anyMatch(view -> view.getClass().isAssignableFrom(VirtualizedScrollPane.class)));
    assertFalse(instance.userListTools.isDisable());
  }

  @Test
  public void testGetRoot() {
    assertEquals(instance.root, instance.getRoot());
  }

  private void setHiddenCategoriesToChatPrefs(ChatUserCategory... hiddenCategories) {
    runOnFxThreadAndWait(() -> chatPrefs.getChannelNameToHiddenCategories()
        .put(CHANNEL_NAME, FXCollections.observableSet(hiddenCategories)));
  }

  private void hideCategory(ChatUserCategory category) throws Exception {
    ObservableMap<String, ObservableSet<ChatUserCategory>> channelNameToHiddenCategories = chatPrefs.getChannelNameToHiddenCategories();
    if (channelNameToHiddenCategories.containsKey(CHANNEL_NAME)) {
      runOnFxThreadAndWait(() -> channelNameToHiddenCategories.get(CHANNEL_NAME).add(category));
    } else {
      runOnFxThreadAndWait(() -> chatPrefs.getChannelNameToHiddenCategories()
          .put(CHANNEL_NAME, new SimpleSetProperty<>(FXCollections.observableSet(category))));
    }
  }

  private void showCategory(ChatUserCategory category) {
    runOnFxThreadAndWait(() -> chatPrefs.getChannelNameToHiddenCategories().get(CHANNEL_NAME).remove(category));
  }

  private void addUsersToChannel(ChatChannelUser... users) {
    addUsersToChannel(Arrays.asList(users));
  }

  private void addUsersToChannel(Collection<ChatChannelUser> users) {
    runOnFxThreadAndWait(() -> users.forEach(user -> chatChannel.addUser(user)));
  }

  private void removeUsersFromChannel(ChatChannelUser... users) {
    removeUsersFromChannel(Arrays.stream(users).toList());
  }

  private void removeUsersFromChannel(Collection<ChatChannelUser> users) {
    runOnFxThreadAndWait(() -> users.forEach(user -> chatChannel.removeUser(user.getUsername())));
  }

  private void assertNotContainUsersInSource(ChatChannelUser... users) throws Exception {
    assertNotContainUsersInSource(Arrays.stream(users).toList());
  }

  private void assertNotContainUsersInSource(Collection<ChatChannelUser> users) throws Exception {
    List<ChatChannelUser> userList = instance.getUserList();
    users.forEach(user -> assertFalse(userList.contains(user)));
  }

  private void assertContainUsersInSource(ChatChannelUser... users) throws Exception {
    assertContainUsersInSource(Arrays.stream(users).toList());
  }

  private void assertContainUsersInSource(Collection<ChatChannelUser> users) throws Exception {
    assertTrue(instance.getUserList().containsAll(users));
  }

  private void assertNoUsersInCategory(ChatUserCategory category) throws Exception {
    assertTrue(instance.getFilteredUserListByCategory(category).isEmpty());
  }

  private void assertNotContainUsersInCategory(ChatUserCategory category, ChatChannelUser... users) throws Exception {
    assertNotContainUsersInCategory(category, Arrays.stream(users).toList());
  }

  private void assertNotContainUsersInCategory(ChatUserCategory category,
                                               Collection<ChatChannelUser> users) throws Exception {
    List<ChatChannelUser> userList = instance.getFilteredUserListByCategory(category);
    assertTrue(users.stream().noneMatch(userList::contains));
  }

  private void assertContainUsersInCategory(ChatUserCategory category, ChatChannelUser... users) throws Exception {
    assertContainUsersInCategory(category, Arrays.stream(users).toList());
  }

  private void assertContainUsersInCategory(ChatUserCategory category,
                                            Collection<ChatChannelUser> users) throws Exception {
    assertTrue(instance.getUserListByCategory(category).containsAll(users));
  }

  private List<ChatChannelUser> generateUsers(SocialStatus status, int count) {
    ArrayList<ChatChannelUser> userList = new ArrayList<>();
    while (userList.size() != count) {
      userList.add(generateUser(status));
    }
    return userList;
  }

  private ChatChannelUser generateUser(SocialStatus socialStatus) {
    return generateUser(socialStatus, false);
  }

  private ChatChannelUser generateUser(SocialStatus socialStatus, boolean isModerator) {
    return ChatChannelUserBuilder.create(RandomStringUtils.randomAlphanumeric(15), CHANNEL_NAME)
        .moderator(isModerator).player(PlayerBeanBuilder.create().socialStatus(socialStatus).get()).get();
  }
}