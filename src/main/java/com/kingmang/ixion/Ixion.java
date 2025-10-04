package com.kingmang.ixion;

import com.kingmang.ixion.api.IxApi;
import com.kingmang.ixion.api.IxionConstant;
import com.kingmang.ixion.exception.IxException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Ixion {

    private String entry;
    private boolean helpRequested = false;
    private boolean compileOnly = false;
    private CompilationTarget target = CompilationTarget.JVM_BYTECODE;

    public static void main(String[] args) {
        Ixion cli = new Ixion();
        cli.parseArguments(args);
        cli.run();
    }

    private void parseArguments(String[] args) {
        for (String arg : args) {
            switch (arg) {
                case "-h":
                case "--help":
                    helpRequested = true;
                    break;
                case "--java":
                    target = CompilationTarget.JAVA_SOURCE;
                    break;
                case "--compile-only":
                    compileOnly = true;
                    break;
                default:
                    if (entry == null && !arg.startsWith("-")) {
                        entry = arg;
                    }
                    break;
            }
        }
    }

    private void printHelp() {
        System.out.println("Usage: ixion [OPTIONS] <entry-file>");
        System.out.println("Compile and run an ixion program.\n");
        System.out.println("Options:");
        System.out.println("  -h, --help        Display this help message");
        System.out.println("  --java            Generate Java source code instead of bytecode");
        System.out.println("  --compile-only    Only compile, do not run\n");
    }

    public void executeBytecode(String className) throws IOException, InterruptedException {
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

    public void compileJavaToBytecode(String projectRoot, String basePath) throws IOException, InterruptedException {
        String javaFile = Path.of(projectRoot, IxionConstant.OUT_DIR, basePath + ".java").toString();
        String classpath = IxionConstant.OUT_DIR + System.getProperty("path.separator") + "target/classes";

        List<String> command = new ArrayList<>();
        command.add("javac");
        command.add("-d");
        command.add(IxionConstant.OUT_DIR);
        command.add("-cp");
        command.add(classpath);
        command.add(javaFile);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.inheritIO();
        Process process = processBuilder.start();

        int status = process.waitFor();
        if (status != 0) {
            throw new IOException("Java compilation failed with exit code " + status);
        }
    }

    public void executeJava(String className) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(
                "java",
                "-cp",
                IxionConstant.OUT_DIR + System.getProperty("path.separator") + "target/classes",
                className
        );
        processBuilder.inheritIO();
        Process process = processBuilder.start();

        int status = process.waitFor();
        if (status != 0) System.err.println("Process finished with exit code " + status);
    }

    public void compileAndRunJava(String projectRoot, String basePath, String className)
            throws IOException, InterruptedException {
        System.out.println("Compiling Java source code...");
        compileJavaToBytecode(projectRoot, basePath);

        if (!compileOnly) {
            System.out.println("Running Java program...");
            executeJava(className);
        }
    }

    public void compileAllJavaFiles(String directory) throws IOException, InterruptedException {
        File dir = new File(directory);
        File[] javaFiles = dir.listFiles((d, name) -> name.endsWith(".java"));

        if (javaFiles != null) {
            List<String> command = new ArrayList<>();
            command.add("javac");
            command.add("-d");
            command.add(IxionConstant.OUT_DIR);
            command.add("-cp");
            command.add(IxionConstant.OUT_DIR + System.getProperty("path.separator") + "target/classes");

            for (File javaFile : javaFiles) {
                command.add(javaFile.getAbsolutePath());
            }

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.inheritIO();
            Process process = processBuilder.start();

            int status = process.waitFor();
            if (status != 0) {
                throw new IOException("Java compilation failed with exit code " + status);
            }
        }
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

        var api = new IxApi();
        String pwd = System.getProperty("user.dir");
        String moduleLocation = Path.of(pwd).toString();

        try {
            String classPath = null;
            String basePath = null;

            if (target == CompilationTarget.JAVA_SOURCE) {
                classPath = api.compileToJava(moduleLocation, entry);
                basePath = classPath.replace(".", "/");

                if (!compileOnly) {
                    compileAndRunJava(moduleLocation, basePath, classPath);
                } else {
                    System.out.println("Java source generated: " +
                            Path.of(moduleLocation, IxionConstant.OUT_DIR, basePath + ".java"));
                }
            } else {
                classPath = api.compile(moduleLocation, entry);

                if (!compileOnly) {
                    executeBytecode(classPath);
                }
            }

        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + e.getMessage());
            System.exit(2);
        } catch (IxException.CompilerError e) {
            IxApi.exit(e.getMessage(), 1);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.exit(3);
        }
    }

    public enum CompilationTarget {
        JVM_BYTECODE,
        JAVA_SOURCE
    }
}