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
 *  */
package org.modelio.juniper.ide.psqlmodeler.command.explorer;

import java.util.List;

import org.modelio.api.module.IModule;
import org.modelio.metamodel.uml.infrastructure.ModelElement;
import org.modelio.vcore.smkernel.mapi.MObject;

public class CreateDatabaseCommand extends RunScriptCommand {

	public CreateDatabaseCommand() {
		super("createDatabase");

	}

	@Override
	public boolean accept(List<MObject> selectedElements, IModule module) {

		MObject element = selectedElements.get(0);
		return ((ModelElement) element).isStereotyped("PersistentProfile",
				"DataModel") && !((ModelElement) element).isStereotyped("PostgreSQLModeler",
						"PsqlDatabase");

	}
}