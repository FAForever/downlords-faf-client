package com.faforever.client.chat;

import com.faforever.client.config.CacheNames;
import com.faforever.client.fx.FxmlLoader;
import com.faforever.client.i18n.I18n;
import com.faforever.client.util.Bytes;
import com.google.common.net.MediaType;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlPreviewResolverImpl implements UrlPreviewResolver {

  private static final Pattern IMGUR_PATTERN = Pattern.compile("https?://imgur\\.com/gallery/(\\w+)");
  private static final String IMGUR_JPG = "http://i.imgur.com/%s.jpg";
  private static final String IMGUR_PNG = "http://i.imgur.com/%s.png";
  private static final String IMGUR_GIF = "http://i.imgur.com/%s.gif";

  @Autowired
  FxmlLoader fxmlLoader;

  @Autowired
  I18n i18n;

  @Override
  @Cacheable(CacheNames.URL_PREVIEW)
  public Preview resolvePreview(String urlString) {
    try {
      String guessedUrl = guessUrl(urlString);

      URL url = new URL(guessedUrl);

      String protocol = url.getProtocol();

      if (!"http".equals(protocol) && !"https".equals(protocol)) {
        // TODO log unhandled protocol
        return null;
      }

      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setInstanceFollowRedirects(true);

      long contentLength = connection.getContentLengthLong();
      String contentType = connection.getContentType();

      Node root = fxmlLoader.loadAndGetRoot("image_preview.fxml");
      ImageView imageView = (ImageView) root.lookup("#imageView");

      if (MediaType.JPEG.toString().equals(contentType)
          || MediaType.PNG.toString().equals(contentType)) {
        imageView.setImage(new Image(guessedUrl));
      }

      String description = i18n.get("urlPreviewDescription", contentType, Bytes.formatSize(contentLength));

      return new Preview(imageView, description);
    } catch (IOException e) {
      return null;
    }
  }

  private String guessUrl(String urlString) {
    Matcher matcher = IMGUR_PATTERN.matcher(urlString);
    if (matcher.find()) {
      return guessImgurUrl(urlString, matcher);
    }

    return urlString;
  }

  private String guessImgurUrl(String urlString, Matcher matcher) {
    String imageId = matcher.group(1);

    String imgurJpgUrl = String.format(IMGUR_JPG, imageId);
    if (testUrl(imgurJpgUrl)) {
      return imgurJpgUrl;
    }

    String imgurPngUrl = String.format(IMGUR_PNG, imageId);
    if (testUrl(imgurPngUrl)) {
      return imgurJpgUrl;
    }

    String imgurGifUrl = String.format(IMGUR_GIF, imageId);
    if (testUrl(imgurGifUrl)) {
      return imgurGifUrl;
    }

    return urlString;
  }

  private static boolean testUrl(String urlString) {
    try {
      return ((HttpURLConnection) new URL(urlString).openConnection()).getResponseCode() == HttpURLConnection.HTTP_OK;
    } catch (IOException e) {
      return false;
    }
  }
}
