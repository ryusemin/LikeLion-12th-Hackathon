package com.demo.nimn.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** One LoginFilter instance serves every request, so a login must never pick up another request's credentials (#9). */
class LoginFilterConcurrencyTest {

    private static final int THREADS = 8;
    private static final int ROUNDS = 2_000;

    @Test
    void concurrentLoginsAuthenticateWithTheirOwnEmailAndPassword() throws Exception {
        // Echoes the credentials it was handed, so the result shows exactly what the filter passed on.
        AuthenticationManager echo = token -> token;
        LoginFilter filter = new LoginFilter(echo, mock(JWTUtil.class), "/api/auth/login", null);

        CyclicBarrier start = new CyclicBarrier(THREADS);
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            List<Future<List<String>>> results = new ArrayList<>();
            for (int t = 0; t < THREADS; t++) {
                int id = t;
                results.add(pool.submit(() -> {
                    List<String> mismatches = new ArrayList<>();
                    String email = "user" + id + "@t.invalid";
                    String password = "password-" + id;
                    start.await();
                    for (int i = 0; i < ROUNDS; i++) {
                        Authentication result = filter.attemptAuthentication(login(email, password), new MockHttpServletResponse());
                        if (!email.equals(result.getPrincipal()) || !password.equals(result.getCredentials())) {
                            mismatches.add(email + " got " + result.getPrincipal());
                        }
                    }
                    return mismatches;
                }));
            }
            List<String> mismatches = new ArrayList<>();
            for (Future<List<String>> f : results) mismatches.addAll(f.get());
            assertThat(mismatches).isEmpty();
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void passesTheRequestCredentialsAsAnUnauthenticatedToken() {
        Authentication result = new LoginFilter(token -> token, mock(JWTUtil.class), "/api/auth/login", null)
                .attemptAuthentication(login("a@t.invalid", "pw"), new MockHttpServletResponse());

        assertThat(result).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(result.getPrincipal()).isEqualTo("a@t.invalid");
        assertThat(result.getCredentials()).isEqualTo("pw");
        assertThat(result.isAuthenticated()).isFalse();
    }

    private static MockHttpServletRequest login(String email, String password) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setContentType("application/json");
        request.setContent(("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}").getBytes(StandardCharsets.UTF_8));
        return request;
    }
}
