package com.faforever.client.chat;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.player.PrivatePlayerInfoController;
import com.faforever.client.preferences.ChatPrefs;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PrivateChatTabController extends AbstractChatTabController {

  private final AvatarService avatarService;
  private final ChatPrefs chatPrefs;

  public Tab privateChatTabRoot;
  public ImageView avatarImageView;
  public Region defaultIconImageView;
  public Node privatePlayerInfo;
  public PrivatePlayerInfoController privatePlayerInfoController;
  public ScrollPane gameDetailScrollPane;

  @Autowired
  public PrivateChatTabController(ChatService chatService, AvatarService avatarService, ChatPrefs chatPrefs) {
    super(chatService);
    this.avatarService = avatarService;
    this.chatPrefs = chatPrefs;
  }

  @Override
  protected void onInitialize() {
    super.onInitialize();

    JavaFxUtil.bindManagedToVisible(avatarImageView, defaultIconImageView);

    chatMessagesViewController.chatChannelProperty().bind(chatChannel);
    avatarImageView.visibleProperty().bind(avatarImageView.imageProperty().isNotNull().when(showing));
    defaultIconImageView.visibleProperty().bind(avatarImageView.imageProperty().isNull().when(showing));

    privateChatTabRoot.textProperty().bind(channelName.when(attached));
    privatePlayerInfoController.chatUserProperty()
        .bind(chatChannel.map(channel -> chatService.getOrCreateChatUser(channel.getName(), channel.getName()))
                         .when(showing));

    avatarImageView.imageProperty()
                   .bind(chatChannel.flatMap(
                       channel -> channelName.map(chanName -> channel.getUser(chanName).orElse(null))
                                             .flatMap(ChatChannelUser::playerProperty)
                                             .flatMap(PlayerBean::avatarProperty)
                                             .map(avatarService::loadAvatar)
                                             .when(showing)));
  }

  @Override
  public Tab getRoot() {
    return privateChatTabRoot;
  }

  @Override
  public void onChatMessage(ChatMessage chatMessage) {
    if (chatMessage.sender().getCategory() == ChatUserCategory.FOE && chatPrefs.isHideFoeMessages()) {
      return;
    }

    if (!hasFocus()) {
      setUnread(true);
      incrementUnreadMessagesCount();
    }
  }
}
