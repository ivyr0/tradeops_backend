package com.tradeops.repo;

import com.tradeops.model.entity.UserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserEntityRepo extends JpaRepository<UserEntity, Long> {
  @EntityGraph(attributePaths = {"roles"})
  Optional<UserEntity> findByUsername(String username);
  Optional<UserEntity> findByEmail(String email);
  @EntityGraph(attributePaths = {"roles"})
  Optional<UserEntity> findById(Long id);

  boolean existsByRoles_Name(String name);

  UserEntity save(UserEntity userEntity);
  boolean existsByUsername(String username);
  boolean existsByEmail(String email);


}
