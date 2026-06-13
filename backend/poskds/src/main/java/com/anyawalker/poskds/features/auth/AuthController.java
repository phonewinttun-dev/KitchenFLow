package com.anyawalker.poskds.features.auth;

import com.anyawalker.poskds.features.auth.dtos.LoginResponse;
import com.anyawalker.poskds.features.auth.exceptions.InvalidRefreshTokenException;
import com.anyawalker.poskds.features.auth.exceptions.TooEarlyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("api/auth")
public class AuthController {
    private final AuthService authService;
    public AuthController(AuthService authService){
        this.authService = authService;
    }
    @PostMapping("/login")
    public ResponseEntity<?> doLogin(Authentication authentication){
        String email = authentication.getName();
        LoginResponse loginResponse = authService.doLogin(email);

        return ResponseEntity.ok(loginResponse);
    }
    @PostMapping("/refresh")
    public ResponseEntity<?> doRefreshToken(@RequestBody Map<String, String> refreshToken) {
        try {
            String currentRefreshToken = refreshToken.get("refresh_token");
            return ResponseEntity.ok(authService.doRefreshToken(currentRefreshToken));
        }
        //avoid using invalid token or outdated refreshToken to be refresh
        catch (InvalidRefreshTokenException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", e.getMessage()));
        }
        //avoid too early refresh
        catch (TooEarlyException e){
            return ResponseEntity
                    .ok(Map.of("status",e.getMessage()));
        }
    }

}
