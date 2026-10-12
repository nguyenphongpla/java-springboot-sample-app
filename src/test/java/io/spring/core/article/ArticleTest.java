package io.spring.core.article;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class ArticleTest {

  @Test
  public void should_get_right_slug() {
    Article article = new Article("a new   title", "desc", "body", Arrays.asList("java"), "123");
    assertThat(article.getSlug(), is("a-new-title"));
  }

  @Test
  public void should_get_right_slug_with_number_in_title() {
    Article article = new Article("a new title 2", "desc", "body", Arrays.asList("java"), "123");
    assertThat(article.getSlug(), is("a-new-title-2"));
  }

  @Test
  public void should_get_lower_case_slug() {
    Article article = new Article("A NEW TITLE", "desc", "body", Arrays.asList("java"), "123");
    assertThat(article.getSlug(), is("a-new-title"));
  }

  @Test
  public void should_handle_other_language() {
    Article article = new Article("中文：标题", "desc", "body", Arrays.asList("java"), "123");
    assertThat(article.getSlug(), is("中文-标题"));
  }

  @Test
  public void should_handle_commas() {
    Article article = new Article("what?the.hell,w", "desc", "body", Arrays.asList("java"), "123");
    assertThat(article.getSlug(), is("what-the-hell-w"));
  }

  @Test
  public void should_only_update_title_and_regenerate_slug() {
    Article article =
        new Article("old title", "old desc", "old body", Arrays.asList("java"), "123");

    article.update("new title", null, null);

    assertThat(article.getTitle(), is("new title"));
    assertThat(article.getSlug(), is("new-title"));
    assertThat(article.getDescription(), is("old desc"));
    assertThat(article.getBody(), is("old body"));
  }

  @Test
  public void should_only_update_description_and_leave_title_and_body_unchanged() {
    Article article =
        new Article("old title", "old desc", "old body", Arrays.asList("java"), "123");

    article.update(null, "new desc", null);

    assertThat(article.getTitle(), is("old title"));
    assertThat(article.getSlug(), is("old-title"));
    assertThat(article.getDescription(), is("new desc"));
    assertThat(article.getBody(), is("old body"));
  }

  @Test
  public void should_only_update_body_and_leave_title_and_description_unchanged() {
    Article article =
        new Article("old title", "old desc", "old body", Arrays.asList("java"), "123");

    article.update(null, null, "new body");

    assertThat(article.getTitle(), is("old title"));
    assertThat(article.getDescription(), is("old desc"));
    assertThat(article.getBody(), is("new body"));
  }

  @Test
  public void should_leave_everything_unchanged_when_all_update_args_are_empty() {
    Article article =
        new Article("old title", "old desc", "old body", Arrays.asList("java"), "123");

    article.update("", "", "");

    assertThat(article.getTitle(), is("old title"));
    assertThat(article.getSlug(), is("old-title"));
    assertThat(article.getDescription(), is("old desc"));
    assertThat(article.getBody(), is("old body"));
  }
}
