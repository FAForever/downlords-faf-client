package com.faforever.client.chat;

import com.faforever.client.chat.avatar.AvatarService;
import com.faforever.client.clan.ClanService;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.player.Player;
import com.faforever.client.theme.UiService;
import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import javafx.application.Platform;
import javafx.beans.WeakInvalidationListener;
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
    if (chatChannelUser.isDisplayed()) {
      if (chatChannelUser.getClan().isEmpty()) {
        chatChannelUser.getPlayer().ifPresent(player -> {
          if (player.getClan() != null) {
            clanService.getClanByTag(player.getClan())
                .thenAccept(optionalClan -> Platform.runLater(() -> {
                      if (optionalClan.isPresent()) {
                        chatChannelUser.setClan(optionalClan.get());
                      } else {
                        chatChannelUser.setClan(null);
                      }
                    })
                );
          } else {
            chatChannelUser.setClan(null);
          }
        });
      }
    } else {
      chatChannelUser.setClan(null);
    }
  }

  public void populateAvatar(ChatChannelUser chatChannelUser) {
    if (chatChannelUser.isDisplayed()) {
      if (chatChannelUser.getAvatar().isEmpty()) {
        chatChannelUser.getPlayer()
            .ifPresent(player -> {
              Image avatar;
              if (!Strings.isNullOrEmpty(player.getAvatarUrl())) {
                avatar = avatarService.loadAvatar(player.getAvatarUrl());
              } else {
                avatar = null;
              }
              Platform.runLater(() -> {
                chatChannelUser.setAvatar(avatar);
              });
            });
      }
    } else {
      chatChannelUser.setAvatar(null);
    }
  }

  public void populateCountry(ChatChannelUser chatChannelUser) {
    if (chatChannelUser.isDisplayed()) {
      if (chatChannelUser.getCountryFlag().isEmpty()) {
        chatChannelUser.getPlayer()
            .ifPresent(player -> {
              Optional<Image> countryFlag = countryFlagService.loadCountryFlag(player.getCountry());
              Platform.runLater(() -> {
                chatChannelUser.setCountryFlag(countryFlag.orElse(null));
                chatChannelUser.setCountryName(i18n.getCountryNameLocalized(player.getCountry()));
              });
            });
      }
    } else {
      chatChannelUser.setCountryFlag(null);
      chatChannelUser.setCountryName(null);
    }
  }

  public void populateGameStatus(ChatChannelUser chatChannelUser) {
    if (chatChannelUser.isDisplayed()) {
      chatChannelUser.getPlayer()
          .ifPresent(player -> {
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
            Platform.runLater(() -> {
              chatChannelUser.setStatusTooltipText(i18n.get(status.getI18nKey()));
              chatChannelUser.setGameStatusImage(playerStatusImage);
              chatChannelUser.setMapImage(mapImage);
            });
          });
    } else {
      chatChannelUser.setStatusTooltipText(null);
      chatChannelUser.setGameStatusImage(null);
      chatChannelUser.setMapImage(null);
    }
  }

  public void associatePlayerToChatUser(ChatChannelUser chatChannelUser, Player player) {
    if (player != null) {
      chatChannelUser.setPlayer(player);
      player.getChatChannelUsers().add(chatChannelUser);
      populateGameStatus(chatChannelUser);
      populateClan(chatChannelUser);
      populateCountry(chatChannelUser);
      populateAvatar(chatChannelUser);
      JavaFxUtil.addListener(chatChannelUser.populatedProperty(),
          (observable) -> {
            populateGameStatus(chatChannelUser);
            populateClan(chatChannelUser);
            populateCountry(chatChannelUser);
            populateAvatar(chatChannelUser);
          });
      JavaFxUtil.addListener(chatChannelUser.gameStatusProperty(),
          new WeakInvalidationListener((observable) -> populateGameStatus(chatChannelUser)));
      JavaFxUtil.addListener(chatChannelUser.clanTagProperty(),
          new WeakInvalidationListener((observable) -> populateClan(chatChannelUser)));
      JavaFxUtil.addListener(player.avatarUrlProperty(),
          new WeakInvalidationListener((observable) -> populateAvatar(chatChannelUser)));
      JavaFxUtil.addListener(player.countryProperty(),
          new WeakInvalidationListener((observable) -> populateCountry(chatChannelUser)));
    }
  }
}


