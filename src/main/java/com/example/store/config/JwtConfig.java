package com.example.store.config;

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties.Jwt;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.Claims;

@Service
public class JwtConfig {
    
    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expirationMs}")
    private int expirationMs;

    public Key getSignInKey() {
        byte[] keyBytes = secret.matches("^[A-Za-z0-9+/=]+$") ? Decoders.BASE64.decode(secret) : secret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUserName(String token){
        return null;
    }

    public <T> T extractClaim(String token, 
        Function<Claims, T> claimsResolver
    ){
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims extractAllClaims(String token){
        return Jwts.parserBuilder()
            .setSigningKey(getSignInKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    public Date extractExpiration(String token){
        return extractClaim(token, Claims::getExpiration);
    }

    public boolean isTokenExpired(String token){
        return extractExpiration(token).before(new Date());
    }


    public String generateToken(UserDetails userDetails){
        return generateToken(Map.of("roles",userDetails.getAuthorities()),userDetails) ;
    }

    public String generateToken(Map<String, Object> claims, UserDetails userDetails){
        
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);

        String token = Jwts.builder()
            .setSubject(userDetails.getUsername())
            .setIssuedAt(new Date())
            .setExpiration(exp)
            .signWith(getSignInKey(), SignatureAlgorithm.HS256)
            .compact();
        
        return token;
    }

    public String extractractUserName(String token){
        return null;
    }

}
