package playground;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

public class Mention {

  public static void main(String[] args) {
    String username = "Downlord";
    String chatMessage = "Hello downlord";

    Pattern mentionPattern = Pattern.compile("\\b(" + Pattern.quote(username) + ")\\b", CASE_INSENSITIVE);

    Matcher matcher = mentionPattern.matcher(chatMessage);
    if (matcher.find()) {
      System.out.println(matcher.replaceAll("<span class='self'>" + matcher.group(1) + "</span>"));
    }
  }
}
