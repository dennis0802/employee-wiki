package com.development.hris.entities;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NewsRepository extends JpaRepository<News, Long>{
  /**
   * Find news by id
   * @param id The news' id
   * @return The news with the id
   */
  News findById(long id);
}
