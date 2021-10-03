package com.faforever.client.chat;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.chat.event.ChatUserColorChangeEvent;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlayerContextMenuController;
import com.faforever.client.game.JoinGameHelper;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.moderator.ModeratorService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.teammatchmaking.TeamMatchmakingService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.Assert;
import com.faforever.client.util.ClipboardUtil;
import com.faforever.commons.api.dto.GroupPermission;
import com.google.common.eventbus.EventBus;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.event.ActionEvent;
import javafx.scene.control.TextInputDialog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Set;

import static com.faforever.client.chat.ChatColorMode.RANDOM;
import static com.faforever.client.player.SocialStatus.SELF;
import static java.util.Locale.US;

@Slf4j
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class ChatUserContextMenuController extends PlayerContextMenuController {

  private final PreferencesService preferencesService;
  private final TeamMatchmakingService teamMatchmakingService;

  private ChatChannelUser chatUser;
  private InvalidationListener playerPropertyInvalidationListener;
  private InvalidationListener socialStatusPropertyInvalidationListener;

  public ChatUserContextMenuController(PlayerService playerService,
                                       EventBus eventBus,
                                       UiService uiService,
                                       PreferencesService preferencesService,
                                       ReplayService replayService,
                                       NotificationService notificationService,
                                       I18n i18n,
                                       JoinGameHelper joinGameHelper,
                                       AvatarService avatarService,
                                       ModeratorService moderatorService,
                                       TeamMatchmakingService teamMatchmakingService) {
    super(avatarService, eventBus, i18n, joinGameHelper, moderatorService, notificationService, playerService, replayService, uiService);

    this.preferencesService = preferencesService;
    this.teamMatchmakingService = teamMatchmakingService;
  }

  @Override
  public void initialize() {
    super.initialize();
    sendPrivateMessageItem.setOnAction(event -> onSendPrivateMessageSelected());
    removeCustomColorButton.setOnAction(event -> onRemoveCustomColor());
    inviteItem.setOnAction(event -> onInviteToGameSelected());
    broadcastMessage.setOnAction(this::onBroadcastMessage);

    JavaFxUtil.bindManagedToVisible(removeCustomColorButton);
  }

  @Override
  protected void initializeListener() {
    super.initializeListener();
    playerPropertyInvalidationListener = observable -> chatUser.getPlayer().ifPresent(super::setPlayer);
    socialStatusPropertyInvalidationListener = observable -> {
      SocialStatus socialStatus = chatUser.getSocialStatus().orElse(null);
      JavaFxUtil.runLater(() -> sendPrivateMessageItem.setVisible(socialStatus != SELF));
    };
  }

  @Override
  protected void setItemVisibility(SocialStatus socialStatus, PlayerStatus playerStatus, GameBean game) {
    super.setItemVisibility(socialStatus, playerStatus, game);
    JavaFxUtil.runLater(() -> inviteItem.setVisible(socialStatus != SELF && playerStatus == PlayerStatus.IDLE));
  }

  @Override
  protected void setModeratorOptions(Set<String> permissions, PlayerBean player) {
    super.setModeratorOptions(permissions, player);
    JavaFxUtil.runLater(() -> broadcastMessage.setVisible(permissions.contains(GroupPermission.ROLE_WRITE_MESSAGE)));
  }

  public void setChatUser(ChatChannelUser chatUser) {
    Assert.checkNotNullIllegalState(this.chatUser, "Chat User already set");
    this.chatUser = chatUser;
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();

    String lowerCaseUsername = chatUser.getUsername().toLowerCase(US);
    colorPicker.setValue(chatPrefs.getUserToColor().getOrDefault(lowerCaseUsername, null));

    colorPicker.valueProperty().addListener((observable, oldValue, newValue) -> {
      String lowerUsername = chatUser.getUsername().toLowerCase(US);
      ChatUserCategory userCategory;
      if (chatUser.isModerator()) {
        userCategory = ChatUserCategory.MODERATOR;
      } else {
        userCategory = chatUser.getSocialStatus().map(status -> switch (status) {
          case FRIEND -> ChatUserCategory.FRIEND;
          case FOE -> ChatUserCategory.FOE;
          default -> ChatUserCategory.OTHER;
        }).orElse(ChatUserCategory.OTHER);
      }
      if (newValue == null) {
        chatPrefs.getUserToColor().remove(lowerUsername);
        chatUser.setColor(chatPrefs.getGroupToColor().getOrDefault(userCategory, null));
      } else {
        chatPrefs.getUserToColor().put(lowerUsername, newValue);
        chatUser.setColor(newValue);
      }
      eventBus.post(new ChatUserColorChangeEvent(chatUser));
    });

    removeCustomColorButton.visibleProperty().bind(chatPrefs.chatColorModeProperty().isNotEqualTo(RANDOM)
        .and(colorPicker.valueProperty().isNotNull()));
    colorPickerMenuItem.visibleProperty().bind(chatPrefs.chatColorModeProperty().isNotEqualTo(RANDOM));

    WeakInvalidationListener weakPlayerPropertyListener = new WeakInvalidationListener(playerPropertyInvalidationListener);
    WeakInvalidationListener weakSocialStatusPropertyListener = new WeakInvalidationListener(socialStatusPropertyInvalidationListener);
    JavaFxUtil.addAndTriggerListener(chatUser.playerProperty(), weakPlayerPropertyListener);
    JavaFxUtil.addAndTriggerListener(chatUser.socialStatusProperty(), weakSocialStatusPropertyListener);
  }

  public void onSendPrivateMessageSelected() {
    eventBus.post(new InitiatePrivateChatEvent(chatUser.getUsername()));
  }

  @Override
  public void onCopyUsernameSelected() {
    ClipboardUtil.copyToClipboard(chatUser.getUsername());
  }

  public void onInviteToGameSelected() {
    teamMatchmakingService.invitePlayer(super.player.getUsername());
  }

  public void onBroadcastMessage(ActionEvent actionEvent) {
    actionEvent.consume();

    TextInputDialog broadcastMessageInputDialog = new TextInputDialog();
    broadcastMessageInputDialog.setTitle(i18n.get("chat.userContext.broadcast"));

    broadcastMessageInputDialog.showAndWait()
        .ifPresent(broadcastMessage -> {
              if (broadcastMessage.isBlank()) {
                log.error("Broadcast message is empty: {}", broadcastMessage);
              } else {
                log.info("Sending broadcast message: {}", broadcastMessage);
                moderatorService.broadcastMessage(broadcastMessage);
              }
            }
        );
  }

  public void onRemoveCustomColor() {
    colorPicker.setValue(null);
  }
}
