package io.spring.graphql.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.netflix.graphql.types.errors.ErrorType;
import com.netflix.graphql.types.errors.TypedGraphQLError;
import graphql.GraphQLError;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ResultPath;
import graphql.schema.DataFetchingEnvironment;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.graphql.types.Error;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.validation.constraints.NotBlank;
import javax.validation.metadata.ConstraintDescriptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GraphQLCustomizeExceptionHandlerTest {

  @Mock private DataFetchingEnvironment dataFetchingEnvironment;
  @Mock private ExecutionStepInfo executionStepInfo;

  private static class Dummy {
    @NotBlank private String email;
  }

  private DataFetcherExceptionHandlerParameters paramsFor(Throwable exception) {
    lenient().when(dataFetchingEnvironment.getExecutionStepInfo()).thenReturn(executionStepInfo);
    lenient().when(executionStepInfo.getPath()).thenReturn(ResultPath.rootPath());
    return DataFetcherExceptionHandlerParameters.newExceptionParameters()
        .dataFetchingEnvironment(dataFetchingEnvironment)
        .exception(exception)
        .build();
  }

  @Test
  public void should_map_invalid_authentication_exception_to_unauthenticated_error()
      throws Exception {
    GraphQLCustomizeExceptionHandler handler = new GraphQLCustomizeExceptionHandler();

    DataFetcherExceptionHandlerResult result =
        handler.onException(paramsFor(new InvalidAuthenticationException()));

    List<GraphQLError> errors = result.getErrors();
    assertEquals(1, errors.size());
    TypedGraphQLError error = (TypedGraphQLError) errors.get(0);
    assertEquals(ErrorType.UNAUTHENTICATED.toString(), error.getExtensions().get("errorType"));
    assertEquals("invalid email or password", error.getMessage());
  }

  @Test
  public void should_map_constraint_violation_exception_to_bad_request_with_field_errors()
      throws Exception {
    ConstraintViolation<?> violation = buildViolation("createUser.arg0.email", "must not be blank");
    ConstraintViolationException exception =
        new ConstraintViolationException(Collections.singleton(violation));

    GraphQLCustomizeExceptionHandler handler = new GraphQLCustomizeExceptionHandler();
    DataFetcherExceptionHandlerResult result = handler.onException(paramsFor(exception));

    List<GraphQLError> errors = result.getErrors();
    assertEquals(1, errors.size());
    TypedGraphQLError error = (TypedGraphQLError) errors.get(0);
    Map<String, Object> extensions = error.getExtensions();
    assertTrue(extensions.containsKey("email"));
    assertEquals(Collections.singletonList("must not be blank"), extensions.get("email"));
  }

  @Test
  public void should_build_error_data_from_constraint_violation_exception() throws Exception {
    ConstraintViolation<?> violation = buildViolation("createUser.arg0.email", "must not be blank");
    ConstraintViolationException exception =
        new ConstraintViolationException(Collections.singleton(violation));

    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(exception);

    assertEquals("BAD_REQUEST", error.getMessage());
    assertEquals(1, error.getErrors().size());
    assertEquals("email", error.getErrors().get(0).getKey());
    assertEquals(Collections.singletonList("must not be blank"), error.getErrors().get(0).getValue());
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private ConstraintViolation<?> buildViolation(String propertyPath, String message)
      throws Exception {
    Annotation notBlank = Dummy.class.getDeclaredField("email").getAnnotation(NotBlank.class);

    ConstraintDescriptor descriptor = org.mockito.Mockito.mock(ConstraintDescriptor.class);
    when(descriptor.getAnnotation()).thenReturn(notBlank);

    Path path = org.mockito.Mockito.mock(Path.class);
    when(path.toString()).thenReturn(propertyPath);

    ConstraintViolation violation = org.mockito.Mockito.mock(ConstraintViolation.class);
    when(violation.getRootBeanClass()).thenReturn((Class) Dummy.class);
    when(violation.getPropertyPath()).thenReturn(path);
    when(violation.getConstraintDescriptor()).thenReturn(descriptor);
    when(violation.getMessage()).thenReturn(message);

    return violation;
  }
}
