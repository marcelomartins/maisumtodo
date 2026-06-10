package net.marcelomartins.maisumtodo.api;

import at.favre.lib.crypto.bcrypt.BCrypt;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import net.marcelomartins.maisumtodo.domain.SystemLogin;
import net.marcelomartins.maisumtodo.infra.AuthTokenService;
import net.marcelomartins.maisumtodo.infra.UserRequest;

import java.util.Locale;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class AuthResource {

    @Inject
    AuthTokenService authTokenService;

    @Inject
    UserRequest userRequest;

    @POST
    @Path("/register")
    public Response register(AuthRequest request) {
        String email = normalizeEmail(request == null ? null : request.email());
        String password = request == null ? null : request.password();
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new BadRequestException("Email and password are required");
        }

        SystemLogin existing = SystemLogin.find("email = ?1", email).firstResult();
        if (existing != null) {
            return Response.status(Response.Status.CONFLICT).build();
        }

        SystemLogin login = new SystemLogin();
        login.email = email;
        login.passwordHash = hashPassword(password);
        login.persist();

        String token = authTokenService.createToken(login);
        return Response.status(Response.Status.CREATED)
                .cookie(authTokenService.createCookie(token))
                .entity(toResponse(login))
                .build();
    }

    @POST
    @Path("/login")
    public Response login(AuthRequest request) {
        String email = normalizeEmail(request == null ? null : request.email());
        String password = request == null ? null : request.password();
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new BadRequestException("Email and password are required");
        }

        SystemLogin login = SystemLogin.find("email = ?1", email).firstResult();
        if (login == null || !verifyPassword(password, login.passwordHash)) {
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }

        String token = authTokenService.createToken(login);
        return Response.ok(toResponse(login))
                .cookie(authTokenService.createCookie(token))
                .build();
    }

    @POST
    @Path("/logout")
    public Response logout() {
        SystemLogin login = userRequest.getSystemLoginOrNull();
        if (login != null) {
            login.revokeAuthTokens();
            login.persist();
        }

        return Response.ok()
                .cookie(authTokenService.clearCookie())
                .build();
    }

    @GET
    @Path("/me")
    public AuthResponse me() {
        return toResponse(userRequest.getSystemLogin());
    }

    private AuthResponse toResponse(SystemLogin login) {
        return new AuthResponse(login.uuid, login.email);
    }

    private String hashPassword(String password) {
        return BCrypt.withDefaults().hashToString(12, password.toCharArray());
    }

    private boolean verifyPassword(String password, String passwordHash) {
        return BCrypt.verifyer().verify(password.toCharArray(), passwordHash).verified;
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public record AuthRequest(String email, String password) {
    }

    public record AuthResponse(String uuid, String email) {
    }
}
