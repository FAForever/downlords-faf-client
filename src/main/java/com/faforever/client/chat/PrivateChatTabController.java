package com.faforever.client.chat;

import com.faforever.client.audio.AudioService;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.game.Game;
import com.faforever.client.game.JoinGameHelper;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapServiceImpl.PreviewSize;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.domain.GameStatus;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.theme.UiService;
import com.faforever.client.uploader.ImageUploadService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.IdenticonUtil;
import com.faforever.client.util.RatingUtil;
import com.faforever.client.util.TimeService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import com.neovisionaries.i18n.CountryCode;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.chat.SocialStatus.FOE;
import static javafx.beans.binding.Bindings.createStringBinding;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PrivateChatTabController extends AbstractChatTabController {

  private final CountryFlagService countryFlagService;
  private final MapService mapService;
  private final JoinGameHelper joinGameHelper;
  private final ReplayService replayService;
  public Tab privateChatTabRoot;
  public WebView messagesWebView;
  public TextInputControl messageTextField;
  public ImageView userImageView;
  public Label usernameLabel;
  public ImageView countryImageView;
  public Label countryLabel;
  public Label globalRatingLevel;
  public Label leaderboardRatingLevel;
  public Label gamesPlayedLabel;
  public Label inGameLabel;
  public ImageView mapPreview;
  public Label gameTitleLabel;
  public VBox gameHostVBox;
  public Label gameHostLabel;
  public VBox gamePreview;
  public Label gamePlayerCountLabel;
  public Label featuredModLabel;
  public Button joinSpectateButton;

  //user references the receiver of the private chat
  private Game userGame;
  private Player userPlayer;
  private boolean userOffline;

  @Inject
  public PrivateChatTabController(UserService userService,
                                  PlatformService platformService,
                                  PreferencesService preferencesService,
                                  PlayerService playerService,
                                  TimeService timeService,
                                  I18n i18n,
                                  ImageUploadService imageUploadService,
                                  UrlPreviewResolver urlPreviewResolver,
                                  NotificationService notificationService,
                                  ReportingService reportingService,
                                  UiService uiService,
                                  AutoCompletionHelper autoCompletionHelper,
                                  EventBus eventBus,
                                  AudioService audioService,
                                  ChatService chatService,
                                  CountryFlagService countryFlagService,
                                  MapService mapService,
                                  WebViewConfigurer webViewConfigurer,
                                  JoinGameHelper joinGameHelper,
                                  ReplayService replayService) {
    super(userService, chatService, platformService, preferencesService, playerService, audioService, timeService, i18n, imageUploadService, urlPreviewResolver, notificationService, reportingService, uiService, autoCompletionHelper, eventBus, webViewConfigurer);
    this.countryFlagService = countryFlagService;
    this.mapService = mapService;
    this.joinGameHelper = joinGameHelper;
    this.replayService = replayService;
  }

  boolean isUserOffline() {
    return userOffline;
  }

  @Override
  public Tab getRoot() {
    return privateChatTabRoot;
  }

  @Override
  public void setReceiver(String username) {
    super.setReceiver(username);
    privateChatTabRoot.setId(username);
    privateChatTabRoot.setText(username);

    userPlayer = playerService.getPlayerForUsername(username);
    if (userPlayer != null) {
      loadDetailPane(userPlayer);
    }
  }

  private void loadDetailPane(Player player) {
    CountryCode countryCode = CountryCode.getByCode(userPlayer.getCountry());

    usernameLabel.setText(player.getUsername());
    userImageView.setImage(IdenticonUtil.createIdenticon(userPlayer.getId()));
    countryImageView.setImage(countryFlagService.loadCountryFlag(userPlayer.getCountry()));
    countryLabel.setText(countryCode == null ? userPlayer.getCountry() : countryCode.getName());

    userPlayer.globalRatingMeanProperty().addListener((observable) -> loadReceiverGlobalRatingInformation(userPlayer));
    userPlayer.globalRatingDeviationProperty().addListener((observable) -> loadReceiverGlobalRatingInformation(userPlayer));
    userPlayer.leaderboardRatingMeanProperty().addListener((observable) -> loadReceiverLadderRatingInformation(userPlayer));
    userPlayer.leaderboardRatingDeviationProperty().addListener((observable) -> loadReceiverLadderRatingInformation(userPlayer));
    loadReceiverGlobalRatingInformation(userPlayer);
    loadReceiverLadderRatingInformation(userPlayer);

    gamesPlayedLabel.textProperty().bind(userPlayer.numberOfGamesProperty().asString());
    loadPlayerGameInformation(userPlayer.getGame());
    userPlayer.gameProperty().addListener((observable, oldValue, newValue) -> {
      loadPlayerGameInformation(newValue);
    });
  }

  private void loadReceiverGlobalRatingInformation(Player player) {
    globalRatingLevel.setText(i18n.get("chat.privateMessage.ratingFormat", Math.round(player.getGlobalRatingMean()), Math.round(player.getGlobalRatingDeviation() * 3f), RatingUtil.getGlobalRating(player)));
  }

  private void loadReceiverLadderRatingInformation(Player player) {
    leaderboardRatingLevel.setText(i18n.get("chat.privateMessage.ratingFormat", Math.round(player.getLeaderboardRatingMean()), Math.round(player.getLeaderboardRatingDeviation() * 3f), RatingUtil.getLeaderboardRating(player)));
  }

  private void loadPlayerGameInformation(Game game) {
    this.userGame = game;

    setIsInGameLabel();

    gameTitleLabel.textProperty().unbind();

    if (game == null) {
      gamePreview.setVisible(false);
      gamePreview.setManaged(false);
      return;
    }

    gamePreview.setVisible(true);
    gamePreview.setManaged(true);

    game.statusProperty().addListener((observable, oldValue, newValue) -> {
      setIsInGameLabel();
      setJoinSpectateButton();
      gameHostVBox.setManaged(false);
      gameHostVBox.setVisible(false);
    });
    setJoinSpectateButton();

    gameTitleLabel.textProperty().bind(game.titleProperty());

    loadMapPreview(game.getMapFolderName());
    game.mapFolderNameProperty().addListener((observable, oldValue, newValue) -> loadMapPreview(newValue));

    //TODO: switch to custom bindings instead of listeners
    gamePlayerCountLabel.textProperty().bind(createStringBinding(
        () -> i18n.get("chat.privateMessage.game.playersFormat", game.getNumPlayers(), game.getMaxPlayers()),
        game.numPlayersProperty(), game.maxPlayersProperty()));

    featuredModLabel.setText(game.getFeaturedMod());

    if (game.getStatus() == GameStatus.OPEN) {
      gameHostVBox.setManaged(true);
      gameHostVBox.setVisible(true);
      gameHostLabel.setText(game.getHost());
    }
  }

  private void setIsInGameLabel() {
    if (userGame != null && userGame.getStatus() == GameStatus.OPEN) {
      inGameLabel.setText(i18n.get("game.gameStatus.lobby"));
    } else if (userGame != null && userGame.getStatus() == GameStatus.PLAYING) {
      inGameLabel.setText(i18n.get("game.gameStatus.playing"));
    } else {
      inGameLabel.setText(i18n.get("chat.privateMessage.gamestate.notInGame"));
    }
  }

  private void setJoinSpectateButton() {
    if (userGame.getStatus() == GameStatus.OPEN) {
      joinSpectateButton.setText(i18n.get("game.join"));
    } else if (userGame.getStatus() == GameStatus.PLAYING) {
      joinSpectateButton.setText(i18n.get("game.spectate"));
    }
  }

  private void loadMapPreview(String mapName) {
    CompletableFuture.supplyAsync(() -> mapService.loadPreview(mapName, PreviewSize.SMALL))
        .thenAccept(image -> mapPreview.setImage(image));
  }

  @FXML
  private void joinSpectateGameButtonClicked(ActionEvent event) {
    if (userGame.getStatus() == GameStatus.OPEN) {
      joinGameHelper.join(userGame);
    } else if (userGame.getStatus() == GameStatus.PLAYING) {
      replayService.runLiveReplay(userGame.getId(), userPlayer.getId());
    } else {
      notificationService.addNotification(new ImmediateNotification(i18n.get("chat.privateMessage.game.joinSpectateError.title"), i18n.get("chat.privateMessage.game.joinSpectateError.text"), Severity.ERROR));
    }
  }

  public void initialize() {
    super.initialize();
    userOffline = false;
    chatService.addChatUsersByNameListener(change -> {
      if (change.wasRemoved()) {
        onPlayerDisconnected(change.getKey(), change.getValueRemoved());
      }
      if (change.wasAdded()) {
        onPlayerConnected(change.getKey(), change.getValueRemoved());
      }
    });
    webViewConfigurer.configureWebView(messagesWebView);
  }

  @Override
  protected TextInputControl getMessageTextField() {
    return messageTextField;
  }

  @Override
  protected WebView getMessagesWebView() {
    return messagesWebView;
  }

  @Override
  public void onChatMessage(ChatMessage chatMessage) {
    Player player = playerService.getPlayerForUsername(chatMessage.getUsername());
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();

    if (player != null && player.getSocialStatus() == FOE && chatPrefs.getHideFoeMessages()) {
      return;
    }

    super.onChatMessage(chatMessage);

    if (!hasFocus()) {
      audioService.playPrivateMessageSound();
      showNotificationIfNecessary(chatMessage);
      setUnread(true);
      incrementUnreadMessagesCount(1);
    }
  }

  @VisibleForTesting
  void onPlayerDisconnected(String userName, ChatUser userItem) {
    if (userName.equals(getReceiver())) {
      userOffline = true;
      Platform.runLater(() -> onChatMessage(new ChatMessage(userName, Instant.now(), i18n.get("chat.operator") + ":", i18n.get("chat.privateMessage.playerLeft", userName), true)));
    }
  }

  @VisibleForTesting
  void onPlayerConnected(String userName, ChatUser userItem) {
    if (userOffline && userName.equals(getReceiver())) {
      userOffline = false;
      Platform.runLater(() -> onChatMessage(new ChatMessage(userName, Instant.now(), i18n.get("chat.operator") + ":", i18n.get("chat.privateMessage.playerReconnect", userName), true)));
    }
  }
}
