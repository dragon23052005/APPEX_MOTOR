package com.apexmotors.auth;

import com.apexmotors.persistence.entity.*;
import com.apexmotors.persistence.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.WebAttributes;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import java.util.concurrent.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "server.address=127.0.0.1",
    "server.servlet.session.cookie.secure=false",
    "spring.datasource.url=jdbc:h2:mem:apex_auth;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
@ActiveProfiles({"mysql", "testdb"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthenticationFlowTest {
    private static final String PASSWORD = "Apex-Test-Only-482!";
    private static final String NEW_PASSWORD = "Apex-Changed-Only-973!";
    @Autowired MockMvc mvc;
    @Autowired UserJpaRepository users;
    @Autowired RoleJpaRepository roles;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired SessionRegistry sessions;
    @LocalServerPort int port;
    String fixtureHash;

    @BeforeAll
    void hashFixture() { fixtureHash = encoder.encode(PASSWORD); }

    @BeforeEach
    void cleanIsolatedAuthDatabase() {
        jdbc.update("DELETE FROM users");
        for (Object principal : sessions.getAllPrincipals()) {
            sessions.getAllSessions(principal, true).forEach(s -> sessions.removeSessionInformation(s.getSessionId()));
        }
    }

    @Test
    void registrationNormalizesEmailHashesPasswordAndCannotGrantPrivileges() throws Exception {
        mvc.perform(post("/register").with(csrf())
                .param("fullName", "  Alex Morgan  ").param("email", " ALEX@Example.com ")
                .param("password", PASSWORD).param("confirmPassword", PASSWORD)
                .param("role", "ADMIN").param("roleId", "1").param("enabled", "false").param("id", "999"))
            .andExpect(redirectedUrl("/login?registered")).andExpect(unauthenticated());
        var user = users.findByEmail("alex@example.com").orElseThrow();
        assertThat(user.getFullName()).isEqualTo("Alex Morgan");
        assertThat(user.isEnabled()).isTrue();
        assertThat(jdbc.queryForObject("SELECT r.name FROM users u JOIN roles r ON r.id=u.role_id WHERE u.id=?",
            String.class, user.getId())).isEqualTo("CUSTOMER");
        assertThat(user.getPasswordHash()).startsWith("$2a$12$").isNotEqualTo(PASSWORD);
        assertThat(encoder.matches(PASSWORD, user.getPasswordHash())).isTrue();
        assertThat(users.count()).isEqualTo(1);
    }

    @Test
    void duplicateEmailIsRejectedWithoutChangingTheExistingAccount() throws Exception {
        createUser("owner@example.com", RoleName.CUSTOMER);
        mvc.perform(registration("OWNER@EXAMPLE.COM"))
            .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("registrationForm", "email"))
            .andExpect(content().string(not(containsString(PASSWORD))));
        assertThat(users.count()).isEqualTo(1);
        assertThat(users.findByEmail("owner@example.com").orElseThrow().getPasswordHash()).isEqualTo(fixtureHash);
    }

    @Test
    void concurrentDuplicateRegistrationCreatesExactlyOneAccount() throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            var gate = new CountDownLatch(1);
            Callable<Integer> attempt = () -> {
                gate.await();
                return mvc.perform(registration("race@example.com")).andReturn().getResponse().getStatus();
            };
            var first = executor.submit(attempt);
            var second = executor.submit(attempt);
            gate.countDown();
            assertThat(java.util.List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(200, 302);
            assertThat(users.count()).isEqualTo(1);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"blankName", "email", "weak", "mismatch", "unicodeOverflow", "longPassword"})
    void invalidRegistrationNeverCreatesAnAccountOrEchoesPasswords(String kind) throws Exception {
        String password = switch (kind) {
            case "weak" -> "short";
            case "unicodeOverflow" -> "Aa9" + "界".repeat(24);
            case "longPassword" -> "Aa9" + "x".repeat(80);
            default -> PASSWORD;
        };
        mvc.perform(post("/register").with(csrf())
                .param("fullName", kind.equals("blankName") ? "   " : "Test Owner")
                .param("email", kind.equals("email") ? "invalid" : "new@example.com")
                .param("password", password)
                .param("confirmPassword", kind.equals("mismatch") ? "Different-Password-483!" : password))
            .andExpect(status().isOk()).andExpect(model().hasErrors())
            .andExpect(content().string(not(containsString("value=\"" + password + "\""))));
        assertThat(users.count()).isZero();
    }

    @Test
    void loginRotatesSessionAndAcceptsNormalizedEmail() throws Exception {
        createUser("owner@example.com", RoleName.CUSTOMER);
        var session = new MockHttpSession();
        String before = session.getId();
        mvc.perform(post("/login").session(session).with(csrf())
                .param("email", " OWNER@EXAMPLE.COM ").param("password", PASSWORD))
            .andExpect(redirectedUrl("/profile")).andExpect(authenticated().withUsername("owner@example.com"));
        assertThat(session.getId()).isNotEqualTo(before);
        mvc.perform(get("/profile").session(session)).andExpect(status().isOk())
            .andExpect(content().string(containsString("owner@example.com")))
            .andExpect(content().string(not(containsString(fixtureHash))))
            .andExpect(content().string(not(containsString(PASSWORD))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"wrongPassword", "missingUser", "disabled", "oversizedPassword"})
    void loginFailuresAreGeneric(String kind) throws Exception {
        createUser("owner@example.com", RoleName.CUSTOMER);
        if (kind.equals("disabled")) jdbc.update("UPDATE users SET enabled=0");
        var result = mvc.perform(post("/login").with(csrf())
                .param("email", kind.equals("missingUser") ? "missing@example.com" : "owner@example.com")
                .param("password", kind.equals("oversizedPassword") ? "x".repeat(80)
                    : kind.equals("wrongPassword") ? "Wrong-Password-482!" : PASSWORD))
            .andExpect(redirectedUrl("/login?error")).andExpect(unauthenticated()).andReturn();
        var session = result.getRequest().getSession(false);
        if (session != null) assertThat(session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION)).isNull();
        mvc.perform(get("/login?error")).andExpect(status().isOk())
            .andExpect(content().string(containsString("Email or password is incorrect.")))
            .andExpect(content().string(not(containsString("disabled"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/register", "/login", "/logout", "/profile", "/profile/password", "/contact"})
    void csrfIsRequiredForEveryMutation(String path) throws Exception {
        mvc.perform(post(path)).andExpect(status().isForbidden()).andExpect(forwardedUrl("/access-denied"));
        mvc.perform(post(path).with(csrf().useInvalidToken())).andExpect(status().isForbidden());
        assertThat(users.count()).isZero();
    }

    @Test
    void publicPagesAndGalleryRemainAvailableAndFormsIncludeCsrf() throws Exception {
        for (String path : java.util.List.of("/", "/collection", "/about", "/contact",
                "/cars/ferrari-sf90-stradale", "/login", "/register", "/css/account.css", "/js/account.js",
                "/assets/cars/ferrari-sf90-stradale/hero.webp")) {
            mvc.perform(get(path)).andExpect(status().isOk());
        }
        mvc.perform(get("/register")).andExpect(content().string(containsString("name=\"_csrf\"")))
            .andExpect(content().string(containsString("aria-describedby=")));
        mvc.perform(get("/contact")).andExpect(content().string(containsString("name=\"_csrf\"")));
        mvc.perform(get("/access-denied")).andExpect(status().isForbidden()).andExpect(view().name("auth/denied"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/profile", "/admin", "/admin/vehicles", "/appointments", "/favorites"})
    void anonymousPrivateRequestsRedirectLocallyToLogin(String path) throws Exception {
        mvc.perform(get(path)).andExpect(redirectedUrl("/login"));
    }

    @Test
    void savedPrivatePageIsRestoredButExternalRedirectParametersAndHostAreIgnored() throws Exception {
        createUser("owner@example.com", RoleName.CUSTOMER);
        var result = mvc.perform(get("/favorites").header("Host", "untrusted.example")
            .param("next", "https://untrusted.example")).andExpect(redirectedUrl("/login")).andReturn();
        var session = (MockHttpSession) result.getRequest().getSession();
        mvc.perform(post("/login").session(session).with(csrf())
                .param("email", "owner@example.com").param("password", PASSWORD)
                .param("next", "//untrusted.example").param("redirect", "https://untrusted.example"))
            .andExpect(redirectedUrl("/favorites"));
    }

    @Test
    void customerCannotAccessAdminWhileAdminCanReachReservedRoutes() throws Exception {
        createUser("customer@example.com", RoleName.CUSTOMER);
        createUser("admin@example.com", RoleName.ADMIN);
        var customer = login("customer@example.com");
        mvc.perform(get("/admin/vehicles").session(customer))
            .andExpect(status().isForbidden()).andExpect(forwardedUrl("/access-denied"));
        mvc.perform(post("/admin/vehicles").session(customer).with(csrf())).andExpect(status().isForbidden());
        // No Phase 3.3 controller exists: authorized ADMIN requests proceed to the ordinary 404.
        var admin = login("admin@example.com");
        mvc.perform(get("/admin/vehicles").session(admin)).andExpect(status().isNotFound());
        mvc.perform(get("/profile").session(admin)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Administrator")));
        mvc.perform(get("/profile").with(user("outsider").roles("OTHER"))).andExpect(status().isForbidden());
    }

    @Test
    void profileUpdateOnlyTouchesTheSignedInUsersAllowedFields() throws Exception {
        createUser("owner@example.com", RoleName.CUSTOMER);
        var other = createUser("other@example.com", RoleName.CUSTOMER);
        var session = login("owner@example.com");
        mvc.perform(post("/profile").session(session).with(csrf())
                .param("fullName", "Updated Owner").param("phone", "+84 123 456 789")
                .param("id", other.getId().toString()).param("email", "other@example.com")
                .param("role", "ADMIN").param("enabled", "false").param("passwordHash", "tampered"))
            .andExpect(redirectedUrl("/profile?updated"));
        var changed = users.findByEmail("owner@example.com").orElseThrow();
        assertThat(changed.getFullName()).isEqualTo("Updated Owner");
        assertThat(changed.getPhone()).isEqualTo("+84 123 456 789");
        assertThat(changed.isEnabled()).isTrue();
        assertThat(changed.getPasswordHash()).isEqualTo(fixtureHash);
        assertThat(users.findByEmail("other@example.com").orElseThrow().getFullName()).isEqualTo("Test Owner");
        assertThat(jdbc.queryForObject("SELECT r.name FROM users u JOIN roles r ON r.id=u.role_id WHERE u.id=?",
            String.class, changed.getId())).isEqualTo("CUSTOMER");
    }

    @Test
    void profileValidationAndHtmlEscapingProtectStoredDetails() throws Exception {
        createUser("owner@example.com", RoleName.CUSTOMER);
        var session = login("owner@example.com");
        mvc.perform(post("/profile").session(session).with(csrf()).param("fullName", " ").param("phone", "<script>"))
            .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("profileForm", "fullName", "phone"));
        mvc.perform(post("/profile").session(session).with(csrf())
                .param("fullName", "<script>alert(1)</script>").param("phone", ""))
            .andExpect(redirectedUrl("/profile?updated"));
        mvc.perform(get("/profile").session(session))
            .andExpect(content().string(containsString("&lt;script&gt;")))
            .andExpect(content().string(not(containsString("<script>alert(1)</script>"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"wrongCurrent", "weakNew", "mismatch", "unchanged"})
    void invalidPasswordChangesLeaveTheOriginalPasswordValid(String kind) throws Exception {
        createUser("owner@example.com", RoleName.CUSTOMER);
        var session = login("owner@example.com");
        String next = kind.equals("weakNew") ? "short" : kind.equals("unchanged") ? PASSWORD : NEW_PASSWORD;
        mvc.perform(post("/profile/password").session(session).with(csrf())
                .param("currentPassword", kind.equals("wrongCurrent") ? "Wrong-Password-482!" : PASSWORD)
                .param("newPassword", next).param("confirmPassword", kind.equals("mismatch") ? PASSWORD : next))
            .andExpect(status().isOk()).andExpect(model().hasErrors())
            .andExpect(content().string(not(containsString("value=\"" + PASSWORD + "\""))))
            .andExpect(content().string(not(containsString("value=\"" + NEW_PASSWORD + "\""))));
        assertThat(users.findByEmail("owner@example.com").orElseThrow().getPasswordHash()).isEqualTo(fixtureHash);
    }

    @Test
    void changingPasswordExpiresAllSessionsAndRejectsTheOldPassword() throws Exception {
        createUser("owner@example.com", RoleName.CUSTOMER);
        var first = login("owner@example.com");
        var second = login("owner@example.com");
        mvc.perform(post("/profile/password").session(first).with(csrf())
                .param("currentPassword", PASSWORD).param("newPassword", NEW_PASSWORD)
                .param("confirmPassword", NEW_PASSWORD))
            .andExpect(redirectedUrl("/login?passwordChanged")).andExpect(unauthenticated());
        assertThat(first.isInvalid()).isTrue();
        mvc.perform(get("/profile").session(second)).andExpect(redirectedUrl("/login?expired"));
        mvc.perform(post("/login").with(csrf()).param("email", "owner@example.com").param("password", PASSWORD))
            .andExpect(redirectedUrl("/login?error")).andExpect(unauthenticated());
        mvc.perform(post("/login").with(csrf()).param("email", "owner@example.com").param("password", NEW_PASSWORD))
            .andExpect(redirectedUrl("/profile")).andExpect(authenticated());
    }

    @Test
    void onlyPostWithCsrfCanLogOutAndTheSessionIsInvalidated() throws Exception {
        createUser("owner@example.com", RoleName.CUSTOMER);
        var session = login("owner@example.com");
        mvc.perform(get("/logout").session(session));
        mvc.perform(get("/profile").session(session)).andExpect(status().isOk()).andExpect(authenticated());
        mvc.perform(post("/logout").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/logout").session(session).with(csrf())).andExpect(redirectedUrl("/login?logout"))
            .andExpect(unauthenticated());
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void realHttpRegistrationLoginProfilePasswordChangeAndLogoutUseSecureCookieSessions() throws Exception {
        try (var first = new HttpSessionClient(); var second = new HttpSessionClient()) {
            var registration = first.get("/register");
            assertThat(registration.statusCode()).isEqualTo(200);
            assertThat(registration.headers().allValues("set-cookie")).anySatisfy(cookie ->
                assertThat(cookie).contains("JSESSIONID=", "HttpOnly", "SameSite=Lax"));
            assertRedirect(first.post("/register", Map.of("_csrf", csrfValue(registration.body()),
                "fullName", "HTTP Test Owner", "email", "http@example.com", "password", PASSWORD,
                "confirmPassword", PASSWORD)), "/login?registered");
            assertRedirect(first.get("/profile"), "/login");
            var loginPage = first.get("/login");
            String sessionBeforeLogin = first.sessionId();
            assertRedirect(first.post("/login", Map.of("_csrf", csrfValue(loginPage.body()),
                "email", "http@example.com", "password", PASSWORD)), "/profile");
            assertThat(first.sessionId()).isNotEqualTo(sessionBeforeLogin);

            var profile = first.get("/profile");
            assertThat(profile.body()).contains("HTTP Test Owner", "Customer").doesNotContain(PASSWORD, "$2a$");
            assertRedirect(first.post("/profile", Map.of("_csrf", csrfValue(profile.body()),
                "fullName", "Updated HTTP Owner", "phone", "+84 123 456 789")), "/profile?updated");
            assertThat(first.get("/profile").body()).contains("Updated HTTP Owner");
            var rejected = first.post("/profile", Map.of("fullName", "Forged update"));
            assertThat(rejected.statusCode()).isEqualTo(403);
            assertThat(rejected.body()).contains("Your form session has expired or could not be verified.");
            var denied = first.get("/admin/vehicles");
            assertThat(denied.statusCode()).isEqualTo(403);
            assertThat(denied.body()).contains("Your account does not have permission");

            var secondLogin = second.get("/login");
            assertRedirect(second.post("/login", Map.of("_csrf", csrfValue(secondLogin.body()),
                "email", "http@example.com", "password", PASSWORD)), "/profile");
            assertRedirect(first.post("/profile/password", Map.of("_csrf", csrfValue(first.get("/profile").body()),
                "currentPassword", PASSWORD, "newPassword", NEW_PASSWORD, "confirmPassword", NEW_PASSWORD)),
                "/login?passwordChanged");
            assertRedirect(first.get("/profile"), "/login");
            assertRedirect(second.get("/profile"), "/login?expired");
            assertRedirect(first.post("/login", Map.of("_csrf", csrfValue(first.get("/login").body()),
                "email", "http@example.com", "password", PASSWORD)), "/login?error");
            assertRedirect(first.post("/login", Map.of("_csrf", csrfValue(first.get("/login").body()),
                "email", "http@example.com", "password", NEW_PASSWORD)), "/profile");
            assertRedirect(first.post("/logout", Map.of("_csrf", csrfValue(first.get("/profile").body()))), "/login?logout");
            assertRedirect(first.get("/profile"), "/login");
        }
    }

    private static String csrfValue(String html) {
        var match = Pattern.compile("<input(?=[^>]*\\bname=\"_csrf\")(?=[^>]*\\bvalue=\"([^\"]+)\")[^>]*>").matcher(html);
        assertThat(match.find()).as("Rendered HTML must contain a CSRF input").isTrue();
        return match.group(1);
    }

    private static void assertRedirect(HttpResponse<String> response, String target) {
        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.headers().firstValue("location")).contains(target);
    }

    private class HttpSessionClient implements AutoCloseable {
        private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        private final HttpClient client = HttpClient.newBuilder().cookieHandler(cookies)
            .followRedirects(HttpClient.Redirect.NEVER).connectTimeout(Duration.ofSeconds(5)).build();

        HttpResponse<String> get(String path) throws Exception {
            return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(15)).GET().build(), HttpResponse.BodyHandlers.ofString());
        }

        HttpResponse<String> post(String path, Map<String, String> values) throws Exception {
            String body = values.entrySet().stream().map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8)).collect(Collectors.joining("&"));
            return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(15)).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        }

        String sessionId() {
            return cookies.getCookieStore().getCookies().stream().filter(cookie -> cookie.getName().equals("JSESSIONID"))
                .map(HttpCookie::getValue).findFirst().orElseThrow();
        }

        @Override
        public void close() { client.close(); }
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder registration(String email) {
        return post("/register").with(csrf()).param("fullName", "Test Owner").param("email", email)
            .param("password", PASSWORD).param("confirmPassword", PASSWORD);
    }

    private UserEntity createUser(String email, RoleName role) {
        var user = new UserEntity();
        user.setFullName("Test Owner");
        user.setEmail(email);
        user.setPasswordHash(fixtureHash);
        user.setRole(roles.findByName(role).orElseThrow());
        return users.saveAndFlush(user);
    }

    private MockHttpSession login(String email) throws Exception {
        return (MockHttpSession) mvc.perform(post("/login").with(csrf())
                .param("email", email).param("password", PASSWORD))
            .andExpect(authenticated()).andExpect(redirectedUrl("/profile"))
            .andReturn().getRequest().getSession(false);
    }
}
