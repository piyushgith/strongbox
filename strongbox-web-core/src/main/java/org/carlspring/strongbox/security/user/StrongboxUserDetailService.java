package org.carlspring.strongbox.security.user;

import org.carlspring.strongbox.users.domain.User;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class StrongboxUserDetailService
        implements UserDetailsService
{

    private static final Logger logger = LoggerFactory.getLogger(StrongboxUserDetailService.class);

   // @Autowired
   // private PasswordEncoder passwordEncoder;

    @Autowired
    CacheManager cacheManager;

    @Override
    public synchronized UserDetails loadUserByUsername(String name)
            throws UsernameNotFoundException
    {
        if (name == null)
        {
            throw new IllegalArgumentException("Username cannot be null.");
        }

        User user = cacheManager.getCache("users").get(name, User.class);
        if (user == null) {
            logger.error("[authenticate] ERROR Cannot find user with that name " + name);
            throw new UsernameNotFoundException("Cannot find user with that name");
        }

        // transform roles to authorities
        List<GrantedAuthority> authorities = new LinkedList<>();
        user.getRoles().forEach(role -> authorities.add(new SimpleGrantedAuthority(role)));

        // extract (detach) user in current transaction
        SpringSecurityUser springUser = new SpringSecurityUser();
        springUser.setEnabled(user.isEnabled());
        springUser.setPassword(user.getPassword());
        springUser.setSalt(user.getSalt());
        springUser.setUsername(user.getUsername());
        springUser.setAuthorities(authorities);

        logger.info("Authorise under " + springUser);

        return springUser;
    }
}
