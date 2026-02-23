package com.sel2in.jobProc.repo;

import com.sel2in.jobProc.entity.JobError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobErrorRepository extends JpaRepository<JobError, Long> {
    List<JobError> findByJobId(Long jobId);
}
