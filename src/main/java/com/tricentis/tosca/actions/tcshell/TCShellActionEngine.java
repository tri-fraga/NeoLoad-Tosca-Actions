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
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.neotys.extensions.action.ActionParameter;
import com.neotys.extensions.action.engine.ActionEngine;
import com.neotys.extensions.action.engine.Context;
import com.neotys.extensions.action.engine.RuntimeMode;
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
	public static final String TCSHELL_SCANNER_DELIMITER = ">|\\?";  // ' > ' or ' ? '
	
	private static final String STATUS_CODE_PARAMETER_ERROR = "NL-TCSHELL-TEST-ACTION-01";
	private static final String STATUS_CODE_PROCESS_ERROR = "NL-TCSHELL-TEST-ACTION-02";
	private static final String STATUS_CODE_TCSHELL_ERROR = "NL-TCSHELL-TEST-ACTION-03";
	private static final String STATUS_CODE_OK = "NL-TCSHELL-OK";
	
	private String workspace;
	private String workspaceUsr;
	private String workspacePwd;
	private String executableNode;
	private String dataExchangeApiUrl;
	private String dataExchangeApiKey;
	private String dataExchangeApiHost;
	private int dataExchangeApiPort;
	private boolean executionMode;
	
	private SampleResult result;
	private boolean stopping;
	
	@Override
	public SampleResult execute(Context context, List<ActionParameter> parameters) {	
		
		result = new SampleResult();

		try {
			getValuesFromParameters(context, parameters);
		} catch (Exception e) {
			result.setError(true);
			result.setStatusCode(STATUS_CODE_PARAMETER_ERROR);
			result.setResponseContent(e.toString());
			return result;
		}
		
		boolean checkVuMode = context.getRuntimeMode() == RuntimeMode.CHECK_VU;
		boolean isActionContainer = context.getCurrentVirtualUser().getCurrentStep() == "ACTIONS";
		
		TCShell tcshellobj = new TCShell(workspace, workspaceUsr, workspacePwd, executionMode);
		tcshellobj.startOutputRecording();
		
		result.setRequestContent(tcshellobj.commandToString());
		result.sampleStart();
		
		try {
			tcshellobj.start();
		} catch(IOException e) {
			result.setError(true);
			result.setStatusCode(STATUS_CODE_PROCESS_ERROR);
			result.setResponseContent(e.toString());
			return result;
		}

		try {
			tcshellobj.selectNode(executableNode);
			tcshellobj.setConfigurationParameters(dataExchangeApiHost, dataExchangeApiPort, dataExchangeApiKey);
			
			// Keep running until loadtest is stopping
			// Run only once if checking UserPath or executing outside action container
			do {
				tcshellobj.run();
			} while(!stopping && !checkVuMode && isActionContainer);
			
			tcshellobj.exit();
			
		} catch (IOException e) {
			result.setError(true);
			result.setStatusCode(STATUS_CODE_TCSHELL_ERROR);
			result.setResponseContent(e.toString());
			return result;
		}
		
		result.sampleEnd();
		result.setStatusCode(STATUS_CODE_OK);
		result.setResponseContent(tcshellobj.stopOutputRecording());

		return result;

	}
	
	private void getValuesFromParameters(Context context, List<ActionParameter> parameters) throws Exception {
		
		dataExchangeApiHost = context.getControllerIp();
		dataExchangeApiPort = 7400;
		executionMode = true;
		
		for (ActionParameter parameter : parameters) {
			String parameterName = parameter.getName();
			if(parameterName.equals(WORKSPACE_PARAMETER)) {
				workspace = parameter.getValue();
			} else if (parameterName.equals(WORKSPACEUSR_PARAMETER)) {
				workspaceUsr = parameter.getValue();
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
		
		if(StringUtils.isEmpty(workspace)) {
			throw new Exception("Required parameter '" + WORKSPACE_PARAMETER + "' is missing");
		}
		
		if(!workspace.toLowerCase().endsWith(".tws")) {
			
			 throw new Exception("Value '" + workspace + "' given for parameter '" + WORKSPACE_PARAMETER + "' is not a valid Tosca workspace. (must end with .tws)");
		}

		if(StringUtils.isEmpty(executableNode) ) {
			throw new Exception("Required parameter '" + EXECUTABLENODE_PARAMETER + "' is missing");
		}
	}
	
	@Override
	public void stopExecute() {
		stopping = true;
	}
}
