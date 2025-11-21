package com.firstspringproject.onlinecodeexecuter.controller;

import com.firstspringproject.onlinecodeexecuter.entity.CodeExecutionRequest;
import com.firstspringproject.onlinecodeexecuter.repository.CodeExecutionRequestRepository;
import com.firstspringproject.onlinecodeexecuter.service.CodeExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/code")
@CrossOrigin(origins = "*")
public class CodeExecutionController {

    @Autowired
    private CodeExecutionRequestRepository repository;

    @Autowired
    private CodeExecutionService codeExecutionService;

    @PostMapping("/execute")
    public CodeExecutionRequest executeCode(@RequestBody CodeRequest codeRequest) {
        // Save request to database
        CodeExecutionRequest request = new CodeExecutionRequest(codeRequest.getCode());
        request = repository.save(request);

        // Execute code asynchronously (Kafka yerine direkt async method)
        codeExecutionService.executeCodeAsync(request.getId());

        return request;
    }

    @GetMapping("/status/{id}")
    public CodeExecutionRequest getStatus(@PathVariable Long id) {
        return repository.findById(id).orElse(null);
    }

    public static class CodeRequest {
        private String code;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }
}