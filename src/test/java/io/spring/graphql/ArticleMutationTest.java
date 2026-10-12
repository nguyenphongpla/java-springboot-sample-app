package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import io.spring.api.exception.NoAuthorizationException;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.article.UpdateArticleParam;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.user.User;
import io.spring.graphql.exception.AuthenticationException;
import io.spring.graphql.types.ArticlePayload;
import io.spring.graphql.types.CreateArticleInput;
import io.spring.graphql.types.DeletionStatus;
import io.spring.graphql.types.UpdateArticleInput;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class ArticleMutationTest {

  @Mock private ArticleCommandService articleCommandService;
  @Mock private ArticleFavoriteRepository articleFavoriteRepository;
  @Mock private ArticleRepository articleRepository;

  private User user;

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticateAs(User user) {
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  private void authenticateAnonymously() {
    AnonymousAuthenticationToken anonymous =
        new AnonymousAuthenticationToken(
            "key", "anonymousUser", Collections.singletonList(() -> "ROLE_ANONYMOUS"));
    SecurityContextHolder.getContext().setAuthentication(anonymous);
  }

  private ArticleMutation newMutation() {
    return new ArticleMutation(articleCommandService, articleFavoriteRepository, articleRepository);
  }

  @Test
  public void should_throw_when_creating_article_anonymously() {
    authenticateAnonymously();
    CreateArticleInput input =
        CreateArticleInput.newBuilder().title("title").description("desc").body("body").build();

    assertThrows(AuthenticationException.class, () -> newMutation().createArticle(input));
  }

  @Test
  public void should_create_article() {
    user = new User("test@test.com", "testuser", "123", "bio", "image");
    authenticateAs(user);
    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("title")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java"))
            .build();
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), user.getId());
    when(articleCommandService.createArticle(any(), eq(user))).thenReturn(article);

    DataFetcherResult<ArticlePayload> result = newMutation().createArticle(input);

    assertEquals(article, result.getLocalContext());
  }

  @Test
  public void should_throw_when_updating_missing_article() {
    user = new User("test@test.com", "testuser", "123", "bio", "image");
    authenticateAs(user);
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());
    UpdateArticleInput input = UpdateArticleInput.newBuilder().title("new title").build();

    assertThrows(
        ResourceNotFoundException.class, () -> newMutation().updateArticle("missing", input));
  }

  @Test
  public void should_throw_no_authorization_when_updating_others_article() {
    user = new User("test@test.com", "testuser", "123", "bio", "image");
    authenticateAs(user);
    User otherUser = new User("other@test.com", "otheruser", "123", "bio", "image");
    Article article =
        new Article("title", "desc", "body", Collections.emptyList(), otherUser.getId());
    when(articleRepository.findBySlug("a-slug")).thenReturn(Optional.of(article));
    UpdateArticleInput input = UpdateArticleInput.newBuilder().title("new title").build();

    assertThrows(
        NoAuthorizationException.class, () -> newMutation().updateArticle("a-slug", input));
  }

  @Test
  public void should_update_article_when_authorized() {
    user = new User("test@test.com", "testuser", "123", "bio", "image");
    authenticateAs(user);
    Article article = new Article("title", "desc", "body", Collections.emptyList(), user.getId());
    when(articleRepository.findBySlug("a-slug")).thenReturn(Optional.of(article));
    UpdateArticleInput input =
        UpdateArticleInput.newBuilder()
            .title("new title")
            .body("new body")
            .description("new desc")
            .build();
    when(articleCommandService.updateArticle(eq(article), any(UpdateArticleParam.class)))
        .thenReturn(article);

    DataFetcherResult<ArticlePayload> result = newMutation().updateArticle("a-slug", input);

    assertEquals(article, result.getLocalContext());
    org.mockito.ArgumentCaptor<UpdateArticleParam> captor =
        org.mockito.ArgumentCaptor.forClass(UpdateArticleParam.class);
    verify(articleCommandService).updateArticle(eq(article), captor.capture());
    assertEquals("new title", captor.getValue().getTitle());
    assertEquals("new body", captor.getValue().getBody());
    assertEquals("new desc", captor.getValue().getDescription());
  }

  @Test
  public void should_favorite_article() {
    user = new User("test@test.com", "testuser", "123", "bio", "image");
    authenticateAs(user);
    Article article = new Article("title", "desc", "body", Collections.emptyList(), user.getId());
    when(articleRepository.findBySlug("a-slug")).thenReturn(Optional.of(article));

    DataFetcherResult<ArticlePayload> result = newMutation().favoriteArticle("a-slug");

    assertEquals(article, result.getLocalContext());
    verify(articleFavoriteRepository).save(new ArticleFavorite(article.getId(), user.getId()));
  }

  @Test
  public void should_unfavorite_article_when_favorited() {
    user = new User("test@test.com", "testuser", "123", "bio", "image");
    authenticateAs(user);
    Article article = new Article("title", "desc", "body", Collections.emptyList(), user.getId());
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());
    when(articleRepository.findBySlug("a-slug")).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(article.getId(), user.getId()))
        .thenReturn(Optional.of(favorite));

    DataFetcherResult<ArticlePayload> result = newMutation().unfavoriteArticle("a-slug");

    assertEquals(article, result.getLocalContext());
    verify(articleFavoriteRepository).remove(favorite);
  }

  @Test
  public void should_noop_when_unfavoriting_article_not_favorited() {
    user = new User("test@test.com", "testuser", "123", "bio", "image");
    authenticateAs(user);
    Article article = new Article("title", "desc", "body", Collections.emptyList(), user.getId());
    when(articleRepository.findBySlug("a-slug")).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(article.getId(), user.getId()))
        .thenReturn(Optional.empty());

    newMutation().unfavoriteArticle("a-slug");

    verify(articleFavoriteRepository, never()).remove(any());
  }

  @Test
  public void should_delete_article_when_authorized() {
    user = new User("test@test.com", "testuser", "123", "bio", "image");
    authenticateAs(user);
    Article article = new Article("title", "desc", "body", Collections.emptyList(), user.getId());
    when(articleRepository.findBySlug("a-slug")).thenReturn(Optional.of(article));

    DeletionStatus status = newMutation().deleteArticle("a-slug");

    assertEquals(true, status.getSuccess());
    verify(articleRepository).remove(article);
  }

  @Test
  public void should_throw_no_authorization_when_deleting_others_article() {
    user = new User("test@test.com", "testuser", "123", "bio", "image");
    authenticateAs(user);
    User otherUser = new User("other@test.com", "otheruser", "123", "bio", "image");
    Article article =
        new Article("title", "desc", "body", Collections.emptyList(), otherUser.getId());
    when(articleRepository.findBySlug("a-slug")).thenReturn(Optional.of(article));

    assertThrows(NoAuthorizationException.class, () -> newMutation().deleteArticle("a-slug"));
  }
}
