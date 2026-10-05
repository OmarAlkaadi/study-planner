package de.studyplanner;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Basis für Integrationstests: startet die komplette Anwendung mit einem echten PostgreSQL
 * (Testcontainers, siehe TestcontainersConfig – Docker muss laufen) und bietet Hilfsmethoden,
 * um HTTP-Anfragen mit JWT zu senden.
 *
 * Der Mailversand ist durch ein Mockito-Objekt ersetzt: Es werden keine echten Mails verschickt,
 * aber die Tests können die "verschickten" Mails auslesen und die Links darin benutzen.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
public abstract class IntegrationTestBase {

    /** Findet das Token in Links wie ".../verify-email?token=abc" oder ".../reset-password?token=abc". */
    private static final Pattern TOKEN_IN_LINK = Pattern.compile("token=([A-Za-z0-9_-]+)");

    /** Findet den Wert im Header "Set-Cookie: refresh_token=abc; Path=/api; ..." */
    private static final Pattern REFRESH_COOKIE_VALUE = Pattern.compile("refresh_token=([^;]*)");

    @Autowired
    protected MockMvc mockMvc;

    /** Ersetzt den echten Mailversand; wird nach jedem Test automatisch zurückgesetzt. */
    @MockitoBean
    protected JavaMailSender mailSender;

    /** Registriert einen neuen Benutzer mit zufälliger E-Mail, bestätigt die E-Mail, meldet ihn an und gibt das Access-Token zurück. */
    protected String registerUser() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@test.de";
        return registerVerifiedUser(email, "geheim123");
    }

    /** Wie registerUser(), aber mit bekannter E-Mail und bekanntem Passwort. */
    protected String registerVerifiedUser(String email, String password) throws Exception {
        register(email, password, "Test User").andExpect(status().isCreated());
        verifyEmail(tokenFromLastMailTo(email));
        return accessToken(login(email, password).andExpect(status().isOk()));
    }

    /** Liest das Access-Token aus einer Login- oder Refresh-Antwort. */
    protected static String accessToken(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.token");
    }

    /** Liest den Wert des Refresh-Token-Cookies aus dem Set-Cookie-Header einer Antwort (oder null). */
    protected static String refreshCookie(ResultActions result) {
        String header = result.andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);
        if (header == null) {
            return null;
        }
        Matcher matcher = REFRESH_COOKIE_VALUE.matcher(header);
        return matcher.find() ? matcher.group(1) : null;
    }

    /** POST /api/auth/refresh mit dem Refresh-Token als Cookie (null = ohne Cookie). */
    protected ResultActions refresh(String refreshToken) throws Exception {
        MockHttpServletRequestBuilder request = post("/api/auth/refresh");
        if (refreshToken != null) {
            request.cookie(new Cookie("refresh_token", refreshToken));
        }
        return mockMvc.perform(request);
    }

    /** POST /api/auth/register ohne Prüfung des Ergebnisses. */
    protected ResultActions register(String email, String password, String displayName) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s", "displayName": "%s"}
                        """.formatted(email, password, displayName)));
    }

    /** POST /api/auth/login ohne Prüfung des Ergebnisses. */
    protected ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, password)));
    }

    /** Löst ein Bestätigungs-Token ein (erwartet Erfolg). Angemeldet wird dabei nicht. */
    protected ResultActions verifyEmail(String token) throws Exception {
        return mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token": "%s"}
                                """.formatted(token)))
                .andExpect(status().isOk());
    }

    /** Alle bisher im Test "verschickten" Mails an diese Adresse (älteste zuerst). */
    protected List<SimpleMailMessage> mailsTo(String email) {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, atLeast(0)).send(captor.capture());
        return captor.getAllValues().stream()
                .filter(mail -> mail.getTo() != null && Arrays.asList(mail.getTo()).contains(email.toLowerCase()))
                .toList();
    }

    /** Liest das Token aus dem Link der neuesten Mail an diese Adresse. */
    protected String tokenFromLastMailTo(String email) {
        List<SimpleMailMessage> mails = mailsTo(email);
        if (mails.isEmpty()) {
            throw new AssertionError("Keine Mail an " + email + " verschickt");
        }
        Matcher matcher = TOKEN_IN_LINK.matcher(mails.get(mails.size() - 1).getText());
        if (!matcher.find()) {
            throw new AssertionError("Kein Link mit Token in der Mail an " + email);
        }
        return matcher.group(1);
    }

    /** Legt einen Kurs an und gibt dessen ID zurück. */
    protected long createCourse(String token, String name, String dayOfWeek) throws Exception {
        String body = """
                {"name": "%s", "lecturer": "Prof. Test", "room": "A 1", "courseType": "VORLESUNG",
                 "dayOfWeek": "%s", "startTime": "08:15", "endTime": "09:45", "ects": 5, "color": "#2563eb"}
                """.formatted(name, dayOfWeek);
        String response = postJson("/api/courses", token, body)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    // Hilfsmethoden: senden GET/POST/PUT/PATCH/DELETE-Anfragen mit JWT im Authorization-Header.
    // MockMvc simuliert die HTTP-Anfragen, ohne einen echten Server zu starten.
    protected ResultActions getJson(String url, String token) throws Exception {
        return mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    protected ResultActions postJson(String url, String token, String body) throws Exception {
        return mockMvc.perform(post(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    protected ResultActions putJson(String url, String token, String body) throws Exception {
        return mockMvc.perform(put(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    protected ResultActions patchJson(String url, String token, String body) throws Exception {
        return mockMvc.perform(patch(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    protected ResultActions deleteRequest(String url, String token) throws Exception {
        return mockMvc.perform(delete(url).header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }
}
