package com.faforever.client.chat;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import com.google.common.eventbus.EventBus;
import javafx.scene.paint.Color;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static com.faforever.client.chat.ChatColorMode.DEFAULT;
import static com.faforever.client.chat.ChatColorMode.RANDOM;
import static com.faforever.client.chat.ChatUserCategory.MODERATOR;
import static java.util.Locale.US;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatUserService implements InitializingBean {

  private final UiService uiService;
  private final MapService mapService;
  private final AvatarService avatarService;
  private final CountryFlagService countryFlagService;
  private final PreferencesService preferencesService;
  private final I18n i18n;
  private final EventBus eventBus;

  @Override
  public void afterPropertiesSet() {
    eventBus.register(this);
  }

  public void populateColor(ChatChannelUser chatChannelUser) {
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
    Optional<PlayerBean> optionalPlayer = chatChannelUser.getPlayer();
    String lowercaseUsername = chatChannelUser.getUsername().toLowerCase(US);

    Color color = null;
    if (chatPrefs.getChatColorMode() == null) {
      chatPrefs.setChatColorMode(DEFAULT);
    }

    if (chatPrefs.getChatColorMode() == DEFAULT && chatPrefs.getUserToColor().containsKey(lowercaseUsername)) {
      color = chatPrefs.getUserToColor().get(lowercaseUsername);
    } else if (chatPrefs.getChatColorMode() == DEFAULT && chatChannelUser.isModerator() && chatPrefs.getGroupToColor()
        .containsKey(MODERATOR)) {
      color = chatPrefs.getGroupToColor().get(MODERATOR);
    } else if (chatPrefs.getChatColorMode() == DEFAULT && optionalPlayer.isPresent()) {
      ChatUserCategory chatUserCategory = optionalPlayer.map(player -> switch (player.getSocialStatus()) {
        case FRIEND -> ChatUserCategory.FRIEND;
        case FOE -> ChatUserCategory.FOE;
        default -> ChatUserCategory.OTHER;
      }).orElse(ChatUserCategory.CHAT_ONLY);
      color = chatPrefs.getGroupToColor().get(chatUserCategory);
    } else if (chatPrefs.getChatColorMode() == RANDOM) {
      color = ColorGeneratorUtil.generateRandomColor(lowercaseUsername.hashCode());
    }
    chatChannelUser.setColor(color);
  }

  public void bindChatUserPlayerProperties(ChatChannelUser chatChannelUser) {

    chatChannelUser.avatarProperty()
        .bind(chatChannelUser.playerProperty().flatMap(PlayerBean::avatarProperty).map(avatarService::loadAvatar));

    chatChannelUser.countryFlagProperty()
        .bind(chatChannelUser.playerProperty()
            .flatMap(PlayerBean::countryProperty)
            .map(countryFlagService::loadCountryFlag)
            .map(possibleFlag -> possibleFlag.orElse(null)));

    chatChannelUser.countryNameProperty()
        .bind(chatChannelUser.playerProperty()
            .flatMap(PlayerBean::countryProperty)
            .map(i18n::getCountryNameLocalized));

    chatChannelUser.gameStatusImageProperty()
        .bind(chatChannelUser.playerProperty().flatMap(PlayerBean::statusProperty).map(status -> switch (status) {
          case HOSTING -> uiService.getThemeImage(UiService.CHAT_LIST_STATUS_HOSTING);
          case LOBBYING -> uiService.getThemeImage(UiService.CHAT_LIST_STATUS_LOBBYING);
          case PLAYING -> uiService.getThemeImage(UiService.CHAT_LIST_STATUS_PLAYING);
          default -> null;
        }));

    chatChannelUser.mapImageProperty()
        .bind(chatChannelUser.playerProperty()
            .flatMap(PlayerBean::gameProperty)
            .flatMap(GameBean::mapFolderNameProperty)
            .map(mapFolderName -> mapService.loadPreview(mapFolderName, PreviewSize.SMALL)));

    chatChannelUser.statusTooltipTextProperty()
        .bind(chatChannelUser.playerProperty()
            .flatMap(PlayerBean::statusProperty)
            .map(PlayerStatus::getI18nKey)
            .map(i18n::get));
  }
}


