package com.mindex.challenge.service.impl;

import com.mindex.challenge.dao.CompensationRepository;
import com.mindex.challenge.data.Compensation;
import com.mindex.challenge.data.Employee;
import com.mindex.challenge.exceptions.NotFoundException;
import com.mindex.challenge.service.CompensationService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CompensationServiceImplTest {
    private String createUrl;
    private String findByEmployeeIdUrl;

    @Autowired
    private CompensationService compensationService;

    @Autowired
    private CompensationRepository compensationRepository;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Before
    public void setup() {
        createUrl = "http://localhost:" + port + "/compensation";
        findByEmployeeIdUrl = "http://localhost:" + port + "/employee/{employeeId}/compensation";
    }

    @Test
    public void testCreateFindByEmployeeId() {
        Employee employee = new Employee();
        employee.setEmployeeId(UUID.randomUUID().toString());
        Compensation compensation = new Compensation();
        compensation.setEmployee(employee);
        compensation.setSalary(new BigDecimal("150000"));
        compensation.setEffectiveDate(LocalDate.parse("2023-01-01"));

        // Create checks
        Compensation createdCompensation = restTemplate.postForEntity(createUrl, compensation, Compensation.class)
                .getBody();

        assertNotNull(createdCompensation);
        assertEquals(compensation, createdCompensation);


        // Find by employeeId checks
        Compensation foundCompensation = restTemplate.getForEntity(findByEmployeeIdUrl, Compensation.class,
                        employee.getEmployeeId())
                .getBody();
        assertNotNull(foundCompensation);
        assertEquals(createdCompensation, foundCompensation);
    }

    @Test
    public void testFindByEmployeeIdMultipleCompensations() {
        String employeeId = UUID.randomUUID().toString();
        // Oldest
        insertCompensation(employeeId, new BigDecimal("100000"), LocalDate.parse("2021-01-01"));
        // Newest
        Compensation expectedCompensation = insertCompensation(employeeId, new BigDecimal("115000"),
                LocalDate.parse("2022-10-01"));
        // In-between
        insertCompensation(employeeId, new BigDecimal("110000"), LocalDate.parse("2022-01-01"));

        // Ensure the most recent compensation is returned
        Compensation latestCompensation = restTemplate.getForEntity(findByEmployeeIdUrl, Compensation.class, employeeId)
                .getBody();
        assertNotNull(latestCompensation);
        assertEquals(latestCompensation, expectedCompensation);
    }

    @Test
    public void testFindByEmployeeIdNotFound() throws NotFoundException {
        String employeeId = UUID.randomUUID().toString();
        exceptionRule.expect(NotFoundException.class);
        exceptionRule.expectMessage("Compensation not found for employee: " + employeeId);

        compensationService.findByEmployeeId(employeeId);
    }

    private Compensation insertCompensation(String employeeId, BigDecimal salary, LocalDate effectiveDate) {
        Employee employee = new Employee();
        employee.setEmployeeId(employeeId);
        Compensation compensation = new Compensation();
        compensation.setEmployee(employee);
        compensation.setSalary(salary);
        compensation.setEffectiveDate(effectiveDate);

        return compensationRepository.insert(compensation);
    }
}
