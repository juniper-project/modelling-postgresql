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
package org.modelio.juniper.ide.psqlmodeler.impl;

import java.util.HashSet;
import java.util.Set;

import org.modelio.api.diagram.IDiagramHandle;
import org.modelio.api.diagram.style.IStyleHandle;
import org.modelio.api.model.IModelingSession;
import org.modelio.api.model.ITransaction;
import org.modelio.api.model.change.IModelChangeEvent;
import org.modelio.api.model.change.IModelChangeHandler;
import org.modelio.api.modelio.Modelio;
import org.modelio.api.module.IModule;
import org.modelio.metamodel.diagrams.AbstractDiagram;
import org.modelio.metamodel.diagrams.StaticDiagram;
import org.modelio.metamodel.factory.ExtensionNotFoundException;
import org.modelio.metamodel.uml.infrastructure.Dependency;
import org.modelio.metamodel.uml.infrastructure.Element;
import org.modelio.metamodel.uml.infrastructure.ModelElement;
import org.modelio.metamodel.uml.statik.Package;
import org.modelio.vcore.smkernel.mapi.MObject;

public class ModelChangeHandlerImpl implements IModelChangeHandler {

	public ModelChangeHandlerImpl(IModule mdac) {
		super();
	}

	private Set<MObject> visited = new HashSet<>();

	@Override
	public void handleModelChange(IModelingSession session,
			IModelChangeEvent event) {
		try (ITransaction t = session.createTransaction("auto modifying model")) {
			for (MObject topLevelElement : event.getCreationEvents()) {
				visitObject(session, (Element) topLevelElement);
			}
			t.commit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void visitObject(IModelingSession session, MObject object)
			throws ExtensionNotFoundException {
		// TODO this is not an optimal implementation, but Modelio insists in
		// calling me twice!!!
		// apparently this is because Modelio doesn't call stop() when
		// upgrading!!
		// On top of that Modelio has loops on the composition relationship :(

		if (visited.contains(object)) {
			return;
		}

		visited.add(object);

		if (object instanceof Package
				&& (((ModelElement) object).isStereotyped("PersistentProfile",
						"DataModel"))) {
			((ModelElement) object).removeStereotypes("JavaDesigner","JavaPackage");
		}else if(object instanceof Dependency && ((ModelElement) object).isStereotyped("JuniperIDE","Stores")){
			if(((Dependency) object).getImpacted().isStereotyped("PostgreSQLModeler","PostgreSQLServer")){
				ModelElement datamodel = ((Dependency) object).getDependsOn();
				session.getModel().createStaticDiagram("Diagram",datamodel,"PostgreSQLModeler", "PostgreSQLDataModelDiagram");
			}
		}else if (object instanceof StaticDiagram
				&& (((ModelElement) object).isStereotyped("PostgreSQLModeler",
						"PostgreSQLDataModelDiagram"))) {
			IStyleHandle style = Modelio.getInstance().getDiagramService()
					.getStyle("softwareDiagramStyle");
			IDiagramHandle diagramHandler = Modelio.getInstance()
					.getDiagramService()
					.getDiagramHandle((AbstractDiagram) object);
			diagramHandler.getDiagramNode().setStyle(style);
			diagramHandler.save();
			diagramHandler.close();

		} 

		for (MObject child : object.getCompositionChildren()) {
			visitObject(session, child);
		}
	}
}
