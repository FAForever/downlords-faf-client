package com.faforever.client.legacy.ladder;

import com.faforever.client.leaderboard.LadderEntryBean;
import com.faforever.client.legacy.htmlparser.HtmlContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses the FAF ladder web view and generates a list of LadderEntryBean.
 */
public class LeaderboardContentHandler extends HtmlContentHandler<List<LadderEntryBean>> {

  private String currentValue;
  private LadderEntryBean currentBean;
  private String currentElement;
  private List<LadderEntryBean> result;

  @Override
  public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
    if (localName.equals("tr")) {
      currentBean = new LadderEntryBean();
      currentElement = null;
    } else if (localName.equals("td")) {
      currentElement = atts.getValue("class");
    }
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    if ("td".equals(localName)) {
      if (currentValue == null || currentValue.trim().isEmpty()) {
        return;
      }

      if ("rank".equals(currentElement)) {
        currentBean.setRank(Integer.parseInt(currentValue));
      } else if ("login".equals(currentElement)) {
        currentBean.setUsername(currentValue);
      } else if ("rating".equals(currentElement)) {
        currentBean.setRating(Integer.parseInt(currentValue));
      } else if ("numGames".equals(currentElement)) {
        currentBean.setGamesPlayed(Integer.parseInt(currentValue));
      } else if ("winRatio".equals(currentElement)) {
        currentBean.setWinLossRatio(Float.parseFloat(currentValue));
      }
    } else if ("tr".equals(localName)) {
      result.add(currentBean);
    }
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    currentValue = new String(ch, start, length);
  }

  @Override
  protected List<LadderEntryBean> getResult() {
    return result;
  }

  @Override
  public void startDocument() throws SAXException {
    result = new ArrayList<>();
  }

}
