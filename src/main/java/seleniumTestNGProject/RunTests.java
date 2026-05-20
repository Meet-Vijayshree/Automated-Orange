package seleniumTestNGProject;

import java.util.Collections;

import org.testng.TestNG;

public class RunTests {
    public static void main(String[] args) {
        TestNG testng = new TestNG();
        testng.setTestSuites(Collections.singletonList("testng.xml"));
        testng.run();
    }
}
