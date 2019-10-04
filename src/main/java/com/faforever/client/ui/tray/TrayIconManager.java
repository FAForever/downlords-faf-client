package com.faforever.client.ui.tray;

import com.faforever.client.i18n.I18n;
import com.faforever.client.ui.StageHolder;
import com.faforever.client.ui.tray.event.UpdateApplicationBadgeEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.VPos;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.TextAlignment;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.InitializingBean;


import javax.inject.Inject;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

@Component
public class TrayIconManager implements InitializingBean {

  private final I18n i18n;
  private final EventBus eventBus;
  private int badgeCount;


  public TrayIconManager(I18n i18n, EventBus eventBus) {
    this.i18n = i18n;
    this.eventBus = eventBus;
  }

  @Override
  public void afterPropertiesSet() {
    eventBus.register(this);
  }

  /**
   * Generates and returns a tray icon. If {@code badgeCount} is greater than 0, a badge (circle) with the badge count
   * generated on top of the icon.
   */
  @Subscribe
  public void onSetApplicationBadgeEvent(UpdateApplicationBadgeEvent event) {
    Platform.runLater(() -> {
      if (event.getDelta().isPresent()) {
        badgeCount += event.getDelta().get();
      } else if (event.getNewValue().isPresent()) {
        badgeCount = event.getNewValue().get();
      } else {
        throw new IllegalStateException("No delta nor new value is available");
      }

      List<Image> icons;
      if (badgeCount < 1) {
        icons = IntStream.range(4, 9)
            .mapToObj(power -> generateTrayIcon((int) Math.pow(2, power)))
            .collect(Collectors.toList());
      } else {
        icons = IntStream.range(4, 9)
            .mapToObj(power -> generateTrayIcon((int) Math.pow(2, power)))
            .map(image -> addBadge(image, badgeCount))
            .collect(Collectors.toList());
      }
      StageHolder.getStage().getIcons().setAll(icons);
    });
  }

  private Image addBadge(Image icon, int badgeCount) {
    int badgeIconSize = (int) (icon.getWidth() * 0.6f);

    BufferedImage appIcon = SwingFXUtils.fromFXImage(icon, null);

    Graphics2D appIconGraphics = appIcon.createGraphics();
    appIconGraphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, (int) (badgeIconSize * .8)));
    appIconGraphics.setRenderingHints(new RenderingHints(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON));
    appIconGraphics.setColor(new java.awt.Color(244, 67, 54));

    int badgeX = appIcon.getWidth() - badgeIconSize;
    int badgeY = appIcon.getHeight() - badgeIconSize;
    appIconGraphics.fillOval(badgeX, badgeY, badgeIconSize, badgeIconSize);

    String numberText = i18n.number(badgeCount);

    int numberX = appIcon.getWidth() - badgeIconSize / 2 - appIconGraphics.getFontMetrics().stringWidth(numberText) / 2;
    int numberY = appIcon.getHeight() - badgeIconSize / 2 + (appIconGraphics.getFontMetrics().getAscent() - appIconGraphics.getFontMetrics().getDescent()) / 2;

    appIconGraphics.setColor(java.awt.Color.WHITE);
    appIconGraphics.drawString(numberText, numberX, numberY);
    return SwingFXUtils.toFXImage(appIcon, new WritableImage(appIcon.getWidth(), appIcon.getHeight()));
  }

  private Image generateTrayIcon(int dimension) {
    Canvas canvas = new Canvas(dimension, dimension);

    WritableImage writableImage = new WritableImage(dimension, dimension);

    GraphicsContext graphicsContext2D = canvas.getGraphicsContext2D();
    graphicsContext2D.setTextAlign(TextAlignment.CENTER);
    graphicsContext2D.setTextBaseline(VPos.CENTER);
    graphicsContext2D.setFontSmoothingType(FontSmoothingType.LCD);
    graphicsContext2D.setFont(javafx.scene.text.Font.loadFont(TrayIconManager.class.getResourceAsStream("/font/dfc-icons.ttf"), dimension));
    graphicsContext2D.setFill(Color.BLACK);
    graphicsContext2D.fillOval(0, 0, dimension, dimension);
    graphicsContext2D.setFill(Color.WHITE);
    graphicsContext2D.fillText("\uE901", dimension / 2, dimension / 2);

    SnapshotParameters snapshotParameters = new SnapshotParameters();
    snapshotParameters.setFill(javafx.scene.paint.Color.TRANSPARENT);
    return fixImage(canvas.snapshot(snapshotParameters, writableImage));
  }

  /**
   * See <a href="http://stackoverflow.com/questions/41029931/snapshot-image-cant-be-used-as-stage-icon">http://stackoverflow.com/questions/41029931/snapshot-image-cant-be-used-as-stage-icon</a>
   */
  private Image fixImage(Image image) {
    return SwingFXUtils.toFXImage(SwingFXUtils.fromFXImage(image, null), null);
  }
}
