package com.mindex.challenge.service.impl;

import com.mindex.challenge.dao.EmployeeRepository;
import com.mindex.challenge.data.Employee;
import com.mindex.challenge.data.ReportingStructure;
import com.mindex.challenge.exceptions.NotFoundException;
import com.mindex.challenge.service.EmployeeService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EmployeeServiceImplTest {

    private String employeeUrl;
    private String employeeIdUrl;
    private String reportingStructureUrl;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Before
    public void setup() {
        employeeUrl = "http://localhost:" + port + "/employee";
        employeeIdUrl = employeeUrl + "/{id}";
        reportingStructureUrl = employeeIdUrl + "/reportingStructure";
    }

    @Test
    public void testCreateReadUpdate() {
        Employee testEmployee = new Employee();
        testEmployee.setFirstName("John");
        testEmployee.setLastName("Doe");
        testEmployee.setDepartment("Engineering");
        testEmployee.setPosition("Developer");

        // Create checks
        Employee createdEmployee = restTemplate.postForEntity(employeeUrl, testEmployee, Employee.class)
                .getBody();

        assertNotNull(createdEmployee.getEmployeeId());
        assertEquals(testEmployee, createdEmployee);


        // Read checks
        Employee readEmployee = restTemplate.getForEntity(employeeIdUrl, Employee.class,
                        createdEmployee.getEmployeeId())
                .getBody();
        assertEquals(createdEmployee.getEmployeeId(), readEmployee.getEmployeeId());
        assertEquals(createdEmployee, readEmployee);


        // Update checks
        readEmployee.setPosition("Development Manager");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Employee updatedEmployee =
                restTemplate.exchange(employeeIdUrl,
                                HttpMethod.PUT,
                                new HttpEntity<>(readEmployee, headers),
                                Employee.class,
                                readEmployee.getEmployeeId())
                        .getBody();

        assertEquals(readEmployee, updatedEmployee);
    }

    @Test
    public void testGetReportingStructureZeroReports() {
        Employee createdEmployee = createEmployee(UUID.randomUUID().toString(), null);
        ReportingStructure expectedReportingStructure = new ReportingStructure(createdEmployee, 0);

        ReportingStructure actualReportingStructure = restTemplate.getForEntity(reportingStructureUrl,
                        ReportingStructure.class, createdEmployee.getEmployeeId())
                .getBody();

        assertNotNull(actualReportingStructure);
        assertEquals(expectedReportingStructure, actualReportingStructure);
    }

    @Test
    public void testGetReportingStructureDirectOnly() {
        Employee directReport1 = createEmployee("Direct Report 1", emptyList());
        Employee directReport2 = createEmployee("Direct Report 2", emptyList());
        Employee directReport3 = createEmployee("Direct Report 3", emptyList());
        Employee employeeToTest = createEmployee("Supervisor", Arrays.asList(directReport1,
                directReport2, directReport3));

        ReportingStructure expectedReportingStructure = new ReportingStructure(employeeToTest, 3);

        ReportingStructure actualReportingStructure = restTemplate.getForEntity(reportingStructureUrl,
                        ReportingStructure.class, employeeToTest.getEmployeeId())
                .getBody();

        assertNotNull(actualReportingStructure);
        assertEquals(expectedReportingStructure, actualReportingStructure);
    }

    @Test
    public void testGetReportingStructureMultipleLayers() {
        Employee level3Employee1 = createEmployee("Level 3 Employee 1", emptyList());
        Employee level2Employee1 = createEmployee("Level 2 Employee 1",
                Collections.singletonList(level3Employee1));
        Employee level3Employee2 = createEmployee("Level 3 Employee 2", emptyList());
        Employee level4Employee1 = createEmployee("Level 4 Employee 1", emptyList());
        Employee level3Employee3 = createEmployee("Level 3 Employee 3",
                Collections.singletonList(level4Employee1));
        Employee level3Employee4 = createEmployee("Level 3 Employee 4", emptyList());
        Employee level2Employee2 = createEmployee("Level 2 Employee 2", Arrays.asList(
                level3Employee2,
                level3Employee3,
                level3Employee4
        ));
        Employee level2Employee3 = createEmployee("Level 2 Employee 3", emptyList());
        Employee level1Employee = createEmployee("VP", Arrays.asList(level2Employee1,
                level2Employee2, level2Employee3));
        Employee unrelatedEmployee = createEmployee("Unrelated Employee", emptyList());
        createEmployee("Unrelated Manager", Collections.singletonList(unrelatedEmployee));


        ReportingStructure expectedReportingStructure = new ReportingStructure(level1Employee, 8);

        ReportingStructure actualReportingStructure = restTemplate.getForEntity(reportingStructureUrl,
                        ReportingStructure.class, level1Employee.getEmployeeId())
                .getBody();

        assertNotNull(actualReportingStructure);
        assertEquals(expectedReportingStructure, actualReportingStructure);

    }

    @Test
    public void testGetReportingStructureNotFound() throws NotFoundException {
        String employeeId = UUID.randomUUID().toString();
        exceptionRule.expect(NotFoundException.class);
        exceptionRule.expectMessage("Invalid employeeId: " + employeeId);

        employeeService.getReportingStructure(employeeId);
    }

    @Test
    public void testGetReportingStructureDirectReportNotFound() throws NotFoundException {
        String invalidDirectReportId = UUID.randomUUID().toString();
        Employee invalidDirectReport = new Employee();
        invalidDirectReport.setEmployeeId(invalidDirectReportId);
        exceptionRule.expect(NotFoundException.class);
        exceptionRule.expectMessage("Invalid employeeId: " + invalidDirectReportId);
        Employee employee = createEmployee(UUID.randomUUID().toString(), Collections.singletonList(invalidDirectReport));

        employeeService.getReportingStructure(employee.getEmployeeId());
    }

    /**
     * Generates and saves an employee to the database
     *
     * @param employeeId    employee id, helpful for debugging when creating multiple employees
     * @param directReports ids of the employee's direct reports
     * @return an Employee with an id that can be fetched from the database
     */
    private Employee createEmployee(String employeeId, List<Employee> directReports) {
        Employee employee = new Employee();
        employee.setEmployeeId(employeeId);
        employee.setFirstName("John");
        employee.setLastName("Doe");
        employee.setDepartment("Engineering");
        employee.setPosition("Developer");
        employee.setDirectReports(directReports);


        return employeeRepository.insert(employee);
    }
}
