
package com.google.refine.tests.expr.functions;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.HasFieldsListImpl;
import com.google.refine.expr.WrappedRow;
import com.google.refine.expr.WrappedCell;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.model.Cell;
import com.google.refine.tests.RefineTest;

/**
 * Test cases for cross function.
 */
public class CrossFunctionTests extends RefineTest {
    static Properties bindings;
    private static OffsetDateTime dateTimeValue = OffsetDateTime.parse("2017-05-12T05:45:00+00:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    
    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    // dependencies
    Project projectGift;
    Project projectAddress;
    
    // data from: https://github.com/OpenRefine/OpenRefine/wiki/GREL-Other-Functions
    @BeforeMethod
    public void SetUp() {
        bindings = new Properties();
        
        String projectName = "My Address Book";
        String input = "friend,address\n"
                        + "john,120 Main St.\n"
                        + "mary,50 Broadway Ave.\n"
                        + "john,999 XXXXXX St.\n"                       // john's 2nd address
                        + "anne,17 Morning Crescent\n"
                        + "2017-05-12T05:45:00Z,dateTime\n"
                        + "1600,integer\n"
                        + "true,boolean\n";
        projectAddress = createCSVProject(projectName, input);
    
        projectName = "Christmas Gifts";
        input = "gift,recipient\n"   
                + "lamp,mary\n"
                + "clock,john\n"
                + "dateTime,2017-05-12T05:45:00Z\n"
                + "integer,1600\n"
                + "boolean,true\n";
        projectGift = createCSVProject(projectName, input);
        
        bindings.put("project", projectGift);
        
        //Add some non-string value cells to each project
        projectAddress.rows.get(4).cells.set(0, new Cell(dateTimeValue, null));
        projectAddress.rows.get(5).cells.set(0, new Cell(1600, null));
        projectAddress.rows.get(6).cells.set(0, new Cell(true, null));
        projectGift.rows.get(2).cells.set(1, new Cell(dateTimeValue, null));
        projectGift.rows.get(3).cells.set(1, new Cell(1600, null));
        projectGift.rows.get(4).cells.set(1, new Cell(true, null));
        
        // add a column address based on column recipient
        bindings.put("columnName", "recipient");
    }
    
    @Test
    public void crossFunctionOneToOneTest() throws Exception {
        Row row = ((Row)((WrappedRow) ((HasFieldsListImpl) invoke("cross", "mary", "My Address Book", "friend")).get(0)).row);
        String address = row.getCell(1).value.toString();
        Assert.assertEquals(address, "50 Broadway Ave.");
    }
    
    /**  
     * To demonstrate that the cross function can join multiple rows.
     */
    @Test
    public void crossFunctionOneToManyTest() throws Exception {
        Row row = ((Row)((WrappedRow) ((HasFieldsListImpl) invoke("cross", "john", "My Address Book", "friend")).get(1)).row);
        String address = row.getCell(1).value.toString();
        Assert.assertEquals(address, "999 XXXXXX St.");
    }
    

    @Test
    public void crossFunctionCaseSensitiveTest() throws Exception {
        Assert.assertNull(invoke("cross", "Anne", "My Address Book", "friend"));
    }
    
    @Test
    public void crossFunctionDateTimeTest() throws Exception {
        Project project = (Project) bindings.get("project");
        Cell c = project.rows.get(2).cells.get(1);
        WrappedCell lookup = new WrappedCell(project, "recipient", c);
        Row row = ((Row)((WrappedRow) ((HasFieldsListImpl) invoke("cross", lookup, "My Address Book", "friend")).get(0)).row);
        String address = row.getCell(1).value.toString();
        Assert.assertEquals(address, "dateTime");
    }
    
    @Test
    public void crossFunctionIntegerTest() throws Exception {
        Project project = (Project) bindings.get("project");
        Cell c = project.rows.get(3).cells.get(1);
        WrappedCell lookup = new WrappedCell(project, "recipient", c);
        Row row = ((Row)((WrappedRow) ((HasFieldsListImpl) invoke("cross", lookup, "My Address Book", "friend")).get(0)).row);
        String address = row.getCell(1).value.toString();
        Assert.assertEquals(address, "integer");
    }
    
    @Test
    public void crossFunctionBooleanTest() throws Exception {
        Project project = (Project) bindings.get("project");
        Cell c = project.rows.get(4).cells.get(1);
        WrappedCell lookup = new WrappedCell(project, "recipient", c);
        Row row = ((Row)((WrappedRow) ((HasFieldsListImpl) invoke("cross", lookup, "My Address Book", "friend")).get(0)).row);
        String address = row.getCell(1).value.toString();
        Assert.assertEquals(address, "boolean");
    }
    
    
    /**
     * If no match, return null.
     * 
     * But if user still apply grel:value.cross("My Address Book", "friend")[0].cells["address"].value, 
     * from the "Preview", the target cell shows "Error: java.lang.IndexOutOfBoundsException: Index: 0, Size: 0".
     * It will still end up with blank if the onError set so.
     */
    @Test
    public void crossFunctionMatchNotFoundTest() throws Exception {
        Assert.assertNull(invoke("cross", "NON-EXIST", "My Address Book", "friend"));
    }
     
    /**
     *  
     *  rest of cells shows "Error: cross expects a string or cell, a project name to join with, and a column name in that project"
     */
    @Test
    public void crossFunctionNonLiteralValue() throws Exception {
        Assert.assertEquals(((EvalError) invoke("cross", 1, "My Address Book", "friend")).message, 
                "cross expects a string or cell, a project name to join with, and a column name in that project");
        
        Assert.assertEquals(((EvalError) invoke("cross", null, "My Address Book", "friend")).message, 
                "cross expects a string or cell, a project name to join with, and a column name in that project");
        
        Assert.assertEquals(((EvalError) invoke("cross", Calendar.getInstance(), "My Address Book", "friend")).message, 
                "cross expects a string or cell, a project name to join with, and a column name in that project");
    }
    
    /**
     * Lookup a control function by name and invoke it with a variable number of args
     */
    private static Object invoke(String name,Object... args) {
        // registry uses static initializer, so no need to set it up
        Function function = ControlFunctionRegistry.getFunction(name);
        if (function == null) {
            throw new IllegalArgumentException("Unknown function "+name);
        }
        if (args == null) {
            return function.call(bindings,new Object[0]);
        } else {
            return function.call(bindings,args);
        }
    }
}
