package com.anyawalker.poskds.features.auth;

import com.anyawalker.poskds.features.auth.dtos.LoginResponse;
import com.anyawalker.poskds.features.auth.dtos.TokenResponse;
import com.anyawalker.poskds.models.entities.Token;
import com.anyawalker.poskds.models.entities.User;
import com.anyawalker.poskds.repos.UserRepo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
public class AuthService {
    UserRepo userRepo;
    TokenService tokenService;
    public AuthService(UserRepo userRepo,TokenService tokenService){
        this.userRepo = userRepo;
        this.tokenService = tokenService;
    }

    public LoginResponse doLogin(String email){

        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null)
            return null;
        //genreate new token
        TokenResponse tokenResponse = tokenService.generateTokens(user);
        return new LoginResponse(user.getUsername(), user.getEmail(), user.getRole(),tokenResponse);
    }

    public TokenResponse doRefreshToken(String refreshToken){
        return tokenService.refreshAccessToken(refreshToken);
    }
}
