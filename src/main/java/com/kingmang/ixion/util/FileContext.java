package com.kingmang.ixion.util;

import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.compiler.Context;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;


public class FileContext {
	@Getter
    private final Node ast;
	@Getter
    private final Context context;
	@Setter
    @Getter
    private Map<String, Class<?>> classMap;
	@Getter
    private final Path path;
	private final Properties optimizations;

	public FileContext(Node ast, Context context, Map<String, Class<?>> classMap, Path path, Properties optimizations) {
		this.ast = ast;
		this.context = context;
		this.classMap = classMap;
		this.path = path;
		this.optimizations = optimizations;
	}

    public Class<?> getCurrentClass() {
		return classMap.get(context.getCurrentClass());
	}

	public boolean shouldOptimize(String optimization) {
		return (boolean) optimizations.getOrDefault(optimization, false);
	}
}
