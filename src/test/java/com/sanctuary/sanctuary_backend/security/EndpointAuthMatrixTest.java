package com.sanctuary.sanctuary_backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanctuary.sanctuary_backend.config.JwtUtil;
import com.sanctuary.sanctuary_backend.config.SecurityConfig;
import com.sanctuary.sanctuary_backend.model.Contact;
import com.sanctuary.sanctuary_backend.model.Role;
import com.sanctuary.sanctuary_backend.model.Sighting;
import com.sanctuary.sanctuary_backend.service.AuthService;
import com.sanctuary.sanctuary_backend.service.ContactService;
import com.sanctuary.sanctuary_backend.service.PanicService;
import com.sanctuary.sanctuary_backend.service.SightingService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockito.invocation.Invocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security matrix for every HTTP route in the app.
 *
 * 1. Every mapped route must be listed in MATRIX, so a new endpoint can't skip these checks.
 * 2. Protected routes reject missing, malformed, unsigned, forged and expired tokens with 401.
 * 3. Protected routes take the user's identity from the JWT only: a userId sent in the
 *    body or query string must never reach a service.
 *
 * Ownership rules inside services (e.g. "this contact isn't yours") are covered by the
 * service tests, since services are mocked here.
 * Dev-only routes (/api/dev/**) are not loaded without the dev profile; see DevProfileGateTest.
 */
@WebMvcTest
@Import({SecurityConfig.class, JwtUtil.class})
@TestPropertySource(properties = "jwt.secret=" + EndpointAuthMatrixTest.SECRET)
class EndpointAuthMatrixTest {

    static final String SECRET = "endpoint-matrix-test-secret-long-enough-for-hs256";
    static final String ATTACKER = "attacker-user-id";
    static final String VICTIM = "victim-user-id";

    enum Access { PUBLIC, AUTHENTICATED }

    record Route(HttpMethod method, String pattern, String path, Access access, String body) {
        String key() {
            return method.name() + " " + pattern;
        }

        @Override
        public String toString() {
            return key();
        }
    }

    private static final String CONTACT_BODY =
        "{\"name\":\"Mom\",\"phone\":\"+15551234567\",\"relationship\":\"PARENT\"}";
    private static final String SIGHTING_BODY =
        "{\"location\":\"Main St\",\"description\":\"Two vans\",\"lat\":1.0,\"lng\":2.0}";
    private static final String CREDENTIALS_BODY =
        "{\"email\":\"someone@example.com\",\"password\":\"password\"}";

    // Add every new route here. everyMappedRouteIsInTheMatrix fails until you do.
    static final List<Route> MATRIX = List.of(
        new Route(HttpMethod.GET, "/api/sightings", "/api/sightings", Access.PUBLIC, null),
        new Route(HttpMethod.POST, "/api/auth/register", "/api/auth/register", Access.PUBLIC, CREDENTIALS_BODY),
        new Route(HttpMethod.POST, "/api/auth/login", "/api/auth/login", Access.PUBLIC, CREDENTIALS_BODY),
        new Route(HttpMethod.POST, "/api/auth/oauth-sync", "/api/auth/oauth-sync", Access.PUBLIC,
            "{\"email\":\"someone@example.com\",\"name\":\"Someone\",\"googleId\":\"google-1\"}"),

        new Route(HttpMethod.POST, "/api/sightings", "/api/sightings", Access.AUTHENTICATED, SIGHTING_BODY),
        new Route(HttpMethod.POST, "/api/sightings/{id}/confirm", "/api/sightings/sighting-1/confirm",
            Access.AUTHENTICATED, null),
        new Route(HttpMethod.DELETE, "/api/sightings/{id}", "/api/sightings/sighting-1", Access.AUTHENTICATED, null),

        new Route(HttpMethod.GET, "/api/contacts", "/api/contacts", Access.AUTHENTICATED, null),
        new Route(HttpMethod.POST, "/api/contacts", "/api/contacts", Access.AUTHENTICATED, CONTACT_BODY),
        new Route(HttpMethod.PUT, "/api/contacts/{contactId}", "/api/contacts/contact-1",
            Access.AUTHENTICATED, CONTACT_BODY),
        new Route(HttpMethod.DELETE, "/api/contacts/{contactId}", "/api/contacts/contact-1",
            Access.AUTHENTICATED, null),

        new Route(HttpMethod.POST, "/api/panic/trigger", "/api/panic/trigger", Access.AUTHENTICATED,
            "{\"lat\":1.0,\"lng\":2.0}"),
        new Route(HttpMethod.POST, "/api/panic/safe", "/api/panic/safe", Access.AUTHENTICATED, null)
    );

    static Stream<Route> publicRoutes() {
        return MATRIX.stream().filter(r -> r.access() == Access.PUBLIC);
    }

    static Stream<Route> protectedRoutes() {
        return MATRIX.stream().filter(r -> r.access() == Access.AUTHENTICATED);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @MockBean
    private AuthService authService;

    @MockBean
    private ContactService contactService;

    @MockBean
    private PanicService panicService;

    @MockBean
    private SightingService sightingService;

    @BeforeEach
    void stubServices() {
        Sighting sighting = new Sighting();
        sighting.setId("sighting-1");

        when(sightingService.getAll()).thenReturn(List.of());
        when(sightingService.create(any())).thenReturn(sighting);
        when(sightingService.confirm(anyString(), anyString())).thenReturn(sighting);
        when(contactService.getContacts(anyString())).thenReturn(List.of());
        when(contactService.addContact(anyString(), any(), any(), any())).thenReturn(new Contact());
        when(contactService.updateContact(anyString(), anyString(), any(), any(), any())).thenReturn(new Contact());
        when(authService.register(any(), any())).thenReturn(Map.of("token", "t"));
        when(authService.login(any(), any())).thenReturn(Map.of("token", "t"));
        when(authService.oauthSync(any(), any(), any())).thenReturn(Map.of("token", "t"));
    }

    // ── 1. Coverage ────────────────────────────────────────────────────────

    @Test
    void everyMappedRouteIsInTheMatrix() {
        Set<String> mapped = new TreeSet<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMapping.getHandlerMethods().entrySet()) {
            if (!entry.getValue().getBeanType().getPackageName().startsWith("com.sanctuary")) {
                continue; // skip Spring's own handlers such as /error
            }
            RequestMappingInfo info = entry.getKey();
            assertThat(info.getMethodsCondition().getMethods())
                .as("%s must declare an HTTP method", info)
                .isNotEmpty();
            info.getMethodsCondition().getMethods().forEach(method ->
                info.getPatternValues().forEach(pattern -> mapped.add(method.name() + " " + pattern)));
        }

        Set<String> listed = MATRIX.stream().map(Route::key).collect(Collectors.toCollection(TreeSet::new));

        assertThat(mapped)
            .as("Every route must be in EndpointAuthMatrixTest.MATRIX, and every MATRIX entry must exist")
            .isEqualTo(listed);
    }

    // ── 2. Authentication ──────────────────────────────────────────────────

    @ParameterizedTest
    @MethodSource("publicRoutes")
    void publicRoute_isReachableWithoutToken(Route route) throws Exception {
        mockMvc.perform(build(route, route.body(), ""))
            .andExpect(status().is2xxSuccessful());
    }

    @ParameterizedTest
    @MethodSource("protectedRoutes")
    void protectedRoute_acceptsValidToken(Route route) throws Exception {
        mockMvc.perform(build(route, route.body(), "").header("Authorization", bearer(ATTACKER)))
            .andExpect(status().is2xxSuccessful());
    }

    @ParameterizedTest
    @MethodSource("protectedRoutes")
    void protectedRoute_rejectsMissingToken(Route route) throws Exception {
        mockMvc.perform(build(route, route.body(), ""))
            .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @MethodSource("protectedRoutes")
    void protectedRoute_rejectsMalformedToken(Route route) throws Exception {
        mockMvc.perform(build(route, route.body(), "").header("Authorization", "Bearer not-a-jwt"))
            .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @MethodSource("protectedRoutes")
    void protectedRoute_rejectsTokenWithoutBearerPrefix(Route route) throws Exception {
        String token = jwtUtil.generateToken(ATTACKER, Role.USER);
        mockMvc.perform(build(route, route.body(), "").header("Authorization", token))
            .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @MethodSource("protectedRoutes")
    void protectedRoute_rejectsUnsignedToken(Route route) throws Exception {
        String unsigned = Jwts.builder()
            .setSubject(ATTACKER)
            .setExpiration(new Date(System.currentTimeMillis() + 60_000))
            .compact();
        mockMvc.perform(build(route, route.body(), "").header("Authorization", "Bearer " + unsigned))
            .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @MethodSource("protectedRoutes")
    void protectedRoute_rejectsTokenSignedWithAnotherSecret(Route route) throws Exception {
        JwtUtil forger = new JwtUtil();
        ReflectionTestUtils.setField(forger, "secret", "a-different-secret-that-is-also-long-enough");
        String forged = forger.generateToken(ATTACKER, Role.USER);
        mockMvc.perform(build(route, route.body(), "").header("Authorization", "Bearer " + forged))
            .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @MethodSource("protectedRoutes")
    void protectedRoute_rejectsExpiredToken(Route route) throws Exception {
        String expired = Jwts.builder()
            .setSubject(ATTACKER)
            .setIssuedAt(new Date(System.currentTimeMillis() - 120_000))
            .setExpiration(new Date(System.currentTimeMillis() - 60_000))
            .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()))
            .compact();
        mockMvc.perform(build(route, route.body(), "").header("Authorization", "Bearer " + expired))
            .andExpect(status().isUnauthorized());
    }

    // ── 3. Identity comes from the token only ──────────────────────────────

    @ParameterizedTest
    @MethodSource("protectedRoutes")
    void protectedRoute_ignoresClientSuppliedUserId(Route route) throws Exception {
        // Attacker is logged in as ATTACKER but tries to act as VICTIM through the body and query.
        String spoofedBody = route.body() == null ? null : route.body().replaceFirst(
            "\\{", "{\"userId\":\"" + VICTIM + "\",\"reportedBy\":\"" + VICTIM + "\",");

        mockMvc.perform(build(route, spoofedBody, "?userId=" + VICTIM)
                .header("Authorization", bearer(ATTACKER)))
            .andExpect(status().is2xxSuccessful());

        String serviceArgs = serviceArguments();
        assertThat(serviceArgs)
            .as("%s passed a client-supplied userId to a service", route)
            .doesNotContain(VICTIM);
        assertThat(serviceArgs)
            .as("%s must pass the authenticated user's id to its service", route)
            .contains(ATTACKER);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private String bearer(String userId) {
        return "Bearer " + jwtUtil.generateToken(userId, Role.USER);
    }

    private MockHttpServletRequestBuilder build(Route route, String body, String query) {
        MockHttpServletRequestBuilder builder = request(route.method(), route.path() + query);
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return builder;
    }

    /** Every argument passed to any mocked service during the request, serialized to JSON. */
    private String serviceArguments() throws Exception {
        List<String> args = new ArrayList<>();
        for (Object service : List.of(authService, contactService, panicService, sightingService)) {
            for (Invocation invocation : Mockito.mockingDetails(service).getInvocations()) {
                for (Object arg : invocation.getArguments()) {
                    args.add(arg instanceof String s ? s : objectMapper.writeValueAsString(arg));
                }
            }
        }
        return String.join(" | ", args);
    }
}
