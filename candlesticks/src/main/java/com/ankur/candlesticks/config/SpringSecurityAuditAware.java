package com.ankur.candlesticks.config;

import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;


public class SpringSecurityAuditAware implements AuditorAware<String> {
  @Override
  public Optional<String> getCurrentAuditor() {
    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
    UserPrincipal userPrincipal = new UserPrincipal("bharankb","admin,superuser"); // or your principal class
    Authentication
      authentication = new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
    securityContext.setAuthentication(authentication);
    SecurityContextHolder.setContext(securityContext);

    Optional<String> auditor=Optional.ofNullable(SecurityContextHolder.getContext())
      .map(sc -> sc.getAuthentication())
      .filter(auth -> auth.isAuthenticated())
      .map(authentication1 -> authentication1.getPrincipal())
      .map(obj -> UserPrincipal.class.cast(obj))
      .map(user -> user.getUsername());
    return auditor;

  }
}
