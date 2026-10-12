package io.spring.core.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import io.spring.core.article.Article;
import io.spring.core.comment.Comment;
import io.spring.core.user.User;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class AuthorizationServiceTest {

  private User newUser(String username) {
    return new User(username + "@test.com", username, "123", "bio", "image");
  }

  @Test
  public void article_owner_can_write_article() {
    User owner = newUser("owner");
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), owner.getId());
    assertThat(AuthorizationService.canWriteArticle(owner, article), is(true));
  }

  @Test
  public void non_owner_cannot_write_article() {
    User owner = newUser("owner");
    User other = newUser("other");
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), owner.getId());
    assertThat(AuthorizationService.canWriteArticle(other, article), is(false));
  }

  @Test
  public void article_owner_can_write_comment_even_if_not_comment_author() {
    User articleOwner = newUser("articleOwner");
    User commentAuthor = newUser("commentAuthor");
    Article article =
        new Article("title", "desc", "body", Arrays.asList("java"), articleOwner.getId());
    Comment comment = new Comment("a comment", commentAuthor.getId(), article.getId());

    assertThat(AuthorizationService.canWriteComment(articleOwner, article, comment), is(true));
  }

  @Test
  public void comment_author_can_write_comment_even_if_not_article_owner() {
    User articleOwner = newUser("articleOwner");
    User commentAuthor = newUser("commentAuthor");
    Article article =
        new Article("title", "desc", "body", Arrays.asList("java"), articleOwner.getId());
    Comment comment = new Comment("a comment", commentAuthor.getId(), article.getId());

    assertThat(AuthorizationService.canWriteComment(commentAuthor, article, comment), is(true));
  }

  @Test
  public void unrelated_user_cannot_write_comment() {
    User articleOwner = newUser("articleOwner");
    User commentAuthor = newUser("commentAuthor");
    User stranger = newUser("stranger");
    Article article =
        new Article("title", "desc", "body", Arrays.asList("java"), articleOwner.getId());
    Comment comment = new Comment("a comment", commentAuthor.getId(), article.getId());

    assertThat(AuthorizationService.canWriteComment(stranger, article, comment), is(false));
  }
}
