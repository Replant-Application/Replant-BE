package com.app.replant.domain.user.security;

import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.enums.UserRole;
import com.app.replant.domain.user.enums.UserStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Spring Security UserDetails 구현
 * User 엔티티를 Spring Security 인증에 사용
 */
@Getter
public class UserDetail implements UserDetails {

    private final User user;

    public UserDetail(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String role = getRoleString(user.getRole());
        return Collections.singleton(new SimpleGrantedAuthority(role));
    }

    private String getRoleString(UserRole role) {
        if (role == null) {
            return "ROLE_USER";
        }
        switch (role) {
            case ADMIN:
                return "ROLE_ADMIN";
            case GRADUATE:
                return "ROLE_GRADUATE";
            case CONTRIBUTOR:
                return "ROLE_CONTRIBUTOR";
            case USER:
            default:
                return "ROLE_USER";
        }
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    public Long getId() {
        return user.getId();
    }

    public String getNickname() {
        return user.getNickname();
    }

    public String getEmail() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // SUSPENDED 상태 제거됨 - INACTIVE나 DELETED 상태는 이미 다른 곳에서 처리
        return user.getStatus() == UserStatus.ACTIVE;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() == UserStatus.ACTIVE;
    }
}
