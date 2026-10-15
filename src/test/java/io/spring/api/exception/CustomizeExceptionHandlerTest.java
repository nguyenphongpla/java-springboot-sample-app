package io.spring.api.exception;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.metadata.ConstraintDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

@ExtendWith(MockitoExtension.class)
public class CustomizeExceptionHandlerTest {

  private final CustomizeExceptionHandler handler = new CustomizeExceptionHandler();

  private static class Dummy {
    @NotBlank private String email;
  }

  private void dummyMethod(String arg) {}

  @Test
  public void should_map_invalid_request_exception_to_unprocessable_entity() {
    BeanPropertyBindingResult errors = new BeanPropertyBindingResult(new Dummy(), "dummy");
    errors.addError(
        new FieldError(
            "dummy", "email", null, false, new String[] {"NotBlank"}, null, "can't be empty"));
    InvalidRequestException exception = new InvalidRequestException(errors);

    ResponseEntity<Object> response =
        handler.handleInvalidRequest(exception, Mockito.mock(WebRequest.class));

    assertThat(response.getStatusCode(), is(HttpStatus.UNPROCESSABLE_ENTITY));
    ErrorResource body = (ErrorResource) response.getBody();
    assertThat(body.getFieldErrors().size(), is(1));
    FieldErrorResource fieldError = body.getFieldErrors().get(0);
    assertThat(fieldError.getField(), is("email"));
    assertThat(fieldError.getMessage(), is("can't be empty"));
  }

  @Test
  public void should_map_invalid_authentication_exception_to_unprocessable_entity_with_message() {
    ResponseEntity<Object> response =
        handler.handleInvalidAuthentication(
            new InvalidAuthenticationException(), Mockito.mock(WebRequest.class));

    assertThat(response.getStatusCode(), is(HttpStatus.UNPROCESSABLE_ENTITY));
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertThat(body.get("message"), is("invalid email or password"));
  }

  @Test
  public void should_map_method_argument_not_valid_to_field_errors() throws Exception {
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Dummy(), "dummy");
    bindingResult.addError(
        new FieldError(
            "dummy", "email", null, false, new String[] {"NotBlank"}, null, "can't be empty"));
    Method method = getClass().getDeclaredMethod("dummyMethod", String.class);
    MethodArgumentNotValidException exception =
        new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);

    ResponseEntity<Object> response =
        handler.handleMethodArgumentNotValid(
            exception, new HttpHeaders(), HttpStatus.BAD_REQUEST, Mockito.mock(WebRequest.class));

    assertThat(response.getStatusCode(), is(HttpStatus.UNPROCESSABLE_ENTITY));
    ErrorResource body = (ErrorResource) response.getBody();
    assertThat(body.getFieldErrors().size(), is(1));
    assertThat(body.getFieldErrors().get(0).getField(), is("email"));
  }

  @Test
  public void should_map_constraint_violation_exception_to_field_errors_stripping_method_and_arg() {
    ConstraintViolation<?> violation = buildViolation("updateUser.arg0.email", "must not be blank");
    ConstraintViolationException exception =
        new ConstraintViolationException(Collections.singleton(violation));

    ErrorResource errorResource =
        handler.handleConstraintViolation(exception, Mockito.mock(WebRequest.class));

    List<FieldErrorResource> errors = errorResource.getFieldErrors();
    assertThat(errors.size(), is(1));
    assertThat(errors.get(0).getField(), is("email"));
    assertThat(errors.get(0).getMessage(), is("must not be blank"));
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private ConstraintViolation<?> buildViolation(String propertyPath, String message) {
    try {
      Annotation notBlank = Dummy.class.getDeclaredField("email").getAnnotation(NotBlank.class);

      ConstraintDescriptor descriptor = Mockito.mock(ConstraintDescriptor.class);
      when(descriptor.getAnnotation()).thenReturn(notBlank);

      Path path = Mockito.mock(Path.class);
      when(path.toString()).thenReturn(propertyPath);

      ConstraintViolation violation = Mockito.mock(ConstraintViolation.class);
      when(violation.getRootBeanClass()).thenReturn((Class) Dummy.class);
      when(violation.getPropertyPath()).thenReturn(path);
      when(violation.getConstraintDescriptor()).thenReturn(descriptor);
      when(violation.getMessage()).thenReturn(message);

      return violation;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
