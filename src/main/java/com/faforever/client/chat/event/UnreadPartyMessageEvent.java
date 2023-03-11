package com.faforever.client.chat.event;

import com.faforever.client.chat.ChatMessage;

public record UnreadPartyMessageEvent(ChatMessage message) {}