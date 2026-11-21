package com.practice.employee.service;

import com.practice.employee.dto.EmployeeRequest;
import com.practice.employee.dto.EmployeeResponse;
import com.practice.employee.exception.DuplicateEmailException;
import com.practice.employee.exception.ResourceNotFoundException;
import com.practice.employee.model.Department;
import com.practice.employee.model.Employee;
import com.practice.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository repository;

    @InjectMocks
    private EmployeeService service;

    private Employee employee(Long id, String first, String last, String email,
                              double salary, Department department) {
        Employee employee = new Employee(first, last, email, salary, department);
        employee.setId(id);
        return employee;
    }

    private EmployeeRequest request(String first, String last, String email,
                                    double salary, Department department) {
        return new EmployeeRequest(first, last, email, salary, department);
    }

    // ---------- create ----------

    @Test
    void create_savesEmployeeAndReturnsResponse() {
        EmployeeRequest request = request("John", "Smith", "john@example.com", 50000, Department.ENGINEERING);
        when(repository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        EmployeeResponse response = service.create(request);

        assertEquals(1L, response.getId().longValue());
        assertEquals("John Smith", response.getFullName());
        assertEquals("john@example.com", response.getEmail());
        assertEquals(50000.0, response.getSalary(), 0.001);
        assertEquals(Department.ENGINEERING, response.getDepartment());
    }

    @Test
    void create_normalizesEmailToTrimmedLowerCase() {
        EmployeeRequest request = request("John", "Smith", "  John.Smith@Example.COM  ", 50000, Department.HR);
        when(repository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(request);

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(repository).save(captor.capture());
        assertEquals("john.smith@example.com", captor.getValue().getEmail());
    }

    @Test
    void create_newEmployeeIsActiveByDefault() {
        EmployeeRequest request = request("John", "Smith", "john@example.com", 50000, Department.SALES);
        when(repository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmployeeResponse response = service.create(request);

        assertTrue(response.isActive());
    }

    @Test
    void create_throwsWhenEmailAlreadyExists() {
        EmployeeRequest request = request("John", "Smith", "john@example.com", 50000, Department.ENGINEERING);
        when(repository.existsByEmail("john@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> service.create(request));

        verify(repository, never()).save(any(Employee.class));
    }

    // ---------- read ----------

    @Test
    void getById_returnsEmployee() {
        Employee employee = employee(1L, "John", "Smith", "john@example.com", 50000, Department.ENGINEERING);
        when(repository.findById(1L)).thenReturn(Optional.of(employee));

        EmployeeResponse response = service.getById(1L);

        assertEquals("John Smith", response.getFullName());
        assertEquals("john@example.com", response.getEmail());
    }

    @Test
    void getById_throwsWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getById(99L));
    }

    @Test
    void getAll_returnsAllEmployees() {
        Employee first = employee(1L, "John", "Smith", "john@example.com", 50000, Department.ENGINEERING);
        Employee second = employee(2L, "Jane", "Doe", "jane@example.com", 60000, Department.HR);
        when(repository.findAll()).thenReturn(List.of(first, second));

        List<EmployeeResponse> responses = service.getAll();

        assertEquals(2, responses.size());
        assertEquals("Jane Doe", responses.get(1).getFullName());
    }

    @Test
    void getAll_returnsEmptyListWhenNoEmployees() {
        when(repository.findAll()).thenReturn(List.of());

        assertTrue(service.getAll().isEmpty());
    }

    // ---------- update ----------

    @Test
    void update_changesAllFields() {
        Employee existing = employee(1L, "John", "Smith", "john@example.com", 50000, Department.ENGINEERING);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));
        EmployeeRequest request = request("Jane", "Doe", "jane@example.com", 65000, Department.FINANCE);

        EmployeeResponse response = service.update(1L, request);

        assertEquals("Jane Doe", response.getFullName());
        assertEquals("jane@example.com", response.getEmail());
        assertEquals(65000.0, response.getSalary(), 0.001);
        assertEquals(Department.FINANCE, response.getDepartment());
    }

    @Test
    void update_throwsWhenEmployeeNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        EmployeeRequest request = request("Jane", "Doe", "jane@example.com", 65000, Department.FINANCE);

        assertThrows(ResourceNotFoundException.class, () -> service.update(99L, request));
    }

    @Test
    void update_throwsWhenEmailBelongsToAnotherEmployee() {
        Employee existing = employee(1L, "John", "Smith", "john@example.com", 50000, Department.ENGINEERING);
        Employee other = employee(2L, "Sam", "Lee", "taken@example.com", 40000, Department.SALES);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.findByEmail("taken@example.com")).thenReturn(Optional.of(other));
        EmployeeRequest request = request("John", "Smith", "taken@example.com", 50000, Department.ENGINEERING);

        assertThrows(DuplicateEmailException.class, () -> service.update(1L, request));

        verify(repository, never()).save(any(Employee.class));
    }

    @Test
    void update_allowsEmployeeToKeepTheirOwnEmail() {
        Employee existing = employee(1L, "John", "Smith", "john@example.com", 50000, Department.ENGINEERING);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.findByEmail("john@example.com")).thenReturn(Optional.of(existing));
        when(repository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));
        EmployeeRequest request = request("John", "Smith", "john@example.com", 55000, Department.ENGINEERING);

        assertDoesNotThrow(() -> service.update(1L, request));
    }

    // ---------- delete ----------

    @Test
    void delete_removesEmployee() {
        Employee employee = employee(1L, "John", "Smith", "john@example.com", 50000, Department.ENGINEERING);
        when(repository.findById(1L)).thenReturn(Optional.of(employee));

        service.delete(1L);

        verify(repository).delete(employee);
    }

    @Test
    void delete_throwsWhenEmployeeNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(99L));

        verify(repository, never()).delete(any(Employee.class));
    }

    // ---------- deactivate ----------

    @Test
    void deactivate_marksEmployeeInactive() {
        Employee employee = employee(1L, "John", "Smith", "john@example.com", 50000, Department.ENGINEERING);
        when(repository.findById(1L)).thenReturn(Optional.of(employee));
        when(repository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmployeeResponse response = service.deactivate(1L);

        assertFalse(response.isActive());
    }

    // ---------- giveRaise ----------

    @Test
    void giveRaise_increasesSalaryByGivenPercent() {
        Employee employee = employee(1L, "John", "Smith", "john@example.com", 50000, Department.ENGINEERING);
        when(repository.findById(1L)).thenReturn(Optional.of(employee));
        when(repository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmployeeResponse response = service.giveRaise(1L, 10);

        assertEquals(55000.0, response.getSalary(), 0.001);
    }

    @Test
    void giveRaise_throwsWhenPercentIsZero() {
        assertThrows(IllegalArgumentException.class, () -> service.giveRaise(1L, 0));

        verify(repository, never()).save(any(Employee.class));
    }

    @Test
    void giveRaise_throwsWhenEmployeeNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.giveRaise(99L, 10));
    }

    // ---------- reports ----------

    @Test
    void getAverageSalary_countsOnlyActiveEmployees() {
        Employee first = employee(1L, "A", "One", "a@example.com", 40000, Department.ENGINEERING);
        Employee second = employee(2L, "B", "Two", "b@example.com", 60000, Department.ENGINEERING);
        Employee inactive = employee(3L, "C", "Three", "c@example.com", 100000, Department.ENGINEERING);
        inactive.setActive(false);
        when(repository.findByDepartment(Department.ENGINEERING)).thenReturn(List.of(first, second, inactive));

        double average = service.getAverageSalary(Department.ENGINEERING);

        assertEquals(50000.0, average, 0.001);
    }

    @Test
    void getAverageSalary_returnsZeroWhenDepartmentIsEmpty() {
        when(repository.findByDepartment(Department.HR)).thenReturn(List.of());

        double average = service.getAverageSalary(Department.HR);

        assertEquals(0.0, average, 0.001);
    }

    @Test
    void getByDepartment_returnsMappedEmployees() {
        Employee employee = employee(1L, "Jane", "Doe", "jane@example.com", 60000, Department.HR);
        when(repository.findByDepartment(Department.HR)).thenReturn(List.of(employee));

        List<EmployeeResponse> responses = service.getByDepartment(Department.HR);

        assertEquals(1, responses.size());
        assertEquals(Department.HR, responses.get(0).getDepartment());
    }

    @Test
    void getHighEarners_returnsEmployeesFromRepository() {
        Employee employee = employee(1L, "Jane", "Doe", "jane@example.com", 60000, Department.HR);
        when(repository.findBySalaryGreaterThanEqual(60000.0)).thenReturn(List.of(employee));

        List<EmployeeResponse> responses = service.getHighEarners(60000.0);

        assertEquals(1, responses.size());
        assertEquals(60000.0, responses.get(0).getSalary(), 0.001);
    }

    @Test
    void searchByLastName_returnsMatchingEmployees() {
        Employee employee = employee(1L, "John", "Smith", "john@example.com", 50000, Department.ENGINEERING);
        when(repository.findByLastNameContainingIgnoreCase("smi")).thenReturn(List.of(employee));

        List<EmployeeResponse> responses = service.searchByLastName("smi");

        assertEquals(1, responses.size());
        assertEquals("John Smith", responses.get(0).getFullName());
    }

    @Test
    void countActive_returnsNumberOfActiveEmployees() {
        Employee first = employee(1L, "A", "One", "a@example.com", 40000, Department.ENGINEERING);
        Employee second = employee(2L, "B", "Two", "b@example.com", 60000, Department.HR);
        when(repository.findByActiveTrue()).thenReturn(List.of(first, second));

        assertEquals(2L, service.countActive());
    }
}
