package br.com.corely.auth.security.jwt;

import br.com.corely.auth.authorization.RolePermissions;
import br.com.corely.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    private final JwtProperties properties;

    private final List<SecretKey> verificationKeys;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.verificationKeys = buildVerificationKeys();
    }

    /**
     * Constrói as chaves de verificação: a chave atual (assinatura) primeiro,
     * seguidas das chaves anteriores aceitas durante a rotação de segredo.
     */
    private List<SecretKey> buildVerificationKeys() {
        List<SecretKey> keys = new ArrayList<>();
        keys.add(signingKey());
        for (String previous : properties.getPreviousSecrets()) {
            if (previous != null && !previous.isBlank() && !previous.equals(properties.getSecret())) {
                keys.add(hmacKey(previous));
            }
        }
        return List.copyOf(keys);
    }

    private SecretKey signingKey() {
        return hmacKey(properties.getSecret());
    }

    private SecretKey hmacKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("studioId", user.getStudio().getId().toString());
        claims.put("role", user.getRole().name());
        claims.put("email", user.getEmail());
        claims.put("name", user.getName());
        claims.put("permissions", RolePermissions.getPermissions(user.getRole()).stream()
                .map(Enum::name)
                .toList());

        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + properties.getAccessTokenExpiration()))
                .signWith(signingKey())
                .compact();
    }

    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("type", "refresh");
        claims.put("jti", UUID.randomUUID().toString());

        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + properties.getRefreshTokenExpiration()))
                .signWith(signingKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public UUID extractUserId(String token) {
        String userId = extractClaim(token, claims -> claims.get("userId", String.class));
        return UUID.fromString(userId);
    }

    public UUID extractStudioId(String token) {
        String studioId = extractClaim(token, claims -> claims.get("studioId", String.class));
        return UUID.fromString(studioId);
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    public String extractName(String token) {
        return extractClaim(token, claims -> claims.get("name", String.class));
    }

    @SuppressWarnings("unchecked")
    public List<String> extractPermissions(String token) {
        return extractClaim(token, claims -> {
            Object perms = claims.get("permissions");
            return perms instanceof List ? (List<String>) perms : List.of();
        });
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        for (SecretKey key : verificationKeys) {
            try {
                return Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
            } catch (Exception ignored) {
                // tenta a próxima chave (rotação de segredo)
            }
        }
        throw new io.jsonwebtoken.JwtException("JWT não pôde ser verificado com nenhuma chave");
    }

    public boolean isTokenValid(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }

    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public long getAccessTokenExpiration() {
        return properties.getAccessTokenExpiration();
    }

    public long getRefreshTokenExpiration() {
        return properties.getRefreshTokenExpiration();
    }
}
