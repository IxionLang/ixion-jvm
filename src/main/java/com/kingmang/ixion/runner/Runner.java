package com.kingmang.ixion.runner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.kingmang.ixion.api.IxionApi;
import lombok.SneakyThrows;


public class Runner {

	static String[] std_files = {
		"string.ix",
		"unit_test.ix"
	};
	@SneakyThrows
    public static void main(String[] args) {
		
		IxionApi api = new IxionApi();
		if(!IxionApi.getOutputDirectory().exists())
			IxionApi.getOutputDirectory().mkdir();

		Files.walk(Path.of(IxionApi.getOutputDirectory().toURI())).filter(Files::isRegularFile).forEach(p -> {
			try {
				Files.delete(p);
			} catch (IOException _) {}
		});

		if(args.length == 0){
			System.err.println("[Error] \n Please enter a file name. Example: ixion test.ix");
			//api.getFiles().add("test.ix");
			//api.compile();
			//api.runClass("testixc", api.getFiles(), args);
		}
		api.getFiles().add(args[0]);

		for(String std_file : std_files)
			api.getFiles().add("std/" + std_file);

		String[] parts = args[0].split("\\.");
		api.compile();
		for (String arg : args){
			if(arg.equals("-l")) System.out.println(consoleLog(args[0]));
			if(arg.equals("-cr")) api.runClass(parts[0] + "ixc", api.getFiles(), args);
		}

	}

	static String consoleLog(String filename){
		return
				"""
				[Ixion version : %s]
				[Shuttle version : %s]
				[Filename: %s]
				
				""".
				formatted(
						RunnerInfo.VERSION,
						RunnerInfo.SHUTTLE_MESSAGE,
						filename
						);
	}

}
