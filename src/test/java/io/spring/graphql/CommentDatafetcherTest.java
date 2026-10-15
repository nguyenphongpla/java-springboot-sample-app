package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import io.spring.application.CommentQueryService;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.graphql.types.Article;
import io.spring.graphql.types.Comment;
import io.spring.graphql.types.CommentsConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class CommentDatafetcherTest {

  @Mock private CommentQueryService commentQueryService;
  @Mock private DataFetchingEnvironment dataFetchingEnvironment;
  @Mock private org.springframework.context.ApplicationContext applicationContext;

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

  private CommentData buildCommentData(String id) {
    CommentData commentData = new CommentData();
    commentData.setId(id);
    commentData.setBody("body-" + id);
    commentData.setArticleId("article1");
    commentData.setCreatedAt(new DateTime());
    commentData.setUpdatedAt(new DateTime());
    commentData.setProfileData(new ProfileData("author1", "author", "bio", "image", false));
    return commentData;
  }

  @Test
  public void should_build_comment_from_local_context() {
    CommentData commentData = buildCommentData("comment1");
    DgsDataFetchingEnvironment dfe =
        new DgsDataFetchingEnvironment(dataFetchingEnvironment, applicationContext);
    when(dataFetchingEnvironment.<CommentData>getLocalContext()).thenReturn(commentData);

    CommentDatafetcher datafetcher = new CommentDatafetcher(commentQueryService);
    DataFetcherResult<Comment> result = datafetcher.getComment(dfe);

    assertEquals("comment1", result.getData().getId());
    assertEquals("body-comment1", result.getData().getBody());
    Map<String, CommentData> localContext = (Map<String, CommentData>) result.getLocalContext();
    assertEquals(commentData, localContext.get("comment1"));
  }

  @Test
  public void should_throw_when_neither_first_nor_last_provided() {
    DgsDataFetchingEnvironment dfe =
        new DgsDataFetchingEnvironment(dataFetchingEnvironment, applicationContext);

    CommentDatafetcher datafetcher = new CommentDatafetcher(commentQueryService);

    assertThrows(
        IllegalArgumentException.class,
        () -> datafetcher.articleComments(null, null, null, null, dfe));
  }

  @Test
  public void should_return_article_comments_with_cursor() {
    Article article = Article.newBuilder().slug("a-slug").build();
    when(dataFetchingEnvironment.<Article>getSource()).thenReturn(article);
    ArticleData articleData = new ArticleData();
    articleData.setId("article1");
    Map<String, ArticleData> articleLocalContext = new HashMap<>();
    articleLocalContext.put("a-slug", articleData);
    when(dataFetchingEnvironment.<Map<String, ArticleData>>getLocalContext())
        .thenReturn(articleLocalContext);

    CommentData commentData = buildCommentData("comment1");
    CursorPager<CommentData> pager = new CursorPager<>(List.of(commentData), Direction.NEXT, false);
    when(commentQueryService.findByArticleIdWithCursor(
            org.mockito.ArgumentMatchers.eq("article1"),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.any()))
        .thenReturn(pager);

    DgsDataFetchingEnvironment dfe =
        new DgsDataFetchingEnvironment(dataFetchingEnvironment, applicationContext);
    CommentDatafetcher datafetcher = new CommentDatafetcher(commentQueryService);
    DataFetcherResult<CommentsConnection> result =
        datafetcher.articleComments(5, null, null, null, dfe);

    assertEquals(1, result.getData().getEdges().size());
    assertEquals("comment1", result.getData().getEdges().get(0).getNode().getId());
  }
}
