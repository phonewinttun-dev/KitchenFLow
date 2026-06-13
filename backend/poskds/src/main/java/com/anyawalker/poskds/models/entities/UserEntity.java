package com.anyawalker.poskds.models.entities;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name")
    private String username;
    
    @Column(name = "email", unique = true)
    private String email;
    
    @Column(name = "password")
    private String password;

    @Column(name = "role", length = 60)
    private String role;

    @OneToOne(mappedBy = "userEntity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private TokenEntity tokenEntity;
    @OneToMany(mappedBy = "userEntity",fetch = FetchType.LAZY)
    private List<OrderEntity> orderEntityList;

    public UserEntity() {}

    public UserEntity(Long id, String name, String email, String password, String role,List<OrderEntity> orderEntityList) {
        this.id = id;
        this.email = email;
        this.username = name;
        this.password = password;
        this.role = role;
        this.orderEntityList = orderEntityList;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<OrderEntity> getOrderEntityList(){
        return this.orderEntityList;
    }
    public void setOrderEntityList(List<OrderEntity> orderEntityList){
        this.orderEntityList = orderEntityList;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String name) {
        this.username = name;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public TokenEntity getToken() {
        return tokenEntity;
    }

    public void setToken(TokenEntity tokenEntity) {
        this.tokenEntity = tokenEntity;
    }
}
