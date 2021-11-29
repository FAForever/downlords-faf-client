package com.faforever.client.chat;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.chat.emojis.Emoticon;
import com.faforever.client.chat.emojis.EmoticonsGroup;
import com.faforever.client.clan.ClanService;
import com.faforever.client.domain.ClanBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.exception.ProgrammingError;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.eventbus.EventBus;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.faforever.client.chat.ChatColorMode.DEFAULT;
import static com.faforever.client.chat.ChatColorMode.RANDOM;
import static com.faforever.client.chat.ChatUserCategory.MODERATOR;
import static java.util.Locale.US;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatUserService implements InitializingBean {

  private static final ClassPathResource EMOTICONS_JSON_FILE_RESOURCE = new ClassPathResource("images/emoticons/emoticons.json");

  private final UiService uiService;
  private final MapService mapService;
  private final AvatarService avatarService;
  private final ClanService clanService;
  private final CountryFlagService countryFlagService;
  private final PreferencesService preferencesService;
  private final I18n i18n;
  private final EventBus eventBus;

  private List<EmoticonsGroup> emoticonsGroups;
  private HashMap<String, String> emoticonShortcodes;

  @Override
  public void afterPropertiesSet() {
    eventBus.register(this);
    loadAndVerifyEmoticons();
  }

  private void loadAndVerifyEmoticons() {
    try {
      emoticonsGroups = new ObjectMapper().readValue(EMOTICONS_JSON_FILE_RESOURCE.getFile(),
          TypeFactory.defaultInstance().constructCollectionType(List.class, EmoticonsGroup.class));
      emoticonShortcodes = new HashMap<>();
      List<Emoticon> emoticons = emoticonsGroups.stream().flatMap(emoticonsGroup -> emoticonsGroup.getEmoticons().stream()).collect(Collectors.toList());
      for (Emoticon emoticon : emoticons) {
        String shortcode = emoticon.getShortcode();
        if (emoticonShortcodes.containsKey(shortcode)) {
          throw new ProgrammingError("Shortcode `" + shortcode + "` is already taken");
        }
        emoticonShortcodes.put(shortcode, emoticon.getSvgFilePath());
      }
    } catch (IOException e) {
      log.error("cannot read emoticons.json file", e);
      emoticonsGroups = Collections.emptyList();
    } catch (ProgrammingError e) {
      log.error(e.getMessage(), e);
      emoticonsGroups = Collections.emptyList();
    }
    log.info("Emoticons verified and loaded");
  }

  public List<EmoticonsGroup> getEmoticonsGroups() {
    return emoticonsGroups;
  }

  public HashMap<String, String> getEmoticonShortcodes() {
    return emoticonShortcodes;
  }

  private void populateClan(ChatChannelUser chatChannelUser) {
    if (chatChannelUser.isDisplayed()) {
      chatChannelUser.getPlayer().ifPresent(player -> {
        if (player.getClan() != null) {
          clanService.getClanByTag(player.getClan())
              .thenAccept(optionalClan -> {
                ClanBean clan = optionalClan.orElse(null);
                chatChannelUser.setClan(clan);
              });
        } else {
          chatChannelUser.setClan(null);
        }
      });
    } else {
      chatChannelUser.setClan(null);
    }
  }

  private void populateAvatar(ChatChannelUser chatChannelUser) {
    if (chatChannelUser.isDisplayed()) {
      chatChannelUser.getPlayer()
          .ifPresent(player -> {
            Image avatar;
            if (player.getAvatar() != null) {
              log.debug("Fetching Avatar {}", player.getAvatar());
              avatar = avatarService.loadAvatar(player.getAvatar());
            } else {
              avatar = null;
            }
            chatChannelUser.setAvatar(avatar);
          });
    } else {
      chatChannelUser.setAvatar(null);
    }
  }

  private void populateCountry(ChatChannelUser chatChannelUser) {
    if (chatChannelUser.isDisplayed()) {
      chatChannelUser.getPlayer()
          .ifPresent(player -> {
            Optional<Image> countryFlag = countryFlagService.loadCountryFlag(player.getCountry());
            chatChannelUser.setCountryFlag(countryFlag.orElse(null));
            chatChannelUser.setCountryName(i18n.getCountryNameLocalized(player.getCountry()));
          });
    } else {
      chatChannelUser.setCountryFlag(null);
      chatChannelUser.setCountryName(null);
    }
  }

  private void populateGameImages(ChatChannelUser chatChannelUser) {
    if (chatChannelUser.isDisplayed()) {
      chatChannelUser.getPlayer()
          .ifPresent(player -> setGameImages(chatChannelUser, player));
    } else {
      chatChannelUser.setStatusTooltipText(null);
      chatChannelUser.setGameStatusImage(null);
      chatChannelUser.setMapImage(null);
    }
  }

  private void populateColor(ChatChannelUser chatChannelUser) {
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
    Optional<PlayerBean> optionalPlayer = chatChannelUser.getPlayer();
    String lowercaseUsername = chatChannelUser.getUsername().toLowerCase(US);

    Color color = null;
    if (chatPrefs.getChatColorMode() == null) {
      chatPrefs.setChatColorMode(DEFAULT);
    }

    if (chatPrefs.getChatColorMode() == DEFAULT && chatPrefs.getUserToColor().containsKey(lowercaseUsername)) {
      color = chatPrefs.getUserToColor().get(lowercaseUsername);
    } else if (chatPrefs.getChatColorMode() == DEFAULT && chatChannelUser.isModerator() && chatPrefs.getGroupToColor().containsKey(MODERATOR)) {
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

  private void setGameImages(ChatChannelUser chatChannelUser, PlayerBean player) {
    PlayerStatus status = player.getStatus();
    Image playerStatusImage = switch (status) {
      case HOSTING -> uiService.getThemeImage(UiService.CHAT_LIST_STATUS_HOSTING);
      case LOBBYING -> uiService.getThemeImage(UiService.CHAT_LIST_STATUS_LOBBYING);
      case PLAYING -> uiService.getThemeImage(UiService.CHAT_LIST_STATUS_PLAYING);
      default -> null;
    };
    Image mapImage;
    if (status != PlayerStatus.IDLE) {
      mapImage = mapService.loadPreview(player.getGame().getMapFolderName(), PreviewSize.SMALL);
    } else {
      mapImage = null;
    }
    chatChannelUser.setStatusTooltipText(i18n.get(status.getI18nKey()));
    chatChannelUser.setGameStatusImage(playerStatusImage);
    chatChannelUser.setMapImage(mapImage);
  }

  public void associatePlayerToChatUser(ChatChannelUser chatChannelUser, PlayerBean player) {
    if (player != null && chatChannelUser.getPlayer().filter(userPlayer -> userPlayer.getUsername().equals(player.getUsername())).isEmpty()) {
      chatChannelUser.setPlayer(player);
      addListeners(chatChannelUser);
      chatChannelUser.setDisplayedInvalidationListener(observable -> {
        if (chatChannelUser.isDisplayed()) {
          addListeners(chatChannelUser);
          populateGameImages(chatChannelUser);
          populateClan(chatChannelUser);
          populateCountry(chatChannelUser);
          populateAvatar(chatChannelUser);
          populateColor(chatChannelUser);
        } else {
          removeListeners(chatChannelUser);
          chatChannelUser.setStatusTooltipText(null);
          chatChannelUser.setGameStatusImage(null);
          chatChannelUser.setMapImage(null);
          chatChannelUser.setCountryFlag(null);
          chatChannelUser.setCountryName(null);
          chatChannelUser.setClan(null);
          chatChannelUser.setAvatar(null);
        }
      });
    } else if (player == null) {
      chatChannelUser.removeListeners();
      chatChannelUser.setPlayer(null);
      chatChannelUser.setStatusTooltipText(null);
      chatChannelUser.setGameStatusImage(null);
      chatChannelUser.setMapImage(null);
      chatChannelUser.setCountryFlag(null);
      chatChannelUser.setCountryName(null);
      chatChannelUser.setClan(null);
      chatChannelUser.setAvatar(null);
      populateColor(chatChannelUser);
    }
  }

  private void addListeners(ChatChannelUser chatChannelUser) {
    chatChannelUser.setAvatarChangeListener((observable, oldValue, newValue) -> {
      if (!Objects.equals(oldValue, newValue)) {
        populateAvatar(chatChannelUser);
      }
    });
    chatChannelUser.setClanTagChangeListener((observable, oldValue, newValue) -> {
      if (!Objects.equals(oldValue, newValue)) {
        populateClan(chatChannelUser);
      }
    });
    chatChannelUser.setCountryChangeListener((observable, oldValue, newValue) -> {
      if (!Objects.equals(oldValue, newValue)) {
        populateCountry(chatChannelUser);
      }
    });
    chatChannelUser.setSocialStatusChangeListener((observable, oldValue, newValue) -> {
      if (!Objects.equals(oldValue, newValue)) {
        populateColor(chatChannelUser);
      }
    });
    chatChannelUser.setGameStatusChangeListener((observable, oldValue, newValue) -> {
      if (!Objects.equals(oldValue, newValue)) {
        populateGameImages(chatChannelUser);
      }
    });
  }

  private void removeListeners(ChatChannelUser chatChannelUser) {
    chatChannelUser.setAvatarChangeListener(null);
    chatChannelUser.setClanTagChangeListener(null);
    chatChannelUser.setCountryChangeListener(null);
    chatChannelUser.setSocialStatusChangeListener(null);
    chatChannelUser.setGameStatusChangeListener(null);
  }
}


