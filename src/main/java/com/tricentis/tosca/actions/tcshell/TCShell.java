package com.tricentis.tosca.actions.tcshell;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;

import com.neotys.extensions.action.engine.SampleResult;


public class TCShell {
	
	public static final String TCSHELL_TCPARAM_ENABLEINTEGRATION = "SendEndUserExperienceToNeoLoad";
	public static final String TCSHELL_TCPARAM_APIHOST = "NeoLoadApiHost"; 
	public static final String TCSHELL_TCPARAM_APIPORT = "NeoLoadApiPort"; 
	public static final String TCSHELL_TCPARAM_APIKEY = "NeoLoadApiKey";
	
	private static final String TCSHELL_SCANNER_DELIMITER = ">|\\?";  // ' > ' or ' ? '
	
	private String workspace;
	private String workspaceUsr;
	private String workspacePwd;
	
	private boolean executionMode;
	private boolean multiuserMode;
	
	private Process process;
	private Scanner scanner;
	private BufferedWriter writer;
	
	private StringBuilder output;
	private boolean outputRecording;

	public TCShell(String workspace, String workspaceUsr, String workspacePwd, boolean executionMode) {
		this.workspace = workspace;
		this.workspaceUsr = workspaceUsr;
		this.workspacePwd = workspacePwd;
		this.executionMode = executionMode;
		this.multiuserMode = !StringUtils.isEmpty(workspaceUsr);
	}

	public void start() throws IOException {
		startNewProcess();
		waitForInput();
	}
	
	public void selectNode(String node) throws IOException {		
        writeLine("jumpToNode " + doubleQuote(node));
        
		if(!executionMode && multiuserMode) {	        
			writeLine("task \"Checkout Tree\"");
		}
	}
	
	public void setConfigurationParameters(String dataExchangeApiHost, int dataExchangeApiPort, String dataExchangeApiKey) throws IOException {
		writeLine("settcparam " + TCSHELL_TCPARAM_ENABLEINTEGRATION + " True");
		writeLine("settcparam " + TCSHELL_TCPARAM_APIHOST + " " + dataExchangeApiHost);
		writeLine("settcparam " + TCSHELL_TCPARAM_APIPORT + " " + dataExchangeApiPort);
		if(dataExchangeApiKey != null)
			writeLine("settcparam " + TCSHELL_TCPARAM_APIKEY + " " + dataExchangeApiKey);
	}
	
	public void run() throws IOException {
		writeLine("task run");
	}
	
	public void exit() throws IOException {
		if(!executionMode) {
			writeLine("Save");
			
			if(multiuserMode) {
				writeLine("CheckinAll");	
			}
		}

		writeLine("exit");
		
		//Do you really want to exit TCShell (yes/no) ?
		writeLine("yes");
		
		if(executionMode) {
			//Save all changes to project (yes/no) > 
			writeLine("no");
		}
		
		writer.close();
	}

	public String commandToString() {
		List<String> command = getCommand(true);
		StringBuilder commandString = new StringBuilder();
		for (String element : command) {
			commandString.append(element);
			commandString.append(" ");
		}
		return commandString.toString();
	}
	
	private List<String> getCommand(boolean hidePassword) {
		List<String> cmd = new ArrayList<>();
		cmd.add("cmd");
		cmd.add("/c");
		cmd.add("tcshell");
		cmd.add("-workspace");
		cmd.add(doubleQuote(workspace));
		if(multiuserMode) {
			cmd.add("-login");
			cmd.add(doubleQuote(workspaceUsr));
			if(hidePassword) {
				cmd.add(doubleQuote("****"));
			} else {
				cmd.add(doubleQuote(workspacePwd != null ? workspacePwd : ""));
			}
			
		}
		if(executionMode) {
			cmd.add("-executionmode");
		}
		
		return cmd;
	}
	

	private void startNewProcess() throws IOException {

		ProcessBuilder processBuilder = null;
		List<String> cmd = getCommand(false);
		
		processBuilder = new ProcessBuilder(cmd);
		process = processBuilder.start();
		
		InputStream stdout = process.getInputStream();
		OutputStream stdin = process.getOutputStream();
		
		scanner = new Scanner(stdout);
		scanner.useDelimiter(TCSHELL_SCANNER_DELIMITER);
		
		writer = new BufferedWriter(new OutputStreamWriter(stdin));
	}


	private void writeLine(String text) throws IOException {
		writeLine(text, true);
	}
	
	private void writeLine(String text, boolean waitForInput) throws IOException {
		
		if(outputRecording) {
			output.append(text);
			output.append("\r\n");
		}
		
		writer.write(text);
		writer.newLine();
		writer.flush();
		
		if(waitForInput) {
			waitForInput();
		}
	}
	
	private void waitForInput() throws IOException {
		String text = scanner.next().trim();
		
		if(outputRecording) {
			output.append(text);
			output.append(" > ");
		}
		
	}
	
	public void startOutputRecording() {
		output = new StringBuilder();
		outputRecording = true;
	}
	
	public String stopOutputRecording() {
		outputRecording = false;
		return output.toString();
	}
	
	private static String doubleQuote(String text) {
		return "\"" + text + "\"";
	}

}
