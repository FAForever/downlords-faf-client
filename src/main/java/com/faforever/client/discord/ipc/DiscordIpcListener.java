package com.faforever.client.discord.ipc;

/**
 * Receives events from the local Discord client. Methods are called on the IPC thread.
 */
public interface DiscordIpcListener {

  void onReady(DiscordUser user);

  /** Called when the user accepted an invite or clicked "Join"; {@code secret} is the activity's join secret. */
  void onJoin(String secret);

  /** Called when the user clicked "Spectate"; {@code secret} is the activity's spectate secret. */
  void onSpectate(String secret);

  /** Called when another user asks to join. Returns whether to accept. */
  boolean onJoinRequest(DiscordUser user);
}
