package com.faforever.client.chat.event;

import com.faforever.client.chat.ChatMessage;
public record UnreadPrivateMessageEvent(ChatMessage message) {}
