package com.demo.nimn.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/** The frontend refreshes on 401, so token problems must be 401 with a code, never 500 (#4). */
class JwtAuthorizationFilterTest {

    private static final String SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private final JWTUtil jwtUtil = new JWTUtil(SECRET);
    private final JwtAuthorizationFilter filter = new JwtAuthorizationFilter(jwtUtil);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void expiredTokenIs401WithExpiredCode() throws Exception {
        String expired = Jwts.builder().claim("email", "a@t.invalid").claim("role", "ROLE_USER")
                .setExpiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)), SignatureAlgorithm.HS256)
                .compact();

        MockHttpServletResponse response = run("Bearer " + expired);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"code\":\"ACCESS_TOKEN_EXPIRED\"");
    }

    @Test
    void malformedTokenIs401WithInvalidCode() throws Exception {
        MockHttpServletResponse response = run("Bearer not-a-jwt");

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"code\":\"INVALID_ACCESS_TOKEN\"");
    }

    @Test
    void validTokenAuthenticatesTheRequest() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletRequest request = request("Bearer " + jwtUtil.createAccessJwt("a@t.invalid", "ROLE_USER"));

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("a@t.invalid");
    }

    @Test
    void missingTokenPassesThroughUnauthenticated() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request(null), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private MockHttpServletResponse run(String authorization) throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request(authorization), response, chain);
        assertThat(chain.getRequest()).as("rejected requests must not reach the controller").isNull();
        return response;
    }

    private static MockHttpServletRequest request(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        if (authorization != null) request.addHeader("Authorization", authorization);
        return request;
    }
}
