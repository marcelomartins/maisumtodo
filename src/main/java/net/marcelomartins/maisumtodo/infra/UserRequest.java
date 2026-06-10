package net.marcelomartins.maisumtodo.infra;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import net.marcelomartins.maisumtodo.domain.SystemLogin;

import java.util.Objects;

@RequestScoped
public class UserRequest {

    @Inject
    AuthTokenService authTokenService;

    @Context
    HttpHeaders httpHeaders;

    private SystemLogin systemLogin;

    public SystemLogin getSystemLogin() {
        if (systemLogin != null) {
            return systemLogin;
        }

        Cookie cookie = httpHeaders == null ? null : httpHeaders.getCookies().get(AuthTokenService.COOKIE_NAME);
        if (cookie == null || cookie.getValue() == null || cookie.getValue().isBlank()) {
            throw unauthorized();
        }

        AuthTokenService.TokenClaims claims = authTokenService.verify(cookie.getValue());
        SystemLogin login = SystemLogin.find("uuid = ?1", claims.userUuid()).firstResult();
        if (login == null || !Objects.equals(login.currentAuthTokenVersion(), claims.authTokenVersion())) {
            throw unauthorized();
        }

        systemLogin = login;
        return systemLogin;
    }

    public SystemLogin getSystemLoginOrNull() {
        try {
            return getSystemLogin();
        } catch (WebApplicationException e) {
            if (e.getResponse() != null && e.getResponse().getStatus() == Response.Status.UNAUTHORIZED.getStatusCode()) {
                return null;
            }
            throw e;
        }
    }

    private WebApplicationException unauthorized() {
        return new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
}
