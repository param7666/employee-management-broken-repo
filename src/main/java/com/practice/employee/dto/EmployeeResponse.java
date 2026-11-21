package com.practice.employee.dto;

import com.practice.employee.model.Department;

public class EmployeeResponse {

    private Long id;
    private String fullName;
    private String email;
    private double salary;
    private Department department;
    private boolean active;

    public EmployeeResponse() {
    }

    public EmployeeResponse(Long id, String fullName, String email, double salary,
                            Department department, boolean active) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.salary = salary;
        this.department = department;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public double getSalary() {
        return salary;
    }

    public Department getDepartment() {
        return department;
    }

    public boolean isActive() {
        return active;
    }
}
