package com.sel2in.jobProc.controller;

import com.sel2in.jobProc.entity.ProcessorDefinition;
import com.sel2in.jobProc.repo.ProcessorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/job")
@RequiredArgsConstructor
public class ProcessorController {

    private final ProcessorRepository processorRepository;

    @GetMapping("/listAll")
    public List<ProcessorDefinition> listAll() {
        return processorRepository.findAll();
    }

    @PostMapping("/add")
    public String add(@RequestBody ProcessorDefinition definition) {
        processorRepository.findByClassName(definition.getClassName()).ifPresent(existing -> {
            definition.setCreatedTs(existing.getCreatedTs());
        });
        processorRepository.save(definition);
        return "Processor saved successfully";
    }

    @DeleteMapping("/remove/{className}")
    public String remove(@PathVariable String className) {
        processorRepository.findByClassName(className).ifPresent(processorRepository::delete);
        return "Processor removed successfully";
    }
}
