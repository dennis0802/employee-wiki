package com.development.hris.security;

import com.development.hris.entities.*;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Arrays;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Data;

@Data
public class UserRegistrationDetails implements UserDetails{

    private String username;
    private String password;
    private String email;
    private String altEmail;
    private boolean isEnabled;
    private List<GrantedAuthority> authorities;

    public UserRegistrationDetails(SiteUser user){
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.email = user.getEmail();
        this.altEmail = user.getAlternateEmail();
        this.isEnabled = user.isEnabled();
        this.authorities = Arrays.stream(user.getRole().split(",")).map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    }   

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities(){
        return authorities;
    }

    @Override
    public String getPassword(){
        return password;
    }

    @Override
    public String getUsername(){
        return username;
    }

    @Override
    public boolean isAccountNonExpired(){
        return true;
    }

    @Override
    public boolean isAccountNonLocked(){
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired(){
        return true;
    }

    @Override
    public boolean isEnabled(){
        return isEnabled;
    }
}
