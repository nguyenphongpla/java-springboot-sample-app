package io.spring.api.security;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class JwtTokenFilterTest {

  @Mock private UserRepository userRepository;
  @Mock private JwtService jwtService;
  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private FilterChain filterChain;

  private JwtTokenFilter newFilter() {
    JwtTokenFilter filter = new JwtTokenFilter();
    ReflectionTestUtils.setField(filter, "userRepository", userRepository);
    ReflectionTestUtils.setField(filter, "jwtService", jwtService);
    return filter;
  }

  @BeforeEach
  public void setUp() {
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_not_authenticate_when_header_missing() throws Exception {
    when(request.getHeader("Authorization")).thenReturn(null);

    newFilter().doFilterInternal(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication(), nullValue());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  public void should_not_authenticate_when_header_has_no_token_part() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Token");

    newFilter().doFilterInternal(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication(), nullValue());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  public void should_not_authenticate_when_token_invalid() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Token abc");
    when(jwtService.getSubFromToken("abc")).thenReturn(Optional.empty());

    newFilter().doFilterInternal(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication(), nullValue());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  public void should_not_authenticate_when_user_no_longer_exists() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Token abc");
    when(jwtService.getSubFromToken("abc")).thenReturn(Optional.of("missing-user-id"));
    when(userRepository.findById("missing-user-id")).thenReturn(Optional.empty());

    newFilter().doFilterInternal(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication(), nullValue());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  public void should_authenticate_with_valid_token() throws Exception {
    User user = new User("test@test.com", "testuser", "123", "bio", "image");
    when(request.getHeader("Authorization")).thenReturn("Token abc");
    when(jwtService.getSubFromToken("abc")).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

    newFilter().doFilterInternal(request, response, filterChain);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication, is(org.hamcrest.CoreMatchers.notNullValue()));
    assertThat(authentication.getPrincipal(), is(user));
    verify(filterChain).doFilter(request, response);
  }

  @Test
  public void should_not_overwrite_existing_authentication() throws Exception {
    User existing = new User("existing@test.com", "existing", "123", "bio", "image");
    UsernamePasswordAuthenticationToken existingAuth =
        new UsernamePasswordAuthenticationToken(existing, null, java.util.Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(existingAuth);

    User other = new User("other@test.com", "other", "123", "bio", "image");
    lenient().when(request.getHeader("Authorization")).thenReturn("Token abc");
    lenient().when(jwtService.getSubFromToken("abc")).thenReturn(Optional.of(other.getId()));

    newFilter().doFilterInternal(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication(), is(existingAuth));
    verify(userRepository, never()).findById(other.getId());
    verify(filterChain).doFilter(request, response);
  }
}
