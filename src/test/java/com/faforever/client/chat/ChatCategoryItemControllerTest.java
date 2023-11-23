package com.faforever.client.chat;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.MouseEvents;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class ChatCategoryItemControllerTest extends PlatformTest {

  private static final String CHANNEL_NAME = "#testChannel";

  @Mock
  private I18n i18n;
  @Mock
  private UiService uiService;

  @Mock
  private ContextMenuBuilder contextMenuBuilder;
  @Spy
  private ChatPrefs chatPrefs;

  @InjectMocks
  private ChatCategoryItemController instance;

  @BeforeEach
  public void setUp() throws Exception {
    loadFxml("theme/chat/chat_user_category.fxml", clazz -> instance);
  }

  @Test
  public void testSetCategoryText() {
    when(i18n.get(ChatUserCategory.OTHER.getI18nKey())).thenReturn("other");

    instance.setChatUserCategory(ChatUserCategory.OTHER);
    instance.setChannelName(CHANNEL_NAME);

    assertEquals("other", instance.categoryLabel.getText());
  }

  @Test
  public void testCheckCategoryColorListener() {
    instance.setChatUserCategory(ChatUserCategory.OTHER);
    instance.setChannelName(CHANNEL_NAME);
    assertTrue(StringUtils.isBlank(instance.categoryLabel.getStyle()));

    chatPrefs.getGroupToColor().put(ChatUserCategory.OTHER, Color.RED);
    assertTrue(StringUtils.contains(instance.categoryLabel.getStyle(), JavaFxUtil.toRgbCode(Color.RED)));

    chatPrefs.getGroupToColor().remove(ChatUserCategory.OTHER);
    assertTrue(StringUtils.isBlank(instance.categoryLabel.getStyle()));
  }

  @Test
  public void testCheckCategoryColorAlreadyInitialized() {
    chatPrefs.getGroupToColor().put(ChatUserCategory.OTHER, Color.RED);
    runOnFxThreadAndWait(() -> {
      instance.setChatUserCategory(ChatUserCategory.OTHER);
      instance.setChannelName(CHANNEL_NAME);
    });
    assertTrue(StringUtils.contains(instance.categoryLabel.getStyle(), JavaFxUtil.toRgbCode(Color.RED)));
  }

//  @Disabled("UI")
//  @Test
//  public void testOnContextMenuRequested() {
//    runOnFxThreadAndWait(() -> {
//      instance.setChatUserCategory(ChatUserCategory.OTHER);
//      instance.setChannelName(CHANNEL_NAME);
//    });
//    ContextMenu contextMenuMock = ContextMenuBuilderHelper.mockContextMenuBuilderAndGetContextMenuMock(contextMenuBuilder);
//
//    runOnFxThreadAndWait(() -> {
//      getRoot().getChildren().add(instance.getRoot());
//      instance.onContextMenuRequested(mock(ContextMenuEvent.class));
//    });
//    verify(contextMenuMock).show(eq(instance.getRoot().getScene().getWindow()), anyDouble(), anyDouble());
//  }

  @Test
  public void testArrowLabelWhenCategoryHidden() {
    setHiddenCategoryToPrefs(ChatUserCategory.OTHER);
    instance.setChatUserCategory(ChatUserCategory.OTHER);
    instance.setChannelName(CHANNEL_NAME);
    assertEquals("˃", instance.arrowLabel.getText());
  }

  @Test
  public void testArrowLabelWhenCategoryVisible() {
    instance.setChatUserCategory(ChatUserCategory.OTHER);
    instance.setChannelName(CHANNEL_NAME);
    assertEquals("˅", instance.arrowLabel.getText());
  }

  @Test
  public void testOnCategoryClickedWhenCategoryVisible() {
    instance.setChatUserCategory(ChatUserCategory.OTHER);
    instance.setChannelName(CHANNEL_NAME);
    instance.onCategoryClicked(MouseEvents.generateClick(MouseButton.PRIMARY, 1));
    assertTrue(chatPrefs.getChannelNameToHiddenCategories().get(CHANNEL_NAME).contains(ChatUserCategory.OTHER));
    assertEquals("˃", instance.arrowLabel.getText());
  }

  @Test
  public void testOnCategoryClickedWhenCategoryHidden() {
    setHiddenCategoryToPrefs(ChatUserCategory.OTHER);
    instance.setChatUserCategory(ChatUserCategory.OTHER);
    instance.setChannelName(CHANNEL_NAME);
    instance.onCategoryClicked(MouseEvents.generateClick(MouseButton.PRIMARY, 1));
    assertFalse(chatPrefs.getChannelNameToHiddenCategories().containsKey(CHANNEL_NAME));
    assertEquals("˅", instance.arrowLabel.getText());
  }

  @Test
  public void testOnCategoryClickedWhenCategoryVisibleAndOtherCategoryHidden() {
    setHiddenCategoryToPrefs(ChatUserCategory.MODERATOR);
    instance.setChatUserCategory(ChatUserCategory.OTHER);
    instance.setChannelName(CHANNEL_NAME);
    instance.onCategoryClicked(MouseEvents.generateClick(MouseButton.PRIMARY, 1));
    assertTrue(chatPrefs.getChannelNameToHiddenCategories().get(CHANNEL_NAME).contains(ChatUserCategory.OTHER));
    assertEquals("˃", instance.arrowLabel.getText());
  }

  @Test
  public void testOnCategoryClickedWhenCategoryHiddenAndOtherCategoryHidden() {
    setHiddenCategoryToPrefs(ChatUserCategory.MODERATOR);
    setHiddenCategoryToPrefs(ChatUserCategory.OTHER);
    runOnFxThreadAndWait(() -> {
      instance.setChatUserCategory(ChatUserCategory.OTHER);
      instance.setChannelName(CHANNEL_NAME);
      instance.onCategoryClicked(MouseEvents.generateClick(MouseButton.PRIMARY, 1));
    });
    assertFalse(chatPrefs.getChannelNameToHiddenCategories().get(CHANNEL_NAME).contains(ChatUserCategory.OTHER));
    assertEquals("˅", instance.arrowLabel.getText());
  }

  @Test
  public void testGetRoot() {
    assertNotNull(instance.getRoot());
  }

  private void setHiddenCategoryToPrefs(ChatUserCategory category) {
    ObservableSet<ChatUserCategory> hiddenCategories = chatPrefs.getChannelNameToHiddenCategories()
        .computeIfAbsent(CHANNEL_NAME, name -> FXCollections.observableSet());
    hiddenCategories.add(category);
  }
}