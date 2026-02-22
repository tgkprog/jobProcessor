package com.sel2in.jobProc.repo;

import com.sel2in.jobProc.entity.JobRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<JobRecord, Long> {
}
