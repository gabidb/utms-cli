package cli.Information;

import java.util.List;
import java.util.Map;

public class SuiteInfo {

    private String name;
    private String status;
    private List<String> testName;
    private Map<String,List<TestInfo>> tests;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, List<TestInfo>> getTests() {
        return tests;
    }

    public void setTests(Map<String, List<TestInfo>> tests) {
        this.tests = tests;
    }

    public List<String> getTestName() {
        return testName;
    }

    public void setTestName(List<String> testName) {
        this.testName = testName;
    }
}
