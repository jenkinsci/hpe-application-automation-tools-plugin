package com.ge.application.automation.steps;

import java.io.PrintStream;

import com.hp.application.automation.tools.sse.sdk.Logger;

/**
 * Simple implementation of Logger
 */
public class PrintStreamLogger implements Logger {
	
	private PrintStream printStream;

	public PrintStreamLogger(PrintStream printStream) {
		this.printStream = printStream;
	}
	
	@Override
	public void log(String message) {
		printStream.println(message);
	}
	
}
