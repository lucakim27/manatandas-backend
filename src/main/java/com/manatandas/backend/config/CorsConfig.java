package com.manatandas.backend.config;

// Superseded by SecurityConfig, which defines CORS via a
// CorsConfigurationSource bean instead — Spring Security's filter chain
// needs CORS declared through its own .cors() configuration, and running
// both this WebMvcConfigurer-based setup and SecurityConfig at the same
// time causes the two to conflict. This class is intentionally inert
// (no @Configuration) and safe to delete.
public class CorsConfig {
}
