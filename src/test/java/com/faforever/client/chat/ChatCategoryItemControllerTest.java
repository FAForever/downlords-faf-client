package com.faforever.client.chat;

import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.MouseEvents;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.fx.contextmenu.helper.ContextMenuBuilderHelper;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyDouble;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChatCategoryItemControllerTest extends UITest {

  private static final String CHANNEL_NAME = "#testChannel";

  @Mock
  private I18n i18n;
  @Mock
  private UiService uiService;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private ContextMenuBuilder contextMenuBuilder;

  private ChatPrefs chatPrefs;

  @InjectMocks
  private ChatCategoryItemController instance;

  @BeforeEach
  public void setUp() throws Exception {
    Preferences preferences = PreferencesBuilder.create().defaultValues().chatPrefs().then().get();
    chatPrefs = preferences.getChat();

    loadFxml("theme/chat/chat_user_category.fxml", clazz -> instance);
  }

  @Test
  public void testSetCategoryText() {
    when(i18n.get(ChatUserCategory.OTHER.getI18nKey())).thenReturn("other");

    instance.setDetails(ChatUserCategory.OTHER, CHANNEL_NAME, null);
    assertEquals("other", instance.categoryLabel.getText());
  }

  @Test
  public void testCheckCategoryColorListener() {
    instance.setDetails(ChatUserCategory.OTHER, CHANNEL_NAME, null);
    assertTrue(StringUtils.isBlank(instance.categoryLabel.getStyle()));

    chatPrefs.getGroupToColor().put(ChatUserCategory.OTHER, Color.RED);
    assertTrue(StringUtils.contains(instance.categoryLabel.getStyle(), JavaFxUtil.toRgbCode(Color.RED)));

    chatPrefs.getGroupToColor().remove(ChatUserCategory.OTHER);
    assertTrue(StringUtils.isBlank(instance.categoryLabel.getStyle()));
  }

  @Test
  public void testCheckCategoryColorAlreadyInitialized() {
    chatPrefs.getGroupToColor().put(ChatUserCategory.OTHER, Color.RED);
    instance.setDetails(ChatUserCategory.OTHER, CHANNEL_NAME, null);
    assertTrue(StringUtils.contains(instance.categoryLabel.getStyle(), JavaFxUtil.toRgbCode(Color.RED)));
  }

  @Test
  public void testOnContextMenuRequested() {
    runOnFxThreadAndWait(() -> getRoot().getChildren().add(instance.getRoot()));
    runOnFxThreadAndWait(() -> instance.setDetails(ChatUserCategory.OTHER, CHANNEL_NAME, null));
    ContextMenu contextMenuMock = ContextMenuBuilderHelper.mockContextMenuBuilderAndGetContextMenuMock(contextMenuBuilder);

    runOnFxThreadAndWait(() -> instance.onContextMenuRequested(mock(ContextMenuEvent.class)));
    verify(contextMenuMock).show(eq(instance.getRoot().getScene().getWindow()), anyDouble(), anyDouble());
  }

  @Test
  public void testArrowLabelWhenCategoryHidden() {
    setHiddenCategoryToPrefs(ChatUserCategory.OTHER);
    instance.setDetails(ChatUserCategory.OTHER, CHANNEL_NAME, null);
    assertEquals("˃", instance.arrowLabel.getText());
  }

  @Test
  public void testArrowLabelWhenCategoryVisible() {
    instance.setDetails(ChatUserCategory.OTHER, CHANNEL_NAME, null);
    assertEquals("˅", instance.arrowLabel.getText());
  }

  @Test
  public void testOnCategoryClickedWhenCategoryVisible() {
    instance.setDetails(ChatUserCategory.OTHER, CHANNEL_NAME, null);
    instance.onCategoryClicked(MouseEvents.generateClick(MouseButton.PRIMARY, 1));
    assertTrue(chatPrefs.getChannelNameToHiddenCategories().get(CHANNEL_NAME).contains(ChatUserCategory.OTHER));
    assertEquals("˃", instance.arrowLabel.getText());
  }

  @Test
  public void testOnCategoryClickedWhenCategoryHidden() {
    setHiddenCategoryToPrefs(ChatUserCategory.OTHER);
    instance.setDetails(ChatUserCategory.OTHER, CHANNEL_NAME, null);
    instance.onCategoryClicked(MouseEvents.generateClick(MouseButton.PRIMARY, 1));
    assertFalse(chatPrefs.getChannelNameToHiddenCategories().containsKey(CHANNEL_NAME));
    assertEquals("˅", instance.arrowLabel.getText());
  }

  @Test
  public void testOnCategoryClickedWhenCategoryVisibleAndOtherCategoryHidden() {
    setHiddenCategoryToPrefs(ChatUserCategory.MODERATOR);
    instance.setDetails(ChatUserCategory.OTHER, CHANNEL_NAME, null);
    instance.onCategoryClicked(MouseEvents.generateClick(MouseButton.PRIMARY, 1));
    assertTrue(chatPrefs.getChannelNameToHiddenCategories().get(CHANNEL_NAME).contains(ChatUserCategory.OTHER));
    assertEquals("˃", instance.arrowLabel.getText());
  }

  @Test
  public void testOnCategoryClickedWhenCategoryHiddenAndOtherCategoryHidden() {
    setHiddenCategoryToPrefs(ChatUserCategory.MODERATOR);
    setHiddenCategoryToPrefs(ChatUserCategory.OTHER);
    instance.setDetails(ChatUserCategory.OTHER, CHANNEL_NAME, null);
    instance.onCategoryClicked(MouseEvents.generateClick(MouseButton.PRIMARY, 1));
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