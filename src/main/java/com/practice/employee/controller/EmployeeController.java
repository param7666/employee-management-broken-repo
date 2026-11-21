package com.practice.employee.controller;

import com.practice.employee.dto.EmployeeRequest;
import com.practice.employee.dto.EmployeeResponse;
import com.practice.employee.model.Department;
import com.practice.employee.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService service;

    public EmployeeController(EmployeeService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest request) {
        EmployeeResponse created = service.create(request);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponse> update(@PathVariable Long id,
                                                   @RequestBody EmployeeRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<EmployeeResponse> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(service.deactivate(id));
    }

    @PatchMapping("/{id}/raise")
    public ResponseEntity<EmployeeResponse> giveRaise(@PathVariable Long id,
                                                      @RequestParam int percent) {
        return ResponseEntity.ok(service.giveRaise(id, percent));
    }

    @GetMapping("/department/{department}")
    public ResponseEntity<List<EmployeeResponse>> getByDepartment(@PathVariable Department department) {
        return ResponseEntity.ok(service.getByDepartment(department));
    }

    @GetMapping("/department/{department}/average-salary")
    public ResponseEntity<Double> getAverageSalary(@PathVariable Department department) {
        return ResponseEntity.ok(service.getAverageSalary(department));
    }

    @GetMapping("/search")
    public ResponseEntity<List<EmployeeResponse>> search(@RequestParam("name") String lastName) {
        return ResponseEntity.ok(service.searchByLastName(lastName));
    }

    @GetMapping("/high-earners")
    public ResponseEntity<List<EmployeeResponse>> highEarners(@RequestParam double minSalary) {
        return ResponseEntity.ok(service.getHighEarners(minSalary));
    }
}
