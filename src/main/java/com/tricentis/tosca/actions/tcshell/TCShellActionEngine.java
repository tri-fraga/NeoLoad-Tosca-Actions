package com.tricentis.tosca.actions.tcshell;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.neotys.extensions.action.ActionParameter;
import com.neotys.extensions.action.engine.ActionEngine;
import com.neotys.extensions.action.engine.Context;
import com.neotys.extensions.action.engine.SampleResult;

public final class TCShellActionEngine implements ActionEngine {
	
	public static final String COMMAND = "TCShell";
	public static final String WORKSPACE_PARAMETER = "workspacePath";
	public static final String WORKSPACEUSR_PARAMETER = "workspaceUser";
	public static final String WORKSPACEPWD_PARAMETER = "workspacePassword";
	public static final String EXECUTION_MODE = "executionMode";
	public static final String EXECUTABLENODE_PARAMETER = "executableNode";
	public static final String DATAEXCHANGEAPIURL_PARAMETER = "dataExchangeAPIURL";
	public static final String DATAEXCHANGEAPIKEY_PARAMETER = "dataExchangeAPIKey";
	public static final String TCSHELLSCRIPT_PARAMETER = "script";
	public static final String ARG_PARAMETER = "arg";
	public static final String TCSHELL_SCANNER_DELIMITER = ">|\\?";  // ' > ' or ' ? '
	
	private static final String STATUS_CODE_PARAMETER_ERROR = "NL-TCSHELL-TEST-ACTION-01";
	private static final String STATUS_CODE_PROCESS_ERROR = "NL-TCSHELL-TEST-ACTION-02";
	private static final String STATUS_CODE_TCSHELL_ERROR = "NL-TCSHELL-TEST-ACTION-03";
	
	protected String tcShellScript;
	protected String workspace;
	protected String workspaceUsr;
	protected String workspacePwd;
	protected String executableNode;
	protected String dataExchangeApiUrl;
	protected String dataExchangeApiKey;
	protected String dataExchangeApiHost;
	protected int dataExchangeApiPort;
	protected boolean executionMode;
	protected boolean interactiveMode;
	protected boolean multiuserMode;
	protected boolean stopping;
	
	private Process process;
	private Scanner scanner;
	private BufferedWriter writer;
	private SampleResult result;
	
	@Override
	public SampleResult execute(Context context, List<ActionParameter> parameters) {
		dataExchangeApiHost = "localhost";//context.getVariableManager().getValue("NL-ControllerIp");
		dataExchangeApiPort = 7400;
		executionMode = true;
		interactiveMode = true;
		multiuserMode = false;
		stopping = false;
		result = new SampleResult();
		
		try {
			getValuesFromParameters(parameters);
		} catch (Exception e) {
			result.setError(true);
			result.setStatusCode(STATUS_CODE_PARAMETER_ERROR);
			result.setResponseContent(e.toString());
			return result;
		}

		try {
			startNewTcShellProcess(context);
		} catch (IOException e) {
			result.setError(true);
			result.setStatusCode(STATUS_CODE_PROCESS_ERROR);
			result.setResponseContent(e.toString());
			return result;
		}
		
		result.sampleStart();
		
		try {
			if(interactiveMode) {
				runTcShellInteractive(); 
			} else {
				readScriptOutput(); // read TCShell script output
			}
		} catch(IOException e) {
			result.setError(true);
			result.setStatusCode(STATUS_CODE_TCSHELL_ERROR);
			addToResponseContent(e.toString());
			result.sampleEnd();
			return result;
		}
		
		result.sampleEnd();
		result.setStatusCode("OK");

		if (context != null) {
			context.getLogger().debug("TCShell execution finished with status code");
		}

		return result;
	}

	
	@Override
	public void stopExecute() {
		addToResponseContent("STOPPING");
		stopping = true;
		//TODO: Stop running execution maybe? But exit gracefully
	}
	
	protected void getValuesFromParameters(List<ActionParameter> parameters) throws Exception {
		for (ActionParameter parameter : parameters) {
			String parameterName = parameter.getName();
			if (parameterName.equals(TCSHELLSCRIPT_PARAMETER)) {
				tcShellScript = parameter.getValue();
				interactiveMode = false;
			} else if(parameterName.equals(WORKSPACE_PARAMETER)) {
				workspace = parameter.getValue();
			} else if (parameterName.equals(WORKSPACEUSR_PARAMETER)) {
				workspaceUsr = parameter.getValue();
				multiuserMode = true;
			} else if (parameterName.equals(WORKSPACEPWD_PARAMETER)) {
				workspacePwd = parameter.getValue();
			} else if (parameterName.equals(EXECUTABLENODE_PARAMETER)) {
				executableNode = parameter.getValue();
			} else if (parameterName.equals(DATAEXCHANGEAPIURL_PARAMETER)) {
				dataExchangeApiUrl = parameter.getValue();
				java.net.URL dataUrl = new java.net.URL(dataExchangeApiUrl);
				dataExchangeApiHost = dataUrl.getHost();
				dataExchangeApiPort = dataUrl.getPort();
			} else if (parameterName.equals(DATAEXCHANGEAPIKEY_PARAMETER)) {
				dataExchangeApiKey = parameter.getValue();
			} else if (parameterName.equals(EXECUTION_MODE)) {
				executionMode = Boolean.parseBoolean(parameter.getValue());
			}
		}
		
		if(executableNode.isBlank()) {
			throw new Exception("Required parameter " + EXECUTABLENODE_PARAMETER + " is missing");
		}
	}
	
	protected void startNewTcShellProcess(Context context) throws IOException {
		
		List<String> cmd = new ArrayList<>();
		cmd.add("cmd");
		cmd.add("/c");
		cmd.add("tcshell");
		cmd.add("-workspace");
		cmd.add(wrapQuotes(workspace));
		if(multiuserMode) {
			cmd.add("-login");
			cmd.add(wrapQuotes(workspaceUsr));
			cmd.add(wrapQuotes(workspacePwd != null ? workspacePwd : ""));
		}
		if(executionMode) {
			cmd.add("-executionmode");
		}
		if(!interactiveMode) {
			cmd.add(wrapQuotes(tcShellScript));
		}
		
		ProcessBuilder processBuilder = null;
		
		processBuilder = new ProcessBuilder(cmd);
		process = processBuilder.start();
		
		if(context != null) {
			context.getLogger().debug("TCShell execution:" + commandToString(processBuilder.command(), " "));
		}

        InputStream stdout = process.getInputStream();
        scanner = new Scanner(stdout);
        
        if(interactiveMode) {
        	scanner.useDelimiter(TCSHELL_SCANNER_DELIMITER);
        	
        	OutputStream stdin = process.getOutputStream();
        	writer = new BufferedWriter(new OutputStreamWriter(stdin));
        }
	}

	protected void runTcShellInteractive() throws IOException {
		waitForInput();
        
        writeLine("jumpToNode " + executableNode);
        
        if(!executionMode && multiuserMode) {	        
        	writeLine("task \"Checkout Tree\"");
        }
        
        // Overwrite the tcparams if set
        if(dataExchangeApiUrl != null) {
        	writeLine("settcparam UseNeoLoadDataExchangeApi True");
	        writeLine("settcparam NeoLoadDataExchangeApiHost " + dataExchangeApiHost);
	        writeLine("settcparam NeoLoadDataExchangeApiPort " + dataExchangeApiPort);
	        if(dataExchangeApiKey != null)
	        	writeLine("settcparam NeoLoadDataExchangeApiKey " + dataExchangeApiKey);
        }

        //TODO: iterate via actions instead?
        while(!stopping) {
        	writeLine("task Run");
        }

        if(!executionMode) {
	        writeLine("Save");
	        
	        if(multiuserMode) {
	        	writeLine("CheckinAll");	
	        }
        }
        
        stopTcShellInteractive();
	}
	
	protected void stopTcShellInteractive() throws IOException {
		writeLine("exit");
        
        //Do you really want to exit TCShell (yes/no) ?
        writeLine("yes");
        
        if(executionMode) {
        	//Save all changes to project (yes/no) > 
            writeLine("no");
        }

        writer.close();
	}
	
	protected void readScriptOutput() {
		while(scanner.hasNextLine()) {
			String next = scanner.nextLine();
			addToResponseContent(next);
		}
	}

	protected void writeLine(String text) throws IOException {
		writeLine(text, true);
	}
	
	protected void writeLine(String text, boolean waitForInput) throws IOException {
		addToResponseContent(text);
		
		writer.write(text);
        writer.newLine();
        writer.flush();
        
        if(waitForInput) {
        	waitForInput();
        }
	}
	
	protected void waitForInput() throws IOException {
		String next = scanner.next().trim() + " > ";
		addToResponseContent(next, false);
	}
	
	protected void addToResponseContent(String line) {
		addToResponseContent(line, true);
	}
	
	protected void addToResponseContent(String line, boolean addNewLine) {
		StringBuilder sb = new StringBuilder();
		String responseContent = result.getResponseContent();
		sb.append(responseContent);	
		sb.append(line);
		if(addNewLine) {
			sb.append("\r\n");
		}

		result.setResponseContent(sb.toString());
	}
	
	public static String wrapQuotes(String text) {
		return "\"" + text + "\"";
	}
	
	private static String commandToString(List<String> command, String separator) {
		StringBuilder commandString = new StringBuilder();
		for (String element : command) {
			commandString.append(element);
			commandString.append(separator);
		}
		return commandString.toString();
	}
}
