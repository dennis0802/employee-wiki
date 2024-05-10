package com.development.hris.entities;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WhistleInfoRepository extends JpaRepository<WhistleInfo, Long>{
  /**
   * Find a submission by id
   * @param id The submission's id
   * @return The submission with the id
   */
  WhistleInfo findById(long id);
}
