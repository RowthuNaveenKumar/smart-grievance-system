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

    private final long EXPIRATION = 86400000; // 24h

    private Key getSigningKey(){
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String email,String role,String userType){

        return Jwts.builder()
                .setSubject(email)
                .claim("role",role)
                .claim("userType",userType)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis()+EXPIRATION))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims extractClaims(String token){

        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token){

        try{
            extractClaims(token);
            return true;
        }
        catch(Exception e){
            return false;
        }
    }
}