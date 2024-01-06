package com.faforever.client.chat.emoticons;

import javafx.scene.image.Image;

import java.util.List;

public record Emoticon(List<String> shortcodes, String base64SvgContent, Image image) {}
