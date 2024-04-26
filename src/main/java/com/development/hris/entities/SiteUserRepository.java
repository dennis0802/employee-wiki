package com.development.hris.entities;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SiteUserRepository extends JpaRepository<SiteUser, Long> {

  /**
   * Find a site user by id
   * @param id The user's id
   * @return The site user with the id
   */
  SiteUser findById(long id);

  /**
   * Find a site user by email
   * @param email The user's email
   * @return The site user with the email, otherwise null
   */
  Optional<SiteUser> findByEmail(String email);

  /**
   * Find a site user by username
   * @param email The user's username
   * @return The site user with the username, otherwise null
   */
  Optional<SiteUser> findByUsername(String username);
}
