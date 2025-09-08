package com.faforever.client.mod;

import java.net.URI;
import java.util.UUID;

public record UploadUrlResponse(URI uploadUrl, UUID requestId) {

}
