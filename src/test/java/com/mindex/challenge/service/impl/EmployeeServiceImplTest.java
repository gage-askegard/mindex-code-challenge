package com.mindex.challenge.service.impl;

import com.mindex.challenge.data.Employee;
import com.mindex.challenge.data.ReportingStructure;
import com.mindex.challenge.exceptions.EmployeeNotFoundException;
import com.mindex.challenge.service.EmployeeService;
import org.apache.commons.lang3.ObjectUtils;
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
import java.util.stream.Collectors;

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
        Employee createdEmployee = restTemplate.postForEntity(employeeUrl, testEmployee, Employee.class).getBody();

        assertNotNull(createdEmployee.getEmployeeId());
        assertEmployeeEquivalence(testEmployee, createdEmployee);


        // Read checks
        Employee readEmployee = restTemplate.getForEntity(employeeIdUrl, Employee.class, createdEmployee.getEmployeeId()).getBody();
        assertEquals(createdEmployee.getEmployeeId(), readEmployee.getEmployeeId());
        assertEmployeeEquivalence(createdEmployee, readEmployee);


        // Update checks
        readEmployee.setPosition("Development Manager");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Employee updatedEmployee =
                restTemplate.exchange(employeeIdUrl,
                        HttpMethod.PUT,
                        new HttpEntity<>(readEmployee, headers),
                        Employee.class,
                        readEmployee.getEmployeeId()).getBody();

        assertEmployeeEquivalence(readEmployee, updatedEmployee);
    }

    @Test
    public void testGetReportingStructureZeroReports() {
        Employee createdEmployee = createEmployee(null, null);
        ReportingStructure expectedReportingStructure = new ReportingStructure(createdEmployee, 0);

        ReportingStructure actualReportingStructure = restTemplate.getForEntity(reportingStructureUrl, ReportingStructure.class, createdEmployee.getEmployeeId()).getBody();

        assertNotNull(actualReportingStructure);
        assertReportingStructureEquivalence(expectedReportingStructure, actualReportingStructure);
    }

    @Test
    public void testGetReportingStructureDirectOnly() {
        Employee directReport1 = createEmployee("Direct Report 1", emptyList());
        Employee directReport2 = createEmployee("Direct Report 2", emptyList());
        Employee directReport3 = createEmployee("Direct Report 3", emptyList());
        Employee employeeToTest = createEmployee("Supervisor", Arrays.asList(directReport1.getEmployeeId(),
                directReport2.getEmployeeId(), directReport3.getEmployeeId()));

        ReportingStructure expectedReportingStructure = new ReportingStructure(employeeToTest, 3);

        ReportingStructure actualReportingStructure = restTemplate.getForEntity(reportingStructureUrl, ReportingStructure.class, employeeToTest.getEmployeeId()).getBody();

        assertNotNull(actualReportingStructure);
        assertReportingStructureEquivalence(expectedReportingStructure, actualReportingStructure);
    }

    @Test
    public void testGetReportingStructureMultipleLayers() {
        Employee level3Employee1 = createEmployee("Level 3 Employee 1", emptyList());
        Employee level2Employee1 = createEmployee("Level 2 Employee 1", Collections.singletonList(level3Employee1.getEmployeeId()));
        Employee level3Employee2 = createEmployee("Level 3 Employee 2", emptyList());
        Employee level4Employee1 = createEmployee("Level 4 Employee 1", emptyList());
        Employee level3Employee3 = createEmployee("Level 3 Employee 3", Collections.singletonList(level4Employee1.getEmployeeId()));
        Employee level3Employee4 = createEmployee("Level 3 Employee 4", emptyList());
        Employee level2Employee2 = createEmployee("Level 2 Employee 2", Arrays.asList(
                level3Employee2.getEmployeeId(),
                level3Employee3.getEmployeeId(),
                level3Employee4.getEmployeeId()
        ));
        Employee level2Employee3 = createEmployee("Level 2 Employee 3", emptyList());
        Employee level1Employee = createEmployee("VP", Arrays.asList(level2Employee1.getEmployeeId(),
                level2Employee2.getEmployeeId(), level2Employee3.getEmployeeId()));
        Employee unrelatedEmployee = createEmployee("Unrelated Employee", emptyList());
        createEmployee("Unrelated Manager", Collections.singletonList(unrelatedEmployee.getEmployeeId()));

        ReportingStructure expectedReportingStructure = new ReportingStructure(level1Employee, 8);

        ReportingStructure actualReportingStructure = restTemplate.getForEntity(reportingStructureUrl, ReportingStructure.class, level1Employee.getEmployeeId()).getBody();

        assertNotNull(actualReportingStructure);
        assertReportingStructureEquivalence(expectedReportingStructure, actualReportingStructure);
    }

    @Test
    public void testGetReportingStructureNotFound() throws EmployeeNotFoundException {
        String employeeId = UUID.randomUUID().toString();
        exceptionRule.expect(EmployeeNotFoundException.class);
        exceptionRule.expectMessage("Invalid employeeId: " + employeeId);

        employeeService.getReportingStructure(employeeId);
    }

    @Test
    public void testGetReportingStructureDirectReportNotFound() throws EmployeeNotFoundException {
        String invalidDirectReport = UUID.randomUUID().toString();
        exceptionRule.expect(EmployeeNotFoundException.class);
        exceptionRule.expectMessage("Invalid employeeId: " + invalidDirectReport);
        Employee employee = createEmployee(null, Collections.singletonList(invalidDirectReport));

        employeeService.getReportingStructure(employee.getEmployeeId());
    }

    /**
     * Generates and saves an employee to the database
     * @param firstName employee first name, helpful for debugging
     * @param directReports ids of the employee's direct reports
     * @return an Employee with an id that can be fetched from the database
     */
    private Employee createEmployee(String firstName, List<String> directReports) {
        Employee employee = new Employee();
        employee.setFirstName(ObjectUtils.defaultIfNull(firstName, "John"));
        employee.setLastName("Doe");
        employee.setDepartment("Engineering");
        employee.setPosition("Developer");
        if (directReports != null && !directReports.isEmpty()) {
            List<Employee> directReportEmployees = directReports.stream().map(employeeId -> {
                Employee directReport = new Employee();
                directReport.setEmployeeId(employeeId);
                return directReport;
            }).collect(Collectors.toList());
            employee.setDirectReports(directReportEmployees);
        }


        return employeeService.create(employee);
    }

    static void assertEmployeeEquivalence(Employee expected, Employee actual) {
        assertEquals(expected.getFirstName(), actual.getFirstName());
        assertEquals(expected.getLastName(), actual.getLastName());
        assertEquals(expected.getDepartment(), actual.getDepartment());
        assertEquals(expected.getPosition(), actual.getPosition());
    }

    private static void assertReportingStructureEquivalence(ReportingStructure expected, ReportingStructure actual) {
        assertEmployeeEquivalence(expected.getEmployee(), actual.getEmployee());
        assertEquals(expected.getNumberOfReports(), actual.getNumberOfReports());
    }

}
