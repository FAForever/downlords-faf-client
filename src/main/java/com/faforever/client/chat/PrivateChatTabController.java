package com.faforever.client.chat;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.player.PrivatePlayerInfoController;
import javafx.beans.value.ObservableValue;
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

  public Tab privateChatTabRoot;
  public ImageView avatarImageView;
  public Region defaultIconImageView;
  public Node privatePlayerInfo;
  public PrivatePlayerInfoController privatePlayerInfoController;
  public ScrollPane gameDetailScrollPane;

  @Autowired
  public PrivateChatTabController(ChatService chatService, AvatarService avatarService) {
    super(chatService);
    this.avatarService = avatarService;
  }

  @Override
  protected void onInitialize() {
    super.onInitialize();

    JavaFxUtil.bindManagedToVisible(avatarImageView, defaultIconImageView);

    chatMessagesViewController.chatChannelProperty().bind(chatChannel);
    avatarImageView.visibleProperty().bind(avatarImageView.imageProperty().isNotNull().when(showing));
    defaultIconImageView.visibleProperty().bind(avatarImageView.imageProperty().isNull().when(showing));

    privateChatTabRoot.textProperty().bind(channelName.when(attached));

    ObservableValue<ChatChannelUser> chatUser = chatChannel.flatMap(
        channel -> channelName.map(chanName -> channel.getUser(chanName).orElse(null)));
    privatePlayerInfoController.chatUserProperty().bind(chatUser.when(showing));

    avatarImageView.imageProperty().bind(chatUser
                                             .flatMap(ChatChannelUser::playerProperty)
                                             .flatMap(PlayerInfo::avatarProperty)
                                             .map(avatarService::loadAvatar).when(showing));
  }

  @Override
  public Tab getRoot() {
    return privateChatTabRoot;
  }
}
