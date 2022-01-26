package com.faforever.client;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

@SpringBootApplication(exclude = {
    JmxAutoConfiguration.class,
    SecurityAutoConfiguration.class,
})
public class Main {
	public static void main(String[] args) throws NoSuchAlgorithmException, KeyManagementException {
		FafClientApplication.applicationMain(args);
	}
}
