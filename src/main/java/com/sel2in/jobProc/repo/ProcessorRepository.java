package com.sel2in.jobProc.repo;

import com.sel2in.jobProc.entity.ProcessorDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ProcessorRepository extends JpaRepository<ProcessorDefinition, Long> {
    Optional<ProcessorDefinition> findByClassName(String className);
}
