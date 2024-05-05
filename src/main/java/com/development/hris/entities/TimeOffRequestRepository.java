package com.development.hris.entities;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TimeOffRequestRepository extends JpaRepository<TimeOffRequest, Long>{
  /**
   * Find a request by id
   * @param id The requests's id
   * @return The request with the id
   */
  TimeOffRequest findById(long id);
}
