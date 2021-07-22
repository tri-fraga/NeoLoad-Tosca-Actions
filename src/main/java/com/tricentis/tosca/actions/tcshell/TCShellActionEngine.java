package com.tricentis.tosca.actions.tcshell;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.neotys.extensions.action.ActionParameter;
import com.neotys.extensions.action.cmd.AbstractCmdActionEngine;
import com.neotys.extensions.action.engine.Context;
import com.neotys.extensions.action.engine.SampleResult;

public final class TCShellActionEngine extends AbstractCmdActionEngine {
	
	public static final String COMMAND = "TCShell";
	public static final String WORKSPACE_PARAMETER = "workspacePath";
	public static final String WORKSPACEUSR_PARAMETER = "workspaceUser";
	public static final String WORKSPACEPWD_PARAMETER = "workspacePassword";
	public static final String EXECUTABLENODE_PARAMETER = "executableNode";
	public static final String DATAEXCHANGEAPIURL_PARAMETER = "dataExchangeAPIURL";
	public static final String DATAEXCHANGEAPIKEY_PARAMETER = "dataExchangeAPIKey";
	public static final String TCSHELLSCRIPT_PARAMETER = "script";
	
	public SampleResult execute(Context context, List<ActionParameter> parameters) {
		List<ActionParameter> tcShellParams = parseTCShellParameters(context, parameters);	
		return super.execute(context, tcShellParams);
	}

	private List<ActionParameter> parseTCShellParameters(Context context, List<ActionParameter> parameters) {
		
		List<ActionParameter> tcShellParams = new ArrayList<ActionParameter>();
		String tcShellScriptParameterValue = null;
		String workspaceUsrParameterValue = null;
		String workspacePwdParameterValue = null;
		int argCount = 1;
		
		tcShellParams.add(new ActionParameter(COMMAND, COMMAND));
		
		// Map the TCShell arguments to AbstractCmdAction arguments
		for (ActionParameter parameter : parameters) {
			if (parameter.getName().equals(TCSHELLSCRIPT_PARAMETER)) {
				tcShellScriptParameterValue = parameter.getValue();
			} else if(parameter.getName().equals(WORKSPACE_PARAMETER)) {
				tcShellParams.add(new ActionParameter("arg" + argCount++, "-workspace"));
				tcShellParams.add(new ActionParameter("arg" + argCount++, "\"" + parameter.getValue() + "\""));
			} else if (parameter.getName().startsWith(WORKSPACEUSR_PARAMETER)) {
				workspaceUsrParameterValue = parameter.getValue();
			} else if (parameter.getName().startsWith(WORKSPACEPWD_PARAMETER)) {
				workspacePwdParameterValue = parameter.getValue();
			} else if (parameter.getName().startsWith(ARG_PARAMETER)) {
				tcShellParams.add(new ActionParameter("arg" + argCount++, parameter.getValue()));
			}
		}
		
		if(workspaceUsrParameterValue != null) {
			if(workspacePwdParameterValue == null) 
				workspacePwdParameterValue =  "";
			
			tcShellParams.add(new ActionParameter("arg" + argCount++, "-login"));
			tcShellParams.add(new ActionParameter("arg" + argCount++, "\"" + workspaceUsrParameterValue + "\""));
			tcShellParams.add(new ActionParameter("arg" + argCount++, "\"" + workspacePwdParameterValue + "\""));
		}
		
		if(tcShellScriptParameterValue == null) {
			tcShellScriptParameterValue = getGeneratedShellScript(context, parameters);
		}
		
		// TCShell script path must be the last argument
		tcShellParams.add(new ActionParameter("arg"  + argCount, "\"" + tcShellScriptParameterValue + "\""));
		
		return tcShellParams;
	}
	
	protected String getGeneratedShellScript(Context context, List<ActionParameter> parameters) {
		String node = "";
		String apiHost = context.getVariableManager().getValue("NL-ControllerIp");
		int apiPort = 7400;
		String apiKey = "";
		
		for (ActionParameter parameter : parameters) {
			if(parameter.getName().equals(EXECUTABLENODE_PARAMETER)) {
				node = parameter.getValue();
			} else if (parameter.getName().equals(DATAEXCHANGEAPIURL_PARAMETER)) {
				try {
					java.net.URL apiUrl = new java.net.URL(parameter.getValue());
					apiHost = apiUrl.getHost();
					apiPort = apiUrl.getPort();
				} catch (MalformedURLException e) {}
			} else if (parameter.getName().equals(DATAEXCHANGEAPIKEY_PARAMETER)) {
				apiKey = parameter.getValue();
			}
		}
		
		final StringBuilder script = new StringBuilder();
		
		script.append("jumptonode \"").append(node).append("\"\n");
		script.append("settcparam NeoLoadDataExchangeApiHost ").append(apiHost).append("\n");
		script.append("settcparam NeoLoadDataExchangeApiPort ").append(apiPort).append("\n");
		if(!apiKey.isEmpty())
			script.append("settcparam NeoLoadDataExchangeApiKey ").append(apiKey).append("\n");
		script.append("settcparam UseNeoLoadDataExchangeApi True\n");
		script.append("task run\n");
		
		String generatedScript = context.getVariableManager().getValue("NL-CustomResources") + 
				"\\tcshell-script-generated.tcs";
		
		try {
			Files.write( Paths.get(generatedScript), script.toString().getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return generatedScript;
	}
	
	@Override
	protected String getStatusCodePrefix() {
		return "NL-TCSHELL-TEST-ACTION";
	}

	@Override
	protected String getCommandParameterName() {
		return COMMAND;
	}

	@Override
	protected String getActionName() {
		return "TCShell";
	}

	@Override
	protected String getCommandNotSetResponse() {
		return "\"" + getCommandParameterName() + "\" not found, please make sure \"" + 
						getCommandParameterName() + "\" is included in your 'path' environment variable.";

	}
	
}
