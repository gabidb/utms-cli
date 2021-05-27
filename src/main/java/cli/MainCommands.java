package cli;
import com.beust.jcommander.Parameter;


import java.nio.file.Path;
import java.util.Random;

public class MainCommands {


    /* TODO
        validateWith - create class that validate the path
     */

    @Parameter(names = {"--help", "-h"},
             description = "Display Usage",
             help = true)
    public boolean help;

    @Parameter(names = {"--config", "-c"},
            description = "The path to the test configuration YAML file",
            converter = PathConverter.class)
    public Path file_path = Path.of("C:\\Users\\gabid\\Documents\\Talent Boost\\TB_Final_Project\\utms-cli\\src\\main\\resources\\testing.yaml"); // default value


    public boolean isHelp(){
        return help;
    }

}
