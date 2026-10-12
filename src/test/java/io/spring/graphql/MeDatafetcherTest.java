package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import io.spring.application.UserQueryService;
import io.spring.application.data.UserData;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
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
public class MeDatafetcherTest {

  @Mock private UserQueryService userQueryService;
  @Mock private JwtService jwtService;
  @Mock private DataFetchingEnvironment dataFetchingEnvironment;

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_return_null_when_anonymous() {
    AnonymousAuthenticationToken anonymous =
        new AnonymousAuthenticationToken(
            "key", "anonymousUser", Collections.singletonList(() -> "ROLE_ANONYMOUS"));
    SecurityContextHolder.getContext().setAuthentication(anonymous);

    MeDatafetcher meDatafetcher = new MeDatafetcher(userQueryService, jwtService);
    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Bearer token123", dataFetchingEnvironment);

    assertNull(result);
  }

  @Test
  public void should_return_current_user_with_token_from_authorization_header() {
    User user = new User("test@test.com", "testuser", "123", "bio", "image");
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(authentication);

    UserData userData = new UserData(user.getId(), "test@test.com", "testuser", "bio", "image");
    when(userQueryService.findById(user.getId())).thenReturn(Optional.of(userData));

    MeDatafetcher meDatafetcher = new MeDatafetcher(userQueryService, jwtService);
    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getMe("Bearer token123", dataFetchingEnvironment);

    assertEquals("test@test.com", result.getData().getEmail());
    assertEquals("testuser", result.getData().getUsername());
    assertEquals("token123", result.getData().getToken());
    assertEquals(user, result.getLocalContext());
  }

  @Test
  public void should_build_user_payload_with_fresh_token() {
    User user = new User("test@test.com", "testuser", "123", "bio", "image");
    when(dataFetchingEnvironment.<User>getLocalContext()).thenReturn(user);
    when(jwtService.toToken(user)).thenReturn("newToken");

    MeDatafetcher meDatafetcher = new MeDatafetcher(userQueryService, jwtService);
    DataFetcherResult<io.spring.graphql.types.User> result =
        meDatafetcher.getUserPayloadUser(dataFetchingEnvironment);

    assertEquals("test@test.com", result.getData().getEmail());
    assertEquals("testuser", result.getData().getUsername());
    assertEquals("newToken", result.getData().getToken());
  }
}
