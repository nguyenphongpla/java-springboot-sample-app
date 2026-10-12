package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import io.spring.api.exception.NoAuthorizationException;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.CommentQueryService;
import io.spring.application.data.CommentData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.comment.Comment;
import io.spring.core.comment.CommentRepository;
import io.spring.core.user.User;
import io.spring.graphql.exception.AuthenticationException;
import io.spring.graphql.types.CommentPayload;
import io.spring.graphql.types.DeletionStatus;
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
public class CommentMutationTest {

  @Mock private ArticleRepository articleRepository;
  @Mock private CommentRepository commentRepository;
  @Mock private CommentQueryService commentQueryService;

  private User user;
  private Article article;

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

  @Test
  public void should_throw_authentication_exception_when_creating_comment_anonymously() {
    authenticateAnonymously();
    CommentMutation mutation =
        new CommentMutation(articleRepository, commentRepository, commentQueryService);

    assertThrows(
        AuthenticationException.class, () -> mutation.createComment("a-slug", "nice post"));
  }

  @Test
  public void should_throw_when_creating_comment_on_missing_article() {
    user = new User("test@test.com", "testuser", "123", "bio", "image");
    authenticateAs(user);
    when(articleRepository.findBySlug("missing-slug")).thenReturn(Optional.empty());

    CommentMutation mutation =
        new CommentMutation(articleRepository, commentRepository, commentQueryService);

    assertThrows(
        ResourceNotFoundException.class, () -> mutation.createComment("missing-slug", "nice post"));
  }

  @Test
  public void should_create_comment() {
    user = new User("test@test.com", "testuser", "123", "bio", "image");
    authenticateAs(user);
    article = new Article("title", "desc", "body", Collections.emptyList(), user.getId());
    when(articleRepository.findBySlug("a-slug")).thenReturn(Optional.of(article));
    CommentData commentData = new CommentData();
    commentData.setId("comment1");
    when(commentQueryService.findById(
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(user)))
        .thenReturn(Optional.of(commentData));

    CommentMutation mutation =
        new CommentMutation(articleRepository, commentRepository, commentQueryService);
    DataFetcherResult<CommentPayload> result = mutation.createComment("a-slug", "nice post");

    assertEquals(commentData, result.getLocalContext());
    org.mockito.Mockito.verify(commentRepository)
        .save(org.mockito.ArgumentMatchers.any(Comment.class));
  }

  @Test
  public void should_remove_comment_when_authorized() {
    user = new User("test@test.com", "testuser", "123", "bio", "image");
    authenticateAs(user);
    article = new Article("title", "desc", "body", Collections.emptyList(), user.getId());
    Comment comment = new Comment("nice post", user.getId(), article.getId());
    when(articleRepository.findBySlug("a-slug")).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    CommentMutation mutation =
        new CommentMutation(articleRepository, commentRepository, commentQueryService);
    DeletionStatus status = mutation.removeComment("a-slug", comment.getId());

    assertEquals(true, status.getSuccess());
    org.mockito.Mockito.verify(commentRepository).remove(comment);
  }

  @Test
  public void should_throw_no_authorization_when_removing_others_comment() {
    user = new User("test@test.com", "testuser", "123", "bio", "image");
    authenticateAs(user);
    User otherUser = new User("other@test.com", "otheruser", "123", "bio", "image");
    article = new Article("title", "desc", "body", Collections.emptyList(), otherUser.getId());
    Comment comment = new Comment("nice post", otherUser.getId(), article.getId());
    when(articleRepository.findBySlug("a-slug")).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    CommentMutation mutation =
        new CommentMutation(articleRepository, commentRepository, commentQueryService);

    assertThrows(
        NoAuthorizationException.class, () -> mutation.removeComment("a-slug", comment.getId()));
  }

  @Test
  public void should_throw_when_removing_missing_comment() {
    user = new User("test@test.com", "testuser", "123", "bio", "image");
    authenticateAs(user);
    article = new Article("title", "desc", "body", Collections.emptyList(), user.getId());
    when(articleRepository.findBySlug("a-slug")).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), "missing")).thenReturn(Optional.empty());

    CommentMutation mutation =
        new CommentMutation(articleRepository, commentRepository, commentQueryService);

    assertThrows(
        ResourceNotFoundException.class, () -> mutation.removeComment("a-slug", "missing"));
  }
}
