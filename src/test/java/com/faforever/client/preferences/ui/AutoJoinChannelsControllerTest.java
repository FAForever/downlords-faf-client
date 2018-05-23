package com.faforever.client.preferences.ui;

import com.faforever.client.chat.OfficialLanguageChannel;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class AutoJoinChannelsControllerTest extends AbstractPlainJavaFxTest {

  private AutoJoinChannelsController instance;
  @Mock
  private PreferencesService preferenceService;
  @Mock
  private Preferences preferences;

  @Before
  public void setUp() throws Exception {
    preferences = new Preferences();
    when(preferenceService.getPreferences()).thenReturn(preferences);

    instance = new AutoJoinChannelsController(preferenceService);
    loadFxml("theme/settings/auto_join_channels.fxml", param -> instance);
  }


  @Test
  public void testOnAddChannelButtonPressed() {
    preferences.getChat().getAutoJoinChannels().clear();
    instance.channelTextField.setText("#newbie");
    instance.onAddChannelButtonPressed();
    List<String> expected = Collections.singletonList("#newbie");
    assertThat(preferences.getChat().getAutoJoinChannels(), is(expected));
  }

  @Test
  public void testLanguageChannels() throws Exception {
    for (OfficialLanguageChannel channel : OfficialLanguageChannel.values()) {
      Locale.setDefault(new Locale(channel.getLocaleLanguage()));
      preferences = new Preferences();
      when(preferenceService.getPreferences()).thenReturn(preferences);

      List<String> expected = Collections.singletonList(channel.getChannelName());
      assertThat(preferences.getChat().getAutoJoinChannels(), is(expected));

      instance = new AutoJoinChannelsController(preferenceService);
      loadFxml("theme/settings/auto_join_channels.fxml", param -> instance);
      assertThat(instance.channelListView.getItems(), is(expected));
    }
  }

  @Test
  public void testOnChannelSelectedAndRemoved() {
    preferences.getChat().getAutoJoinChannels().clear();
    preferences.getChat().getAutoJoinChannels().add("#newbie");
    assertFalse(preferences.getChat().getAutoJoinChannels().isEmpty());
    assertFalse(instance.channelListView.getItems().isEmpty());

    instance.channelListView.getSelectionModel().select("#newbie");
    instance.removeButton.fire();

    assertTrue(preferences.getChat().getAutoJoinChannels().isEmpty());
    assertTrue(instance.channelListView.getItems().isEmpty());
  }

}
