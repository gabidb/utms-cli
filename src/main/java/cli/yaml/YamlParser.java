package cli.yaml;
import java.util.ArrayList;
import java.util.Map;


public class YamlParser {
    public Map<String, String> project;

    public ArrayList<Map<String,ArrayList<Map<String,Map<String,Object>>>>> suites;

    public Map<String, String> getProject() {
        return project;
    }

    public void setProject(Map<String, String> project) {
        this.project = project;
    }

    public ArrayList<Map<String, ArrayList<Map<String, Map<String, Object>>>>> getSuites() {
        return suites;
    }

    public void setSuites(ArrayList<Map<String, ArrayList<Map<String, Map<String, Object>>>>> suites) {
        this.suites = suites;
    }
}