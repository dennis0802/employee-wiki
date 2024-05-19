package com.development.hris.token;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.development.hris.entities.SiteUser;

@Repository
public interface PasswordTokenRepository extends CrudRepository<PasswordRefreshToken, Long> {

  PasswordRefreshToken findById(long id);

  PasswordRefreshToken findByToken(String token);

  PasswordRefreshToken findByUser(SiteUser user);
}