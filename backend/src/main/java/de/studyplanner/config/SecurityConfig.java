package de.studyplanner.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Set;

/**
 * Sicherheitskonfiguration:
 * - Login/Registrierung sind öffentlich, alles andere braucht ein gültiges JWT
 *   im Header "Authorization: Bearer &lt;token&gt;".
 * - Die Tokens werden mit HMAC-SHA256 signiert (Spring Security OAuth2 Resource Server).
 * - Keine Sessions (stateless), daher ist CSRF-Schutz nicht nötig.
 * - /actuator/health ist öffentlich (Healthcheck von Docker Compose); nginx leitet /actuator nicht nach außen weiter.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    /** HS256 braucht mindestens 256 Bit = 32 Byte. */
    private static final int MIN_SECRET_BYTES = 32;

    /**
     * Platzhalter, die in früheren Versionen dieses (öffentlichen) Repositorys standen. Wer einen davon benutzt,
     * hätte einen Schlüssel, den jeder kennt – damit ließen sich Login-Tokens für beliebige Konten fälschen.
     */
    static final Set<String> KNOWN_PLACEHOLDERS = Set.of(
            "dev-secret-bitte-in-produktion-aendern-1234567890",
            "bitte-in-der-env-datei-einen-langen-geheimen-schluessel-setzen",
            "hier-einen-langen-zufaelligen-geheimen-schluessel-eintragen");

    private final SecretKey secretKey;

    /** Liest das JWT-Geheimnis (app.jwt.secret bzw. Umgebungsvariable JWT_SECRET) und erzeugt daraus den HMAC-Schlüssel. */
    public SecurityConfig(@Value("${app.jwt.secret:}") String secret) {
        this.secretKey = new SecretKeySpec(resolveSecret(secret), "HmacSHA256");
    }

    /**
     * Prüft das JWT-Geheimnis:
     * - leer → zufälliger Schlüssel nur für diesen Start (sicher, aber Access-Tokens werden beim Neustart
     *   ungültig – das Frontend holt sich dann über das Refresh-Cookie einfach ein neues)
     * - öffentlich bekannter Platzhalter oder kürzer als 32 Byte → Start wird abgebrochen
     */
    static byte[] resolveSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            log.warn("JWT_SECRET ist nicht gesetzt – für diesen Start wird ein zufälliger Schlüssel benutzt. "
                    + "Für feste Schlüssel JWT_SECRET in der .env setzen (z. B. mit: openssl rand -base64 48).");
            byte[] random = new byte[48];
            new SecureRandom().nextBytes(random);
            return random;
        }
        if (KNOWN_PLACEHOLDERS.contains(secret.trim())) {
            throw new IllegalStateException("JWT_SECRET ist ein öffentlich bekannter Platzhalter. Bitte einen eigenen, "
                    + "zufälligen Wert setzen (z. B. mit: openssl rand -base64 48) oder die Zeile leer lassen.");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET muss mindestens 32 Zeichen lang sein.");
        }
        return keyBytes;
    }

    /**
     * Regeln für alle HTTP-Anfragen: welche Pfade öffentlich sind und dass alle anderen ein JWT brauchen.
     * oauth2ResourceServer(...jwt...) sorgt dafür, dass der Header "Authorization: Bearer ..." automatisch
     * mit dem JwtDecoder geprüft wird. Fehlt das Token oder ist es ungültig/abgelaufen → HTTP 401.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login",
                                "/api/auth/refresh", "/api/auth/logout",
                                "/api/auth/verify-email", "/api/auth/resend-verification",
                                "/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));
        return http.build();
    }

    /** BCrypt zum Hashen und Prüfen von Passwörtern (genutzt von AuthService und DemoDataInitializer). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Signiert neue Tokens – wird vom JwtService benutzt. */
    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    }

    /** Prüft Signatur und Ablaufzeit eingehender Tokens – wird automatisch von Spring Security benutzt. */
    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
