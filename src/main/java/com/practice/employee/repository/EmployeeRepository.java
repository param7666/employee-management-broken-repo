package com.practice.employee.repository;

import com.practice.employee.model.Department;
import com.practice.employee.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Employee> findByDepartment(Department department);

    List<Employee> findByActiveTrue();

    @Query("select e from Employee e where e.salary > :salary")
    List<Employee> findBySalaryGreaterThanEqual(@Param("salary") double salary);

    @Query("select e from Employee e where e.lastName like concat('%', :lastName, '%')")
    List<Employee> findByLastNameContainingIgnoreCase(@Param("lastName") String lastName);
}
