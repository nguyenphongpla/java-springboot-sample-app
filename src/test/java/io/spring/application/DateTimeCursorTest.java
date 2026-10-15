package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class DateTimeCursorTest {

  @Test
  public void should_return_null_when_cursor_string_is_null() {
    assertThat(DateTimeCursor.parse(null), nullValue());
  }

  @Test
  public void should_parse_millis_string_back_to_same_instant() {
    DateTime original = new DateTime(2024, 1, 1, 12, 0, DateTimeZone.UTC);
    String cursor = new DateTimeCursor(original).toString();

    DateTime parsed = DateTimeCursor.parse(cursor);

    assertThat(parsed.getMillis(), is(original.getMillis()));
  }

  @Test
  public void should_throw_on_malformed_cursor_string() {
    org.junit.jupiter.api.Assertions.assertThrows(
        NumberFormatException.class, () -> DateTimeCursor.parse("not-a-number"));
  }
}
