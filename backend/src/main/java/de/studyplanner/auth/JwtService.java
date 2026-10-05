package de.studyplanner.auth;

import de.studyplanner.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Erstellt signierte JWTs (JSON Web Tokens) nach Login und Registrierung.
 * Das Prüfen eingehender Tokens übernimmt Spring Security automatisch (JwtDecoder in SecurityConfig).
 * Die Gültigkeitsdauer kommt aus application.yml (app.jwt.expiration-minutes, Standard 1440 = 24 Stunden).
 */
@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final long expirationMinutes;

    /** Der JwtEncoder kommt als Bean aus SecurityConfig, die Ablaufzeit per @Value aus der Konfiguration. */
    public JwtService(JwtEncoder jwtEncoder, @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        this.jwtEncoder = jwtEncoder;
        this.expirationMinutes = expirationMinutes;
    }

    /**
     * Baut ein Token mit Aussteller, Ausstellungs- und Ablaufzeit, der Benutzer-ID als "sub" (Subject)
     * und der E-Mail als Zusatzinfo und signiert es mit HMAC-SHA256.
     * Die ID im "sub" liest CurrentUser bei späteren Anfragen wieder aus.
     */
    public String createToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("study-planner")
                .issuedAt(now)
                .expiresAt(now.plus(expirationMinutes, ChronoUnit.MINUTES))
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
