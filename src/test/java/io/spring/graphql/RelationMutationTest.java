package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.exception.AuthenticationException;
import io.spring.graphql.types.ProfilePayload;
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
public class RelationMutationTest {

  @Mock private UserRepository userRepository;
  @Mock private ProfileQueryService profileQueryService;

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

  private RelationMutation newMutation() {
    return new RelationMutation(userRepository, profileQueryService);
  }

  @Test
  public void should_throw_when_following_anonymously() {
    authenticateAnonymously();

    assertThrows(AuthenticationException.class, () -> newMutation().follow("target"));
  }

  @Test
  public void should_throw_when_following_missing_user() {
    user = new User("test@test.com", "testuser", "123", "bio", "image");
    authenticateAs(user);
    when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> newMutation().follow("missing"));
  }

  @Test
  public void should_follow_user() {
    user = new User("test@test.com", "testuser", "123", "bio", "image");
    authenticateAs(user);
    User target = new User("target@test.com", "target", "123", "bio", "image");
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
    when(profileQueryService.findByUsername("target", user))
        .thenReturn(Optional.of(new ProfileData("t1", "target", "bio", "image", true)));

    ProfilePayload payload = newMutation().follow("target");

    assertEquals("target", payload.getProfile().getUsername());
    verify(userRepository).saveRelation(new FollowRelation(user.getId(), target.getId()));
  }

  @Test
  public void should_throw_when_unfollowing_missing_user() {
    user = new User("test@test.com", "testuser", "123", "bio", "image");
    authenticateAs(user);
    when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> newMutation().unfollow("missing"));
  }

  @Test
  public void should_throw_when_unfollowing_user_not_followed() {
    user = new User("test@test.com", "testuser", "123", "bio", "image");
    authenticateAs(user);
    User target = new User("target@test.com", "target", "123", "bio", "image");
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
    when(userRepository.findRelation(user.getId(), target.getId())).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> newMutation().unfollow("target"));
  }

  @Test
  public void should_unfollow_user() {
    user = new User("test@test.com", "testuser", "123", "bio", "image");
    authenticateAs(user);
    User target = new User("target@test.com", "target", "123", "bio", "image");
    FollowRelation relation = new FollowRelation(user.getId(), target.getId());
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
    when(userRepository.findRelation(user.getId(), target.getId()))
        .thenReturn(Optional.of(relation));
    when(profileQueryService.findByUsername("target", user))
        .thenReturn(Optional.of(new ProfileData("t1", "target", "bio", "image", false)));

    ProfilePayload payload = newMutation().unfollow("target");

    assertEquals("target", payload.getProfile().getUsername());
    verify(userRepository).removeRelation(relation);
  }
}
