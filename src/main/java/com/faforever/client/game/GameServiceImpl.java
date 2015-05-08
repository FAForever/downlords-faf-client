package com.faforever.client.game;

import com.faforever.client.legacy.ServerAccessor;
import com.faforever.client.legacy.domain.GameLaunchInfo;
import com.faforever.client.legacy.OnGameInfoListener;
import com.faforever.client.supcom.ForgedAllianceService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.Callback;
import com.faforever.client.util.ConcurrentUtil;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameServiceImpl implements GameService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  ServerAccessor serverAccessor;

  @Autowired
  UserService userService;

  @Autowired
  ForgedAllianceService forgedAllianceService;

  @Override
  public void publishPotentialPlayer() {
    String username = userService.getUsername();
//    serverAccessor.publishPotentialPlayer(username);
  }

  @Override
  public void addOnGameInfoListener(OnGameInfoListener listener) {
    serverAccessor.addOnGameInfoMessageListener(listener);
  }

  @Override
  public void hostGame(NewGameInfo newGameInfo, Callback<Void> callback) {
    serverAccessor.requestNewGame(newGameInfo, gameLaunchCallback(callback));
  }

  @Override
  public void joinGame(GameInfoBean gameInfoBean, Callback<Void> callback) {
    // FIXME implement
    String password = "test";
    serverAccessor.requestJoinGame(gameInfoBean, password, gameLaunchCallback(callback));
  }

    private Callback<GameLaunchInfo> gameLaunchCallback(final Callback<Void> callback) {
    return new Callback<GameLaunchInfo>() {
      @Override
      public void success(GameLaunchInfo gameLaunchInfo) {
        List<String> args = fixMalformedArgs(gameLaunchInfo.getArgs());
        try {
          Process process = forgedAllianceService.startGame(gameLaunchInfo.getUid(), gameLaunchInfo.getMod(), args);
          serverAccessor.notifyGameStarted();
          waitForProcessTerminationInBackground(process);

          Platform.runLater(() -> callback.success(null));
        } catch (Exception e) {
          Platform.runLater(() -> callback.error(e));
        }
      }

      @Override
      public void error(Throwable e) {
        // FIXME implement
        logger.warn("Could not create game", e);
      }
    };
  }

  private void waitForProcessTerminationInBackground(Process process) {
    ConcurrentUtil.executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        int exitCode = process.waitFor();
        logger.info("Forged Alliance terminated with exit code {}", exitCode);
        serverAccessor.notifyGameTerminated();
        return null;
      }
    });
  }

  private List<String> fixMalformedArgs(List<String> gameLaunchMessage) {
    ArrayList<String> fixedArgs = new ArrayList<>();

    for (String combinedArg : gameLaunchMessage) {
      String[] split = combinedArg.split(" ");

      Collections.addAll(fixedArgs, split);
    }
    return fixedArgs;
  }
}
