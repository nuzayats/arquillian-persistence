package arq.business;

import arq.entity.Dept;
import arq.entity.Employee;
import org.dbunit.Assertion;
import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.persistence.ShouldMatchDataSet;
import org.jboss.arquillian.persistence.UsingDataSet;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@RunWith(Arquillian.class)
public class HumanResourcesBeanIT {
    @Deployment
    public static Archive<?> createDeploymentPackage() {
        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "test.war")
                .addPackage(Dept.class.getPackage())
                .addClass(HumanResourcesBean.class)
                .addAsResource("datasets/addDept-expected.xml") // to be loaded by DBUnit on the server side
                .addAsResource("test-persistence.xml", "META-INF/persistence.xml");
//        System.out.println(webArchive.toString(true));
        return webArchive;
    }

    @Resource(lookup = "java:jboss/datasources/ExampleDS")
    private DataSource ds;
    @PersistenceContext
    private EntityManager em;
    @EJB
    private HumanResourcesBean humanResourcesBean;

    @Test
    @UsingDataSet("input.xml")
    @ShouldMatchDataSet(value = "addEmployee-expected.xml", orderBy = "id")
    public void addEmployeeTest() throws Exception {
        Employee emp = new Employee();
        emp.setId(2002);
        emp.setName("Todd");
        humanResourcesBean.addEmployee(emp, 200);
    }

    @Test
    @UsingDataSet("input.xml")
    @ShouldMatchDataSet(value = "addDept-expected.xml", orderBy = "id")
    public void addDeptTest() throws Exception {
        Dept dept = new Dept();
        dept.setId(300);
        dept.setName("Engineering");
        Employee emp = new Employee();
        emp.setId(3000);
        emp.setName("Carl");
        humanResourcesBean.addDept(dept, emp);
    }

    @Test
    @UsingDataSet("input.xml")
    public void addDeptTestWithDbUnit() throws Exception {
        Dept dept = new Dept();
        dept.setId(300);
        dept.setName("Engineering");
        Employee emp = new Employee();
        emp.setId(3000);
        emp.setName("Carl");

        humanResourcesBean.addDept(dept, emp);
        em.flush(); // force JPA to execute DMLs before assertion

        final IDataSet expectedDataSet = getDataSet("/datasets/addDept-expected.xml");
        assertTable(expectedDataSet.getTable("Dept"), "select * from dept order by id");
        assertTable(expectedDataSet.getTable("Employee"), "select * from employee order by id");
    }

    private static IDataSet getDataSet(String path) throws DataSetException {
        return new FlatXmlDataSetBuilder().build(HumanResourcesBeanIT.class.getResource(path));
    }

    private void assertTable(ITable expectedTable, String sql) throws SQLException, DatabaseUnitException {
        try (Connection cn = ds.getConnection()) {
            IDatabaseConnection icn = null;
            try {
                icn = new DatabaseConnection(cn);
                final ITable queryTable = icn.createQueryTable(expectedTable.getTableMetaData().getTableName(), sql);
                Assertion.assertEquals(expectedTable, queryTable);
            } finally {
                if (icn != null) {
                    icn.close();
                }
            }
        }
    }
}
