package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import graphql.execution.DataFetcherResult;
import graphql.relay.PageInfo;
import graphql.schema.DataFetchingEnvironment;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.types.Article;
import io.spring.graphql.types.ArticlesConnection;
import io.spring.graphql.types.Profile;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class ArticleDatafetcherTest {

  @Mock private ArticleQueryService articleQueryService;
  @Mock private UserRepository userRepository;
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

  private void authenticateAs(User user) {
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  private ArticleDatafetcher newDatafetcher() {
    return new ArticleDatafetcher(articleQueryService, userRepository);
  }

  private ArticleData buildArticleData(String slug) {
    ArticleData articleData = new ArticleData();
    articleData.setId(slug + "-id");
    articleData.setSlug(slug);
    articleData.setTitle("title-" + slug);
    articleData.setDescription("desc");
    articleData.setBody("body");
    articleData.setFavorited(false);
    articleData.setFavoritesCount(0);
    articleData.setCreatedAt(new DateTime());
    articleData.setUpdatedAt(new DateTime());
    articleData.setTagList(Collections.emptyList());
    articleData.setProfileData(new ProfileData("author1", "author", "bio", "image", false));
    return articleData;
  }

  @Test
  public void should_throw_when_feed_missing_first_and_last() {
    assertThrows(
        IllegalArgumentException.class,
        () -> newDatafetcher().getFeed(null, null, null, null, null));
  }

  @Test
  public void should_return_feed_for_current_user() {
    User user = new User("test@test.com", "testuser", "123", "bio", "image");
    authenticateAs(user);
    ArticleData articleData = buildArticleData("a-slug");
    CursorPager<ArticleData> pager = new CursorPager<>(List.of(articleData), Direction.NEXT, false);
    when(articleQueryService.findUserFeedWithCursor(eq(user), any())).thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        newDatafetcher().getFeed(5, null, null, null, null);

    assertEquals(1, result.getData().getEdges().size());
    assertEquals("a-slug", result.getData().getEdges().get(0).getNode().getSlug());
    PageInfo pageInfo = result.getData().getPageInfo();
    assertEquals(false, pageInfo.isHasNextPage());
  }

  @Test
  public void should_return_user_feed_for_profile() {
    Profile profile = Profile.newBuilder().username("target").build();
    when(dataFetchingEnvironment.<Profile>getSource()).thenReturn(profile);
    User target = new User("target@test.com", "target", "123", "bio", "image");
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
    ArticleData articleData = buildArticleData("b-slug");
    CursorPager<ArticleData> pager = new CursorPager<>(List.of(articleData), Direction.NEXT, false);
    when(articleQueryService.findUserFeedWithCursor(eq(target), any())).thenReturn(pager);

    DgsDataFetchingEnvironment dfe = new DgsDataFetchingEnvironment(dataFetchingEnvironment);
    DataFetcherResult<ArticlesConnection> result =
        newDatafetcher().userFeed(5, null, null, null, dfe);

    assertEquals(1, result.getData().getEdges().size());
    assertEquals("b-slug", result.getData().getEdges().get(0).getNode().getSlug());
  }

  @Test
  public void should_throw_when_user_feed_profile_missing() {
    Profile profile = Profile.newBuilder().username("missing").build();
    when(dataFetchingEnvironment.<Profile>getSource()).thenReturn(profile);
    when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

    DgsDataFetchingEnvironment dfe = new DgsDataFetchingEnvironment(dataFetchingEnvironment);
    assertThrows(
        ResourceNotFoundException.class, () -> newDatafetcher().userFeed(5, null, null, null, dfe));
  }

  @Test
  public void should_return_user_favorites_for_profile() {
    Profile profile = Profile.newBuilder().username("target").build();
    when(dataFetchingEnvironment.<Profile>getSource()).thenReturn(profile);
    ArticleData articleData = buildArticleData("c-slug");
    CursorPager<ArticleData> pager = new CursorPager<>(List.of(articleData), Direction.NEXT, false);
    when(articleQueryService.findRecentArticlesWithCursor(
            eq(null), eq(null), eq("target"), any(), eq(null)))
        .thenReturn(pager);

    DgsDataFetchingEnvironment dfe = new DgsDataFetchingEnvironment(dataFetchingEnvironment);
    DataFetcherResult<ArticlesConnection> result =
        newDatafetcher().userFavorites(5, null, null, null, dfe);

    assertEquals(1, result.getData().getEdges().size());
    assertEquals("c-slug", result.getData().getEdges().get(0).getNode().getSlug());
  }

  @Test
  public void should_return_user_articles_for_profile() {
    Profile profile = Profile.newBuilder().username("target").build();
    when(dataFetchingEnvironment.<Profile>getSource()).thenReturn(profile);
    ArticleData articleData = buildArticleData("d-slug");
    CursorPager<ArticleData> pager = new CursorPager<>(List.of(articleData), Direction.NEXT, false);
    when(articleQueryService.findRecentArticlesWithCursor(
            eq(null), eq("target"), eq(null), any(), eq(null)))
        .thenReturn(pager);

    DgsDataFetchingEnvironment dfe = new DgsDataFetchingEnvironment(dataFetchingEnvironment);
    DataFetcherResult<ArticlesConnection> result =
        newDatafetcher().userArticles(5, null, null, null, dfe);

    assertEquals(1, result.getData().getEdges().size());
    assertEquals("d-slug", result.getData().getEdges().get(0).getNode().getSlug());
  }

  @Test
  public void should_throw_when_articles_missing_first_and_last() {
    assertThrows(
        IllegalArgumentException.class,
        () -> newDatafetcher().getArticles(null, null, null, null, null, null, null, null));
  }

  @Test
  public void should_return_recent_articles() {
    ArticleData articleData = buildArticleData("e-slug");
    CursorPager<ArticleData> pager = new CursorPager<>(List.of(articleData), Direction.NEXT, false);
    when(articleQueryService.findRecentArticlesWithCursor(
            eq("java"), eq("author"), eq("fan"), any(), eq(null)))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        newDatafetcher().getArticles(5, null, null, null, "author", "fan", "java", null);

    assertEquals(1, result.getData().getEdges().size());
    assertEquals("e-slug", result.getData().getEdges().get(0).getNode().getSlug());
  }

  @Test
  public void should_resolve_article_payload_article() {
    io.spring.core.article.Article coreArticle =
        new io.spring.core.article.Article(
            "title", "desc", "body", Collections.emptyList(), "user1");
    when(dataFetchingEnvironment.<io.spring.core.article.Article>getLocalContext())
        .thenReturn(coreArticle);
    ArticleData articleData = buildArticleData(coreArticle.getSlug());
    articleData.setId(coreArticle.getId());
    when(articleQueryService.findById(coreArticle.getId(), null))
        .thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result = newDatafetcher().getArticle(dataFetchingEnvironment);

    assertEquals(coreArticle.getSlug(), result.getData().getSlug());
  }

  @Test
  public void should_throw_when_article_payload_article_missing() {
    io.spring.core.article.Article coreArticle =
        new io.spring.core.article.Article(
            "title", "desc", "body", Collections.emptyList(), "user1");
    when(dataFetchingEnvironment.<io.spring.core.article.Article>getLocalContext())
        .thenReturn(coreArticle);
    when(articleQueryService.findById(coreArticle.getId(), null)).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> newDatafetcher().getArticle(dataFetchingEnvironment));
  }

  @Test
  public void should_resolve_comment_article() {
    CommentData commentData = new CommentData();
    commentData.setId("comment1");
    commentData.setArticleId("article1");
    when(dataFetchingEnvironment.<CommentData>getLocalContext()).thenReturn(commentData);
    ArticleData articleData = buildArticleData("f-slug");
    articleData.setId("article1");
    when(articleQueryService.findById("article1", null)).thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result = newDatafetcher().getCommentArticle(dataFetchingEnvironment);

    assertEquals("f-slug", result.getData().getSlug());
  }

  @Test
  public void should_find_article_by_slug() {
    ArticleData articleData = buildArticleData("g-slug");
    when(articleQueryService.findBySlug("g-slug", null)).thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result = newDatafetcher().findArticleBySlug("g-slug");

    assertEquals("g-slug", result.getData().getSlug());
    Map<String, ArticleData> localContext = (Map<String, ArticleData>) result.getLocalContext();
    assertEquals(articleData, localContext.get("g-slug"));
  }

  @Test
  public void should_throw_when_article_slug_missing() {
    when(articleQueryService.findBySlug("missing", null)).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> newDatafetcher().findArticleBySlug("missing"));
  }
}
