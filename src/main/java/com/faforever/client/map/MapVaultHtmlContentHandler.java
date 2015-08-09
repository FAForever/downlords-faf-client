package com.faforever.client.map;

import com.faforever.client.game.MapInfoBean;
import com.faforever.client.game.MapSize;
import com.faforever.client.legacy.htmlparser.HtmlContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MapVaultHtmlContentHandler extends HtmlContentHandler<List<MapInfoBean>> {

  private enum MapProperty {
    PLAYS,
    DOWNLOADS,
    NAME,
    DESCRIPTION,
    MAP_MISC,
    RATING,
  }

  /**
   * Extracts data out of "8 players, 10x10 km, v.25".
   */
  private static final Pattern MAP_MISC_PATTERN = Pattern.compile("(\\d+)\\s+players,\\s+(\\d+)x(\\d+)\\s+km,\\s+v.(\\d+)");

  private MapProperty currentProperty;
  private List<MapInfoBean> result;
  private MapInfoBean currentBean;
  private String currentValue;

  @Override
  protected List<MapInfoBean> getResult() {
    return result;
  }

  @Override
  public void startDocument() throws SAXException {
    result = new ArrayList<>();
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
    if (localName.equals("td") && "map".equals(atts.getValue("class"))) {
      // Start of table cell is start of map
      currentBean = new MapInfoBean();
      currentBean.setId(Integer.parseInt(atts.getValue("id")));
      currentBean.setTechnicalName(atts.getValue("folder"));
      return;
    }

    if (localName.equals("div") && "map_name".equals(atts.getValue("class"))) {
      currentProperty = MapProperty.NAME;
      return;
    }

    if (localName.equals("div") && "map_desc".equals(atts.getValue("class"))) {
      currentProperty = MapProperty.DESCRIPTION;
      return;
    }

    if (localName.equals("div") && "map_misc".equals(atts.getValue("class"))) {
      currentProperty = MapProperty.MAP_MISC;
      return;
    }

    if (localName.equals("span") && "plays".equals(atts.getValue("class"))) {
      currentProperty = MapProperty.PLAYS;
      return;
    }

    if (localName.equals("span") && "downloads".equals(atts.getValue("class"))) {
      currentProperty = MapProperty.DOWNLOADS;
      return;
    }

    currentProperty = null;
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    if ("td".equals(localName)) {
      if (currentBean != null) {
        // End of cell means end of map
        result.add(currentBean);
        currentBean = null;
      }
      return;
    }

    if (currentProperty == null || currentValue == null || currentValue.trim().isEmpty()) {
      return;
    }

    switch (currentProperty) {
      case PLAYS:
        currentBean.setPlays(Integer.parseInt(currentValue));
        return;
      case DOWNLOADS:
        currentBean.setDownloads(Integer.parseInt(currentValue));
        return;
      case NAME:
        currentBean.setDisplayName(currentValue.trim().replaceAll("[\\s\\n]+", " ").replace("_"," "));
        return;
      case DESCRIPTION:
        currentBean.setDescription(currentValue.replaceAll("[\\s\\n]+", " "));
        return;
      case RATING:
        currentBean.setRating(Float.parseFloat(currentValue));
        return;
      case MAP_MISC:
        Matcher matcher = MAP_MISC_PATTERN.matcher(currentValue);
        if (matcher.matches()) {
          currentBean.setPlayers(Integer.parseInt(matcher.group(1)));

          int mapWidth = Integer.parseInt(matcher.group(2));
          int mapHeight = Integer.parseInt(matcher.group(3));
          currentBean.setSize(new MapSize(mapWidth, mapHeight));

          currentBean.setVersion(Integer.parseInt(matcher.group(4)));
        }
        return;

      default:
        throw new IllegalStateException("Unhandled property: " + currentProperty);
    }
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    currentValue = new String(ch, start, length);
  }
}
