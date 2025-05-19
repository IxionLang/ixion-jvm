package test;

import com.kingmang.ixion.api.IxionApi;
import com.kingmang.ixion.runner.Runner;

import java.nio.file.Path;

public class Test {
    public static void main(String[] args) throws Exception {
        IxionApi api = new IxionApi();
        api.getFiles().add("src/main/java/test/Test.ix");
        for(String std_file : Runner.STD_FILES)
            api.getFiles().add("std/" + std_file);
        api.compile();
        api.runClass(Path.of("out"), "Testixc", new String[]{});
    }
}
