package com.faforever.client.chat.emoticons;

public record Reaction(String messageId, String targetMessageId, Emoticon emoticon, String reactorName) {}
