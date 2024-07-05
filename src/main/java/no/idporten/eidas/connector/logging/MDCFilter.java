package no.idporten.eidas.connector.logging;

import io.opentelemetry.api.trace.Span;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class MDCFilter extends OncePerRequestFilter {

    public static final String MDC_IP_ADDRESS = "ip_address";
    public static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        addToMDC(request);
        try {
            filterChain.doFilter(request, response);
        } finally {
            clearMDC();
        }
    }

    protected void addToMDC(HttpServletRequest request) {
        MDC.put(MDC_IP_ADDRESS, ipAddress(request));
    }

    protected void clearMDC() {
        MDC.remove(MDC_IP_ADDRESS);
    }

    protected String ipAddress(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(X_FORWARDED_FOR_HEADER)).orElse(request.getRemoteAddr());
    }

    public static String getTraceId() {
        return Span.current().getSpanContext().getTraceId();
    }

}
