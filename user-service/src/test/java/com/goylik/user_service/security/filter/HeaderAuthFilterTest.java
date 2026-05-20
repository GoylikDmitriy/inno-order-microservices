package com.goylik.user_service.security.filter;

import com.goylik.user_service.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HeaderAuthFilterTest {
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    private HeaderAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new HeaderAuthFilter();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_ShouldSetAuthentication_WhenBothHeadersAreValid() throws ServletException, IOException {
        when(request.getHeader("X-User-Id")).thenReturn("123");
        when(request.getHeader("X-User-Role")).thenReturn("ROLE_USER");
        when(request.getRequestURI()).thenReturn("/api/users/some-endpoint");

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(auth.getPrincipal()).isInstanceOf(UserPrincipal.class);
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        assertThat(principal.userId()).isEqualTo(123L);
        assertThat(auth.getAuthorities()).hasSize(1);
        assertThat(auth.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldNotSetAuthentication_WhenUserIdIsMissing() throws ServletException, IOException {
        when(request.getHeader("X-User-Id")).thenReturn(null);
        when(request.getHeader("X-User-Role")).thenReturn("ROLE_USER");
        when(request.getRequestURI()).thenReturn("/api/users/some-endpoint");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldNotSetAuthentication_WhenRoleIsMissing() throws ServletException, IOException {
        when(request.getHeader("X-User-Id")).thenReturn("123");
        when(request.getHeader("X-User-Role")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/users/some-endpoint");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldReturn401_WhenUserIdIsNotNumber() throws ServletException, IOException {
        when(request.getHeader("X-User-Id")).thenReturn("abc");
        when(request.getHeader("X-User-Role")).thenReturn("ROLE_USER");
        when(request.getRequestURI()).thenReturn("/api/users/some-endpoint");

        StringWriter responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        assertThat(responseWriter.toString()).contains("Invalid X-User-Id format");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldReturn401_WhenUserIdIsEmptyString() throws ServletException, IOException {
        when(request.getHeader("X-User-Id")).thenReturn("");
        when(request.getHeader("X-User-Role")).thenReturn("ROLE_USER");
        when(request.getRequestURI()).thenReturn("/api/users/some-endpoint");

        StringWriter responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        assertThat(responseWriter.toString()).contains("Invalid X-User-Id format");
        verify(filterChain, never()).doFilter(request, response);
    }
}