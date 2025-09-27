package com.kingmang.ixion;

import com.kingmang.ixion.api.IxApi;
import com.kingmang.ixion.api.IxionConstant;
import com.kingmang.ixion.exception.IxException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

public class Ixion {

    private String entry;
    private boolean helpRequested = false;

    public static void main(String[] args) {
        Ixion cli = new Ixion();
        cli.parseArguments(args);
        cli.run();
    }

    private void parseArguments(String[] args) {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-h":
                case "--help":
                    helpRequested = true;
                    break;
                default:
                    if (entry == null && !args[i].startsWith("-")) {
                        entry = args[i];
                    }
                    break;
            }
        }
    }

    private void printHelp() {
        System.out.println("Usage: ixion [OPTIONS] <entry-file>");
        System.out.println("Compile and run an ixion program.\n");
        System.out.println("Options:");
        System.out.println("  -h, --help    Display this help message\n");
    }

    public void execute(String className) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(
                "java",
                "--enable-preview",
                "-cp",
                IxionConstant.OUT_DIR + System.getProperty("path.separator") + "target/classes",
                className
        );
        processBuilder.inheritIO();
        Process process = processBuilder.start();

        int status = process.waitFor();
        if (status != 0) System.err.println("Process finished with exit code " + status);
    }

    public void run() {
        if (helpRequested) {
            printHelp();
            return;
        }

        if (entry == null) {
            System.err.println("Error: Entry file is required");
            printHelp();
            System.exit(1);
            return;
        }

        var imp = new IxApi();
        String pwd = System.getProperty("user.dir");
        String moduleLocation = Path.of(pwd).toString();

        String classPath = null;
        try {
            classPath = imp.compile(moduleLocation, entry);
        } catch (FileNotFoundException e) {
            System.err.println("file not exist");
            System.exit(2);
        } catch (IxException.CompilerError e) {
            IxApi.exit(e.getMessage(), 1);
        }

        try {
            execute(classPath);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}