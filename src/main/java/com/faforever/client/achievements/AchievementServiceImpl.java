package com.faforever.client.achievements;

import com.faforever.client.api.dto.AchievementDefinition;
import com.faforever.client.api.dto.PlayerAchievement;
import com.faforever.client.config.CacheNames;
import com.faforever.client.player.PlayerService;
import com.faforever.client.remote.AssetService;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.UpdatedAchievementsMessage;
import com.google.common.annotations.VisibleForTesting;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static com.github.nocatch.NoCatch.noCatch;


@Lazy
@Service
public class AchievementServiceImpl implements AchievementService {

  private static final int ACHIEVEMENT_IMAGE_SIZE = 128;
  private final ObservableList<PlayerAchievement> readOnlyPlayerAchievements;
  @VisibleForTesting
  final ObservableList<PlayerAchievement> playerAchievements;

  private final FafService fafService;
  private final PlayerService playerService;
  private final AssetService assetService;

  @Inject
  // TODO cut dependencies if possible
  public AchievementServiceImpl(FafService fafService, PlayerService playerService, AssetService assetService) {
    this.fafService = fafService;
    this.playerService = playerService;
    this.assetService = assetService;

    playerAchievements = FXCollections.observableArrayList();
    readOnlyPlayerAchievements = FXCollections.unmodifiableObservableList(playerAchievements);
  }

  @Override
  public CompletableFuture<List<PlayerAchievement>> getPlayerAchievements(Integer playerId) {
    int currentPlayerId = playerService.getCurrentPlayer().orElseThrow(() -> new IllegalStateException("Player has to be set")).getId();
    if (Objects.equals(currentPlayerId, playerId)) {
      if (readOnlyPlayerAchievements.isEmpty()) {

        return reloadAchievements();
      }
      return CompletableFuture.completedFuture(readOnlyPlayerAchievements);
    }

    return fafService.getPlayerAchievements(playerId);
  }

  @Override
  public CompletableFuture<List<AchievementDefinition>> getAchievementDefinitions() {
    return fafService.getAchievementDefinitions();
  }

  @Override
  public CompletableFuture<AchievementDefinition> getAchievementDefinition(String achievementId) {
    return fafService.getAchievementDefinition(achievementId);
  }

  @Override
  @Cacheable(CacheNames.ACHIEVEMENT_IMAGES)
  public Image getImage(AchievementDefinition achievementDefinition, AchievementState achievementState) {
    URL url;
    switch (achievementState) {
      case REVEALED:
        url = noCatch(() -> new URL(achievementDefinition.getRevealedIconUrl()));
        break;
      case UNLOCKED:
        url = noCatch(() -> new URL(achievementDefinition.getUnlockedIconUrl()));
        break;
      default:
        throw new UnsupportedOperationException("Not yet implemented");
    }
    return assetService.loadAndCacheImage(url, Paths.get("achievements").resolve(achievementState.name().toLowerCase()),
        null, ACHIEVEMENT_IMAGE_SIZE, ACHIEVEMENT_IMAGE_SIZE);
  }

  private CompletableFuture<List<PlayerAchievement>> reloadAchievements() {
    CompletableFuture<List<PlayerAchievement>> achievementsLoadedFuture = new CompletableFuture<>();
    int playerId = playerService.getCurrentPlayer().orElseThrow(() -> new IllegalStateException("Player has to be set")).getId();
    fafService.getPlayerAchievements(playerId).thenAccept(achievements -> {
      playerAchievements.setAll(achievements);
      achievementsLoadedFuture.complete(readOnlyPlayerAchievements);
    });
    return achievementsLoadedFuture;
  }

  @PostConstruct
  void postConstruct() {
    fafService.addOnMessageListener(UpdatedAchievementsMessage.class, updatedAchievementsMessage -> reloadAchievements());
  }
}
