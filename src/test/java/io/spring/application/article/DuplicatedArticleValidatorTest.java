package io.spring.application.article;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import io.spring.application.ArticleQueryService;
import io.spring.application.data.ArticleData;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class DuplicatedArticleValidatorTest {

  @Mock private ArticleQueryService articleQueryService;

  private DuplicatedArticleValidator newValidator() {
    DuplicatedArticleValidator validator = new DuplicatedArticleValidator();
    ReflectionTestUtils.setField(validator, "articleQueryService", articleQueryService);
    return validator;
  }

  @Test
  public void should_not_throw_and_be_valid_when_title_is_null() {
    assertThat(newValidator().isValid(null, null), is(true));
  }

  @Test
  public void should_not_throw_and_be_valid_when_title_is_empty() {
    assertThat(newValidator().isValid("", null), is(true));
  }

  @Test
  public void should_be_valid_when_slug_not_already_taken() {
    when(articleQueryService.findBySlug("a-new-title", null)).thenReturn(Optional.empty());

    assertThat(newValidator().isValid("a new title", null), is(true));
  }

  @Test
  public void should_be_invalid_when_slug_already_taken() {
    when(articleQueryService.findBySlug("a-new-title", null))
        .thenReturn(Optional.of(new ArticleData()));

    assertThat(newValidator().isValid("a new title", null), is(false));
  }
}
