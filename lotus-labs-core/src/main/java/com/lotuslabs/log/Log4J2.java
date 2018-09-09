package com.lotuslabs.log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;

public class Log4J2 {

	private Log4J2() throws IOException {
		String log4jConfigFile = System.getProperty("user.dir") + File.separator + "log4j2.xml";
		ConfigurationSource source = new ConfigurationSource(new FileInputStream(log4jConfigFile));
		Configurator.initialize(null, source);
	}

	public static void init() {
		try {
			new Log4J2();
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}
}
