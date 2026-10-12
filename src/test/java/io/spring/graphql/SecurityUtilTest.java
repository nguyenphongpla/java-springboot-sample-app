package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.spring.core.user.User;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtilTest {

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_return_empty_when_user_is_anonymous() {
    AnonymousAuthenticationToken anonymous =
        new AnonymousAuthenticationToken(
            "key", "anonymousUser", Collections.singletonList(() -> "ROLE_ANONYMOUS"));
    SecurityContextHolder.getContext().setAuthentication(anonymous);

    Optional<User> currentUser = SecurityUtil.getCurrentUser();

    assertFalse(currentUser.isPresent());
  }

  @Test
  public void should_return_current_user_when_authenticated() {
    User user = new User("test@test.com", "testuser", "123", "bio", "image");
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(authentication);

    Optional<User> currentUser = SecurityUtil.getCurrentUser();

    assertTrue(currentUser.isPresent());
    assertEquals(user, currentUser.get());
  }
}
