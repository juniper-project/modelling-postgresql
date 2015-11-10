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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.modelio.api.diagram.IDiagramGraphic;
import org.modelio.api.diagram.IDiagramHandle;
import org.modelio.api.diagram.IDiagramNode;
import org.modelio.api.diagram.dg.IDiagramDG;
import org.modelio.api.diagram.tools.DefaultBoxTool;
import org.modelio.api.model.IModelingSession;
import org.modelio.api.model.ITransaction;
import org.modelio.api.modelio.Modelio;
import org.modelio.api.module.IModule;
import org.modelio.api.module.commands.CommandScope;
import org.modelio.juniper.ide.psqlmodeler.impl.MDACConfigurator;
import org.modelio.metamodel.uml.infrastructure.ModelElement;
import org.modelio.vcore.smkernel.mapi.MObject;

public class SimpleBoxTool extends DefaultBoxTool {

	private ElementCreatorCommand command;
	private String ownerStereotypeModule;
	private String ownerStereotypeName;	
	
	public void initialize(List<CommandScope> sourceScopes, List<CommandScope> targetScopes, Map<String,String> parameters, IModule module) {
		super.initialize(sourceScopes, targetScopes, parameters, module);
		
		if ("elementFactory".equals(this.getParameter("type"))) {
			command = new MDACConfigurator.CallElementFactoryMdacCommand(
					this.getParameter("name"));
		} else if ("element".equals(this.getParameter("type"))) {
			try {
				command = new MDACConfigurator.CreateSingleElementMdacCommand(
						this.getParameter("metaclass"),
						this.getParameter("stereotypeModule"),
						this.getParameter("stereotype"),
						this.getParameter("addOp"));
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if ("script".equals(this.getParameter("type"))) {
			command = new MDACConfigurator.RunScriptMdacCommand(
					this.getParameter("path"));
		} else if ("java".equals(this.getParameter("type"))) {
			command = new MDACConfigurator.RunJavaMdacCommand(
					this.getParameter("command"));
		}
		
		System.out.println("====> Initialized: " + this.getParameters());
		ownerStereotypeModule = this.getParameter("ownerStereotypeModule");
		ownerStereotypeName   = this.getParameter("ownerStereotype");	
	}
	
	@Override
	public boolean acceptElement(IDiagramHandle representation, IDiagramGraphic graphic) {
		ModelElement owner = null;

		if (graphic instanceof IDiagramDG) {
			owner = representation.getDiagram().getOrigin();
		} else {
			owner = (ModelElement) graphic.getElement();
		}
		return acceptElement(owner);
	}
	
	protected boolean acceptElement(ModelElement owner) {
		if (command != null) {
			List<MObject> selectedElements = new ArrayList<MObject>();
			selectedElements.add(owner);
			boolean accepts = command.accept(selectedElements, getModule());			
			boolean ownerStereotype = owner.isStereotyped(ownerStereotypeModule, ownerStereotypeName );
			return accepts && ownerStereotype;
		} else {
			return false;
		}
	}

	@Override
	public void actionPerformed(final IDiagramHandle representation,
			final IDiagramGraphic graphic, final Rectangle rec) {
		ModelElement parent = null;

		if (graphic instanceof IDiagramDG) {
			parent = representation.getDiagram().getOrigin();
		} else {
			parent = (ModelElement) graphic.getElement();
		}
		
		if (command == null) {
			ModelElement child = createElement(parent);
			unmaskElement(representation, rec, child);
		} else {
			List<MObject> selectedElements = new ArrayList<MObject>();
			selectedElements.add(parent);
			command.setElementCreationListener(new IElementCreationListener() {
				
				@Override
				public void lastCreatedElementChanged(ModelElement element) {
					if (element != null) {
						unmaskElement(representation, rec, element);
					}
				}
			});
			command.actionPerformed(selectedElements, getModule());
		}

	}

	private void unmaskElement(final IDiagramHandle representation, final Rectangle rec,
			final ModelElement child) {
		Display.getDefault().syncExec(new Runnable() {

			@Override
			public void run() {
				IModelingSession session = Modelio.getInstance().getModelingSession();
				
				try (ITransaction transaction = session.createTransaction("");) {
					List<IDiagramGraphic> graph = representation.unmask(child, rec.x,
							rec.y);
					if (graph.size() > 0)
						((IDiagramNode) graph.get(0)).setBounds(rec);
					representation.save();

					transaction.commit();
				} catch (Exception e) {
					e.printStackTrace();
				}				
			}
			
		});
	}
	
	protected ModelElement createElement(ModelElement owner) {
		throw new RuntimeException("Command not implemented!");
	}


	
}
