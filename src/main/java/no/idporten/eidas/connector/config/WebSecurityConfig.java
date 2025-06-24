package no.idporten.eidas.connector.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class WebSecurityConfig {

    @Value("${idporten-security.csp-header}")
    private String cspHeader;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                //because we're going all over the eu..maybe dynamic csrf one day
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests((authorize) ->
                        authorize
                                .requestMatchers("/**").permitAll()
                                .anyRequest().permitAll()
                )
                .headers((headers) ->
                        headers
                                .contentSecurityPolicy((c) -> c.policyDirectives(cspHeader)
                                )
                ).build();

    }

}

