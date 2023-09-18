package com.mindex.challenge;

import com.mindex.challenge.dao.CompensationRepository;
import com.mindex.challenge.dao.EmployeeRepository;
import com.mindex.challenge.data.Compensation;
import com.mindex.challenge.data.Employee;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.Assert.assertEquals;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DataBootstrapTest {

    private static final String LENNON_ID = "16a596ae-edd3-4847-99fe-c4518e82c86f";

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CompensationRepository compensationRepository;

    @Test
    public void testEmployees() {
        Employee employee = employeeRepository.findByEmployeeId(LENNON_ID);
        assertNotNull(employee);
        assertEquals("John", employee.getFirstName());
        assertEquals("Lennon", employee.getLastName());
        assertEquals("Development Manager", employee.getPosition());
        assertEquals("Engineering", employee.getDepartment());
    }

    @Test
    public void testCompensation() {
        Compensation compensation = compensationRepository.findFirstByEmployee_EmployeeIdOrderByEffectiveDateDesc(LENNON_ID);
        assertNotNull(compensation);
        assertEquals(LENNON_ID, compensation.getEmployee().getEmployeeId());
        assertEquals(new BigDecimal("200000"), compensation.getSalary());
        assertEquals(LocalDate.parse("2022-03-01"), compensation.getEffectiveDate());
    }
}