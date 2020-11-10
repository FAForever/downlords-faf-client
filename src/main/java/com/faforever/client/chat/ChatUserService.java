package com.faforever.client.chat;

import com.faforever.client.chat.avatar.AvatarService;
import com.faforever.client.chat.event.ChatUserGameChangeEvent;
import com.faforever.client.chat.event.ChatUserPopulateEvent;
import com.faforever.client.clan.ClanService;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.theme.UiService;
import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.scene.image.Image;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatUserService implements InitializingBean {

  private final UiService uiService;
  private final MapService mapService;
  private final AvatarService avatarService;
  private final ClanService clanService;
  private final CountryFlagService countryFlagService;
  private final I18n i18n;
  private final EventBus eventBus;

  @Override
  public void afterPropertiesSet() {
    eventBus.register(this);
  }

  public void populateClan(ChatChannelUser chatChannelUser) {
    if (chatChannelUser.getClan().isEmpty()) {
      chatChannelUser.getPlayer().ifPresent(player -> {
        if (player.getClan() != null) {
          clanService.getClanByTag(player.getClan())
              .thenAccept(optionalClan -> Platform.runLater(() -> {
                    if (optionalClan.isPresent()) {
                      chatChannelUser.setClan(optionalClan.get());
                      chatChannelUser.setClanTag(String.format("[%s]", optionalClan.get().getTag()));
                    } else {
                      chatChannelUser.setClan(null);
                      chatChannelUser.setClanTag(null);
                    }
                  })
              );
        } else {
          chatChannelUser.setClan(null);
          chatChannelUser.setClanTag(null);
        }
      });
    }
  }

  public void populateAvatar(ChatChannelUser chatChannelUser) {
    if (chatChannelUser.getAvatar().isEmpty()) {
      chatChannelUser.getPlayer()
          .ifPresent(player -> Platform.runLater(() -> {
            if (!Strings.isNullOrEmpty(player.getAvatarUrl())) {
              chatChannelUser.setAvatar(avatarService.loadAvatar(player.getAvatarUrl()));
            } else {
              chatChannelUser.setAvatar(null);
            }
          }));
    }
  }

  public void populateCountry(ChatChannelUser chatChannelUser) {
    if (chatChannelUser.getCountryFlag().isEmpty()) {
      chatChannelUser.getPlayer()
          .ifPresent(player -> Platform.runLater(() -> {
            Optional<Image> countryFlag = countryFlagService.loadCountryFlag(player.getCountry());
            if (countryFlag.isPresent()) {
              chatChannelUser.setCountryFlag(countryFlag.get());
            } else {
              chatChannelUser.setCountryFlag(null);
            }
            chatChannelUser.setCountryName(i18n.getCountryNameLocalized(player.getCountry()));
          }));
    }
  }

  public void populateGameStatus(ChatChannelUser chatChannelUser) {
    chatChannelUser.getPlayer()
        .ifPresent(player -> Platform.runLater(() -> {
          PlayerStatus status = player.getStatus();
          Image playerStatusImage = switch (status) {
            case HOSTING -> uiService.getThemeImage(UiService.CHAT_LIST_STATUS_HOSTING);
            case LOBBYING -> uiService.getThemeImage(UiService.CHAT_LIST_STATUS_LOBBYING);
            case PLAYING -> uiService.getThemeImage(UiService.CHAT_LIST_STATUS_PLAYING);
            default -> null;
          };
          chatChannelUser.setStatusTooltipText(i18n.get(status.getI18nKey()));
          chatChannelUser.setStatusImage(playerStatusImage);

          if (status != PlayerStatus.IDLE) {
            chatChannelUser.setMapImage(mapService.loadPreview(player.getGame().getMapFolderName(), PreviewSize.SMALL));
          } else {
            chatChannelUser.setMapImage(null);
          }

          chatChannelUser.setStatus(status);
        }));
  }

  @Subscribe
  public void onChatUserPopulate(ChatUserPopulateEvent event) {
    ChatChannelUser chatChannelUser = event.getChatChannelUser();
    if (chatChannelUser.isDisplayed() && !chatChannelUser.isPopulated()) {
      chatChannelUser.setPopulated(true);
      populateAvatar(chatChannelUser);
      populateClan(chatChannelUser);
      populateCountry(chatChannelUser);
      populateGameStatus(chatChannelUser);
    } else if (!chatChannelUser.isDisplayed()) {
      chatChannelUser.setClan(null);
      chatChannelUser.setClanTag(null);
      chatChannelUser.setAvatar(null);
      chatChannelUser.setCountryFlag(null);
      chatChannelUser.setCountryName(null);
      chatChannelUser.setStatus(null);
      chatChannelUser.setMapImage(null);
      chatChannelUser.setStatusImage(null);
      chatChannelUser.setPopulated(false);
    }
  }

  @Subscribe
  public void onChatUserGameChange(ChatUserGameChangeEvent event) {
    ChatChannelUser chatChannelUser = event.getChatChannelUser();
    if (chatChannelUser.isDisplayed()) {
      populateGameStatus(chatChannelUser);
    } else if (!chatChannelUser.isDisplayed()) {
      chatChannelUser.setStatus(null);
      chatChannelUser.setMapImage(null);
      chatChannelUser.setStatusImage(null);
    }
  }
}


