package com.faforever.client.game;

import com.faforever.client.legacy.OnGameInfoListener;
import com.faforever.client.util.Callback;
import javafx.collections.MapChangeListener;

import java.util.List;

public interface GameService {

  void publishPotentialPlayer();

  void addOnGameInfoListener(OnGameInfoListener listener);

  void hostGame(NewGameInfo name, Callback<Void> callback);

  void cancelLadderSearch();

  void joinGame(GameInfoBean gameInfoBean, String password, Callback<Void> callback);

  List<GameTypeBean> getGameTypes();

  void addOnGameTypeInfoListener(MapChangeListener<String, GameTypeBean> changeListener);
}
