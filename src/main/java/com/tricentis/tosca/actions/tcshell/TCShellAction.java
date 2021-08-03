package com.tricentis.tosca.actions.tcshell;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.google.common.base.Optional;
import com.neotys.extensions.action.Action;
import com.neotys.extensions.action.ActionParameter;
import com.neotys.extensions.action.engine.ActionEngine;

public final class TCShellAction implements Action{
	private static final String BUNDLE_NAME = "com.tricentis.tosca.actions.tcshell.bundle";
	private static final String DISPLAY_NAME = ResourceBundle.getBundle(BUNDLE_NAME, Locale.getDefault()).getString("displayName");
	private static final String DISPLAY_PATH = ResourceBundle.getBundle(BUNDLE_NAME, Locale.getDefault()).getString("displayPath");
	private static final ImageIcon LOGO_ICON = new ImageIcon(TCShellAction.class.getResource(ResourceBundle.getBundle(BUNDLE_NAME,
			 Locale.getDefault()).getString("iconFile")));
	
	@Override
	public String getType() {
		return "TCShell";
	}

	@Override
	public List<ActionParameter> getDefaultActionParameters() {
		final List<ActionParameter> parameters = new ArrayList<ActionParameter>();
	    parameters.add(new ActionParameter(TCShellActionEngine.WORKSPACE_PARAMETER, "C:\\Tosca_Projects\\Tosca_Workspaces\\PathToYour\\Workspace.tws"));
	    parameters.add(new ActionParameter(TCShellActionEngine.WORKSPACEUSR_PARAMETER, "Admin"));
	    parameters.add(new ActionParameter(TCShellActionEngine.WORKSPACEPWD_PARAMETER, ""));
	    parameters.add(new ActionParameter(TCShellActionEngine.EXECUTABLENODE_PARAMETER, "/Execution/ExecutionLists/Automated/ExecutionList"));
		return parameters;
	}

	@Override
	public Class<? extends ActionEngine> getEngineClass() {
		return TCShellActionEngine.class;
	}

	@Override
	public Icon getIcon() {
		return LOGO_ICON;
	}

	@Override
	public String getDescription() {
		final StringBuilder description = new StringBuilder();
		description.append(
				"Launches a Tosca test on the Load Generator machine. Tosca and its workspace must be available on Load Generator. The given executable node will be continuously executed until the load test is stopped. Resources copied under the 'custom-resources' folder of the project are automatically copied to all Load Generators.\n")
				.append("Use the variable '${NL-CustomResources}' to access the synchronized folder on the Load Generator.\n\n")
				.append("Possible parameters are:\n")
				.append("- ").append(TCShellActionEngine.WORKSPACE_PARAMETER).append(" (Required): Path to the Tosca Commander Workspace (.tws) on your Load Generator machine.\n")
				.append("- ").append(TCShellActionEngine.WORKSPACEUSR_PARAMETER).append(" (Optional): User to open the workspace with.\n")
				.append("- ").append(TCShellActionEngine.WORKSPACEPWD_PARAMETER).append(" (Optional): Password to open the workspace with.\n")
				.append("- ").append(TCShellActionEngine.EXECUTABLENODE_PARAMETER).append(" (Required): The Node in Tosca that should be executed. Can be an ExecutionList or an ExecutionListEntry\n")
				.append("- ").append(TCShellActionEngine.EXECUTION_MODE).append(" (Optional): True (Default) if workspace should be opened in execution only mode and results should not be saved. Set to false if execution results should be saved.\n")
				.append("- ").append(TCShellActionEngine.DATAEXCHANGEAPIURL_PARAMETER).append(" (Optional): DataExchange API address. Default: http://${NL-ControllerIp}:7400/DataExchange/v1/Service.svc/\n")
				.append("- ").append(TCShellActionEngine.DATAEXCHANGEAPIKEY_PARAMETER).append(" (Optional): DataExchange API access key.\n");
		return description.toString();
	}

	@Override
	public String getDisplayName() {
		return DISPLAY_NAME;
	}

	@Override
	public String getDisplayPath() {
		return DISPLAY_PATH;
	}

	@Override
	public Optional<String> getMinimumNeoLoadVersion() {
		return Optional.of("7.9");
	}

	@Override
	public Optional<String> getMaximumNeoLoadVersion() {
		return Optional.absent();
	}
}
