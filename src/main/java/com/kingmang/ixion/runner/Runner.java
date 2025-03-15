package com.kingmang.ixion.runner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.kingmang.ixion.api.IxionApi;


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
		if(args.length == 0){
			api.getFiles().add("test.ix");
			api.compile();
			api.runClass("testixc", api.getFiles(), args);	
		}
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
