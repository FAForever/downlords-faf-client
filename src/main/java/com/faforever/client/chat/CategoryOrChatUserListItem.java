package com.faforever.client.chat;

import lombok.Value;

/**
 * Represents either a chat user or a chat user category.
 * <p>
 * All attempts to use a tree table view failed (bugs everywhere) so it was decided to use a simple list view that
 * contains "category" items.
 * <p>
 * If it's a category-only object, {@code user} will be {@code null}. If it's a chat user, {@code category} will be
 * {@code null}.
 */
@Value
class CategoryOrChatUserListItem {
  ChatUserCategory category;
  ChatChannelUser user;
}
