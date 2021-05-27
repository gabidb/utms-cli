package cli;

import cli.Information.ProjectInfo;
import cli.Information.SuiteInfo;
import cli.Information.TestInfo;
import cli.yaml.YamlParser;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tomcat.util.json.JSONParser;
import org.json.JSONArray;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import java.io.*;
import java.lang.reflect.Field;
import java.time.ZoneId;
import java.util.*;

public class Main {

    //TODO
    // Validate the input file

    MainCommands mainCommands = new MainCommands();

    public static void main(String[] args) throws Exception{

        Main main = new Main();

        main.handleInput(args);
        main.loadYAML();
    }

    void handleInput(String[] args){
        JCommander jCommander = new JCommander(mainCommands);

        try{
            jCommander.parse(args);
        }
        catch (ParameterException e){
            System.out.println(e.getMessage());
            showUsage(jCommander);
        }

        if(mainCommands.isHelp()){
            showUsage(jCommander);
        }
    }

    void showUsage(JCommander jCommander){
        jCommander.usage();
        System.exit(0);
    }

    void loadYAML() {

        Constructor constructor = new Constructor(YamlParser.class);
        Yaml yaml1 = new Yaml(constructor);


        InputStream input = null;
        try {
            input = new FileInputStream(new File("C:\\Users\\gabid\\Documents\\Computer Science\\test\\src\\main\\resources\\testing.yaml"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        YamlParser data = yaml1.loadAs(input, YamlParser.class);

        //List of name of suites to use as keys
        List<String> suites = new ArrayList<>();
        for (int i = 0; i < data.suites.size(); i++) {
            data.suites.get(i).entrySet().forEach(e -> {
                suites.add(e.getKey());
            });
        }

        Map<String, List<Map<String, Map<String, Object>>>> testMap = new HashMap<>();
        //Map SuiteName - Test collection
        for (int i = 0; i < data.suites.size(); i++) {
            List<Map<String, Map<String, Object>>> list1 = new ArrayList<>();
            list1.addAll(data.suites.get(i).get(suites.get(i)));
            testMap.put(suites.get(i), list1);
        }

        loadInfo(testMap, suites, data);

    }

    void loadInfo(Map<String, List<Map<String, Map<String, Object>>>> testMap, List<String> suites, YamlParser data){
        ProjectInfo projectInfo = new ProjectInfo();

        projectInfo.setName(data.project.get("name"));
        projectInfo.setDescription(data.project.get("description"));
        //TODO set project status

        List<SuiteInfo> suiteInfoList = new ArrayList<>();

        // trying testMap
        for (int i = 0; i < suites.size(); i++) {
            SuiteInfo suiteInfo = new SuiteInfo();
            suiteInfo.setName(suites.get(i));
            List<String> testNames = new ArrayList<>();
            Map<String,List<TestInfo>> map = new HashMap<>();

            for (int j = 0; j < testMap.get(suites.get(i)).size(); j++) {
                List<TestInfo> testInfoList = new ArrayList<>();
                TestInfo testInfo = new TestInfo();

                testMap.get(suites.get(i)).get(j).entrySet().forEach(e -> {
                    String testName = e.getKey(); //SETTING TEST NAME
                    testNames.add(testName);
                    testInfo.setDescription(e.getValue().get("description")); //SETTING TEST DESCRIPTION

                    if((Boolean)e.getValue().get("enabled")){
                        setTestInfo(testInfo,e.getValue().get("command"));
                    }
                    else {
                        testInfo.setStatus("skipped");
                        setSkippedTest(testInfo);
                    }
                    testInfoList.add(testInfo);
                });

                //CHECKING SUITE STATUS
                if(testInfoList.get(0).getStatus().equals("failing"))
                    suiteInfo.setStatus("failing");
                else
                    suiteInfo.setStatus("passing");

                if(!map.containsKey(testMap.get(suites.get(i)).get(j).toString())) {
                    testMap.get(suites.get(i)).get(j).entrySet().forEach(d->{
                        map.put(d.getKey(), testInfoList);
                    });
                }
            }

            suiteInfo.setTestName(testNames);
            suiteInfo.setTests(map);
            suiteInfoList.add(suiteInfo);
        }
        overallStatus(suiteInfoList, projectInfo);
        createJson(projectInfo, suiteInfoList,suites); //create JSON
    }

    void setSkippedTest(TestInfo testInfo){
        testInfo.setDescription("null");
        testInfo.setError("null");
        testInfo.setOutput("null");
        testInfo.setStartDate("null");
        testInfo.setEndDate("null");
    }

    void setTestInfo(TestInfo testInfo, Object command){

        try {
            Process process = Runtime.getRuntime().exec("cmd.exe /c "+ command);

            process.waitFor();
            int exitValue = process.exitValue();  // SETTING TEST STATUS
            if(exitValue == 0){
                testInfo.setStatus("passed");
                getProcessInfo(testInfo, process);
            }
            else{
                testInfo.setStatus("failing");
                testInfo.setOutput(""); // SETTING OUTPUT IN BASE64 STRING
                testInfo.setStartDate(""); //SETTING STARTING TIME
                testInfo.setEndDate("");
                try (final BufferedReader b = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    if ((line = b.readLine()) != null)
                        testInfo.setError(line); //SETTING ERROR
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    void getProcessInfo(TestInfo testInfo, Process process){

        String output = null;
        try {
            output = printResults(process);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ProcessHandle.Info info = process.info();
        String na = "<Not Available>";
        String startDate = info.startInstant().map(i -> i.atZone(ZoneId.systemDefault())
                .toLocalDateTime().toString())
                .orElse(na);

        // TODO find endTIme
        String endDate = info.startInstant().map(i->i.atZone(ZoneId.systemDefault())
                .toLocalDateTime().toString())
                .orElse(na); //find when the process finished

        testInfo.setError(""); //SETTING ERROR
        testInfo.setOutput(output); // SETTING OUTPUT IN BASE64 STRING
        testInfo.setStartDate(startDate); //SETTING STARTING TIME
        testInfo.setEndDate(endDate);

    }

    String printResults(Process process) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = "";
        while ((line = reader.readLine()) != null) {
            Base64.Encoder encoder = Base64.getEncoder();
            String encodedString = encoder.encodeToString(line.getBytes());

            return encodedString;
        }
        return line;
    }

    void overallStatus(List<SuiteInfo> suiteInfoList, ProjectInfo projectInfo){
        int counter = 0;
        for(SuiteInfo s: suiteInfoList){
            if(s.getStatus().equals("failed"))
                counter++;
        }
        if(counter > 0)
            projectInfo.setStatus("failed");
        else
            projectInfo.setStatus("passed");
    }

    void createJson(ProjectInfo projectInfo, List<SuiteInfo> suiteInfoList, List<String> suites){

        Map<String, Map<String, Map<String,String>>> suiteMap = new LinkedHashMap<>();
        List<Map<String,Map<String,String>>> testList = new ArrayList<>();
        Map<String,String> suiteStatus = new LinkedHashMap<>();
        Map<String, Map<String,String>> testMap = new LinkedHashMap<>();

        for(int i = 0; i < suites.size(); i++) {
            testMap = new LinkedHashMap<>();

            for(int j = 0; j < suiteInfoList.get(i).getTestName().size(); j++) {
                Map<String, String> testItem = new LinkedHashMap<>();

                try {
                    testItem.put("description",suiteInfoList.get(i).getTests().get(suiteInfoList.get(i).getTestName().get(j)).get(0).getDescription().toString());

                } catch (NullPointerException e) {
                    testItem.put("description", "");
                }

                testItem.put("output",suiteInfoList.get(i).getTests().get(suiteInfoList.get(i).getTestName().get(j)).get(0).getOutput());
                testItem.put("error",suiteInfoList.get(i).getTests().get(suiteInfoList.get(i).getTestName().get(j)).get(0).getError());
                testItem.put("status",suiteInfoList.get(i).getTests().get(suiteInfoList.get(i).getTestName().get(j)).get(0).getStatus());
                testItem.put("startDate",suiteInfoList.get(i).getTests().get(suiteInfoList.get(i).getTestName().get(j)).get(0).getStartDate());
                testItem.put("endDate", suiteInfoList.get(i).getTests().get(suiteInfoList.get(i).getTestName().get(j)).get(0).getEndDate());

                testMap.put(suiteInfoList.get(i).getTestName().get(j), testItem);

            }
            testList.add(testMap);
            suiteMap.put(suiteInfoList.get(i).getName(), testMap);
            suiteStatus.put("status", suiteInfoList.get(i).getStatus());
        }


        JSONObject result = new JSONObject();
        JSONObject project = new JSONObject();
        JSONArray suite = new JSONArray();

        try {
            Field changeMap = result.getClass().getDeclaredField("map");
            changeMap.setAccessible(true);
            changeMap.set(result, new LinkedHashMap<>());
            changeMap.setAccessible(false);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.getMessage();
        }

        //List<Map<String,Object>> js = new ArrayList<>();
        for(int i = 0; i < suites.size(); i++) {
            Map<String,Object> jsonOrderedMap = new LinkedHashMap<>();

            jsonOrderedMap.put("name", suites.get(i));
            jsonOrderedMap.put("status", suiteStatus.get("status"));
            jsonOrderedMap.put("tests", testList.get(i));

            //System.out.println(jsonOrderedMap);
            suite.put(jsonOrderedMap);
        }
//        suite.put(result);

//        for (int i =0; i < js.size(); i++){
//            suite.put(js.get(i));
        //       }

        result.put("project",project);
        result.put("status", projectInfo.getStatus());
        result.put("suites", suite);

        project.put("name", projectInfo.getName());
        project.put("description", projectInfo.getDescription());

        String str = result.toString();
        //System.out.println(str);
        ObjectMapper mapper = new ObjectMapper();
        Object json = null;
        try {
            json = mapper.readValue(str, Object.class);
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json));
        } catch (IOException e ) {
            e.printStackTrace();
        }

    }
}
