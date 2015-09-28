package com.faforever.client.fa;

import com.faforever.client.game.Faction;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

/**
 * Knows how to starts/stop Forged Alliance with proper parameters. Downloading maps, mods and updates as well as
 * notifying the server about whether the game is running or not is <strong>not</strong> this service's responsibility.
 */
public interface ForgedAllianceService {

  Process startGame(int uid, String gameType, Faction faction, List<String> additionalArgs, RatingMode ratingMode) throws IOException;

  Process startReplay(Path path, @Nullable Integer replayId) throws IOException;

  Process startReplay(URL replayUrl, Integer replayId) throws IOException;
}
