package com.lowcode.bi.security;

import com.lowcode.bi.entity.User;
import com.lowcode.bi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String[] parts = username.split("@");
        if (parts.length != 2) {
            throw new UsernameNotFoundException("用户名格式不正确，请使用 用户名@租户编码 格式");
        }

        String actualUsername = parts[0];
        String tenantCode = parts[1];

        User user = userRepository.findByUsernameAndDeletedFalse(actualUsername)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));

        if (!user.getTenant().getCode().equals(tenantCode)) {
            throw new UsernameNotFoundException("租户不匹配: " + username);
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new UsernameNotFoundException("用户已被禁用: " + username);
        }

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());

        return new org.springframework.security.core.userdetails.User(
                user.getUsername() + "@" + user.getTenant().getCode(),
                user.getPasswordHash(),
                Collections.singletonList(authority)
        );
    }
}
