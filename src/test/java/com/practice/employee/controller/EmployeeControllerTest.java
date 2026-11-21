package com.practice.employee.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.practice.employee.dto.EmployeeRequest;
import com.practice.employee.dto.EmployeeResponse;
import com.practice.employee.exception.DuplicateEmailException;
import com.practice.employee.exception.ResourceNotFoundException;
import com.practice.employee.model.Department;
import com.practice.employee.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmployeeService service;

    private EmployeeRequest validRequest() {
        return new EmployeeRequest("John", "Smith", "john@example.com", 50000, Department.ENGINEERING);
    }

    private EmployeeResponse response(Long id, String fullName, boolean active) {
        return new EmployeeResponse(id, fullName, "john@example.com", 50000, Department.ENGINEERING, active);
    }

    private String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    // ---------- POST /api/employees ----------

    @Test
    void createEmployee_returns201WithBody() throws Exception {
        given(service.create(any(EmployeeRequest.class))).willReturn(response(1L, "John Smith", true));

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fullName").value("John Smith"));
    }

    @Test
    void createEmployee_withBlankFirstName_returns400() throws Exception {
        EmployeeRequest request = new EmployeeRequest("", "Smith", "john@example.com", 50000, Department.ENGINEERING);

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("First name is required"));
    }

    @Test
    void createEmployee_withInvalidEmail_returns400() throws Exception {
        EmployeeRequest request = new EmployeeRequest("John", "Smith", "not-an-email", 50000, Department.ENGINEERING);

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email must be valid"));
    }

    @Test
    void createEmployee_withZeroSalary_returns400() throws Exception {
        EmployeeRequest request = new EmployeeRequest("John", "Smith", "john@example.com", 0, Department.ENGINEERING);

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Salary must be greater than zero"));
    }

    @Test
    void createEmployee_withDuplicateEmail_returns409() throws Exception {
        given(service.create(any(EmployeeRequest.class)))
                .willThrow(new DuplicateEmailException("Email already in use: john@example.com"));

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    // ---------- GET ----------

    @Test
    void getById_returns200() throws Exception {
        given(service.getById(1L)).willReturn(response(1L, "John Smith", true));

        mockMvc.perform(get("/api/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void getById_whenNotFound_returns404() throws Exception {
        given(service.getById(99L)).willThrow(new ResourceNotFoundException("Employee not found with id: 99"));

        mockMvc.perform(get("/api/employees/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Employee not found with id: 99"));
    }

    @Test
    void getAll_returnsListOfEmployees() throws Exception {
        given(service.getAll()).willReturn(List.of(
                response(1L, "John Smith", true),
                response(2L, "Jane Doe", true)));

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].fullName").value("Jane Doe"));
    }

    @Test
    void getByDepartment_returns200() throws Exception {
        given(service.getByDepartment(Department.HR)).willReturn(List.of(response(1L, "Jane Doe", true)));

        mockMvc.perform(get("/api/employees/department/HR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getByDepartment_withUnknownDepartment_returns400() throws Exception {
        mockMvc.perform(get("/api/employees/department/MARKETING"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAverageSalary_returnsNumber() throws Exception {
        given(service.getAverageSalary(Department.ENGINEERING)).willReturn(52500.0);

        mockMvc.perform(get("/api/employees/department/ENGINEERING/average-salary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(52500.0));
    }

    @Test
    void searchByLastName_returnsMatches() throws Exception {
        given(service.searchByLastName("smi")).willReturn(List.of(response(1L, "John Smith", true)));

        mockMvc.perform(get("/api/employees/search").param("lastName", "smi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("John Smith"));
    }

    @Test
    void highEarners_returnsMatches() throws Exception {
        given(service.getHighEarners(50000.0)).willReturn(List.of(response(1L, "John Smith", true)));

        mockMvc.perform(get("/api/employees/high-earners").param("minSalary", "50000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ---------- PUT ----------

    @Test
    void updateEmployee_returns200() throws Exception {
        given(service.update(eq(1L), any(EmployeeRequest.class))).willReturn(response(1L, "John Smith", true));

        mockMvc.perform(put("/api/employees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("John Smith"));
    }

    @Test
    void updateEmployee_withInvalidBody_returns400() throws Exception {
        EmployeeRequest request = new EmployeeRequest("John", "", "john@example.com", 50000, Department.ENGINEERING);

        mockMvc.perform(put("/api/employees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Last name is required"));
    }

    // ---------- DELETE ----------

    @Test
    void deleteEmployee_returns204() throws Exception {
        mockMvc.perform(delete("/api/employees/1"))
                .andExpect(status().isNoContent());

        verify(service).delete(1L);
    }

    @Test
    void deleteEmployee_whenNotFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Employee not found with id: 99"))
                .when(service).delete(99L);

        mockMvc.perform(delete("/api/employees/99"))
                .andExpect(status().isNotFound());
    }

    // ---------- PATCH ----------

    @Test
    void deactivateEmployee_returns200() throws Exception {
        given(service.deactivate(1L)).willReturn(response(1L, "John Smith", false));

        mockMvc.perform(patch("/api/employees/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void giveRaise_returns200() throws Exception {
        given(service.giveRaise(1L, 10)).willReturn(
                new EmployeeResponse(1L, "John Smith", "john@example.com", 55000, Department.ENGINEERING, true));

        mockMvc.perform(patch("/api/employees/1/raise").param("percent", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salary").value(55000.0));
    }

    @Test
    void giveRaise_withInvalidPercent_returns400() throws Exception {
        given(service.giveRaise(1L, 0)).willThrow(new IllegalArgumentException("Raise percent must be greater than zero"));

        mockMvc.perform(patch("/api/employees/1/raise").param("percent", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Raise percent must be greater than zero"));
    }
}
