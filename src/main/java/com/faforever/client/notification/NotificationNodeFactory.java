package com.faforever.client.notification;

import javafx.scene.Node;

public interface NotificationNodeFactory {

  Node createPersistentNotificationNode(PersistentNotification notification);

  Node createTransientNotificationPane(TransientNotification notification);
}
