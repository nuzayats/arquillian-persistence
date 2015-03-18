package arq.business;

import arq.entity.Dept;
import arq.entity.Employee;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.Collection;

@Stateless
@LocalBean
public class HumanResourcesBean {

    @PersistenceContext
    private EntityManager em;

    public void addEmployee(Employee employee, Integer deptId) {
        final Dept dept = em.find(Dept.class, deptId);
        dept.getEmployees().add(employee);
        employee.setDept(dept);
        em.persist(employee);
    }

    public void addDept(Dept dept, Employee employee) {
        Collection<Employee> employees = new ArrayList<>();
        dept.setEmployees(employees);
        employees.add(employee);
        employee.setDept(dept);
        em.persist(dept);
        em.persist(employee);
    }
}
