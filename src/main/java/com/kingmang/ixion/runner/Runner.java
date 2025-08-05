package com.kingmang.ixion.runner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.kingmang.ixion.api.IxionApi;
import com.kingmang.ixion.test_runner.TestRunner;
import com.kingmang.ixion.test_runner.entities.IxTest;
import com.kingmang.ixion.test_runner.entities.IxTestTimeBound;
import lombok.SneakyThrows;

public class Runner {
	public static final String[] STD_FILES = {
			"string.ix",
			"unit_test.ix"
	};

	@SuppressWarnings({"resource"})
    @SneakyThrows
	private static void deleteTemp() {
		if (!IxionApi.getOutputDirectory().exists()) {
			if (!IxionApi.getOutputDirectory().mkdir()) {
				System.err.println("[Warning] Failed to create folder /out");
			}
		}
		Files.walk(Path.of(IxionApi.getOutputDirectory().toURI()))
			.filter(Files::isRegularFile)
			.forEach(p -> {
				try {
					Files.delete(p);
				} catch (IOException e) {
					System.err.println("[Warning] Failed to delete file: " + p);
				}
			});
	}

	@SneakyThrows
	public static void run(String path) {
		IxionApi api = new IxionApi();
		api.getFiles().add(path);
		api.compile();
		String fileName = Path.of(path).getFileName().toString();
		String className;
		if (fileName.contains(".")) {
			className = fileName.substring(0, fileName.lastIndexOf(".")).replace(".", "");
		} else {
			className = fileName.replace(".", "");
		}
		className += "ixc";
		api.runClass(className, List.of(fileName), new String[]{});
	}

	private static void runTests() {
		TestRunner runner = new TestRunner();
		runner.run(new IxTest[]{
			IxTest.builder()
				.name("basic test")
				.filename("./tests/base.ix")
				.timeBound(new IxTestTimeBound(
					100,
					35
				))
				.build(),
			IxTest.builder().name("oop test").filename("./tests/oop.ix").timeBound(null).build(),
			IxTest.builder().name("nullable test").filename("./tests/nullable.ix").timeBound(null).build(),
		});
	}

	@SneakyThrows
	public static void main(String[] args) {
		if (args.length < 1) {
			incorrectCommand();
			return;
		}

		// обработка команды
		String command = args[0];
        switch (command) {
            case "-l" -> {
				if (args.length < 2) { incorrectCommand(); return; }
				String inputFilePath = args[1];
				if (!Files.exists(Path.of(inputFilePath))) {
					System.err.println("[Error] file not found: " + inputFilePath);
					return;
				}
				System.out.println(consoleLog(inputFilePath));
			}
            case "-r" -> {
				if (args.length < 2) { incorrectCommand(); return; }
				String inputFilePath = args[1];
				if (!Files.exists(Path.of(inputFilePath))) {
					System.err.println("[Error] file not found: " + inputFilePath);
					return;
				}
				deleteTemp(); run(inputFilePath);
			}
            case "-t" -> runTests();
            default -> System.err.println("[Error] unknown command: " + command);
        }
	}

	private static void incorrectCommand() {
		System.err.println("[Error] \n Enter command and file.");
		System.out.println("| console log: -l");
		System.out.println("| run file: -r filename");
		System.out.println("| run tests: -t");
	}

	static String consoleLog(String filename) {
		return """
                [Ixion version : %s]
                [Shuttle version : %s]
                [Filename: %s]
                """.formatted(
				RunnerInfo.VERSION,
				RunnerInfo.SHUTTLE_MESSAGE,
				filename
		);
	}
}
