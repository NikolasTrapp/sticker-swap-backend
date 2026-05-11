package br.com.stickerswap.infrastructure.security;

import br.com.stickerswap.infrastructure.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.MediaType;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.Set;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/actuator/health",
            "/actuator/info",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/auth/register",
            "/auth/email-confirmations/**",
            "/auth/password-reset-requests",
            "/auth/password-resets",
            "/ws/**"   // JWT validated at STOMP CONNECT level by JwtChannelInterceptor
    };

    private final AppProperties appProperties;

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerFilterChain(
            HttpSecurity http,
            RateLimitingFilter rateLimitingFilter,
            RegisteredClientRepository registeredClientRepository
    ) {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = new OAuth2AuthorizationServerConfigurer();
        RequestMatcher endpointsMatcher = authorizationServerConfigurer.getEndpointsMatcher();

        http
                .securityMatcher(endpointsMatcher)
                .with(authorizationServerConfigurer, authorizationServer ->
                        authorizationServer
                                .oidc(Customizer.withDefaults())
                                .clientAuthentication(clientAuth -> {
                                    clientAuth.authenticationConverters(converters ->
                                            converters.add(0, new RevocationPublicClientAuthenticationConverter(
                                                    appProperties.oauth().webClient().clientId()
                                            ))
                                    );
                                    clientAuth.authenticationProviders(providers ->
                                            providers.add(0, new RevocationPublicClientAuthenticationProvider(
                                                    registeredClientRepository
                                            ))
                                    );
                                })
                )
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                browserHtmlMatcher()
                        ))
                .oauth2ResourceServer(rs -> rs.jwt(Customizer.withDefaults()))
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain loginFilterChain(
            HttpSecurity http,
            RateLimitingFilter rateLimitingFilter,
            LoginSuccessHandler loginSuccessHandler
    ) {
        http
                .securityMatcher("/login", "/logout", "/error")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(loginSuccessHandler)
                        .permitAll())
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain apiFilterChain(
            HttpSecurity http,
            RateLimitingFilter rateLimitingFilter,
            ActiveUserFilter activeUserFilter
    ) {
        http
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .oauth2ResourceServer(rs -> rs.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterAfter(activeUserFilter, BearerTokenAuthenticationFilter.class)
                .addFilterAfter(rateLimitingFilter, ActiveUserFilter.class);
        return http.build();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcRegisteredClientRepository(jdbcTemplate);
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository
    ) {
        return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository
    ) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer(appProperties.security().issuer())
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(appProperties.security().cors().allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-XSRF-TOKEN", "X-Requested-With"));
        config.setExposedHeaders(List.of("Location"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private static MediaTypeRequestMatcher browserHtmlMatcher() {
        MediaTypeRequestMatcher matcher = new MediaTypeRequestMatcher(MediaType.TEXT_HTML);
        // Ignore */* so AJAX requests (Accept: application/json, text/plain, */*) are not
        // redirected to the login page — they should receive a 401 instead.
        matcher.setIgnoredMediaTypes(Set.of(MediaType.ALL));
        return matcher;
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthConverter.setAuthoritiesClaimName("role");
        grantedAuthConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(grantedAuthConverter);
        return converter;
    }
}
