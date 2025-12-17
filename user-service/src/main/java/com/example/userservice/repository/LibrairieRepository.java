package com.example.userservice.repository;

import com.example.userservice.model.Librairie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LibrairieRepository extends JpaRepository<Librairie, Long> {
}