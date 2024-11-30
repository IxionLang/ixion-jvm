package com.kingmang.ixion.class_utils;

import org.objectweb.asm.ClassWriter;

public class CustomClassWriter extends ClassWriter {

	private final ClassLoader loader;

	public CustomClassWriter(int flags, ClassLoader loader) {
		super(flags);
		this.loader = loader == null ? ClassLoader.getSystemClassLoader() : loader;
	}

	@Override
	protected ClassLoader getClassLoader() {
		return loader;
	}
}
