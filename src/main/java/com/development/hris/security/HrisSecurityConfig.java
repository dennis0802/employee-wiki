package com.development.hris.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class HrisSecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests((requests) -> requests
				.requestMatchers("/", "/index", "/user", "/styles.css", "/img/*", "/js/*", "/test", "/applicant*", "/error", "/uploads/news/*").permitAll()
				.requestMatchers("/hrViewPayroll", "/uploads/pay/*").hasAnyAuthority("HR_PAYROLL", "ADMIN")
				.requestMatchers("/hr*", "/hrUploads/*").hasAnyAuthority("HR", "HR_PAYROLL", "ADMIN")//hasAuthority("HR_PAYROLL")
				.requestMatchers("/admin*", "/hr*", "/hrUploads/*").hasAuthority("ADMIN")
				.anyRequest().authenticated()
			)
			.csrf((csrf) -> csrf.ignoringRequestMatchers("/api/events/create", "/api/events*", "/api/events/move", "/api/events/setColor", "/api/events/delete"))
			.formLogin((form) -> form
				.loginPage("/login")
				.defaultSuccessUrl("/index", true)
				.permitAll()
			)
			.logout((logout) -> logout.permitAll());
		return http.build();
	}
}
