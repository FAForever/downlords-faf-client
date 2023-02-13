package com.faforever.client.chat;

import javafx.beans.value.ObservableValue;
import lombok.NonNull;

public record ChatListItem(ChatChannelUser user, @NonNull ChatUserCategory category,
                           String channelName, ObservableValue<Integer> numCategoryItemsProperty) {}
