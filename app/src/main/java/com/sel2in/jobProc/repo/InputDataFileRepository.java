package com.sel2in.jobProc.repo;

import com.sel2in.jobProc.entity.InputDataFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InputDataFileRepository extends JpaRepository<InputDataFile, Long> {
    List<InputDataFile> findByInputDataId(Long inputDataId);
}
