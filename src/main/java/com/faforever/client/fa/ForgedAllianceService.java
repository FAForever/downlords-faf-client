package com.faforever.client.fa;

import com.faforever.client.game.Faction;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;

/**
 * Knows how to starts/stop Forged Alliance with proper parameters. Downloading maps, mods and updates as well as
 * notifying the server about whether the game is running or not is <strong>not</strong> this service's responsibility.
 */
public interface ForgedAllianceService {

  Process startGame(int uid, Faction faction, List<String> additionalArgs, RatingMode ratingMode, int gpgPort, boolean rehost) throws IOException;

  Process startReplay(Path path, @Nullable Integer replayId) throws IOException;

  Process startReplay(URI replayUri, Integer replayId) throws IOException;
}
