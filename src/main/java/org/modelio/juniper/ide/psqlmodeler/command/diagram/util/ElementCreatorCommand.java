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

import org.modelio.api.module.commands.DefaultModuleCommandHandler;
import org.modelio.metamodel.uml.infrastructure.ModelElement;

public abstract class ElementCreatorCommand extends DefaultModuleCommandHandler {
	private ModelElement lastCreatedElement;
	private IElementCreationListener elementCreationListener;
	public void setElementCreationListener(
			IElementCreationListener elementCreationListener) {
		this.elementCreationListener = elementCreationListener;
	}
	public synchronized void  setLastCreatedElement(ModelElement lastCreatedElement) {
		this.lastCreatedElement = lastCreatedElement;
		if (elementCreationListener != null) {
			elementCreationListener.lastCreatedElementChanged(lastCreatedElement);
		}
	}
	public synchronized ModelElement getLastCreatedElement() {
		return lastCreatedElement;
	}
}
