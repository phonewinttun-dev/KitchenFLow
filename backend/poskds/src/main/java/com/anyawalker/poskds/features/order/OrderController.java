package com.anyawalker.poskds.features.order;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("api/cashier")
public class OrderController {
    @GetMapping("/view_orders")
    public ResponseEntity<?> viewAllOrders(@AuthenticationPrincipal Jwt jwt){
        Long userId = jwt.getClaim("userId");
        String email = jwt.getSubject();
        return ResponseEntity.ok(Map.of(
                "userId",userId,
                "subject",email,
                "status","this user can see the all orders!"
        ));
    }
}
