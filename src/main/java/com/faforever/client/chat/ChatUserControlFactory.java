package com.faforever.client.chat;

import com.faforever.client.legacy.domain.PlayerInfo;

public interface ChatUserControlFactory {

  ChatUserControl createChatUserControl(PlayerInfoBean playerInfoBean);
}
