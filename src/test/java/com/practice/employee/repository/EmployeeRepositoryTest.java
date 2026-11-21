package com.practice.employee.repository;

import com.practice.employee.model.Department;
import com.practice.employee.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class EmployeeRepositoryTest {

    @Autowired
    private EmployeeRepository repository;

    @BeforeEach
    void setUp() {
        repository.save(new Employee("John", "Smith", "john@example.com", 30000, Department.ENGINEERING));
        repository.save(new Employee("Jane", "Doe", "jane@example.com", 50000, Department.HR));
        repository.save(new Employee("Sam", "Smithson", "sam@example.com", 70000, Department.ENGINEERING));
    }

    @Test
    void save_assignsGeneratedId() {
        Employee saved = repository.save(new Employee("Neha", "Rao", "neha@example.com", 45000, Department.FINANCE));

        assertNotNull(saved.getId());
    }

    @Test
    void findByEmail_returnsMatchingEmployee() {
        Optional<Employee> found = repository.findByEmail("jane@example.com");

        assertTrue(found.isPresent());
        assertEquals("Jane", found.get().getFirstName());
    }

    @Test
    void findByEmail_returnsEmptyWhenNoMatch() {
        assertTrue(repository.findByEmail("nobody@example.com").isEmpty());
    }

    @Test
    void existsByEmail_returnsTrueForExistingEmail() {
        assertTrue(repository.existsByEmail("john@example.com"));
    }

    @Test
    void existsByEmail_returnsFalseForUnknownEmail() {
        assertFalse(repository.existsByEmail("nobody@example.com"));
    }

    @Test
    void findByDepartment_returnsOnlyThatDepartment() {
        List<Employee> engineers = repository.findByDepartment(Department.ENGINEERING);

        assertEquals(2, engineers.size());
        assertTrue(engineers.stream().allMatch(e -> e.getDepartment() == Department.ENGINEERING));
    }

    @Test
    void findByActiveTrue_excludesInactiveEmployees() {
        Employee jane = repository.findByEmail("jane@example.com").get();
        jane.setActive(false);
        repository.save(jane);

        List<Employee> active = repository.findByActiveTrue();

        assertEquals(2, active.size());
        assertTrue(active.stream().noneMatch(e -> e.getEmail().equals("jane@example.com")));
    }

    @Test
    void findBySalaryGreaterThanEqual_includesEmployeesAtTheBoundary() {
        List<Employee> result = repository.findBySalaryGreaterThanEqual(50000);

        assertEquals(2, result.size());
    }

    @Test
    void findByLastNameContainingIgnoreCase_ignoresCase() {
        List<Employee> result = repository.findByLastNameContainingIgnoreCase("SMITH");

        assertEquals(2, result.size());
    }

    @Test
    void saveAndFlush_withDuplicateEmail_throwsException() {
        Employee duplicate = new Employee("Other", "Person", "john@example.com", 20000, Department.SALES);

        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(duplicate));
    }

    @Test
    void delete_removesEmployee() {
        Employee jane = repository.findByEmail("jane@example.com").get();

        repository.delete(jane);

        assertFalse(repository.existsByEmail("jane@example.com"));
        assertEquals(2, repository.count());
    }
}
