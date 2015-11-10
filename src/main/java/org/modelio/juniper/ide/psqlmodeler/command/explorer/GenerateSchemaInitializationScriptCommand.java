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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.emf.common.util.EList;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.modelio.api.modelio.Modelio;
import org.modelio.api.module.IModule;
import org.modelio.api.module.IModuleAPIConfiguration;
import org.modelio.api.module.IPeerModule;
import org.modelio.api.module.commands.DefaultModuleCommandHandler;
import org.modelio.metamodel.uml.infrastructure.Dependency;
import org.modelio.metamodel.uml.infrastructure.ModelElement;
import org.modelio.metamodel.uml.infrastructure.ModelTree;
import org.modelio.metamodel.uml.statik.AggregationKind;
import org.modelio.metamodel.uml.statik.AssociationEnd;
import org.modelio.metamodel.uml.statik.Attribute;
import org.modelio.metamodel.uml.statik.BindableInstance;
import org.modelio.metamodel.uml.statik.Classifier;
import org.modelio.metamodel.uml.statik.DataType;
import org.modelio.metamodel.uml.statik.Instance;
import org.modelio.metamodel.uml.statik.NameSpace;
import org.modelio.metamodel.uml.statik.Package;
import org.modelio.vcore.smkernel.mapi.MObject;

public class GenerateSchemaInitializationScriptCommand extends
		DefaultModuleCommandHandler {

	@Override
	public boolean accept(List<MObject> selectedElements, IModule module) {

		return super.accept(selectedElements, module) && selectedElements.size() == 1
				&& (((ModelElement) selectedElements.get(0)).isStereotyped(
						"JuniperIDE", "JUNIPERModel"));
	}

	@Override
	public void actionPerformed(List<MObject> selectedElements, IModule module) {

		IPeerModule jdesigner = Modelio.getInstance().getModuleService()
				.getPeerModule("JavaDesigner");

		IModuleAPIConfiguration config = jdesigner.getConfiguration();

		String fileLocation = config.getProjectSpacePath().toString()
				+ "\\deploymentScripts\\" + selectedElements.get(0).getName();
		new File(fileLocation + "\\").mkdirs();

		List<ModelTree> packages = ((Package) selectedElements.get(0))
				.getOwnedElement();

		List<NameSpace> databases = new ArrayList<NameSpace>();

		BufferedWriter bwScript = null;
		String userTag;
		String passwordTag;
		try {

			bwScript = new BufferedWriter(new FileWriter(fileLocation
					+ "\\deploymentCompositionScript.sh"));
			for (ModelTree _package : packages) {
				if (_package.isStereotyped("JuniperIDE",
						"SoftwareArchitectureModel")) {

					List<? extends MObject> els = _package
							.getCompositionChildren();

					for (MObject el : els) {
						if (((ModelElement) el).isStereotyped(
								"PostgreSQLModeler", "PostgreSQLServer")) {
							userTag = ((ModelElement) el).getTagValue(
									"PostgreSQLModeler", "user");
							passwordTag = ((ModelElement) el).getTagValue(
									"PostgreSQLModeler", "user");
							EList<Dependency> dependencies = ((ModelElement) el)
									.getDependsOnDependency();

							for (Dependency dependency : dependencies) {

								if (dependency.isStereotyped("JuniperIDE",
										"Stores")) {

									NameSpace datamodel = (NameSpace) dependency
											.getDependsOn();
									if (datamodel.isStereotyped(
											"PersistentProfile", "DataModel")) {
										for (MObject database : datamodel
												.getCompositionChildren()) {
											if (((ModelElement) database)
													.isStereotyped(
															"PostgreSQLModeler",
															"PsqlDatabase")) {
												databases
														.add((NameSpace) database);
											}
										}
									}

								}
							}

							editScriptFile(el, databases, bwScript, userTag,
									passwordTag, fileLocation);
						}

					}

				}

			}

			bwScript.close();

		} catch (IOException e) {

			e.printStackTrace();
		}

		messageBox();
	}

	void editScriptFile(MObject server, List<NameSpace> databases,
			BufferedWriter bwScript, String userTag, String passwordTag,
			String fileLocation) throws IOException {

		String ipServer = "";
		String serverConfig = "";
		String scriptFile = "";
		Map<String, String> sqlFileString = new HashMap<String, String>();

		if (((NameSpace) server).getRepresenting().size() > 1) {
			for (Instance el : ((NameSpace) server).getRepresenting()) {

				if (el.isStereotyped("PostgreSQLModeler", "Master")) {
					serverConfig = "master";
					ipServer = ((BindableInstance) el).getCluster()
							.getTagValue("JuniperIDE", "ip");
					scriptFile += editConfigFileString(scriptFile,
							sqlFileString, userTag, passwordTag, databases);

				}
			}

		} else {
			serverConfig = "simplePostgreSQL";
			for (Instance el : ((NameSpace) server).getRepresenting()) {

				ipServer = ((BindableInstance) el).getCluster().getTagValue(
						"JuniperIDE", "ip");

				scriptFile += editConfigFileString(scriptFile, sqlFileString,
						userTag, passwordTag, databases);

			}

		}

		editScriptDeployConfig(bwScript, scriptFile, serverConfig, ipServer,
				fileLocation, sqlFileString);
	}

	private void editScriptDeployConfig(BufferedWriter bwScript,
			String scriptFile, String server, String ipServer,
			String fileLocation, Map<String, String> sqlFileString)
			throws IOException {

		File dossier = new File(fileLocation + "\\" + server + ipServer);

		if (!dossier.exists() || !dossier.isDirectory()) {
			new File(fileLocation + "\\" + server + ipServer).mkdirs();
		}

		for (Entry<String, String> sqlFile : sqlFileString.entrySet()) {
			BufferedWriter bwSqlFile = new BufferedWriter(new FileWriter(
					fileLocation + "\\" + server + ipServer + "\\"
							+ sqlFile.getKey()));
			bwSqlFile.write(sqlFile.getValue());

			bwSqlFile.close();
			bwScript.write("scp " + server + ipServer + "/" + sqlFile.getKey()
					+ " root@" + ipServer + ":./" + server + ipServer + "\n");
		}

		BufferedWriter bwDataConfigFile = new BufferedWriter(
				new FileWriter(fileLocation + "\\" + server + ipServer
						+ "\\dataConfigFile.sh"));
		bwDataConfigFile.write(scriptFile);

		bwDataConfigFile.close();

		bwScript.write("scp " + server + ipServer + "/dataConfigFile.sh root@"
				+ ipServer + ":./" + server + ipServer + "\nssh root@"
				+ ipServer + " \" cd " + server + ipServer
				+ "; bash ./dataConfigFile.sh \"\n");

	}

	private String editConfigFileString(String scriptFile,
			Map<String, String> sqlFileString, String userTag,
			String passwordTag, List<NameSpace> databases) {

		scriptFile += "sudo -u postgres -s psql -c \"create user " + userTag
				+ " with encrypted password \'" + passwordTag + "\';\"\n";
		scriptFile += "sudo -u postgres -s psql -c \'alter role " + userTag
				+ " with createdb;\'\n";
		scriptFile += "export PGUSER=" + userTag + "\nexport PGPASSWORD="
				+ passwordTag + "\nexport PGHOST=localhost\n";
		for (NameSpace database : databases) {
			scriptFile += "su - postgres -c \'createdb -O " + userTag + " "
					+ database.getName() + "\'\n";
			scriptFile += "sudo -u postgres psql -d " + database.getName()
					+ " -c \' create extension hstore;\'\n";
			scriptFile += "psql -U " + userTag + " " + database.getName()
					+ " -f " + database.getName() + "SqlFile.txt";
			for (MObject entity : database.getCompositionChildren()) {

				String nameSqlFile = database.getName() + "SqlFile.txt";

				if (((ModelElement) entity).isStereotyped("PersistentProfile",
						"Entity")) {

					String sqlString = sqlFileString.get(nameSqlFile);
					if (sqlString == null) {
						sqlString = createElementSqlString(entity);
					} else {
						sqlString += createElementSqlString(entity);
					}
					sqlFileString.put(nameSqlFile, sqlString);

				}
			}
		}
		return scriptFile;
	}

	private String createElementSqlString(MObject entity) {
		String stringSqlFile = "";
		int nbAttributes = 0;
		stringSqlFile += "create table " + entity.getName() + " (";
		for (MObject attribute : ((Classifier) entity).getOwnedAttribute()) {

			if (((Attribute) attribute).isStereotyped("PersistentProfile",
					"Identifier")) {
				if (nbAttributes < ((Classifier) entity).getOwnedAttribute()
						.size() - 1) {
					stringSqlFile += attribute.getName()
							+ " "
							+ transformationType((DataType) ((Attribute) attribute)
									.getType()) + " PRIMARY KEY,";
				} else {
					stringSqlFile += attribute.getName()
							+ " "
							+ transformationType((DataType) ((Attribute) attribute)
									.getType()) + " PRIMARY KEY";
				}
			} else {
				if (nbAttributes < ((Classifier) entity).getOwnedAttribute()
						.size() - 1) {
					stringSqlFile += attribute.getName()
							+ " "
							+ transformationType((DataType) ((Attribute) attribute)
									.getType()) + ",";
				} else {
					stringSqlFile += attribute.getName()
							+ " "
							+ transformationType((DataType) ((Attribute) attribute)
									.getType());
				}

			}
			nbAttributes++;
		}

		if (((Classifier) entity).getOwnedEnd().size() <= 0) {
			stringSqlFile += ");\n";
		} else {
			if (((Classifier) entity).getOwnedAttribute().size() > 0) {
				stringSqlFile += ", ";
			}

			int nbAssociation = 0;
			for (AssociationEnd associationEnd : ((Classifier) entity)
					.getOwnedEnd()) {
				if (associationEnd.getAggregation().equals(
						AggregationKind.KINDISCOMPOSITION)) {
					if (associationEnd
							.isStereotyped("PostgreSQLModeler", "Xml")) {
						stringSqlFile += associationEnd.getName() + " xml";
					} else if (associationEnd.isStereotyped(
							"PostgreSQLModeler", "Json")) {
						stringSqlFile += associationEnd.getName() + " json";
					} else if (associationEnd.isStereotyped(
							"PostgreSQLModeler", "Hstore")) {
						stringSqlFile += associationEnd.getName() + " hstore";
					}
				} else {
					MObject entityRef = associationEnd.getOpposite().getOwner();
					String typeForeignKey = "";
					String foreignKeyName = "";
					for (Attribute attr : ((Classifier) entityRef)
							.getOwnedAttribute()) {
						if (attr.isStereotyped("PersistentProfile",
								"Identifier")) {
							typeForeignKey = transformationType((DataType) ((Attribute) attr)
									.getType());
							foreignKeyName = attr.getName();
						}
					}

					String tableReferences = entityRef.getName();

					stringSqlFile += associationEnd.getName() + " "
							+ typeForeignKey + " references " + tableReferences
							+ "(" + foreignKeyName + ")";
				}
				if (nbAssociation < ((Classifier) entity).getOwnedEnd().size() - 1) {
					stringSqlFile += ", ";
				}
				nbAssociation++;
			}
			stringSqlFile += ");\n";
		}

		return stringSqlFile;
	}

	private String transformationType(DataType datatype) {
		String type = datatype.getName();

		if (type.equals("integer")) {
			return "integer";
		} else if (type.equals("long")) {
			return "bigint";
		} else if (type.equals("short")) {
			return "smallint";
		} else if (type.equals("double")) {
			return "float";
		} else if (type.equals("float")) {
			return "real";
		} else if (type.equals("byte")) {
			return "bytea";
		} else if (type.equals("boolean")) {
			return "boolean";
		} else if (type.equals("date")) {
			return "date";
		} else {
			return "text";
		}
	}

	private void messageBox() {

		MessageBox msg = new MessageBox(Display.getCurrent().getActiveShell(),
				SWT.ICON_INFORMATION | SWT.OK);
		msg.setMessage("Code generated succesfully");
		msg.open();
	}
}
