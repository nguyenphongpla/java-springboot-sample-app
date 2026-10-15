package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import io.spring.application.CursorPager.Direction;
import org.junit.jupiter.api.Test;

public class CursorPageParameterTest {

  @Test
  public void should_use_default_limit_when_limit_is_zero() {
    CursorPageParameter<String> param = new CursorPageParameter<>(null, 0, Direction.NEXT);
    assertThat(param.getLimit(), is(20));
  }

  @Test
  public void should_use_default_limit_when_limit_is_negative() {
    CursorPageParameter<String> param = new CursorPageParameter<>(null, -5, Direction.NEXT);
    assertThat(param.getLimit(), is(20));
  }

  @Test
  public void should_cap_limit_at_max_limit() {
    CursorPageParameter<String> param = new CursorPageParameter<>(null, 10000, Direction.NEXT);
    assertThat(param.getLimit(), is(1000));
  }

  @Test
  public void should_use_given_limit_when_within_bounds() {
    CursorPageParameter<String> param = new CursorPageParameter<>(null, 5, Direction.NEXT);
    assertThat(param.getLimit(), is(5));
  }

  @Test
  public void query_limit_should_be_limit_plus_one() {
    CursorPageParameter<String> param = new CursorPageParameter<>(null, 5, Direction.NEXT);
    assertThat(param.getQueryLimit(), is(6));
  }

  @Test
  public void is_next_should_be_true_for_next_direction() {
    CursorPageParameter<String> param = new CursorPageParameter<>(null, 5, Direction.NEXT);
    assertThat(param.isNext(), is(true));
  }

  @Test
  public void is_next_should_be_false_for_prev_direction() {
    CursorPageParameter<String> param = new CursorPageParameter<>(null, 5, Direction.PREV);
    assertThat(param.isNext(), is(false));
  }
}
