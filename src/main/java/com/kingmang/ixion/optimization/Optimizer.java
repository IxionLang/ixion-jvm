package com.kingmang.ixion.optimization;

import java.util.Properties;

public class Optimizer {
	public static Properties defOptimizations(){
		Properties properties = new Properties();
		properties.put("constant.string.concat", true);
		return properties;
	}


}
