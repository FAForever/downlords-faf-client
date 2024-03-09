package com.faforever.client.chat.emoticons;

import java.util.List;

public record Emoticon(List<String> shortcodes, String base64SvgContent) {}
