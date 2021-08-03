# NeoLoad Tosca Actions
Advanced NeoLoad Actions to trigger the End User Experience intetegration in Tricentis Tosca

## Measure the end user experience in NeoLoad using Tricentis Tosca 

This integration allows Tosca to communicate with NeoLoads DataExchangeApi to send Tosca's TestStep and TestCase duration timings to NeoLoad during execution when the analysis of the [End User Experience](https://www.neotys.com/blog/why-end-user-experience-is-important-2/) (EUX) is required.

## Using the Advanced TCShell Action

This approach allows you to quickly open an existing repository and continuously triggering the EUX test until the execution is stopped.
1. To install the custom action copy the `tricentis-advanced-action-tosca-x.x.x.jar` to `<controller-install-dir>/extlib`
2. Create separate EUX User Path in Neoload and add an the new `Tosca TCShell` Action to it which will trigger the execution of an Tosca ExecutionList or  ExecutionEntry <p align="center"><img src="/screenshots/Advanced-TCShell-Action.png" alt="Advanced TCShell Action" /></p>
4. Trigger the load test in combination with your EUX User Path in NeoLoad. Only one instance of Tosca should be run per LoadGenerator.

## Using the Executable Test Script Action

The approach allows you to create your own TCShell script which will be used for the EUX test. The disadvantage is that the workspace will be opened and closed with every iteration in NeoLoad.

1. In Tosca set the following [test configuration parameters](https://support.tricentis.com/community/manuals_detail.do?lang=en&url=tosca_commander/tcp_creation.htm) on ExecutionLists or ExecutionEntries which should be executed as part of your EUX test

	Name | Value | Description
	------------ | ------------- | -------------
	SendEndUserExperienceToNeoLoad | True/False | Whether the NeoLoad DataExchangeApi should be used
	NeoLoadApiHost |  Default: localhost | (Optional) The hostname of the NeoLoad DataExchangeApi
	NeoLoadApiPort |  Default: 7400 | (Optional) The port of the NeoLoad DataExchangeApi
	NeoLoadApiKey |  Default: empty | (Optional) E.g. abcb6dcd-ea95-4a6a-9c64-80ff55ff778d

	Alternatively this can also be set as part of your TCShell script as shown in [examples/RunToscaExecution.tcs](./examples/RunToscaExecution.tcs)
	
	_Hint: Set the repetition property on the ExecutionEntry to let one execution run repeatedly. This will increase the TPS as the workspace does not have to be reopened by the UserPath constantly._	

2. Prepare your Tosca execution environment to allow executions via the command line using a [TCShell Script](https://support.tricentis.com/community/manuals_detail.do?lang=en&url=tosca_commander/script_mode.htm) (for local execution when Tosca is installed on the same machine as the NeoLoad LoadGenerator)\
    Example script: [examples/RunToscaExecution.tcs](https://github.com/Neotys-Labs/Tricentis-Tosca/raw/master/examples/RunToscaExecution.tcs)

4. Create separate EUX User Path in Neoload and add an [Executable Test Script](https://www.neotys.com/documents/doc/neoload/latest/en/html/#8677.htm) Action to it which will trigger the execution of an Tosca Execution List or Entry using the `TCShell.exe` Example: \
	`TCShell -executionmode -workspace "C:\Tosca_Projects\Tosca_Workspaces\PathToYour\Workspace.tws" "${NL-CustomResources}\RunToscaExecution.tcs"`
  <p align="center"><img src="https://github.com/Neotys-Labs/Tricentis-Tosca/raw/master/screenshots/Tosca-EUX-NeoLoad.png" alt="Tosca EUX User Path" /></p>

5. Trigger the load test in combination with your EUX User Path in NeoLoad. Only one instance of Tosca should be run per LoadGenerator.

## More Information
	
_Hint: It is recommended to set the Population Parameter Stop Policy to Indeterminate. This will allow last iteration to close Tosca workspace without locking it._

Aditionally to the TestStep and TestCase durations timings the integration will collect browser performance timings similar to the [NeoLoad Selenium EUX integration](https://www.neotys.com/documents/doc/neoload/latest/en/html/#23676.htm) when automating web applications.

<p align="center"><img src="https://github.com/Neotys-Labs/Tricentis-Tosca/raw/master/screenshots/Tosca-EUX-NeoLoad-Metrics.png" alt="Tosca EUX Performance Metrics" /></p>