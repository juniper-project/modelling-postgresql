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
import java.io.FileReader;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import javax.script.ScriptEngine;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.modelio.api.model.IModelingSession;
import org.modelio.api.model.ITransaction;
import org.modelio.api.model.IUmlModel;
import org.modelio.api.modelio.Modelio;
import org.modelio.api.module.IModule;
import org.modelio.api.module.commands.DefaultModuleCommandHandler;
import org.modelio.juniper.ide.psqlmodeler.command.diagram.util.ElementCreatorCommand;
import org.modelio.metamodel.uml.infrastructure.ModelElement;
import org.modelio.metamodel.uml.infrastructure.ModelTree;
import org.modelio.metamodel.uml.statik.Class;
import org.modelio.metamodel.uml.statik.Instance;
import org.modelio.metamodel.uml.statik.Package;
import org.modelio.vcore.smkernel.mapi.MObject;

// Code generated from https://docs.google.com/a/softeam-rd.eu/spreadsheet/ccc?key=tOe2i8M594kRL4RZrAXXjag

public class MDACConfigurator {
	public static class RunScriptMdacCommand extends ElementCreatorCommand {
		private String scriptName;
		private String outputExtension;

		public RunScriptMdacCommand(String scriptName) {
			this(scriptName, null);
		}

		public RunScriptMdacCommand(String scriptName, String outputExtension) {
			this.scriptName = scriptName;
			this.outputExtension = outputExtension;
		}

		public boolean accept(List<MObject> selectedElements, IModule module) {
			return selectedElements.size() == 1;
		}

		public void actionPerformed(final List<MObject> selectedElements, final IModule module) {
			ProgressMonitorDialog progress = new ProgressMonitorDialog(Display.getCurrent().getActiveShell());
			try {
				progress.run(true, false, new IRunnableWithProgress() {

					@Override
					public void run(IProgressMonitor arg0)
							throws InvocationTargetException, InterruptedException {					
						arg0.beginTask("Running...", IProgressMonitor.UNKNOWN);
						runScript(selectedElements, module);
						arg0.done();
					}
				});
			} catch (InvocationTargetException | InterruptedException e) {
				e.printStackTrace();
			}
		}

		private void runScript(List<MObject> selectedElements, IModule module) {
			this.setLastCreatedElement(null);
			
			ScriptEngine jythonEngine = module.getJythonEngine();
			IModelingSession session = Modelio.getInstance().getModelingSession();

			jythonEngine.put("selectedElement", selectedElements.get(0));
			jythonEngine.put("modellingSession", session);
			jythonEngine.put("selectedElements", selectedElements);

			String path = getScriptPath(module);

			try (ITransaction transaction = session
					.createTransaction("RunningJythonScript-" + scriptName)) {
				jythonEngine.eval(new FileReader(path));
				transaction.commit();

				String res = (String) jythonEngine.get("resultFile");
				jythonEngine.put("resultFile", null);

				if (res != null) {
					PrintWriter pw = new PrintWriter(
							selectGenerationPath(outputExtension));
					pw.println(res);
					pw.close();
				}
				
				ModelElement createdElement = (ModelElement)jythonEngine.get("createdElement");
				if (createdElement != null) {
					this.setLastCreatedElement(createdElement);					
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		private String getScriptPath(IModule module) {
			String path = module.getConfiguration().getModuleResourcesPath()
					.toString()
					+ scriptName;
			return path;
		}

		@Override
		public boolean isActiveFor(List<MObject> selectedElements, IModule module) {
			return super.isActiveFor(selectedElements, module)
					&& new File(getScriptPath(module)).exists();
		}

		private File result = null;
		private File selectGenerationPath(final String extension) {
			result = null;
			Display.getDefault().syncExec(new Runnable() {
				
				@Override
				public void run() {
					showDialogAndSelectGenerationPath(extension);
				}
			});
			return result;
		}

		private void showDialogAndSelectGenerationPath(String extension) {
			FileDialog dialog = new FileDialog(Display.getDefault()
					.getActiveShell(), SWT.SAVE);

			if (extension != null) {
				String[] extensions = { "*" + extension }; //$NON-NLS-1$
				dialog.setFilterNames(extensions);
				dialog.setFilterExtensions(extensions);
			}

			String selectFilename = dialog.open();

			if (selectFilename != null) {

				if (extension != null && !selectFilename.endsWith(extension)) {
					selectFilename = selectFilename + extension;
				}

				result = new File(selectFilename);
			}
		}

	};

	public static class RunJavaMdacCommand extends ElementCreatorCommand  {
	    private DefaultModuleCommandHandler command;

	    public RunJavaMdacCommand(String className) {
	   	 try {
	   		 System.out.println("RunJavaMdacCommand-" + className);
	   		 this.command = (DefaultModuleCommandHandler) java.lang.Class
	   				 .forName(className).newInstance();
	   	 } catch (ClassNotFoundException | InstantiationException
	   			 | IllegalAccessException | NoClassDefFoundError e) {
	   		 e.printStackTrace();
	   		 System.out.println("RunJavaMdacCommand-" + className);
	   	 }
	    }

	    public boolean accept(List<MObject> selectedElements, IModule module) {
	   	 return command == null || command.accept(selectedElements, module);
	    }

	    public void actionPerformed(List<MObject> selectedElements, IModule module) {
	   	 if (command == null) {
	   		 System.err.println("Malformed RunJavaMdacCommand");
	   	 } else {
	   		 command.actionPerformed(selectedElements, module);
	   	 }
	    }

	    @Override
	    public boolean isActiveFor(List<MObject> selectedElements, IModule module) {
	   	 return this.command != null
	   			 && super.isActiveFor(selectedElements, module);
	    }

		public void setLastCreatedElement(ModelElement lastCreatedElement) {
			if (command instanceof ElementCreatorCommand) {
				((ElementCreatorCommand)command).setLastCreatedElement(lastCreatedElement);
			}
		}
		public ModelElement getLastCreatedElement() {
			if (command instanceof ElementCreatorCommand) {
				return ((ElementCreatorCommand)command).getLastCreatedElement();
			} else {
				return null;
			}
		}		
	};

	public static class CallElementFactoryMdacCommand extends ElementCreatorCommand  {
	    private String element;

	    public CallElementFactoryMdacCommand(String element) {
	   	 this.element = element;
	    }

	    public boolean accept(List<MObject> selectedElements, IModule module) {
	   	 return selectedElements.size() == 1;
	    }

	    public void actionPerformed(List<MObject> selectedElements, IModule module) {
	   	 ElementFactory ef = new ElementFactory();
	   	 try {
	   		 setLastCreatedElement(null);
	   		 ModelElement createdElement = (ModelElement) ElementFactory.class.getMethod("create" + element,
	   				 new java.lang.Class[] { ModelTree.class }).invoke(ef,
	   				 selectedElements.get(0));
	   		 setLastCreatedElement(createdElement);
	   	 } catch (Exception e) {
	   		 e.printStackTrace();
	   	 }
	    }

	};

	public static class CreateSingleElementMdacCommand  extends ElementCreatorCommand {
	    public static ModelElement createSingleElement(ModelElement parent,
	   		 String metaclass, String stereotype, String stereotypeModule,
	   		 String addOp) {
	   	 IModelingSession session = Modelio.getInstance().getModelingSession();

	   	 try (ITransaction transaction = session
	   			 .createTransaction("CreateSingleElement-" + metaclass + "-"
	   					 + stereotype + "-" + addOp)) {
	   		 try {
	   			 IUmlModel factory = session.getModel();

	   			 ModelElement el = null;

	   			 try {
	   				 el = (ModelElement) IUmlModel.class.getMethod(
	   						 "create" + metaclass, new java.lang.Class[0])
	   						 .invoke(factory);
	   			 } catch (Exception e) {
	   				 el = (ModelElement) factory.createElement(metaclass);
	   			 }

	   			 if (stereotype != null && !stereotype.isEmpty()) {
	   				 el.addStereotype(stereotypeModule, stereotype);
	   				 el.setName(stereotype);
	   			 } else {
	   				 el.setName(metaclass);
	   			 }

	   			 if (addOp == null || (addOp != null && addOp.isEmpty())) {
	   				 ((ModelTree) parent).getOwnedElement().add((ModelTree) el);
	   			 } else {
	   				 List list = null;
	   				 for (Method m : parent.getClass().getMethods()) {
	   					 if (m.getName().equals("get" + addOp)
	   							 && m.getParameterTypes().length == 0) {
	   						 if (m.getReturnType().getName().contains("EList")) {
	   							 list = (List) m.invoke(parent);
	   							 break;
	   						 } else {
	   							 continue;
	   						 }
	   					 } else if (m.getName().equals("set" + addOp)
	   							 && m.getParameterTypes().length == 1) {
	   						 m.invoke(parent, el);
	   						 break;
	   					 }
	   				 }

	   				 if (list != null)
	   					 list.add(el);
	   			 }
	   			 transaction.commit();
	   			 return el;
	   		 } catch (Exception e) {
	   			 e.printStackTrace();
	   			 transaction.rollback();
	   			 return null;
	   		 }
	   	 }
	    }

	    private String metaclass;
	    private String stereotype;
	    private String addOp;
	    private String stereotypeModule;
	    
	    public CreateSingleElementMdacCommand(String metaclass,
	   		 String stereotypeModule, String stereotype) throws Exception {
	   	 this(metaclass, stereotypeModule, stereotype, null);
	    }

	    public CreateSingleElementMdacCommand(String metaclass,
	   		 String stereotypeModule, String stereotype, String addOp)
	   		 throws Exception {
	   	 this.metaclass = metaclass;
	   	 this.stereotype = stereotype;
	   	 this.stereotypeModule = stereotypeModule;
	   	 this.addOp = addOp;
	    }

	    public boolean accept(List<MObject> selectedElements, IModule module) {
	   	 return selectedElements.size() == 1;
	    }

	    @SuppressWarnings({ "rawtypes", "unchecked" })
	    public void actionPerformed(List<MObject> selectedElements, IModule module) {
	   	 IModelingSession session = Modelio.getInstance().getModelingSession();
	   	 ModelElement parent = (ModelElement) selectedElements.get(0);

	   	 setLastCreatedElement(null);
	   	 ModelElement createdElement = createSingleElement(parent, metaclass, stereotype, stereotypeModule, addOp);
	   	 setLastCreatedElement(createdElement);
	    }
	    
	}
public static class ElementFactory {
	public ElementFactory() {}
	@SuppressWarnings("unused")
	public Package createJUNIPERApplication(ModelTree parent) {
		IModelingSession session = Modelio.getInstance().getModelingSession();
		try(ITransaction transaction = session.createTransaction("JUNIPER Application")) {
			IUmlModel factory = session.getModel();
			ModelElement el0,el1,el2,el3,el4;
			el0 = CreateSingleElementMdacCommand.createSingleElement(parent, "Package", "JUNIPERModel", "JuniperIDE", "OwnedElement");
			el0.setName("JUNIPER Application");
			el1 = CreateSingleElementMdacCommand.createSingleElement(el0, "Package", "SoftwareArchitectureModel", "JuniperIDE", "OwnedElement");
			el1.setName("Software platform");
			el2 = CreateSingleElementMdacCommand.createSingleElement(el1, "Package", "DataModel", "PersistentProfile", "OwnedElement");
			el2.setName("BusinessObjects");
			el3 = CreateSingleElementMdacCommand.createSingleElement(el2, "StaticDiagram", "PersistentDiagram", "PersistentProfile", "Product");
			el3.setName("Diagram");
			el2 = CreateSingleElementMdacCommand.createSingleElement(el1, "ClassDiagram", "", "", "Product");
			el2.setName("Diagram");
			el1 = CreateSingleElementMdacCommand.createSingleElement(el0, "Package", "HardwarePlatformModel", "JuniperIDE", "OwnedElement");
			el1.setName("Hardware platform");
			el2 = CreateSingleElementMdacCommand.createSingleElement(el1, "ClassDiagram", "", "", "Product");
			el2.setName("Diagram");
			el1 = CreateSingleElementMdacCommand.createSingleElement(el0, "Package", "AdvicesPackage", "JuniperIDE", "OwnedElement");
			el1.setName("Scheduling Advices");
			transaction.commit();
			return (Package) el0;
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	@SuppressWarnings("unused")
	public Instance createCloudnode(ModelTree parent) {
		IModelingSession session = Modelio.getInstance().getModelingSession();
		try(ITransaction transaction = session.createTransaction("Cloud node")) {
			IUmlModel factory = session.getModel();
			ModelElement el0,el1,el2,el3,el4;
			el0 = CreateSingleElementMdacCommand.createSingleElement(parent, "Instance", "HwComputingResource_Instance", "MARTEDesigner", "Declared");
			el0.setName("Cloud node");
			el1 = CreateSingleElementMdacCommand.createSingleElement(el0, "BindableInstance", "HwProcessor_Instance", "MARTEDesigner", "Part");
			el1.setName("CPU");
			transaction.commit();
			return (Instance) el0;
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	@SuppressWarnings("unused")
	public Class createJUNIPERProgram(ModelTree parent) {
		IModelingSession session = Modelio.getInstance().getModelingSession();
		try(ITransaction transaction = session.createTransaction("JUNIPER Program")) {
			IUmlModel factory = session.getModel();
			ModelElement el0,el1,el2,el3,el4;
			el0 = CreateSingleElementMdacCommand.createSingleElement(parent, "Class", "JUNIPERProgram", "JuniperIDE", "OwnedElement");
			el0.setName("JUNIPER Program");
			el1 = CreateSingleElementMdacCommand.createSingleElement(el0, "Operation", "", "", "OwnedOperation");
			el1.setName("execute");
			transaction.commit();
			return (Class) el0;
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}

//	public void init(IModule module) throws Exception {
//		String resPath = module.getConfiguration().getModuleResourcesPath().toString();
//		IModuleAction action = null;
//		IMetamodelExtensions metamodelExtensions = Modelio.getInstance().getModelingSession().getMetamodelExtensions();
//		action = new DefaultModuleAction(module,
//			"Create Juniper model", "Create Juniper model",
//			"Create Juniper model", resPath+"/res/icons/juniper_icon_16.png", 
//			"Elements", resPath+"/res/icons/elements16.png", true, true, 
//			new CallElementFactoryMdacCommand("JUNIPERApplication"));
//		action.addAllowedMetaclass(Package.class);
//		action.addAllowedStereotype(metamodelExtensions.getStereotype("JuniperIDE", "RootPackage", Metamodel.getMClass(Package.class)));
//		module.registerAction(ActionLocation.contextualpopup, action);
//
//		action = new DefaultModuleAction(module,
//			"Create cloud node", "Create cloud node",
//			"Create cloud node", resPath+"/res/icons/Server16.png", 
//			"Elements", resPath+"/res/icons/elements16.png", true, true, 
//			new CallElementFactoryMdacCommand("Cloudnode"));
//		action.addAllowedMetaclass(Package.class);
//		action.addAllowedStereotype(metamodelExtensions.getStereotype("JuniperIDE", "HardwarePlatformModel", Metamodel.getMClass(Package.class)));
//		module.registerAction(ActionLocation.contextualpopup, action);
//
//		action = new DefaultModuleAction(module,
//			"Create CPU", "Create CPU",
//			"Create CPU", resPath+"/res/icons/Router16.png", 
//			"Elements", resPath+"/res/icons/elements16.png", true, true, 
//			new CreateSingleElementMdacCommand("BindableInstance", "MARTEDesigner", "HwProcessor_Instance", "Part"));
//		action.addAllowedMetaclass(Instance.class);
//		action.addAllowedStereotype(metamodelExtensions.getStereotype("MARTEDesigner", "HwComputingResource_Instance", Metamodel.getMClass(Instance.class)));
//		module.registerAction(ActionLocation.contextualpopup, action);
//
//		action = new DefaultModuleAction(module,
//			"Create cloud disk", "Create cloud disk",
//			"Create cloud disk", resPath+"/res/icons/Image16.png", 
//			"Elements", resPath+"/res/icons/elements16.png", true, true, 
//			new CreateSingleElementMdacCommand("BindableInstance", "MARTEDesigner", "HwDrive_Instance", "Part"));
//		action.addAllowedMetaclass(Instance.class);
//		action.addAllowedStereotype(metamodelExtensions.getStereotype("MARTEDesigner", "HwComputingResource_Instance", Metamodel.getMClass(Instance.class)));
//		module.registerAction(ActionLocation.contextualpopup, action);
//
//		action = new DefaultModuleAction(module,
//			"Create Juniper program instance", "Create Juniper program instance",
//			"Create Juniper program instance", resPath+"/res/icons/HwComponent_kindPort_bindableinstance.png", 
//			"Elements", resPath+"/res/icons/elements16.png", true, true, 
//			new CreateSingleElementMdacCommand("BindableInstance", "JuniperIDE", "ProgramInstance", "Part"));
//		action.addAllowedMetaclass(Instance.class);
//		action.addAllowedStereotype(metamodelExtensions.getStereotype("MARTEDesigner", "HwComputingResource_Instance", Metamodel.getMClass(Instance.class)));
//		module.registerAction(ActionLocation.contextualpopup, action);
//
//		action = new DefaultModuleAction(module,
//			"Create non preemptive software region instance", "Create non preemptive software region instance",
//			"Create non preemptive software region instance", resPath+"/res/icons/System16.png", 
//			"Elements", resPath+"/res/icons/elements16.png", true, true, 
//			new CreateSingleElementMdacCommand("BindableInstance", "JuniperIDE", "SwMutualExclusionResource", "Part"));
//		action.addAllowedMetaclass(Instance.class);
//		action.addAllowedStereotype(metamodelExtensions.getStereotype("MARTEDesigner", "HwComputingResource_Instance", Metamodel.getMClass(Instance.class)));
//		module.registerAction(ActionLocation.contextualpopup, action);
//
//		action = new DefaultModuleAction(module,
//			"Create non preemptive software region", "Create non preemptive software region",
//			"Create non preemptive software region", resPath+"/res/icons/System16.png", 
//			"Elements", resPath+"/res/icons/elements16.png", true, true, 
//			new CreateSingleElementMdacCommand("Class", "JuniperIDE", "SwMutualExclusionResource"));
//		action.addAllowedMetaclass(Package.class);
//		action.addAllowedStereotype(metamodelExtensions.getStereotype("JuniperIDE", "SoftwareArchitectureModel", Metamodel.getMClass(Package.class)));
//		module.registerAction(ActionLocation.contextualpopup, action);
//
//		action = new DefaultModuleAction(module,
//			"Create Juniper program", "Create Juniper program",
//			"Create Juniper program", resPath+"/res/icons/HwComponent_kindPort_class.png", 
//			"Elements", resPath+"/res/icons/elements16.png", true, true, 
//			new RunScriptMdacCommand("/res/scripts/createJuniperProgram.py"));
//		action.addAllowedMetaclass(Package.class);
//		action.addAllowedStereotype(metamodelExtensions.getStereotype("JuniperIDE", "SoftwareArchitectureModel", Metamodel.getMClass(Package.class)));
//		module.registerAction(ActionLocation.contextualpopup, action);
//
//		action = new DefaultModuleAction(module,
//			"Create request/response stream", "Create request/response stream",
//			"Create request/response stream", resPath+"/res/icons/RequestResponseStream16.png", 
//			"Elements", resPath+"/res/icons/elements16.png", true, true, 
//			new CreateSingleElementMdacCommand("Port", "JuniperIDE", "RequestResponseStream", "InternalStructure"));
//		action.addAllowedMetaclass(Class.class);
//		action.addAllowedStereotype(metamodelExtensions.getStereotype("JuniperIDE", "JUNIPERProgram", Metamodel.getMClass(Class.class)));
//		module.registerAction(ActionLocation.contextualpopup, action);
//
//		action = new DefaultModuleAction(module,
//			"Create class", "Create class",
//			"Create class", resPath+"/res/icons/class.png", 
//			"Elements", resPath+"/res/icons/elements16.png", true, true, 
//			new CreateSingleElementMdacCommand("Class", "JavaDesigner", "JavaClass"));
//		action.addAllowedMetaclass(Package.class);
//		action.addAllowedStereotype(metamodelExtensions.getStereotype("JuniperIDE", "SoftwareArchitectureModel", Metamodel.getMClass(Package.class)));
//		module.registerAction(ActionLocation.contextualpopup, action);
//
//		action = new DefaultModuleAction(module,
//			"Create interface", "Create interface",
//			"Create interface", resPath+"/res/icons/interface.png", 
//			"Elements", resPath+"/res/icons/elements16.png", true, true, 
//			new CreateSingleElementMdacCommand("Interface", "JavaDesigner", "JavaInterface"));
//		action.addAllowedMetaclass(Package.class);
//		action.addAllowedStereotype(metamodelExtensions.getStereotype("JuniperIDE", "SoftwareArchitectureModel", Metamodel.getMClass(Package.class)));
//		module.registerAction(ActionLocation.contextualpopup, action);
//
//		action = new DefaultModuleAction(module,
//			"Create attribute", "Create attribute",
//			"Create attribute", resPath+"/res/icons/attribute_property.png", 
//			"Elements", resPath+"/res/icons/elements16.png", true, true, 
//			new CreateSingleElementMdacCommand("Attribute", "JavaDesigner", "JavaAttributeProperty", "OwnedAttribute"));
//		action.addAllowedMetaclass(Class.class);
//		action.addAllowedStereotype(metamodelExtensions.getStereotype("JavaDesigner", "JavaClass", Metamodel.getMClass(Class.class)));
//		module.registerAction(ActionLocation.contextualpopup, action);
//
//		action = new DefaultModuleAction(module,
//			"Create operation", "Create operation",
//			"Create operation", resPath+"/res/icons/operation.png", 
//			"Elements", resPath+"/res/icons/elements16.png", true, true, 
//			new CreateSingleElementMdacCommand("Operation", "", "", "OwnedOperation"));
//		action.addAllowedMetaclass(Classifier.class);
//		module.registerAction(ActionLocation.contextualpopup, action);
//
//		action = new DefaultModuleAction(module,
//			"Create parameter", "Create parameter",
//			"Create parameter", resPath+"/res/icons/parameter.png", 
//			"Elements", resPath+"/res/icons/elements16.png", true, true, 
//			new CreateSingleElementMdacCommand("Parameter", "", "", "IO"));
//		action.addAllowedMetaclass(Operation.class);
//		module.registerAction(ActionLocation.contextualpopup, action);
//
//		action = new DefaultModuleAction(module,
//			"Create return parameter", "Create return parameter",
//			"Create return parameter", resPath+"/res/icons/parameter.return.png", 
//			"Elements", resPath+"/res/icons/elements16.png", true, true, 
//			new CreateSingleElementMdacCommand("Parameter", "", "", "Return"));
//		action.addAllowedMetaclass(Operation.class);
//		module.registerAction(ActionLocation.contextualpopup, action);
//
//		action = new DefaultModuleAction(module,
//			"Create initial node", "Create initial node",
//			"Create initial node", resPath+"/res/icons/uml/InitialNode16.png", 
//			"Elements", resPath+"/res/icons/elements16.png", true, true, 
//			new CreateSingleElementMdacCommand("InitialNode", "", "", "ContainedNode"));
//		action.addAllowedMetaclass(ActivityPartition.class);
//		module.registerAction(ActionLocation.contextualpopup, action);
//
//		action = new DefaultModuleAction(module,
//			"Create final node", "Create final node",
//			"Create final node", resPath+"/res/icons/uml/ActivityFinalNode16.png", 
//			"Elements", resPath+"/res/icons/elements16.png", true, true, 
//			new CreateSingleElementMdacCommand("ActivityFinalNode", "", "", "ContainedNode"));
//		action.addAllowedMetaclass(ActivityPartition.class);
//		module.registerAction(ActionLocation.contextualpopup, action);
//
//		action = new DefaultModuleAction(module,
//			"Create partition", "Create partition",
//			"Create partition", resPath+"/res/icons/uml/ActivityPartition16.png", 
//			"Elements", resPath+"/res/icons/elements16.png", true, true, 
//			new CreateSingleElementMdacCommand("ActivityPartition", "", "", "OwnedGroup"));
//		action.addAllowedMetaclass(Activity.class);
//		module.registerAction(ActionLocation.contextualpopup, action);
//
//		action = new DefaultModuleAction(module,
//			"Create chunk", "Create chunk",
//			"Create chunk", resPath+"/res/icons/activity.png", 
//			"Elements", resPath+"/res/icons/elements16.png", true, true, 
//			new RunScriptMdacCommand("/res/scripts/createChunk.py"));
//		action.addAllowedMetaclass(ActivityPartition.class);
//		module.registerAction(ActionLocation.contextualpopup, action);
//
//		action = new DefaultModuleAction(module,
//			"Import advices...", "Import advices...",
//			"Import advices...", resPath+"/res/icons/serviceContract.png", 
//			"Schedulling adviser", resPath+"/res/icons/serviceContract.png", true, true, 
//			new RunScriptMdacCommand("/res/scripts/importSchedulingAdvices.py"));
//		action.addAllowedMetaclass(Package.class);
//		action.addAllowedStereotype(metamodelExtensions.getStereotype("JuniperIDE", "AdvicesPackage", Metamodel.getMClass(Package.class)));
//		module.registerAction(ActionLocation.contextualpopup, action);
//
//		action = new DefaultModuleAction(module,
//			"Advice details...", "Advice details...",
//			"Advice details...", resPath+"/res/icons/serviceContract.png", 
//			"Schedulling adviser", resPath+"/res/icons/serviceContract.png", false, false, 
//			new RunScriptMdacCommand("/res/scripts/showAdvice.py"));
//		action.addAllowedMetaclass(Element.class);
//		action.addAllowedStereotype(metamodelExtensions.getStereotype("JuniperIDE", "Advice", Metamodel.getMClass(Element.class)));
//		module.registerAction(ActionLocation.contextualpopup, action);
//
//		action = new DefaultModuleAction(module,
//			"Export XML model to scheduling advisor...", "Export XML model to scheduling advisor...",
//			"Export XML model to scheduling advisor...", resPath+"/res/icons/Alarm_association.png", 
//			"Schedulling adviser", resPath+"/res/icons/serviceContract.png", false, false, 
//			new RunScriptMdacCommand("/res/scripts/exportXML.py"));
//		action.addAllowedMetaclass(Package.class);
//		action.addAllowedStereotype(metamodelExtensions.getStereotype("JuniperIDE", "JUNIPERModel", Metamodel.getMClass(Package.class)));
//		module.registerAction(ActionLocation.contextualpopup, action);
//
//		action = new DefaultModuleAction(module,
//			"Run schedullability analyser...", "Run schedullability analyser...",
//			"Run schedullability analyser...", resPath+"/res/icons/Alarm_association.png", 
//			"Schedullability analyser", resPath+"/res/icons/Alarm_association.png", false, false, 
//			new RunJavaMdacCommand("org.modelio.juniper.ide.command.explorer.RunSchedulabilityAnalyser"));
//		action.addAllowedMetaclass(Package.class);
//		action.addAllowedStereotype(metamodelExtensions.getStereotype("JuniperIDE", "JUNIPERModel", Metamodel.getMClass(Package.class)));
//		module.registerAction(ActionLocation.contextualpopup, action);
//
//		action = new DefaultModuleAction(module,
//			"Generate behavior models from software model", "Generate behavior models from software model",
//			"Generate behavior models from software model", resPath+"/res/icons/activity.png", 
//			"Model transformations", resPath+"/res/icons/transformation16.png", true, true, 
//			new RunScriptMdacCommand("/res/scripts/generateBehaviorModels.py"));
//		action.addAllowedMetaclass(Package.class);
//		action.addAllowedStereotype(metamodelExtensions.getStereotype("JuniperIDE", "JUNIPERModel", Metamodel.getMClass(Package.class)));
//		module.registerAction(ActionLocation.contextualpopup, action);
//
//		action = new DefaultModuleAction(module,
//			"Generate Hardware platform model from Software platform model", "Generate Hardware platform model from Software platform model",
//			"Generate Hardware platform model from Software platform model", resPath+"/res/icons/bdata_system.png", 
//			"Model transformations", resPath+"/res/icons/transformation16.png", true, true, 
//			new RunScriptMdacCommand("/res/scripts/generateHwPlatform.py"));
//		action.addAllowedMetaclass(Package.class);
//		action.addAllowedStereotype(metamodelExtensions.getStereotype("JuniperIDE", "JUNIPERModel", Metamodel.getMClass(Package.class)));
//		module.registerAction(ActionLocation.contextualpopup, action);
//
//		action = new DefaultModuleAction(module,
//			"Generate code", "Generate code",
//			"Generate code", resPath+"/res/icons/generation.png", 
//			"", resPath+"", true, true, 
//			new RunJavaMdacCommand("org.modelio.juniper.ide.command.explorer.GenerateCodeCommand"));
//		action.addAllowedMetaclass(Package.class);
//		action.addAllowedStereotype(metamodelExtensions.getStereotype("JuniperIDE", "JUNIPERModel", Metamodel.getMClass(Package.class)));
//		module.registerAction(ActionLocation.contextualpopup, action);
//
//		action = new DefaultModuleAction(module,
//			"Validate model...", "Validate model...",
//			"Validate model...", resPath+"/res/icons/validate_16.png", 
//			"", resPath+"", false, false, 
//			new RunJavaMdacCommand("org.modelio.modelvalidator.command.explorer.ValidateModelCommand"));
//		action.addAllowedMetaclass(Package.class);
//		action.addAllowedStereotype(metamodelExtensions.getStereotype("JuniperIDE", "JUNIPERModel", Metamodel.getMClass(Package.class)));
//		module.registerAction(ActionLocation.contextualpopup, action);
//
//	}
}
