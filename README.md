# Employee Management - Fix the Broken Spring Boot Project

A small Employee Management REST API (Spring Boot 3.3, Java 17, Spring Data JPA, H2).
The code compiles, but it has **19 hidden bugs** spread across the repository, service,
controller, DTO and exception-handler layers. Your job: make every test pass.

## Rules

1. **Do not edit anything under `src/test`.** The tests are the specification.
2. Fix the production code in `src/main` only.
3. Fix one bug at a time and re-run the tests so you can see the failure count drop.
4. Read the failure message first (expected vs actual), then trace back to the code.

## How to run

Requires JDK 17+ and Maven (or just open the folder in IntelliJ / Eclipse / STS).

```bash
mvn test                                        # run everything (55 tests)
mvn test -Dtest=EmployeeServiceTest             # one class
mvn test -Dtest=EmployeeServiceTest#delete*     # tests matching a name
mvn spring-boot:run                             # start the app on :8080
```

H2 console (when the app is running): http://localhost:8080/h2-console
JDBC URL: `jdbc:h2:mem:employeedb`, user `sa`, empty password.

## Project layout

```
src/main/java/com/practice/employee
  controller/   EmployeeController        - REST endpoints
  service/      EmployeeService           - business logic
  repository/   EmployeeRepository        - Spring Data JPA queries
  model/        Employee, Department      - JPA entity + enum
  dto/          EmployeeRequest/Response, ErrorResponse
  exception/    custom exceptions + GlobalExceptionHandler
src/test/java/com/practice/employee
  service/      EmployeeServiceTest       - 24 tests (Mockito)
  controller/   EmployeeControllerTest    - 20 tests (@WebMvcTest + MockMvc)
  repository/   EmployeeRepositoryTest    - 11 tests (@DataJpaTest)
```

## Intended behaviour (summary)

| Endpoint | Behaviour |
|---|---|
| `POST /api/employees` | Create. 201 on success, 400 on invalid body, 409 on duplicate email |
| `GET /api/employees` / `GET /api/employees/{id}` | List / fetch one. 404 if the id does not exist |
| `PUT /api/employees/{id}` | Update. Same validation rules as create. Email must stay unique |
| `DELETE /api/employees/{id}` | 204 on success, 404 if the id does not exist |
| `PATCH /api/employees/{id}/deactivate` | Marks the employee inactive |
| `PATCH /api/employees/{id}/raise?percent=10` | Raises salary by that percent. Percent must be > 0 |
| `GET /api/employees/department/{dept}` | Employees of a department (unknown department = 400) |
| `GET /api/employees/department/{dept}/average-salary` | Average salary of **active** employees, 0 if none |
| `GET /api/employees/search?lastName=smi` | Case-insensitive "contains" search on last name |
| `GET /api/employees/high-earners?minSalary=50000` | Salary greater than **or equal to** minSalary |

Emails are stored trimmed and in lower case and must be unique.

## Hints (no spoilers)

- Bugs are of different kinds: wrong HTTP status, wrong operator, wrong field, missing
  validation, a missing check, arithmetic mistakes, and a query that does not do what its name says.
- Some tests fail for more than one reason. Fixing a bug can reveal the next one.
- If you get stuck, `ANSWER_KEY.md` (shipped separately) lists every bug and its fix.
