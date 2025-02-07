package com.kingmang.ixion.api;


import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.class_utils.CustomClassLoader;
import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.compiler.Scope;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.exceptions.ParserException;
import com.kingmang.ixion.optimization.Optimizer;
import com.kingmang.ixion.parser.Lexer;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.parser.Parser;
import com.kingmang.ixion.parser.impl.LexerImpl;
import com.kingmang.ixion.parser.impl.ParserImpl;
import com.kingmang.ixion.parser.tokens.Token;
import lombok.Getter;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class IxionApi {

	@Getter
	private static final File outputDirectory = new File("out");
	private final List<Path> classpath = null;
	@Getter
	private final List<String> files = new ArrayList<>();


	public void runClass(String className, List<String> files, String[] args) {
		try {
			String[] parts = className.split("\\.");
			for (String file : files) {
				if (file != null) {
					try {
						runClass(Path.of(outputDirectory.toURI()), parts[0], args);
					} catch (Exception _) {}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void runClass(Path outputFolder, String className, String[] args) throws Exception {
		File folder = outputFolder.toFile();
		URLClassLoader classLoader = new URLClassLoader(new URL[]{folder.toURI().toURL()});
		Class<?> clazz = classLoader.loadClass(className);
		Method mainMethod = clazz.getMethod("main", String[].class);
		mainMethod.invoke(null, (Object) args);

		classLoader.close();
	}


	public Node analysis(Path path) throws IOException, ParserException {
		String source = Files.readString(path);

		Lexer lexer = new LexerImpl(source);
		List<Token> lexResult = lexer.tokenize();

		Parser parser = new ParserImpl(lexResult);
		return parser.parse();
	}


	public void compile() {

		Properties optimizations = getOptimizationConfiguration();

		List<Path> paths = files.stream().map(Path::of).toList();

		CustomClassLoader classPathLoader = null;
		try {
			classPathLoader = CustomClassLoader.loadClasspath(classpath);
		} catch (IOException e) {
			error(2, "Failed whilst reading classpath: %s\n%s".formatted(
					classpath.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator)),
					e.getLocalizedMessage()
			));
		}

		CustomClassLoader buildClassLoader = new CustomClassLoader(classPathLoader);

		ArrayList<FileContext> fileContexts = new ArrayList<>();

		for(Path path : paths) {
			try {
				Node program = analysis(path);
				Context context = new Context();
				context.setSource(path.getFileName().toString());
				context.setLoader(buildClassLoader);
				Scope redefinitionResolver = new Scope(context);
				context.setScope(redefinitionResolver);

				program.buildClasses(context);

				Map<String, Class<?>> classMap = new HashMap<>();

				for(Map.Entry<String, ClassWriter> classes : context.getClassWriterMap().entrySet()) {
					byte[] klassRep = classes.getValue().toByteArray();
					Class<?> klass = buildClassLoader.define(classes.getKey().replace('/', '.'), klassRep);

					classMap.put(classes.getKey(), klass);
				}

				fileContexts.add(new FileContext(program, context, classMap, path, optimizations));
			} catch (IOException e) {
				error(2, "Failure reading file '%s': %s", path.toString(), e.getClass().getSimpleName().replace("Exception", ""));
			} catch (ParserException e) {
				error(-1, e.defaultError(path.toString()));
			} catch (IxException e) {
				error(-2, e.defaultError(path.toString()));
			}

		}

		CustomClassLoader preprocessLoader = new CustomClassLoader(classPathLoader);
		for(FileContext fc : fileContexts) {
			try {
				fc.getAst().preprocess(fc.getContext());

				Map<String, Class<?>> classMap = new HashMap<>();

				for(Map.Entry<String, ClassWriter> classes : fc.getContext().getClassWriterMap().entrySet()) {
					byte[] klassRep = classes.getValue().toByteArray();
					Class<?> klass = preprocessLoader.define(classes.getKey().replace('/', '.'), klassRep);

					classMap.put(classes.getKey(), klass);
				}
				fc.setClassMap(classMap);
				fc.getContext().setLoader(preprocessLoader);
			} catch (IxException e) {
				error(-2, e.defaultError(fc.getPath().toString()));
			}
		}

		for(FileContext fc : fileContexts) {
			Scope scope = new Scope(fc);
			fc.getContext().setScope(scope);

			try {
				fc.getAst().visit(fc);
			} catch (IxException e) {
				error(-2, e.defaultError(fc.getPath().toString()));
			}

            String packageDir = fc.getContext().getPackageName().replace('/', File.separatorChar);

			for(Map.Entry<String, Class<?>> classEntry : fc.getClassMap().entrySet()) {
				String baseClassName = classEntry.getKey();
				byte[] klassRep = fc.getContext().getClassWriterMap().get(baseClassName).toByteArray();

				String className = baseClassName + ".class";

				if(className.contains("/")) {
					className = className.substring(className.lastIndexOf('/'));
				}

				Path classFile = Path.of(String.valueOf(outputDirectory), packageDir, className);
				try {
					Files.createDirectories(classFile.getParent());
					Files.write(classFile, klassRep);
				} catch (IOException e) {
					error(3, "Failure writing file '%s': %s", classFile.toString(), e.getClass().getSimpleName().replace("Exception", ""));
				}

			}
		}
	}



	private void error(int code, String format, Object... args) {
		System.err.printf(format + "\n", args);
		System.exit(code);
	}


	private Properties getOptimizationConfiguration() {
		Properties optimizations;
		optimizations = Optimizer.defOptimizations();
		return optimizations;
	}

}
