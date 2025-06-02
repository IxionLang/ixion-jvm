package com.kingmang.ixion.runner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.kingmang.ixion.api.IxionApi;
import lombok.SneakyThrows;

public class Runner {
	public static final String[] STD_FILES = {
			"string.ix",
			"unit_test.ix"
	};

	@SneakyThrows
	public static void main(String[] args) {
		IxionApi api = new IxionApi();
		if (!IxionApi.getOutputDirectory().exists()) {
			IxionApi.getOutputDirectory().mkdir();
		}

		Files.walk(Path.of(IxionApi.getOutputDirectory().toURI()))
				.filter(Files::isRegularFile)
				.forEach(p -> {
					try {
						Files.delete(p);
					} catch (IOException e) {
						System.err.println("[Warning] Не удалось удалить файл: " + p);
					}
				});

		if (args.length < 2) {
			System.err.println("[Error] \n Пожалуйста, введите команду и имя файла. Пример: ixion -r test.ix");
			return;
		}

		String command = args[0];
		String inputFilePath = args[1];

		if (!Files.exists(Path.of(inputFilePath))) {
			System.err.println("[Error] Файл не найден: " + inputFilePath);
			return;
		}

		api.getFiles().add(inputFilePath);

		//for (String file : STD_FILES)
			//api.getFiles().add("std/" + file);


		String[] parts = inputFilePath.split("\\.");

		if(command.equals("-l"))
			System.out.println(consoleLog(inputFilePath));

		else if (command.equals("-r")) {
			api.compile();
			api.runClass(parts[0] + "ixc", api.getFiles(), args);

		} else
			System.err.println("[Error] Неизвестная команда: " + command);

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
