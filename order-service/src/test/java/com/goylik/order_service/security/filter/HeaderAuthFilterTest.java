package com.goylik.order_service.security.filter;

import com.goylik.order_service.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class HeaderAuthFilterTest {
    private HeaderAuthFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new HeaderAuthFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_ShouldSetAuthentication_WhenBothHeadersArePresent() throws Exception {
        request.addHeader("X-User-Id", "123");
        request.addHeader("X-User-Role", "ROLE_USER");

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(auth.getPrincipal()).isInstanceOf(UserPrincipal.class);

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        assertThat(principal.userId()).isEqualTo(123L);
        assertThat(auth.getAuthorities()).hasSize(1);
        assertThat(auth.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    void doFilterInternal_ShouldNotSetAuthentication_WhenUserIdIsMissing() throws Exception {
        request.addHeader("X-User-Role", "ROLE_USER");

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();
    }

    @Test
    void doFilterInternal_ShouldNotSetAuthentication_WhenRoleIsMissing() throws Exception {
        request.addHeader("X-User-Id", "123");

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();
    }

    @Test
    void doFilterInternal_ShouldNotSetAuthentication_WhenBothHeadersAreMissing() throws Exception {
        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();
    }

    @Test
    void doFilterInternal_ShouldSetAuthenticationWithAdminRole() throws Exception {
        request.addHeader("X-User-Id", "1");
        request.addHeader("X-User-Role", "ROLE_ADMIN");

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");
    }
}
