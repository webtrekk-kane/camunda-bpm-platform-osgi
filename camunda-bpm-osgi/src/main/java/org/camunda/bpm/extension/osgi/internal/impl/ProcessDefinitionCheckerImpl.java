package org.camunda.bpm.extension.osgi.internal.impl;

import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.camunda.bpm.extension.osgi.internal.ProcessDefinitionDeployer;
import org.camunda.bpm.extension.osgi.internal.ProcessDefinitionParser;
import org.camunda.bpm.extension.osgi.internal.ProcessDefintionChecker;
import org.osgi.framework.Bundle;

/**
 * Implementation of the {@link ProcessDefintionChecker} that tries to deploy
 * processes using a {@link ProcessDefinitionDeployer}.
 * 
 * @author Ronny Bräunlich
 * 
 */
public class ProcessDefinitionCheckerImpl implements ProcessDefintionChecker {

	private static final Logger LOGGER = Logger
			.getLogger(ProcessDefinitionCheckerImpl.class.getName());

	private volatile ProcessDefinitionDeployer deployer;

	public ProcessDefinitionCheckerImpl() {}
	
	public ProcessDefinitionCheckerImpl(ProcessDefinitionDeployer deployer) {
		this.deployer = deployer;
	}

	@Override
	public void checkBundle(Bundle bundle) {
		List<URL> pathList = ProcessDefinitionParser.scanForProcesses(bundle);
		if (!pathList.isEmpty()) {
			deployer.deployProcessDefinitions(bundle.getSymbolicName(),
					pathList);
		} else {
			LOGGER.log(Level.FINE, "No process found in bundle {}",
					bundle.getSymbolicName());
		}
	}

	public void bundleRemoved(Bundle bundle) {
		// so far do nothing
	}

}
