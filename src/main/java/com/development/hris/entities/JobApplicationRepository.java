package com.development.hris.entities;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobApplicationRepository extends JpaRepository<PayrollData, Long>{
  /**
   * Find a site user by id
   * @param id The user's id
   * @return The site user with the id
   */
  PayrollData findById(long id);
}
