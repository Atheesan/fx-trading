package com.fxai.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    public static final long ACCESS_SECONDS = 600;
    public static final long REFRESH_SECONDS = 604800;
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    private final String dummyHash;
    private final SecureRandom random = new SecureRandom();

    public AuthService(JdbcTemplate jdbc, PasswordEncoder passwords, JwtEncoder encoder) {
        this.jdbc = jdbc;
        this.passwords = passwords;
        this.encoder = encoder;
        this.dummyHash = passwords.encode(UUID.randomUUID().toString());
    }

    public record Tokens(String accessToken, String refreshToken, UUID userId) {}
    private record User(UUID id, String passwordHash) {}
    private record Session(UUID userId, UUID family, Instant expires, boolean used, boolean revoked) {}

    @Transactional
    public Tokens register(String email, String password) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO app_user(id,email,password_hash,created_at) VALUES (?,?,?,?)",
                id, normalize(email), passwords.encode(password), Timestamp.from(Instant.now()));
        return issue(id, UUID.randomUUID(), Instant.now().plusSeconds(REFRESH_SECONDS));
    }

    @Transactional
    public Tokens login(String email, String password) {
        var users = jdbc.query("SELECT id,password_hash FROM app_user WHERE email=?",
                (rs, n) -> new User(rs.getObject("id", UUID.class), rs.getString("password_hash")), normalize(email));
        String hash = users.isEmpty() ? dummyHash : users.getFirst().passwordHash();
        boolean valid = passwords.matches(password, hash);
        if (users.isEmpty() || !valid) throw new AuthFailure();
        return issue(users.getFirst().id(), UUID.randomUUID(), Instant.now().plusSeconds(REFRESH_SECONDS));
    }

    // Replay revocation must commit even though the request fails authentication.
    @Transactional(noRollbackFor = AuthFailure.class)
    public Tokens refresh(String token) {
        if (token == null || token.length() > 128) throw new AuthFailure();
        var sessions = jdbc.query("SELECT user_id,family_id,expires_at,used,revoked FROM refresh_session WHERE token_hash=?",
                (rs, n) -> new Session(rs.getObject("user_id", UUID.class), rs.getObject("family_id", UUID.class),
                        rs.getTimestamp("expires_at").toInstant(), rs.getBoolean("used"), rs.getBoolean("revoked")), hash(token));
        if (sessions.isEmpty()) throw new AuthFailure();
        Session session = sessions.getFirst();
        // Serialize refresh/logout across the entire family, including rotated tokens.
        jdbc.queryForObject("SELECT id FROM app_user WHERE id=? FOR UPDATE", UUID.class, session.userId());
        Boolean revoked = jdbc.queryForObject("SELECT (revoked OR used) FROM refresh_session WHERE token_hash=?", Boolean.class, hash(token));
        if (session.used() || Boolean.TRUE.equals(revoked) || !session.expires().isAfter(Instant.now())) {
            revoke(session.family());
            throw new AuthFailure();
        }
        jdbc.update("UPDATE refresh_session SET used=TRUE WHERE token_hash=?", hash(token));
        return issue(session.userId(), session.family(), session.expires());
    }

    @Transactional
    public void logout(String token) {
        if (token == null || token.length() > 128) return;
        var sessions = jdbc.query("SELECT user_id,family_id FROM refresh_session WHERE token_hash=?",
                (rs, n) -> new UUID[]{rs.getObject("user_id", UUID.class), rs.getObject("family_id", UUID.class)}, hash(token));
        if (!sessions.isEmpty()) {
            jdbc.queryForObject("SELECT id FROM app_user WHERE id=? FOR UPDATE", UUID.class, sessions.getFirst()[0]);
            revoke(sessions.getFirst()[1]);
        }
    }

    private void revoke(UUID family) {
        jdbc.update("UPDATE refresh_session SET revoked=TRUE WHERE family_id=?", family);
    }

    private Tokens issue(UUID userId, UUID family, Instant refreshExpiry) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        jdbc.update("INSERT INTO refresh_session(token_hash,family_id,user_id,expires_at) VALUES (?,?,?,?)",
                hash(refresh), family, userId, Timestamp.from(refreshExpiry));
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer("fxai").subject(userId.toString())
                .issuedAt(now).expiresAt(now.plusSeconds(ACCESS_SECONDS)).id(UUID.randomUUID().toString()).build();
        String access = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new Tokens(access, refresh, userId);
    }

    static String normalize(String email) { return email.strip().toLowerCase(Locale.ROOT); }

    static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
