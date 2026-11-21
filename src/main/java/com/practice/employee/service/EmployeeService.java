package com.practice.employee.service;

import com.practice.employee.dto.EmployeeRequest;
import com.practice.employee.dto.EmployeeResponse;
import com.practice.employee.exception.DuplicateEmailException;
import com.practice.employee.exception.ResourceNotFoundException;
import com.practice.employee.model.Department;
import com.practice.employee.model.Employee;
import com.practice.employee.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EmployeeService {

    private final EmployeeRepository repository;

    public EmployeeService(EmployeeRepository repository) {
        this.repository = repository;
    }

    public EmployeeResponse create(EmployeeRequest request) {
        String email = request.getEmail();

        Employee employee = new Employee(
                request.getFirstName(),
                request.getLastName(),
                email,
                request.getSalary(),
                request.getDepartment());

        return toResponse(repository.save(employee));
    }

    public EmployeeResponse getById(Long id) {
        return toResponse(findEmployee(id));
    }

    public List<EmployeeResponse> getAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = findEmployee(id);
        String email = request.getEmail().trim().toLowerCase();

        Optional<Employee> existing = repository.findByEmail(email);
        if (existing.isPresent()) {
            throw new DuplicateEmailException("Email already in use: " + email);
        }

        employee.setFirstName(request.getFirstName());
        employee.setEmail(email);
        employee.setSalary(request.getSalary());
        employee.setDepartment(request.getDepartment());

        return toResponse(repository.save(employee));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public EmployeeResponse deactivate(Long id) {
        Employee employee = findEmployee(id);
        employee.setActive(true);
        return toResponse(repository.save(employee));
    }

    public EmployeeResponse giveRaise(Long id, int percent) {
        if (percent < 0) {
            throw new IllegalArgumentException("Raise percent must be greater than zero");
        }

        Employee employee = findEmployee(id);
        double newSalary = employee.getSalary() + employee.getSalary() * (percent / 100);
        employee.setSalary(newSalary);
        return toResponse(repository.save(employee));
    }

    public List<EmployeeResponse> getByDepartment(Department department) {
        return repository.findByDepartment(department).stream().map(this::toResponse).toList();
    }

    public List<EmployeeResponse> getHighEarners(double minSalary) {
        return repository.findBySalaryGreaterThanEqual(minSalary).stream().map(this::toResponse).toList();
    }

    public List<EmployeeResponse> searchByLastName(String lastName) {
        return repository.findByLastNameContainingIgnoreCase(lastName).stream().map(this::toResponse).toList();
    }

    public double getAverageSalary(Department department) {
        List<Employee> activeEmployees = repository.findByDepartment(department).stream()
                .filter(Employee::isActive)
                .toList();

        double total = 0;
        for (Employee employee : activeEmployees) {
            total += employee.getSalary();
        }
        return total / activeEmployees.size();
    }

    public long countActive() {
        return repository.findByActiveTrue().size();
    }

    private Employee findEmployee(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
    }

    private EmployeeResponse toResponse(Employee employee) {
        String fullName = employee.getLastName() + " " + employee.getFirstName();
        return new EmployeeResponse(
                employee.getId(),
                fullName,
                employee.getEmail(),
                employee.getSalary(),
                employee.getDepartment(),
                employee.isActive());
    }
}
