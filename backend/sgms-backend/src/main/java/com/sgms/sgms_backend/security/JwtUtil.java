package com.sgms.sgms_backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private final long EXPIRATION=86400000;

    private Key getKey(){

        return Keys.hmacShaKeyFor(secret.getBytes());

    }

    public String generateToken(String email,String role,String userType){

        return Jwts.builder()
                .setSubject(email)
                .claim("role",role)
                .claim("userType",userType)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis()+EXPIRATION))
                .signWith(getKey())
                .compact();

    }

    public Claims extractClaims(String token){

        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

    }

}