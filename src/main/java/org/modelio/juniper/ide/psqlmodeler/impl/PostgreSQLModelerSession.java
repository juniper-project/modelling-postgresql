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

import java.io.File;
import java.util.Map;

import org.modelio.api.log.ILogService;
import org.modelio.api.model.IModelingSession;
import org.modelio.api.modelio.Modelio;
import org.modelio.api.module.DefaultModuleSession;
import org.modelio.api.module.ModuleException;
import org.modelio.vbasic.version.Version;

/**
 * Implementation of the IModuleSession interface.
 * <br>This default implementation may be inherited by the module developers in order to simplify the code writing of the module session.
 */
public class PostgreSQLModelerSession extends DefaultModuleSession {

	private ModelChangeHandlerImpl modelHandler;
	/**
	 * Constructor.
	 * @param module the Module this session is instanciated for.
	 */
	public PostgreSQLModelerSession(PostgreSQLModelerModule module) {
		super(module);
	}

	/**
	 * @see org.modelio.api.module.DefaultModuleSession#start()
	 */
	@Override
	public boolean start() throws ModuleException {
		// get the version of the module
		Version moduleVersion = this.module.getVersion();

		// get the Modelio log service
		ILogService logService = Modelio.getInstance().getLogService();

		String message = "Start of " + this.module.getName() + " " + moduleVersion;
		logService.info(this.module, message);
		
		// register the model change listener	    
	    IModelingSession session = Modelio.getInstance().getModelingSession();
	     
	    this.modelHandler = new ModelChangeHandlerImpl(this.module);

	    // Sometimes when we upgrade a module, Modelio doesn't remove old handlers, and doesn't call stop() either!
	    session.removeModelHandler(this.modelHandler);
	    
	    session.addModelHandler(modelHandler);
	    
	    File dataStylefile = new File(module.getConfiguration().getModuleResourcesPath()+"/res/style/dataDiagramStyle.style");
	    Modelio.getInstance().getDiagramService().registerStyle("dataDiagramStyle","main", dataStylefile);

		return super.start();
	}

	/**
	 * @see org.modelio.api.module.DefaultModuleSession#stop()
	 */
	@Override
	public void stop() throws ModuleException {
		super.stop();
	}

	public static boolean install(String modelioPath, String mdaPath) throws ModuleException {
		return DefaultModuleSession.install(modelioPath, mdaPath);
	}

	/**
	 * @see org.modelio.api.module.DefaultModuleSession#select()
	 */
	@Override
	public boolean select() throws ModuleException {
		return super.select();
	}

	/**
	 * @see org.modelio.api.module.DefaultModuleSession#unselect()
	 */
	@Override
	public void unselect() throws ModuleException {
		super.unselect();
	}

	/**
	 * @see org.modelio.api.module.DefaultModuleSession#upgrade(org.modelio.api.modelio.Version, java.util.Map)
	 */
	@Override
	public void upgrade(Version oldVersion, Map<String, String> oldParameters) throws ModuleException {
		super.upgrade(oldVersion, oldParameters);
	}
}
