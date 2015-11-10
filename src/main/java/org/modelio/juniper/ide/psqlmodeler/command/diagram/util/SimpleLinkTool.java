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
package org.modelio.juniper.ide.psqlmodeler.command.diagram.util;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.modelio.api.diagram.IDiagramGraphic;
import org.modelio.api.diagram.IDiagramHandle;
import org.modelio.api.diagram.IDiagramLink;
import org.modelio.api.diagram.IDiagramLink.LinkRouterKind;
import org.modelio.api.diagram.ILinkPath;
import org.modelio.api.diagram.dg.IDiagramDG;
import org.modelio.api.diagram.tools.DefaultLinkTool;
import org.modelio.api.model.IModelingSession;
import org.modelio.api.model.ITransaction;
import org.modelio.api.modelio.Modelio;
import org.modelio.api.module.IModule;
import org.modelio.api.module.commands.CommandScope;
import org.modelio.metamodel.uml.infrastructure.ModelElement;

public class SimpleLinkTool extends DefaultLinkTool {

	private String scriptPath;

	public void initialize(List<CommandScope> sourceScopes,
			List<CommandScope> targetScopes, Map<String, String> parameters,
			IModule module) {
		super.initialize(sourceScopes, targetScopes, parameters, module);
		this.scriptPath = this.getParameter("scriptPath");
		System.out.println("ScriptPath: " + scriptPath);
	}

	@Override
	public boolean acceptFirstElement(IDiagramHandle representation,
			IDiagramGraphic graphic) {
		ModelElement owner = null;

		if (graphic instanceof IDiagramDG) {
			owner = representation.getDiagram().getOrigin();
		} else {
			owner = (ModelElement) graphic.getElement();
		}

		for (CommandScope scope : this.getSourceScopes()) {
			if (scope.isMatching(owner, false)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean acceptSecondElement(IDiagramHandle representation,
			IDiagramGraphic graphic_source, IDiagramGraphic graphic_target) {
		ModelElement owner = null;

		if (graphic_target instanceof IDiagramDG) {
			owner = representation.getDiagram().getOrigin();
		} else {
			owner = (ModelElement) graphic_target.getElement();
		}

		for (CommandScope scope : this.getTargetScopes()) {
			if (scope.isMatching(owner, false)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void actionPerformed(IDiagramHandle representation,
			IDiagramGraphic graphic_source, IDiagramGraphic graphic_target,
			LinkRouterKind kind, ILinkPath path) {
		IModelingSession session = Modelio.getInstance().getModelingSession();
		try (ITransaction transaction = session.createTransaction("");) {

			ModelElement source = (ModelElement) graphic_source.getElement();
			ModelElement target = (ModelElement) graphic_target.getElement();

			List<ModelElement> elements = createLink(source, target);

			for (ModelElement element : elements) {
				List<IDiagramGraphic> graph = representation.unmask(element, 0,
						0);
				for (IDiagramGraphic g : graph) {
					if (g instanceof IDiagramLink) {
						IDiagramLink link = (IDiagramLink) g;
						link.setRouterKind(kind);
						link.setPath(path);
					}
				}
			}
			representation.save();
			transaction.commit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected List<ModelElement> createLink(ModelElement source,
			ModelElement target) {
		if (this.scriptPath != null && !this.scriptPath.isEmpty()) {
			
			System.out.println("Running script");
			ScriptEngine jythonEngine = this.getModule().getJythonEngine();
			IModelingSession session = Modelio.getInstance()
					.getModelingSession();

			jythonEngine.put("source", source);
			jythonEngine.put("target", target);
			jythonEngine.put("elements", new ArrayList<ModelElement>());
			jythonEngine.put("modellingSession", session);

			String path = getScriptPath(this.scriptPath);
			try {
				jythonEngine.eval(new FileReader(path));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (ScriptException e) {
				e.printStackTrace();
			}
			return (List<ModelElement>) jythonEngine.get("elements");
		} else {
			return new ArrayList<ModelElement>();
		}
	}

	private String getScriptPath(String scriptName) {
		String path = this.getModule().getConfiguration()
				.getModuleResourcesPath().toString()
				+ scriptName;
		return path;
	}

}
