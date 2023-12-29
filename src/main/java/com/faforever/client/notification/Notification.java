package com.faforever.client.notification;

public sealed interface Notification permits ImmediateNotification, PersistentNotification, ServerNotification, TransientNotification {}
