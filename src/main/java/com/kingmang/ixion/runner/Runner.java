package com.kingmang.ixion.runner;

import com.kingmang.ixion.api.IxionApi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


public class Runner {


	public static void main(String[] args) throws IOException {
		IxionApi api = new IxionApi();
		if(!IxionApi.getOutputDirectory().exists())
			IxionApi.getOutputDirectory().mkdir();

		Files.walk(Path.of(IxionApi.getOutputDirectory().toURI())).filter(Files::isRegularFile).forEach(p -> {
			try {
				Files.delete(p);
			} catch (IOException _) {}
		});
		api.getFiles().add(args[0]);
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
