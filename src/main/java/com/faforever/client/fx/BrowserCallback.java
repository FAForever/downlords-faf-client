package com.faforever.client.fx;

import com.faforever.client.chat.InitiatePrivateChatEvent;
import com.faforever.client.chat.UrlPreviewResolver;
import com.faforever.client.clan.ClanService;
import com.faforever.client.clan.ClanTooltipController;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.replay.ExternalReplayInfoGenerator;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.StageHolder;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.stage.Popup;
import javafx.stage.PopupWindow;
import javafx.stage.PopupWindow.AnchorLocation;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.faforever.client.util.RatingUtil.getGlobalRating;
import static com.faforever.client.util.RatingUtil.getLeaderboardRating;

@SuppressWarnings("WeakerAccess")
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class BrowserCallback {
  private final PlatformService platformService;
  private final UrlPreviewResolver urlPreviewResolver;
  private final ReplayService replayService;
  private final EventBus eventBus;
  private final ExternalReplayInfoGenerator externalReplayInfoGenerator;
  private final Pattern replayUrlPattern;
  private final ClanService clanService;
  private final UiService uiService;
  private final PlayerService playerService;
  private final I18n i18n;
  @VisibleForTesting
  Popup clanInfoPopup;
  private Tooltip linkPreviewTooltip;
  private Popup playerInfoPopup;
  private double lastMouseX;
  private double lastMouseY;

  BrowserCallback(PlatformService platformService, ClientProperties clientProperties,
                  UrlPreviewResolver urlPreviewResolver, ReplayService replayService, EventBus eventBus,
                  ExternalReplayInfoGenerator externalReplayInfoGenerator, ClanService clanService, UiService uiService, PlayerService playerService, I18n i18n) {
    this.platformService = platformService;
    this.urlPreviewResolver = urlPreviewResolver;
    this.replayService = replayService;
    this.eventBus = eventBus;
    this.externalReplayInfoGenerator = externalReplayInfoGenerator;
    this.clanService = clanService;
    this.uiService = uiService;
    this.playerService = playerService;
    this.i18n = i18n;

    String urlFormat = clientProperties.getVault().getReplayDownloadUrlFormat();
    String[] splitFormat = urlFormat.split("%s");
    replayUrlPattern = Pattern.compile(Pattern.quote(splitFormat[0]) + "(\\d+)" + Pattern.compile(splitFormat.length == 2 ? splitFormat[1] : ""));
  }

  /**
   * Called from JavaScript when user clicked a URL.
   */
  @SuppressWarnings("unused")
  public void openUrl(String url) {
    Matcher replayUrlMatcher = replayUrlPattern.matcher(url);
    if (!replayUrlMatcher.matches()) {
      platformService.showDocument(url);
      return;
    }

    String replayId = replayUrlMatcher.group(1);

    replayService.findById(Integer.parseInt(replayId))
        .thenAccept(replay -> Platform.runLater(() -> externalReplayInfoGenerator.showExternalReplayInfo(replay, replayId)));
  }

  /**
   * Called from JavaScript when user clicks on user name in chat
   */
  @SuppressWarnings("unused")
  public void openPrivateMessageTab(String username) {
    eventBus.post(new InitiatePrivateChatEvent(username));
  }

  /**
   * Called from JavaScript when user no longer hovers over an URL.
   */
  @SuppressWarnings("unused")
  public void hideUrlPreview() {
    if (linkPreviewTooltip != null) {
      linkPreviewTooltip.hide();
      linkPreviewTooltip = null;
    }
  }

  /**
   * Called from JavaScript when user hovers over an URL.
   */
  @SuppressWarnings("unused")
  public void previewUrl(String urlString) {
    urlPreviewResolver.resolvePreview(urlString).thenAccept(optionalPreview -> optionalPreview.ifPresent(preview -> {
      linkPreviewTooltip = new Tooltip(preview.getDescription());
      linkPreviewTooltip.setAutoHide(true);
      linkPreviewTooltip.setAnchorLocation(AnchorLocation.CONTENT_BOTTOM_LEFT);
      linkPreviewTooltip.setGraphic(preview.getNode());
      linkPreviewTooltip.setContentDisplay(ContentDisplay.TOP);
      Platform.runLater(() -> linkPreviewTooltip.show(StageHolder.getStage(), lastMouseX + 20, lastMouseY));
    }));
  }

  /**
   * Called from JavaScript when user hovers over a clan tag.
   */
  @SuppressWarnings("unused")
  public void showClanInfo(String clanTag) {
    clanService.getClanByTag(clanTag).thenAccept(clan -> Platform.runLater(() -> {
      if (!clan.isPresent() || clanTag.isEmpty()) {
        return;
      }
      ClanTooltipController clanTooltipController = uiService.loadFxml("theme/chat/clan_tooltip.fxml");
      clanTooltipController.setClan(clan.get());
      clanTooltipController.getRoot().getStyleClass().add("tooltip");

      clanInfoPopup = new Popup();
      clanInfoPopup.getContent().setAll(clanTooltipController.getRoot());
      clanInfoPopup.setAnchorLocation(AnchorLocation.CONTENT_TOP_LEFT);
      clanInfoPopup.setAutoHide(true);
      clanInfoPopup.show(StageHolder.getStage(), lastMouseX, lastMouseY + 10);
    }));
  }

  /**
   * Called from JavaScript when user no longer hovers over a clan tag.
   */
  @SuppressWarnings("unused")
  public void hideClanInfo() {
    if (clanInfoPopup == null) {
      return;
    }
    Platform.runLater(() -> {
      clanInfoPopup.hide();
      clanInfoPopup = null;
    });
  }

  /**
   * Called from JavaScript when user clicks on clan tag.
   */
  @SuppressWarnings("unused")
  public void showClanWebsite(String clanTag) {
    clanService.getClanByTag(clanTag).thenAccept(clan -> {
      if (!clan.isPresent()) {
        return;
      }
      platformService.showDocument(clan.get().getWebsiteUrl());
    });
  }

  /**
   * Called from JavaScript when user hovers over a user name.
   */
  @SuppressWarnings("unused")
  public void showPlayerInfo(String username) {
    Optional<Player> playerOptional = playerService.getPlayerForUsername(username);

    if (!playerOptional.isPresent()) {
      return;
    }

    Player player = playerOptional.get();

    playerInfoPopup = new Popup();
    Label label = new Label();
    label.getStyleClass().add("tooltip");
    playerInfoPopup.getContent().setAll(label);

    label.textProperty().bind(Bindings.createStringBinding(
        () -> i18n.get("userInfo.ratingFormat", getGlobalRating(player), getLeaderboardRating(player)),
        player.leaderboardRatingMeanProperty(), player.leaderboardRatingDeviationProperty(),
        player.globalRatingMeanProperty(), player.globalRatingDeviationProperty()
    ));

    playerInfoPopup.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_BOTTOM_LEFT);

    Platform.runLater(() -> playerInfoPopup.show(StageHolder.getStage(), lastMouseX, lastMouseY - 10));
  }

  /**
   * Called from JavaScript when user no longer hovers over a user name.
   */
  @SuppressWarnings("unused")
  public void hidePlayerInfo() {
    if (playerInfoPopup == null) {
      return;
    }
    Platform.runLater(() -> {
      playerInfoPopup.hide();
      playerInfoPopup = null;
    });
  }

  void setLastMouseX(double screenX) {
    lastMouseX = screenX;
  }

  void setLastMouseY(double screenY) {
    lastMouseY = screenY;
  }
}
