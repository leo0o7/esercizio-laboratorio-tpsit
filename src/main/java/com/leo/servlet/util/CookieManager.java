package com.leo.servlet.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.UUID;

public class CookieManager {

    private static final String COOKIE_NAME = "userId";
    private static final int COOKIE_MAX_AGE = 60 * 60 * 24 * 365;

    public static String getUserId(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        String userId = null;

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (COOKIE_NAME.equals(cookie.getName())) {
                    userId = cookie.getValue();
                    break;
                }
            }
        }

        if (userId == null) {
            userId = UUID.randomUUID().toString();
            Cookie newCookie = new Cookie(COOKIE_NAME, userId);
            newCookie.setMaxAge(COOKIE_MAX_AGE);
            newCookie.setPath("/");
            response.addCookie(newCookie);
        }

        return userId;
    }
}
