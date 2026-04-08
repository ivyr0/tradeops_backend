package com.tradeops.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "courier_users")
public class CourierUser {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @ToString.Include
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @ToString.Include
    private String phone;

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinTable(
            name = "courier_roles",
            joinColumns = @JoinColumn(name = "courier_user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id")
    )
    private List<Role> roles;

    @Column(nullable = false)
    private Boolean isActive = true;
}
