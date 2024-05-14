package com.development.hris.entities;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomWebAppElementRepository extends JpaRepository<CustomWebAppElement, Long>{
    /**
     * Find an element by id
     * @param id The element's id
     * @return The element with id, null otherwise
     */
    CustomWebAppElement findById(long id);
}
