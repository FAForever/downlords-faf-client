package com.faforever.client.logging.analysis;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class LogAnalyzerServiceTest extends ServiceTest {

  private static final String SOUND_EXPECTED_TEXT = "Sound issue detected";
  private static final String MINIMIZED_EXPECTED_TEXT = "Game was minimized";
  private static final String MORE_INFO_BUTTON = "More Info";

  @Mock
  private I18n i18n;

  @Mock
  private ClientProperties clientProperties;

  @Mock
  private PlatformService platformService;

  @InjectMocks
  private LogAnalyzerService logAnalyzerService;

  @Test
  public void testAnalyzeLogContentsWhenGameMinimizedTrace() {
    final String logContents = "info: Minimized true";

    when(i18n.get("game.log.analysis.minimized")).thenReturn(MINIMIZED_EXPECTED_TEXT);

    Map<String, Action> result = logAnalyzerService.analyzeLogContents(logContents);

    assertTrue(result.containsKey(MINIMIZED_EXPECTED_TEXT));
  }

  @Test
  public void testAnalyzeLogContentsWhenXactTrace() {
    final String logContents = "warning: SND\nXACT";

    when(i18n.get("game.log.analysis.moreInfoBtn")).thenReturn(MORE_INFO_BUTTON);
    when(i18n.get("game.log.analysis.snd", MORE_INFO_BUTTON)).thenReturn(SOUND_EXPECTED_TEXT);

    Map<String, Action> result = logAnalyzerService.analyzeLogContents(logContents);

    assertEquals(1, result.size());
    assertNotNull(result.get(SOUND_EXPECTED_TEXT));
  }

  @Test
  public void testAnalyzeLogContentsWhenGameMinimizedAndXactTrace() {
    final String logContents = "info: Minimized true\nwarning: SND\nXACT";

    when(i18n.get("game.log.analysis.minimized")).thenReturn(MINIMIZED_EXPECTED_TEXT);
    when(i18n.get("game.log.analysis.moreInfoBtn")).thenReturn(MORE_INFO_BUTTON);
    when(i18n.get("game.log.analysis.snd", MORE_INFO_BUTTON)).thenReturn(SOUND_EXPECTED_TEXT);

    Map<String, Action> result = logAnalyzerService.analyzeLogContents(logContents);

    assertEquals(2, result.size());
    assertNotNull(result.get(SOUND_EXPECTED_TEXT));
    assertNull(result.get(MINIMIZED_EXPECTED_TEXT));
  }

  @Test
  public void testAnalyzeLogContentsWhenNoRelevantTraces() {
    final String logContents = "Some other log content";

    Map<String, Action> result = logAnalyzerService.analyzeLogContents(logContents);

    assertTrue(result.isEmpty());
  }
}
