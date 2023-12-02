package com.faforever.client.update;

import java.net.URL;

public record UpdateInfo(String name, String fileName, URL url, int size, URL releaseNotesUrl, boolean prerelease) {}
