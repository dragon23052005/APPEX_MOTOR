package com.apexmotors.auth;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.*;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class LocalLoginSuccessHandlerTest {
    @ParameterizedTest
    @ValueSource(strings = {"https://evil.example", "//evil.example", "/%2f%2fevil.example",
        "/profile/../admin", "/profile\\evil", "https://evil.example/%5cfoo", "://invalid"})
    void unsafeSavedPathsFallBackToLocalProfile(String url) throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var cache = mock(RequestCache.class);
        var saved = mock(SavedRequest.class);
        when(cache.getRequest(request, response)).thenReturn(saved);
        when(saved.getRedirectUrl()).thenReturn(url);
        new LocalLoginSuccessHandler(cache).onAuthenticationSuccess(request, response, null);
        assertThat(response.getRedirectedUrl()).isEqualTo("/profile");
        verify(cache).removeRequest(request, response);
    }
}
