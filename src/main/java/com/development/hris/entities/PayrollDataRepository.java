package com.development.hris.entities;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PayrollDataRepository extends JpaRepository<PayrollData, Long>{
  /**
   * Find a payroll by id
   * @param id The payroll's id
   * @return The payroll with the id
   */
  PayrollData findById(long id);
}
