package org.andidev.applicationname.config.interceptor;

import javax.annotation.PostConstruct;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.andidev.applicationname.entity.User;
import static org.andidev.applicationname.util.ApplicationUtils.getUser;
import static org.andidev.applicationname.util.ApplicationUtils.isAuthenticatedUser;
import static org.andidev.applicationname.util.StringUtils.quote;
import org.joda.time.DateTimeZone;
import org.springframework.format.datetime.joda.JodaTimeContext;
import org.springframework.format.datetime.joda.JodaTimeContextHolder;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.util.CookieGenerator;
import org.springframework.web.util.WebUtils;

@Slf4j
public class TimeZoneInterceptor extends HandlerInterceptorAdapter {

    private CookieGenerator cookieGenerator = new CookieGenerator();
    @Setter
    private String parameterName = "timezone";
    @Setter
    private String sessionAttributeName = "timezone";
    @Setter
    private String cookieName = "timezone";
    @Setter
    private DateTimeZone defaultTimeZone = DateTimeZone.forID("GMT");

    @PostConstruct
    public void init() {
        cookieGenerator.setCookieName(cookieName);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        DateTimeZone timeZone;
        if (isAuthenticatedUser()) {
            timeZone = getTimeZoneFromParameter(request, response, request.getSession());
            if (timeZone != null) {
                log.trace("Setting time zone to {} from parameter", quote(timeZone));
                setTimeZoneInJodaTimeContextHolder(timeZone);
                log.trace("Setting time zone to {} in session", quote(timeZone));
                setTimeZoneInSession(request.getSession(), timeZone);
                return true;
            }

            timeZone = getTimeZoneFromSession(request.getSession());
            if (timeZone != null) {
                log.trace("Setting time zone to {} from session", quote(timeZone));
                setTimeZoneInJodaTimeContextHolder(timeZone);
                return true;
            }

            timeZone = getTimeZoneFromUserSettings(getUser());
            if (timeZone != null) {
                log.trace("Setting time zone to {} from user settings", quote(timeZone));
                setTimeZoneInJodaTimeContextHolder(timeZone);
                return true;
            }

            timeZone = defaultTimeZone;
            log.trace("Setting time zone to {} from default value", quote(timeZone));
            setTimeZoneInJodaTimeContextHolder(timeZone);
            return true;
        } else {
            timeZone = getTimeZoneFromParameter(request, response, request.getSession());
            if (timeZone != null) {
                log.trace("Setting time zone to {} from parameter", quote(timeZone));
                setTimeZoneInJodaTimeContextHolder(timeZone);
                log.trace("Setting time zone to {} in cookie", quote(timeZone));
                setTimeZoneInCookie(response, timeZone);
                return true;
            }

            timeZone = getTimeZoneFromCookie(request);
            if (timeZone != null) {
                log.trace("Setting time zone to {} from cookie", quote(timeZone));
                setTimeZoneInJodaTimeContextHolder(timeZone);
                return true;
            }

            timeZone = defaultTimeZone;
            log.trace("Setting time zone to {} from default value", quote(timeZone));
            setTimeZoneInJodaTimeContextHolder(timeZone);
            return true;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        JodaTimeContextHolder.resetJodaTimeContext();
    }

    private DateTimeZone getTimeZoneFromParameter(HttpServletRequest request, HttpServletResponse response, HttpSession session) {
        String paraneter = request.getParameter(parameterName);
        if (paraneter == null) {
            return null;
        }
        try {
            return DateTimeZone.forID(paraneter);
        } catch (IllegalArgumentException e) {
            log.warn("Provided timezone = " + quote(paraneter) + " is invalid, please use one of the following time zones: " + DateTimeZone.getAvailableIDs());
            return null;
        }
    }

    private DateTimeZone getTimeZoneFromCookie(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, cookieName);
        if (cookie == null) {
            return null;
        }
        return DateTimeZone.forID(cookie.getValue());
    }

    private void setTimeZoneInCookie(HttpServletResponse response, DateTimeZone timeZone) {
        if (timeZone == null) {
            cookieGenerator.removeCookie(response);
        } else {
            cookieGenerator.addCookie(response, timeZone.getID());
        }
    }

    private DateTimeZone getTimeZoneFromSession(HttpSession session) {
        return (DateTimeZone) session.getAttribute(sessionAttributeName);
    }

    private void setTimeZoneInSession(HttpSession session, DateTimeZone timeZone) {
        session.setAttribute(sessionAttributeName, timeZone);
    }

    private DateTimeZone getTimeZoneFromUserSettings(User user) {
        return user.getTimeZone();
    }

    private void setTimeZoneInJodaTimeContextHolder(DateTimeZone timeZone) {
        JodaTimeContext context = new JodaTimeContext();
        context.setTimeZone(timeZone);
        JodaTimeContextHolder.setJodaTimeContext(context);
    }

    private static class InvalidTimeZoneException extends RuntimeException {

        public InvalidTimeZoneException(String message) {
            super(message);
        }
    }
}