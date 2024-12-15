package com.kingmang.ixion.class_utils;

import com.kingmang.ixion.util.Unthrow;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class CustomClassLoader extends URLClassLoader {
	public CustomClassLoader(List<Path> classpath, ClassLoader parent) {
		this(classpath == null ? new URL[0] : classpath.stream().filter(p -> !Files.isDirectory(p)).map(p -> Unthrow.wrap(() -> p.toFile().toURI().toURL())).toArray(URL[]::new), parent);
	}

	public CustomClassLoader(ClassLoader parent) {
		this(new URL[0], parent);
	}

	protected CustomClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
		try {
			File directory = new File("lib/");
			if (directory.exists() && directory.isDirectory()) {
				for (File library : Objects.requireNonNull(directory.listFiles())) {
					this.addURL(library.toURI().toURL());
				}
			}
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	public Class<?> define(String name, byte[] b) {
		return defineClass(name, b, 0, b.length);
	}

	public static CustomClassLoader loadClasspath(List<Path> classpath) throws IOException {
		CustomClassLoader loader = new CustomClassLoader(classpath, ClassLoader.getSystemClassLoader());

		if(classpath == null) return loader;

		for(Path p : classpath) {
			if(Files.isDirectory(p)) {
				try(Stream<Path> stream = Files.walk(p, Integer.MAX_VALUE)) {

					List<Path> classes = stream
							.map(p::relativize)
							.filter(f -> f.toString().endsWith(".class"))
							.toList();

					for(Path klass : classes) {
						StringBuilder className = new StringBuilder();

						for(int i = 0; i < klass.getNameCount(); i++) {
							className.append(klass.getName(i)).append('.');
						}

						className.delete(className.length() - 7, className.length());

						Path abs = p.resolve(klass);

						byte[] classData = Files.readAllBytes(abs);

						loader.define(className.toString(), classData);
					}
				}
			}
		}
		return loader;
	}

}
