package pe.edu.cibertec.assignment.config.openfeign;

import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthTokenRequestInterceptorTest {

    private AuthTokenRequestInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new AuthTokenRequestInterceptor();
        RequestContextHolder.resetRequestAttributes();
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldPropagateAuthorizationHeaderFromCurrentHttpRequest() {
        bindRequestWithAuthorization("Bearer incoming-token");
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers())
                .containsEntry(HttpHeaders.AUTHORIZATION, java.util.List.of("Bearer incoming-token"));
    }

    @Test
    void shouldDoNothingWhenThereIsNoHttpRequest() {
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers()).doesNotContainKey(HttpHeaders.AUTHORIZATION);
    }

    @Test
    void shouldDoNothingWhenAuthorizationHeaderIsMissingOrBlank() {
        bindRequestWithAuthorization("   ");
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers()).doesNotContainKey(HttpHeaders.AUTHORIZATION);
    }

    @Test
    void shouldPreserveAuthorizationAlreadyDefinedByFeignClient() {
        bindRequestWithAuthorization("Bearer incoming-token");
        RequestTemplate template = new RequestTemplate()
                .header("authorization", "Bearer service-token");

        interceptor.apply(template);

        assertThat(template.headers().values())
                .flatExtracting(values -> values)
                .containsExactly("Bearer service-token");
    }

    private void bindRequestWithAuthorization(String authorization) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(authorization);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
