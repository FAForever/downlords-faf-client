package com.faforever.client.chat;

import com.faforever.client.domain.AbstractEntityBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import javafx.beans.property.SetProperty;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MuteService implements InitializingBean {

  private final PreferencesService preferencesService;
  private final ChatService chatService;

  private ObservableSet<Integer> mutedUserIds;

  @Override
  public void afterPropertiesSet() throws Exception {
    mutedUserIds = preferencesService.getPreferences().getChat().getMutedUserIds();

    JavaFxUtil.addListener(mutedUserIds, (SetChangeListener<Integer>) observable -> preferencesService.storeInBackground());
  }

  public ObservableSet<Integer> getMutedUserIds() {
    return mutedUserIds;
  }

  public boolean isPossibleToMuteUser(String username) {
    return getPlayer(username).isPresent();
  }

  public boolean isUserMuted(String username) {
    return mutedUserIds.contains(getPlayerId(username));
  }

  public void unmuteUser(String username) {
    mutedUserIds.remove(getPlayerId(username));
  }

  public void muteUser(String username) {
    mutedUserIds.add(getPlayerId(username));
  }

  private Integer getPlayerId(String username) {
    return getPlayer(username).map(AbstractEntityBean::getId).orElse(null);
  }

  private Optional<PlayerBean> getPlayer(String username) {
    return chatService.getOrCreateChatUser(username, username, false).getPlayer();
  }
}
