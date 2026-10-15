package io.spring.application.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;

  private UserService newService() {
    return new UserService(userRepository, "default-image", passwordEncoder);
  }

  @Test
  public void should_encode_password_on_create() {
    when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");
    RegisterParam registerParam = new RegisterParam("test@test.com", "testuser", "plain-password");

    User user = newService().createUser(registerParam);

    assertThat(user.getPassword(), is("encoded-password"));
  }

  @Test
  public void should_encode_new_password_on_update() {
    User target = new User("test@test.com", "testuser", "old-encoded-password", "bio", "image");
    when(passwordEncoder.encode("new-plain-password")).thenReturn("new-encoded-password");
    UpdateUserParam param = UpdateUserParam.builder().password("new-plain-password").build();

    newService().updateUser(new UpdateUserCommand(target, param));

    assertThat(target.getPassword(), is("new-encoded-password"));
    verify(userRepository).save(target);
  }

  @Test
  public void should_not_change_password_when_update_param_has_no_password() {
    User target = new User("test@test.com", "testuser", "old-encoded-password", "bio", "image");
    UpdateUserParam param = UpdateUserParam.builder().bio("new bio").build();

    newService().updateUser(new UpdateUserCommand(target, param));

    assertThat(target.getPassword(), is("old-encoded-password"));
    assertThat(target.getPassword(), not(is("")));
    verify(passwordEncoder, never()).encode(any());
    verify(userRepository).save(target);
  }
}
