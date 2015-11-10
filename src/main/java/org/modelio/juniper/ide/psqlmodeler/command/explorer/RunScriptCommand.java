/**
 * Copyright 2014 Modeliosoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modelio.juniper.ide.psqlmodeler.command.explorer;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.modelio.api.model.IModelingSession;
import org.modelio.api.model.ITransaction;
import org.modelio.api.modelio.Modelio;
import org.modelio.api.module.IModule;
import org.modelio.api.module.commands.DefaultModuleCommandHandler;
import org.modelio.metamodel.uml.infrastructure.ModelElement;
import org.modelio.vcore.smkernel.mapi.MObject;

public abstract class RunScriptCommand extends DefaultModuleCommandHandler {

	protected String scriptPath;
	
	public RunScriptCommand(String scriptPath) {
		this.scriptPath = scriptPath;
	}
	
	@Override
	public boolean accept(List<MObject> selectedElements, IModule module) {
		return super.accept(selectedElements, module) && selectedElements.size() == 1;
	}

	@Override
	public void actionPerformed(List<MObject> selectedElements, IModule module) {
		IModelingSession session = Modelio.getInstance().getModelingSession();

		try (ITransaction transaction = session
				.createTransaction("RunScript-"+scriptPath)) {
			runScript(module, scriptPath, (ModelElement) selectedElements.get(0));
			transaction.commit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static String runScript(IModule module, String scriptName,
			ModelElement selectedElement) throws FileNotFoundException,
			ScriptException {
		List<MObject> selectedElements = new ArrayList<MObject>();
		selectedElements.add(selectedElement);

		ScriptEngine jythonEngine = module.getJythonEngine();
		IModelingSession session = Modelio.getInstance().getModelingSession();

		jythonEngine.put("selectedElement", selectedElements.get(0));
		jythonEngine.put("modellingSession", session);
		jythonEngine.put("selectedElements", selectedElements);

		String path = getScriptPath(module, "/res/scripts/" + scriptName
				+ ".py");
		jythonEngine.eval(new FileReader(path));

		return (String) jythonEngine.get("resultFile");
	}

	private static String getScriptPath(IModule module, String scriptName) {
		String path = module.getConfiguration().getModuleResourcesPath()
				.toString()
				+ scriptName;
		return path;
	}
}
