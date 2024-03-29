package com.faforever.client.fx;

import com.faforever.client.test.PlatformTest;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;
import org.testfx.util.WaitForAsyncUtils;

import java.nio.file.Path;

import static com.faforever.client.fx.JavaFxUtil.PATH_STRING_CONVERTER;
import static com.faforever.client.test.IsUtilityClassMatcher.isUtilityClass;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class JavaFxUtilTest extends PlatformTest {

  @Test
  public void testPathToStringConverter() throws Exception {
    Path path = Path.of(".");

    Path fromString = PATH_STRING_CONVERTER.fromString(path.toString());
    String toString = PATH_STRING_CONVERTER.toString(path);

    assertThat(fromString, is(path));
    assertThat(toString, is(path.toAbsolutePath().toString()));
  }

  @Test
  public void testPathToStringConverterNull() throws Exception {
    assertThat(PATH_STRING_CONVERTER.fromString(null), is(nullValue()));
    assertThat(PATH_STRING_CONVERTER.toString(null), is(nullValue()));
  }

  @Test
  public void testIsUtilityClass() throws Exception {
    assertThat(JavaFxUtil.class, isUtilityClass());
  }

  @Test
  public void testToRgbCode() throws Exception {
    assertThat(JavaFxUtil.toRgbCode(Color.AZURE), is("#F0FFFF"));
  }

  @Test
  public void testMakeNumericTestFieldAcceptsNegativeNumbers() {
    testMakeNumeric("-5000", 4, true, "-5000");
    testMakeNumeric("-5000", 4, false, "5000");
    testMakeNumeric("50000", 4, true, "5000");
    testMakeNumeric("500A", 4, true, "500");
  }

  private void testMakeNumeric(String input, int maxLength, boolean allowNegative, String output) {
    //Arrange
    TextField textField = new TextField();
    //Act
    JavaFxUtil.makeNumericTextField(textField, maxLength, allowNegative);
    textField.setText(input);

    //Assert
    WaitForAsyncUtils.waitForFxEvents();
    assertThat(textField.getText(), is(output));
  }
}
