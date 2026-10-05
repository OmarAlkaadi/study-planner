package de.studyplanner.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Zentrale Fehlerbehandlung. Alle Fehler werden im Standardformat "Problem Details"
 * (RFC 9457) zurückgegeben: { "status": 404, "title": "...", "detail": "..." }.
 * Standardfehler von Spring MVC (z. B. 405, ungültiges JSON) übernimmt die Basisklasse.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** NotFoundException → HTTP 404. */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Nicht gefunden", ex.getMessage());
    }

    /** BadRequestException → HTTP 400. */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ProblemDetail> handleBadRequest(BadRequestException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Ungültige Anfrage", ex.getMessage());
    }

    /** InvalidCredentialsException → HTTP 401 (mit "code", falls gesetzt). */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCredentials(InvalidCredentialsException ex) {
        ResponseEntity<ProblemDetail> response = problem(HttpStatus.UNAUTHORIZED, "Anmeldung fehlgeschlagen", ex.getMessage());
        if (ex.getCode() != null) {
            response.getBody().setProperty("code", ex.getCode());
        }
        return response;
    }

    /**
     * EmailNotVerifiedException → HTTP 403 mit "code": "EMAIL_NOT_VERIFIED".
     * Am Code erkennt das Frontend diesen Fall und bietet an, die Bestätigungsmail erneut zu senden.
     */
    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ProblemDetail> handleEmailNotVerified(EmailNotVerifiedException ex) {
        ResponseEntity<ProblemDetail> response = problem(HttpStatus.FORBIDDEN, "E-Mail nicht bestätigt", ex.getMessage());
        response.getBody().setProperty("code", EmailNotVerifiedException.CODE);
        return response;
    }

    /** ForbiddenException → HTTP 403 (z. B. Änderungen am Demo-Account). */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ProblemDetail> handleForbidden(ForbiddenException ex) {
        return problem(HttpStatus.FORBIDDEN, "Nicht erlaubt", ex.getMessage());
    }

    /** TooManyRequestsException → HTTP 429 (z. B. Login nach zu vielen Fehlversuchen gesperrt). */
    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ProblemDetail> handleTooManyRequests(TooManyRequestsException ex) {
        return problem(HttpStatus.TOO_MANY_REQUESTS, "Zu viele Versuche", ex.getMessage());
    }

    /**
     * Verletzte Datenbankregel (UNIQUE, CHECK, Fremdschlüssel) → HTTP 409.
     * Die genaue Ursache landet nur im Log, nicht beim Client.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Datenbankregel verletzt: {}", ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "Konflikt", "Die Daten verletzen eine Datenbankregel.");
    }

    /** Auffangnetz für alle anderen Fehler → HTTP 500. Der Stacktrace wird nur geloggt. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("Unerwarteter Fehler", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Serverfehler", "Ein unerwarteter Fehler ist aufgetreten.");
    }

    /** Validierungsfehler (@Valid) mit Fehlermeldung pro Feld. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Bitte überprüfe deine Eingaben.");
        body.setTitle("Ungültige Eingabe");
        body.setProperty("errors", errors);
        return handleExceptionInternal(ex, body, headers, HttpStatus.BAD_REQUEST, request);
    }

    /** Baut eine Problem-Details-Antwort mit Status, Titel und Beschreibung. */
    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, String detail) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setTitle(title);
        return ResponseEntity.status(status).body(body);
    }
}
