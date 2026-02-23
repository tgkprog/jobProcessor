package com.sel2in.jobProc.repo;

import com.sel2in.jobProc.entity.InputDataParam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InputDataParamRepository extends JpaRepository<InputDataParam, Long> {
    List<InputDataParam> findByInputDataId(Long inputDataId);
}
