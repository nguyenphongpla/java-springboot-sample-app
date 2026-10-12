package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import graphql.schema.DataFetchingEnvironment;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.graphql.types.Article;
import io.spring.graphql.types.Comment;
import io.spring.graphql.types.Profile;
import io.spring.graphql.types.ProfilePayload;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class ProfileDatafetcherTest {

  @Mock private ProfileQueryService profileQueryService;
  @Mock private DataFetchingEnvironment dataFetchingEnvironment;

  @BeforeEach
  public void setUp() {
    AnonymousAuthenticationToken anonymous =
        new AnonymousAuthenticationToken(
            "key", "anonymousUser", Collections.singletonList(() -> "ROLE_ANONYMOUS"));
    SecurityContextHolder.getContext().setAuthentication(anonymous);
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_return_profile_for_user() {
    User user = new User("test@test.com", "testuser", "123", "bio", "image");
    when(dataFetchingEnvironment.<User>getLocalContext()).thenReturn(user);
    ProfileData profileData = new ProfileData("1", "testuser", "bio", "image", true);
    when(profileQueryService.findByUsername("testuser", null)).thenReturn(Optional.of(profileData));

    ProfileDatafetcher datafetcher = new ProfileDatafetcher(profileQueryService);
    Profile profile = datafetcher.getUserProfile(dataFetchingEnvironment);

    assertEquals("testuser", profile.getUsername());
    assertEquals("bio", profile.getBio());
    assertEquals("image", profile.getImage());
    assertEquals(true, profile.getFollowing());
  }

  @Test
  public void should_throw_when_user_profile_not_found() {
    User user = new User("test@test.com", "testuser", "123", "bio", "image");
    when(dataFetchingEnvironment.<User>getLocalContext()).thenReturn(user);
    when(profileQueryService.findByUsername("testuser", null)).thenReturn(Optional.empty());

    ProfileDatafetcher datafetcher = new ProfileDatafetcher(profileQueryService);

    assertThrows(
        ResourceNotFoundException.class, () -> datafetcher.getUserProfile(dataFetchingEnvironment));
  }

  @Test
  public void should_return_author_profile_for_article() {
    Article article = Article.newBuilder().slug("a-slug").build();
    when(dataFetchingEnvironment.<Article>getSource()).thenReturn(article);
    ArticleData articleData = new ArticleData();
    articleData.setProfileData(new ProfileData("1", "author", "bio", "image", false));
    Map<String, ArticleData> localContext = new HashMap<>();
    localContext.put("a-slug", articleData);
    when(dataFetchingEnvironment.<Map<String, ArticleData>>getLocalContext())
        .thenReturn(localContext);
    when(profileQueryService.findByUsername("author", null))
        .thenReturn(Optional.of(new ProfileData("1", "author", "bio", "image", false)));

    ProfileDatafetcher datafetcher = new ProfileDatafetcher(profileQueryService);
    Profile profile = datafetcher.getAuthor(dataFetchingEnvironment);

    assertEquals("author", profile.getUsername());
  }

  @Test
  public void should_return_author_profile_for_comment() {
    Comment comment = Comment.newBuilder().id("comment1").build();
    when(dataFetchingEnvironment.<Comment>getSource()).thenReturn(comment);
    CommentData commentData = new CommentData();
    commentData.setProfileData(new ProfileData("1", "commenter", "bio", "image", false));
    Map<String, CommentData> localContext = new HashMap<>();
    localContext.put("comment1", commentData);
    when(dataFetchingEnvironment.<Map<String, CommentData>>getLocalContext())
        .thenReturn(localContext);
    when(profileQueryService.findByUsername("commenter", null))
        .thenReturn(Optional.of(new ProfileData("1", "commenter", "bio", "image", false)));

    ProfileDatafetcher datafetcher = new ProfileDatafetcher(profileQueryService);
    Profile profile = datafetcher.getCommentAuthor(dataFetchingEnvironment);

    assertEquals("commenter", profile.getUsername());
  }

  @Test
  public void should_query_profile_by_username_argument() {
    when(dataFetchingEnvironment.getArgument("username")).thenReturn("testuser");
    ProfileData profileData = new ProfileData("1", "testuser", "bio", "image", false);
    when(profileQueryService.findByUsername("testuser", null)).thenReturn(Optional.of(profileData));

    ProfileDatafetcher datafetcher = new ProfileDatafetcher(profileQueryService);
    ProfilePayload payload = datafetcher.queryProfile("testuser", dataFetchingEnvironment);

    assertEquals("testuser", payload.getProfile().getUsername());
  }
}
