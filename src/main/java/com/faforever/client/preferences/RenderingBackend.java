package com.faforever.client.preferences;

import lombok.Getter;

@Getter
public enum RenderingBackend {
    DIRECTX_9("settings.game.renderingBackend.directx9", null, null),
    DIRECTX_12("settings.game.renderingBackend.directx12", "zip", "d3d9.dll"),
    VULKAN_DXVK("settings.game.renderingBackend.vulkan", "tar.gz", "x32/d3d9.dll");

    private final String i18nKey;
    private final String archiveFormat;
    private final String dllPathInArchive;

    RenderingBackend(String i18nKey, String archiveFormat, String dllPathInArchive) {
        this.i18nKey = i18nKey;
        this.archiveFormat = archiveFormat;
        this.dllPathInArchive = dllPathInArchive;
    }

    public boolean requiresDownload() {
        return archiveFormat != null;
    }
}
