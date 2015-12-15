/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.platform.commandline.executor.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.nuxeo.common.Environment;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.ecm.platform.commandline.executor.service.cmdtesters.CommandTestResult;
import org.nuxeo.ecm.platform.commandline.executor.service.cmdtesters.CommandTester;
import org.nuxeo.ecm.platform.commandline.executor.service.executors.Executor;
import org.nuxeo.ecm.platform.commandline.executor.service.executors.ShellExecutor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * POJO implementation of the {@link CommandLineExecutorService} interface. Also handles the Extension Point logic.
 *
 * @author tiry
 */
public class CommandLineExecutorComponent extends DefaultComponent implements CommandLineExecutorService {

    public static final String EP_ENV = "environment";

    public static final String EP_CMD = "command";

    public static final String EP_CMDTESTER = "commandTester";

    public static final String DEFAULT_TESTER = "SystemPathTester";

    public static final String DEFAULT_EXECUTOR = "ShellExecutor";

    protected static Map<String, CommandLineDescriptor> commandDescriptors = new HashMap<String, CommandLineDescriptor>();

    protected static EnvironmentDescriptor env = new EnvironmentDescriptor();

    protected static Map<String, EnvironmentDescriptor> envDescriptors = new HashMap<>();

    protected static Map<String, CommandTester> testers = new HashMap<>();

    protected static Map<String, Executor> executors = new HashMap<String, Executor>();

    private static final Log log = LogFactory.getLog(CommandLineExecutorComponent.class);

    @Override
    public void activate(ComponentContext context) throws Exception {
        commandDescriptors = new HashMap<String, CommandLineDescriptor>();
        env = new EnvironmentDescriptor();
        testers = new HashMap<String, CommandTester>();
        executors = new HashMap<String, Executor>();
        executors.put(DEFAULT_EXECUTOR, new ShellExecutor());
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        commandDescriptors = null;
        env = null;
        testers = null;
        executors = null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (EP_ENV.equals(extensionPoint)) {
            EnvironmentDescriptor desc = (EnvironmentDescriptor) contribution;
            String name = desc.getName();
            if (name == null) {
                env.merge(desc);
            } else {
                for (String envName : name.split(",")) {
                    if (envDescriptors.containsKey(envName)) {
                        envDescriptors.get(envName).merge(desc);
                    } else {
                        envDescriptors.put(envName, desc);
                    }
                }
            }
        } else if (EP_CMD.equals(extensionPoint)) {
            CommandLineDescriptor desc = (CommandLineDescriptor) contribution;
            String name = desc.getName();

            log.debug("Registering command: " + name);

            if (!desc.isEnabled()) {
                commandDescriptors.remove(name);
                log.info("Command configured to not be enabled: " + name);
                return;
            }

            String testerName = desc.getTester();
            if (testerName == null) {
                testerName = DEFAULT_TESTER;
                log.debug("Using default tester for command: " + name);
            }

            CommandTester tester = testers.get(testerName);
            boolean cmdAvailable = false;
            if (tester == null) {
                log.error("Unable to find tester '" + testerName + "', command will not be available: " + name);
            } else {
                log.debug("Using tester '" + testerName + "' for command: " + name);
                CommandTestResult testResult = tester.test(desc);
                cmdAvailable = testResult.succeed();
                if (cmdAvailable) {
                    log.info("Registered command: " + name);
                } else {
                    desc.setInstallErrorMessage(testResult.getErrorMessage());
                    log.warn("Command not available: " + name + " (" + desc.getInstallErrorMessage() + ". "
                            + desc.getInstallationDirective() + ')');
                }
            }
            desc.setAvailable(cmdAvailable);
            commandDescriptors.put(name, desc);
        } else if (EP_CMDTESTER.equals(extensionPoint)) {
            CommandTesterDescriptor desc = (CommandTesterDescriptor) contribution;
            CommandTester tester;
            try {
                tester = (CommandTester) desc.getTesterClass().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
            testers.put(desc.getName(), tester);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor)
            throws Exception {
    }

    /*
     * Service interface
     */
    @Override
    public ExecResult execCommand(String commandName, CmdParameters params) throws CommandNotAvailable {
        CommandAvailability availability = getCommandAvailability(commandName);
        if (!availability.isAvailable()) {
            throw new CommandNotAvailable(availability);
        }

        CommandLineDescriptor cmdDesc = commandDescriptors.get(commandName);
        Executor executor = executors.get(cmdDesc.getExecutor());
        EnvironmentDescriptor commandEnv = envDescriptors.get(commandName);
        if (commandEnv == null) {
            commandEnv = envDescriptors.get(cmdDesc.getCommand());
        }
        EnvironmentDescriptor environment = new EnvironmentDescriptor().merge(env).merge(commandEnv);
        return executor.exec(cmdDesc, params, environment);
    }

    @Override
    public CommandAvailability getCommandAvailability(String commandName) {
        if (!commandDescriptors.containsKey(commandName)) {
            return new CommandAvailability(commandName + " is not a registered command");
        }

        CommandLineDescriptor desc = commandDescriptors.get(commandName);
        if (desc.isAvailable()) {
            return new CommandAvailability();
        } else {
            return new CommandAvailability(desc.getInstallationDirective(), desc.getInstallErrorMessage());
        }
    }

    @Override
    public List<String> getRegistredCommands() {
        List<String> cmds = new ArrayList<String>();
        cmds.addAll(commandDescriptors.keySet());
        return cmds;
    }

    @Override
    public List<String> getAvailableCommands() {
        List<String> cmds = new ArrayList<String>();

        for (String cmdName : commandDescriptors.keySet()) {
            CommandLineDescriptor cmd = commandDescriptors.get(cmdName);
            if (cmd.isAvailable()) {
                cmds.add(cmdName);
            }
        }
        return cmds;
    }

    public static boolean isWindows() {
        String osName = System.getProperty("os.name");
        return osName.toLowerCase().contains("windows");
    }

    @Override
    public boolean isValidParameter(String parameter) {
        Pattern VALID_PATTERN;
        if (isWindows()) {
            VALID_PATTERN = VALID_PARAMETER_PATTERN_WIN;
        } else {
            VALID_PATTERN = VALID_PARAMETER_PATTERN;
        }
        return VALID_PATTERN.matcher(parameter).matches();
    }

    @Override
    public void checkParameter(String parameter) {
        if (!isValidParameter(parameter)) {
            Pattern VALID_PATTERN;
            if (isWindows()) {
                VALID_PATTERN = VALID_PARAMETER_PATTERN_WIN;
            } else {
                VALID_PATTERN = VALID_PARAMETER_PATTERN;
            }
            throw new IllegalArgumentException(String.format("'%s' contains illegal characters. It should match: %s",
                    parameter, VALID_PATTERN));
        }
    }

    // ******************************************
    // for testing

    public static CommandLineDescriptor getCommandDescriptor(String commandName) {
        return commandDescriptors.get(commandName);
    }

    @Override
    public CmdParameters getDefaultCmdParameters() {
        CmdParameters params = new CmdParameters();
        params.addNamedParameter("java.io.tmpdir", System.getProperty("java.io.tmpdir"));
        params.addNamedParameter(Environment.NUXEO_TMP_DIR, Framework.getProperty(Environment.NUXEO_TMP_DIR));
        return params;
    }

}
