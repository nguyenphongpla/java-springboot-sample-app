package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.application.TagsQueryService;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TagDatafetcherTest {

  @Mock private TagsQueryService tagsQueryService;

  @Test
  public void should_delegate_to_tags_query_service() {
    List<String> tags = Arrays.asList("java", "spring");
    when(tagsQueryService.allTags()).thenReturn(tags);

    TagDatafetcher tagDatafetcher = new TagDatafetcher(tagsQueryService);
    List<String> result = tagDatafetcher.getTags();

    assertEquals(tags, result);
    verify(tagsQueryService).allTags();
  }
}
