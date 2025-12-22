package com.faforever.client;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;

@SpringBootApplication(exclude = {
		JmxAutoConfiguration.class,
})
public class Main {
	public static void main(String[] args) {
		FafClientApplication.applicationMain(args);
	}
}
