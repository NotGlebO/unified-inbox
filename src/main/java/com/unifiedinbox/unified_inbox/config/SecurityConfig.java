
package com.unifiedinbox.unified_inbox.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
// import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import com.unifiedinbox.unified_inbox.integration.gmail.GmailOAuthSuccessHandler;

@Configuration
public class SecurityConfig {

    


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DefaultOAuth2AuthorizationRequestResolver
    gmailAuthorizationRequestResolver(
            ClientRegistrationRepository registrations
    ) {
        var resolver = new DefaultOAuth2AuthorizationRequestResolver(
                registrations,
                "/oauth2/authorization"
        );

        resolver.setAuthorizationRequestCustomizer(
                request -> request
                        .additionalParameters(params -> {
                            params.put("access_type", "offline");
                            params.put("prompt", "consent");
                        })
        );

        return resolver;
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            DefaultOAuth2AuthorizationRequestResolver gmailAuthorizationRequestResolver,
            GmailOAuthSuccessHandler gmailOAuthSuccessHandler
    ) throws Exception {

        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/login",
                    "/register",
                    "/css/**",
                    "/js/**",
                    "/icons/**",
                    "/api/csrf",
                    "/error"
                ).permitAll()

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/auth/register",
                    "/register"
                ).permitAll()

                .anyRequest().authenticated()
            )

            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/organizations", true)
                .failureUrl("/login?error")
                .permitAll()
            )
            .oauth2Login(oauth -> oauth
            .loginPage("/login")
            .authorizationEndpoint(endpoint -> endpoint
                .authorizationRequestResolver(
                    gmailAuthorizationRequestResolver
                )
            )
            .successHandler(gmailOAuthSuccessHandler)
        )

            .oauth2Client(oauth2 -> oauth2
            .authorizationCodeGrant(code -> code
                    .authorizationRequestResolver(
                            gmailAuthorizationRequestResolver
                    )
            )
    )

            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )

            .csrf(csrf -> csrf
                .csrfTokenRepository(
                    CookieCsrfTokenRepository.withHttpOnlyFalse()
                )
            );

        return http.build();
    }

}
