package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

public class CursorPagerTest {

  private ArticleData articleWithSlug(String slug) {
    ArticleData data = new ArticleData();
    data.setSlug(slug);
    data.setUpdatedAt(new org.joda.time.DateTime());
    return data;
  }

  @Test
  public void should_have_next_but_not_previous_when_direction_next_and_has_extra() {
    List<ArticleData> data = List.of(articleWithSlug("a"), articleWithSlug("b"));
    CursorPager<ArticleData> pager = new CursorPager<>(data, Direction.NEXT, true);

    assertThat(pager.hasNext(), is(true));
    assertThat(pager.hasPrevious(), is(false));
  }

  @Test
  public void should_have_neither_next_nor_previous_when_direction_next_and_no_extra() {
    List<ArticleData> data = List.of(articleWithSlug("a"));
    CursorPager<ArticleData> pager = new CursorPager<>(data, Direction.NEXT, false);

    assertThat(pager.hasNext(), is(false));
    assertThat(pager.hasPrevious(), is(false));
  }

  @Test
  public void should_have_previous_but_not_next_when_direction_prev_and_has_extra() {
    List<ArticleData> data = List.of(articleWithSlug("a"));
    CursorPager<ArticleData> pager = new CursorPager<>(data, Direction.PREV, true);

    assertThat(pager.hasPrevious(), is(true));
    assertThat(pager.hasNext(), is(false));
  }

  @Test
  public void should_return_null_cursors_when_data_is_empty() {
    CursorPager<ArticleData> pager =
        new CursorPager<>(Collections.emptyList(), Direction.NEXT, false);

    assertThat(pager.getStartCursor(), nullValue());
    assertThat(pager.getEndCursor(), nullValue());
  }

  @Test
  public void should_return_first_and_last_item_cursors_when_data_is_non_empty() {
    ArticleData first = articleWithSlug("a");
    ArticleData last = articleWithSlug("b");
    CursorPager<ArticleData> pager = new CursorPager<>(List.of(first, last), Direction.NEXT, false);

    assertThat(pager.getStartCursor().toString(), is(first.getCursor().toString()));
    assertThat(pager.getEndCursor().toString(), is(last.getCursor().toString()));
  }
}
