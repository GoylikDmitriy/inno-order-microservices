package com.goylik.auth_service.security;

import com.goylik.auth_service.model.entity.UserCredentials;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UserDetailsAdapter implements UserDetails {
    private final UserCredentials credentials;

    public UserDetailsAdapter(UserCredentials credentials) {
        if (credentials == null) {
            throw new IllegalArgumentException("User credentials cannot be null.");
        }

        this.credentials = credentials;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(credentials.getRole().name()));
    }

    @Override
    public @Nullable String getPassword() {
        return credentials.getPassword();
    }

    @Override
    public String getUsername() {
        return credentials.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return credentials.getActive();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return credentials.getActive();
    }
}
