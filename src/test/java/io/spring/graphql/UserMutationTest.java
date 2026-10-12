package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.application.user.RegisterParam;
import io.spring.application.user.UserService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.types.CreateUserInput;
import io.spring.graphql.types.Error;
import io.spring.graphql.types.UpdateUserInput;
import io.spring.graphql.types.UserPayload;
import io.spring.graphql.types.UserResult;
import java.util.Collections;
import java.util.Optional;
import javax.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class UserMutationTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder encryptService;
  @Mock private UserService userService;

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private UserMutation newMutation() {
    return new UserMutation(userRepository, encryptService, userService);
  }

  @Test
  public void should_create_user() {
    CreateUserInput input =
        CreateUserInput.newBuilder()
            .email("test@test.com")
            .username("testuser")
            .password("123456")
            .build();
    User user = new User("test@test.com", "testuser", "123456", "", "image");
    when(userService.createUser(any(RegisterParam.class))).thenReturn(user);

    DataFetcherResult<UserResult> result = newMutation().createUser(input);

    assertEquals(user, result.getLocalContext());
    assertTrue(result.getData() instanceof UserPayload);
  }

  @Test
  public void should_return_error_data_when_create_user_validation_fails() {
    CreateUserInput input =
        CreateUserInput.newBuilder()
            .email("bad-email")
            .username("testuser")
            .password("123456")
            .build();
    when(userService.createUser(any(RegisterParam.class)))
        .thenThrow(new ConstraintViolationException(Collections.emptySet()));

    DataFetcherResult<UserResult> result = newMutation().createUser(input);

    assertTrue(result.getData() instanceof Error);
  }

  @Test
  public void should_login_with_valid_credentials() {
    User user = new User("test@test.com", "testuser", "encoded", "", "image");
    when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
    when(encryptService.matches("123456", "encoded")).thenReturn(true);

    DataFetcherResult<UserPayload> result = newMutation().login("123456", "test@test.com");

    assertEquals(user, result.getLocalContext());
  }

  @Test
  public void should_throw_when_login_with_unknown_email() {
    when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

    assertThrows(
        InvalidAuthenticationException.class,
        () -> newMutation().login("123456", "missing@test.com"));
  }

  @Test
  public void should_throw_when_login_with_wrong_password() {
    User user = new User("test@test.com", "testuser", "encoded", "", "image");
    when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
    when(encryptService.matches("wrong", "encoded")).thenReturn(false);

    assertThrows(
        InvalidAuthenticationException.class, () -> newMutation().login("wrong", "test@test.com"));
  }

  @Test
  public void should_return_null_when_updating_user_anonymously() {
    AnonymousAuthenticationToken anonymous =
        new AnonymousAuthenticationToken(
            "key", "anonymousUser", Collections.singletonList(() -> "ROLE_ANONYMOUS"));
    SecurityContextHolder.getContext().setAuthentication(anonymous);
    UpdateUserInput input = UpdateUserInput.newBuilder().bio("new bio").build();

    DataFetcherResult<UserPayload> result = newMutation().updateUser(input);

    assertNull(result);
  }

  @Test
  public void should_update_user_when_authenticated() {
    User user = new User("test@test.com", "testuser", "123", "bio", "image");
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(authentication);
    UpdateUserInput input = UpdateUserInput.newBuilder().bio("new bio").build();

    DataFetcherResult<UserPayload> result = newMutation().updateUser(input);

    assertEquals(user, result.getLocalContext());
  }
}
