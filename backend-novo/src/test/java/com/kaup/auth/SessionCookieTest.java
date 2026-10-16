package com.kaup.auth;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.session.web.http.CookieSerializer.CookieValue;

import static org.assertj.core.api.Assertions.*;

class SessionCookieTest {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void cookieIsHostOnlyHttpOnlyLaxAndSecureOutsideDevelopment(boolean secure) {
        var properties = new ServerProperties();
        properties.getServlet().getSession().getCookie().setSecure(secure);
        var serializer = new AuthConfiguration().cookieSerializer(properties);
        var response = new MockHttpServletResponse();
        serializer.writeCookieValue(new CookieValue(new MockHttpServletRequest(), response, "synthetic-session-id"));
        String cookie = response.getHeader("Set-Cookie");
        assertThat(cookie).contains("KAUPSESSION=synthetic-session-id", "Path=/", "HttpOnly", "SameSite=Lax")
                .doesNotContain("Domain=", "Max-Age=");
        assertThat(cookie.contains("Secure")).isEqualTo(secure);
    }
}
