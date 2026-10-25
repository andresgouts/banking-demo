package com.banking.demo.security.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityUtilsTest {

    @BeforeEach
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnNullWhenNoAuthentication() {
        assertThat(SecurityUtils.getAuthenticatedUserId()).isNull();
        assertThat(SecurityUtils.getAuthenticatedId()).isNull();
    }

    @Test
    void shouldReturnNullWhenAnonymousUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, Collections.emptyList())
        );
        assertThat(SecurityUtils.getAuthenticatedUserId()).isNull();
        assertThat(SecurityUtils.getAuthenticatedId()).isNull();
    }

    @Test
    void shouldReturnAuthenticatedUserId() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("1", null, Collections.emptyList())
        );
        assertThat(SecurityUtils.getAuthenticatedUserId()).isEqualTo("1");
        assertThat(SecurityUtils.getAuthenticatedId()).isEqualTo(1L);
    }

    @Test
    void shouldAllowWhenOwnerMatchesAuthenticatedId() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("1", null, Collections.emptyList())
        );

        assertThatCode(() -> SecurityUtils.validateAuthenticatedOwner(1L))
                .doesNotThrowAnyException();
        assertThatCode(() -> SecurityUtils.validateAuthenticatedOwner("1"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectWhenOwnerDoesNotMatchAuthenticatedId() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("1", null, Collections.emptyList())
        );

        assertThatThrownBy(() -> SecurityUtils.validateAuthenticatedOwner(2L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage(SecurityUtils.ACCESS_DENIED_MESSAGE);
    }

    @Test
    void shouldNotThrowWhenNoAuthenticatedUserContext() {
        assertThatCode(() -> SecurityUtils.validateAuthenticatedOwner(1L))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldIgnoreNullOrBlankOwnerId() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("1", null, Collections.emptyList())
        );

        assertThatCode(() -> SecurityUtils.validateAuthenticatedOwner((Long) null))
                .doesNotThrowAnyException();
        assertThatCode(() -> SecurityUtils.validateAuthenticatedOwner("   "))
                .doesNotThrowAnyException();
    }
}
