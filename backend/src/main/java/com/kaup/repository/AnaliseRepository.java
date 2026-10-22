package com.kaup.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.kaup.entity.Analise;

@Repository
public interface AnaliseRepository extends JpaRepository <Analise, Long>{ }
