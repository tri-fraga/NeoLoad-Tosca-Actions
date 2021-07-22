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
				"Launches a Tosca test on the Load Generator machine. Tosca and its workspace must be available on Load Generator. Resources copied under the 'custom-resources' folder of the project are automatically copied to all Load Generators.\n")
				.append("Use the variable '${NL-CustomResources}' to access the synchronized folder on the Load Generator.\n\n")
				.append("Possible parameters are:\n")
				.append("- ").append(TCShellActionEngine.WORKSPACE_PARAMETER).append(" (Required): Path to the Tosca Commander Workspace (.tws) on your Load Generator.\n")
				.append("- ").append(TCShellActionEngine.WORKSPACEUSR_PARAMETER).append(" (Optional): User to open the workspace with.\n")
				.append("- ").append(TCShellActionEngine.WORKSPACEPWD_PARAMETER).append(" (Optional): Password to open the workspace with.\n")
				.append("- ").append(TCShellActionEngine.EXECUTABLENODE_PARAMETER).append(" (Optional): The Node in Tosca that should be executed. Can be an ExecutionList or an ExecutionListEntry. Not used if script is set.\n")
				.append("- ").append(TCShellActionEngine.DATAEXCHANGEAPIURL_PARAMETER).append(" (Optional): DataExchange API address. Default is: http://${NL-ControllerIp}:7400/DataExchange/v1/Service.svc/. Not used if script is set.\n")
				.append("- ").append(TCShellActionEngine.DATAEXCHANGEAPIKEY_PARAMETER).append(" (Optional): DataExchange API access key. Not used if script is set.\n")
				.append("- ").append(TCShellActionEngine.TCSHELLSCRIPT_PARAMETER).append(" (Optional): Path to the TCShell script file that should be used, could contain advanced steps like checkout and saving of results.\n")
				.append("- ").append(TCShellActionEngine.ARG_PARAMETER).append(" (Optional): Additional TCShell start argument number X. If the argument uses values create a separate argument for the name and each value.");
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
		return Optional.of("5.1");
	}

	@Override
	public Optional<String> getMaximumNeoLoadVersion() {
		return Optional.absent();
	}
}
