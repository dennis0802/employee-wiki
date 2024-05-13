package com.development.hris.entities;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OpenJobRepository extends JpaRepository<OpenJob, Long>{
  /**
   * Find a job posting by id
   * @param id The posting's id
   * @return The posting with the id
   */
  OpenJob findById(long id);
}
