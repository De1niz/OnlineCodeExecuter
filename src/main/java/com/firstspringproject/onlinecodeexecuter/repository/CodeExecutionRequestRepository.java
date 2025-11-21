package com.firstspringproject.onlinecodeexecuter.repository;

import com.firstspringproject.onlinecodeexecuter.entity.CodeExecutionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CodeExecutionRequestRepository extends JpaRepository<CodeExecutionRequest, Long> {
}