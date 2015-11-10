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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.modelio.api.modelio.Modelio;
import org.modelio.api.module.IModule;
import org.modelio.api.module.IModuleAPIConfiguration;
import org.modelio.api.module.IPeerModule;
import org.modelio.api.module.commands.DefaultModuleCommandHandler;
import org.modelio.metamodel.uml.infrastructure.ModelElement;
import org.modelio.metamodel.uml.infrastructure.ModelTree;
import org.modelio.metamodel.uml.statik.BindableInstance;
import org.modelio.metamodel.uml.statik.Instance;
import org.modelio.metamodel.uml.statik.NameSpace;
import org.modelio.metamodel.uml.statik.Package;
import org.modelio.vcore.smkernel.mapi.MObject;

//import org.modelio.module.javadesigner.api.JavaDesignerParameters;
public class GenerateDeploymentScriptCommand extends
		DefaultModuleCommandHandler {

	@Override
	public boolean accept(List<MObject> selectedElements, IModule module) {

		return super.accept(selectedElements, module) && selectedElements.size() == 1
				&& (((ModelElement) selectedElements.get(0)).isStereotyped(
						"JuniperIDE", "JUNIPERModel"));
	}

	@Override
	public void actionPerformed(List<MObject> selectedElements, IModule module) {

		Path modulePath = module.getConfiguration().getModuleResourcesPath();
		IPeerModule jdesigner = Modelio.getInstance().getModuleService()
				.getPeerModule("JavaDesigner");

		IModuleAPIConfiguration config = jdesigner.getConfiguration();

		String fileLocation = config.getProjectSpacePath().toString()
				+ "\\deploymentScripts\\" + selectedElements.get(0).getName();
		new File(fileLocation + "\\").mkdirs();

		List<BindableInstance> slaves = new ArrayList<BindableInstance>();
		Map<NameSpace, String> masters = new HashMap<NameSpace, String>();
		Map<String, List<String>> compositionReplicationDBs = new HashMap<String, List<String>>();
		Map<String, Map<String, String>> infoReplicationDB = new HashMap<String, Map<String, String>>();
		List<Map<String, String>> infoSimpleDBs = new ArrayList<Map<String, String>>();

		List<ModelTree> packag = ((Package) selectedElements.get(0))
				.getOwnedElement();

		for (ModelTree pack : packag) {
			if (pack.isStereotyped("JuniperIDE", "HardwarePlatformModel")) {

				List<Instance> nodes = ((Package) pack).getDeclared();

				for (Instance node : nodes) {

					List<BindableInstance> els = node.getPart();

					for (BindableInstance el : els) {
						if (el.isStereotyped("JuniperIDE", "ProgramInstance")) {
							if (el.getBase().isStereotyped("PostgreSQLModeler",
									"PostgreSQLServer")) {
								if (el.isStereotyped("PostgreSQLModeler",
										"Master")) {

									masters.put(el.getBase(), el.getCluster()
											.getTagValue("JuniperIDE", "ip"));

								} else if (el.isStereotyped(
										"PostgreSQLModeler", "StandBy")) {

									slaves.add(el);
								} else {

									Map<String, String> infoSimpleDB = new HashMap<String, String>();
									infoSimpleDB.put("ip", el.getCluster()
											.getTagValue("JuniperIDE", "ip"));
									infoSimpleDB
											.put("user",
													el.getBase()
															.getTagValue(
																	"PostgreSQLModeler",
																	"user"));
									infoSimpleDB.put(
											"password",
											el.getBase().getTagValue(
													"PostgreSQLModeler",
													"password"));
									infoSimpleDBs.add(infoSimpleDB);
								}
							}
						}
					}
				}
			}
		}

		for (Entry<NameSpace, String> master : masters.entrySet()) {

			compositionReplicationDBs.put(master.getValue(),
					new ArrayList<String>());
			Map<String, String> info = new HashMap<String, String>();
			info.put("user",
					master.getKey().getTagValue("PostgreSQLModeler", "user"));
			info.put("password",
					master.getKey()
							.getTagValue("PostgreSQLModeler", "password"));
			infoReplicationDB.put(master.getValue(), info);
		}

		for (BindableInstance slave : slaves) {

			compositionReplicationDBs.get(masters.get(slave.getBase())).add(
					slave.getCluster().getTagValue("JuniperIDE", "ip"));
		}

		editReplicationDeploymentScript(compositionReplicationDBs,
				infoReplicationDB, infoSimpleDBs, fileLocation, modulePath);

	}

	private void editSimpleDeploymentScript(BufferedWriter bwScript, String ip,
			String fileLocation, Path modulePath) {

		try {

			new File(fileLocation + "\\simplePostgreSQL" + ip).mkdirs();

			File to = new File(fileLocation + "\\simplePostgreSQL" + ip
					+ "\\installPostgreSQL94.sh");

			File from = new File(modulePath
					+ "/res/postgresql/installPostgreSQL94.sh");

			Files.copy(from.toPath(), to.toPath());

			File to2 = new File(fileLocation + "\\simplePostgreSQL" + ip
					+ "\\script_needs.sh");
			File from2 = new File(modulePath
					+ "/res/postgresql/script_needs.sh");
			Files.copy(from2.toPath(), to2.toPath());

			File to3 = new File(fileLocation + "\\simplePostgreSQL" + ip
					+ "\\script_postgres94.sh");
			File from3 = new File(modulePath
					+ "/res/postgresql/script_postgres94.sh");
			Files.copy(from3.toPath(), to3.toPath());

			File to4 = new File(fileLocation + "\\simplePostgreSQL" + ip
					+ "\\pg_hba.conf");
			File from4 = new File(modulePath
					+ "/res/postgresql/pg_hba_simple.conf");
			Files.copy(from4.toPath(), to4.toPath());

			File to5 = new File(fileLocation + "\\simplePostgreSQL" + ip
					+ "\\postgresql.conf");
			File from5 = new File(modulePath
					+ "/res/postgresql/postgresql_simple.conf");
			Files.copy(from5.toPath(), to5.toPath());

			bwScript.write("scp -r simplePostgreSQL"
					+ ip
					+ " root@"
					+ ip
					+ ":.\n ssh root@"
					+ ip
					+ " \" cd simplePostgreSQL"
					+ ip
					+ "; bash ./script_needs.sh ; bash ./script_postgres94.sh; bash ./installPostgreSQL94.sh \"\n");

		} catch (IOException e) {

			e.printStackTrace();
		}

	}

	/**
	 * edit the deployment file
	 * 
	 * @param slaves
	 *            : list of slave's ip
	 * @param masters
	 *            : list of master's ip
	 */
	public void editReplicationDeploymentScript(
			Map<String, List<String>> compositionDB,
			Map<String, Map<String, String>> infoDB,
			List<Map<String, String>> infoSimpleDBs, String fileLocation,
			Path modulePath) {

		String ipsPghbaMaster = "";
		try {

			BufferedWriter bwScript = new BufferedWriter(new FileWriter(
					fileLocation + "\\deploymentScript.sh"));

			for (Map<String, String> infoSimpleDB : infoSimpleDBs) {
				editSimpleDeploymentScript(bwScript, infoSimpleDB.get("ip"),
						fileLocation, modulePath);

			}

			String filePgHba = readFile(modulePath
					+ "/res/postgresql/pg_hba.conf");
			String filePostgresql = readFile(modulePath
					+ "/res/postgresql/postgresql.conf");

			for (Entry<String, List<String>> db : compositionDB.entrySet()) {

				String masterIP = db.getKey();
				String filePgHbaMaster = filePgHba;
				String filePostgresqlMaster = filePostgresql;
				String slavesIp = "";
				for (String slaveIP : db.getValue()) {

					slavesIp += " " + slaveIP;
					ipsPghbaMaster += "host       replication "
							+ infoDB.get(masterIP).get("user") + "  " + slaveIP
							+ "/32  md5\n";
					String filePgHbaSlave = filePgHba;
					filePgHbaSlave = filePgHbaSlave.replace(
							"host       replication  rep  IP_Master/32  md5",
							"host       replication "
									+ infoDB.get(masterIP).get("user") + "  "
									+ masterIP + "/32  md5\n");
					String fileRecovery = "standby_mode = 'on'\nprimary_conninfo = 'host="
							+ masterIP
							+ " port=5432 user="
							+ infoDB.get(masterIP).get("user")
							+ " password="
							+ infoDB.get(masterIP).get("password")
							+ "'\ntrigger_file = '/tmp/postgresql.trigger.5432'";
					String filePostgresqlSlave = filePostgresql;

					new File(fileLocation + "\\slave" + slaveIP).mkdirs();

					File to = new File(fileLocation + "\\slave" + slaveIP
							+ "\\installReplicationSlavePostgreSQL.sh");
					File from = new File(
							modulePath
									+ "/res/postgresql/installReplicationSlavePostgreSQL.sh");
					Files.copy(from.toPath(), to.toPath());

					File to1 = new File(fileLocation + "\\slave" + slaveIP
							+ "\\id_rsa.pub");
					File from1 = new File(modulePath
							+ "/res/postgresql/id_rsa.pub");
					Files.copy(from1.toPath(), to1.toPath());

					File to2 = new File(fileLocation + "\\slave" + slaveIP
							+ "\\id_rsa");
					File from2 = new File(modulePath + "/res/postgresql/id_rsa");
					Files.copy(from2.toPath(), to2.toPath());

					File to3 = new File(fileLocation + "\\slave" + slaveIP
							+ "\\script_needs.sh");
					File from3 = new File(modulePath
							+ "/res/postgresql/script_needs.sh");
					Files.copy(from3.toPath(), to3.toPath());

					File to5 = new File(fileLocation + "\\slave" + slaveIP
							+ "\\script_postgres94.sh");
					File from5 = new File(modulePath
							+ "/res/postgresql/script_postgres94.sh");
					Files.copy(from5.toPath(), to5.toPath());

					File to4 = new File(fileLocation + "\\slave" + slaveIP
							+ "\\script_ssh.sh");
					File from4 = new File(modulePath
							+ "/res/postgresql/script_ssh.sh");
					Files.copy(from4.toPath(), to4.toPath());

					BufferedWriter bwPgHba = new BufferedWriter(new FileWriter(
							fileLocation + "\\slave" + slaveIP
									+ "\\pg_hba.conf"));
					bwPgHba.write(filePgHbaSlave);

					BufferedWriter bwpostgres = new BufferedWriter(
							new FileWriter(fileLocation + "\\slave" + slaveIP
									+ "/postgresql.conf"));
					bwpostgres.write(filePostgresqlSlave);
					BufferedWriter bwRecovery = new BufferedWriter(
							new FileWriter(fileLocation + "\\slave" + slaveIP
									+ "\\recovery.conf"));
					bwRecovery.write(fileRecovery);

					bwScript.write("scp -r slave"
							+ slaveIP
							+ " root@"
							+ slaveIP
							+ ":.\n ssh root@"
							+ slaveIP
							+ " \"cd slave"
							+ slaveIP
							+ "; bash ./script_needs.sh ; bash ./script_postgres94.sh; bash ./installReplicationSlavePostgreSQL.sh\"\n");

					bwPgHba.close();
					bwpostgres.close();
					bwRecovery.close();

				}

				String pghbaMaster = filePgHbaMaster.replace(
						"host       replication  rep  IP_Master/32  md5",
						ipsPghbaMaster);
				String postgresqlMaster = filePostgresqlMaster;

				postgresqlMaster = postgresqlMaster.replace(
						"max_wal_senders = 1", "max_wal_senders = "
								+ db.getValue().size());

				new File(fileLocation + "\\master" + masterIP).mkdirs();

				File toDirMaster = new File(fileLocation + "\\master"
						+ masterIP + "\\installReplicationMasterPostgreSQL.sh");
				File fromLocale = new File(
						modulePath
								+ "/res/postgresql/installReplicationMasterPostgreSQL.sh");
				Files.copy(fromLocale.toPath(), toDirMaster.toPath());

				File toDirMaster2 = new File(fileLocation + "\\master"
						+ masterIP + "\\id_rsa.pub");
				File fromLocale2 = new File(modulePath
						+ "/res/postgresql/id_rsa.pub");
				Files.copy(fromLocale2.toPath(), toDirMaster2.toPath());

				File toDirMaster1 = new File(fileLocation + "\\master"
						+ masterIP + "\\id_rsa");
				File fromLocale1 = new File(modulePath
						+ "/res/postgresql/id_rsa");
				Files.copy(fromLocale1.toPath(), toDirMaster1.toPath());

				File toDirMaster3 = new File(fileLocation + "\\master"
						+ masterIP + "\\script_needs.sh");
				File fromLocale3 = new File(modulePath
						+ "/res/postgresql/script_needs.sh");
				Files.copy(fromLocale3.toPath(), toDirMaster3.toPath());

				File toDirMaster5 = new File(fileLocation + "\\master"
						+ masterIP + "\\script_postgres94.sh");
				File fromLocale5 = new File(modulePath
						+ "/res/postgresql/script_postgres94.sh");
				Files.copy(fromLocale5.toPath(), toDirMaster5.toPath());

				File toDirMaster4 = new File(fileLocation + "\\master"
						+ masterIP + "\\script_ssh.sh");
				File fromLocale4 = new File(modulePath
						+ "/res/postgresql/script_ssh.sh");
				Files.copy(fromLocale4.toPath(), toDirMaster4.toPath());

				BufferedWriter bwPgHbaMaster = new BufferedWriter(
						new FileWriter(fileLocation + "\\master" + masterIP
								+ "\\pg_hba.conf"));
				bwPgHbaMaster.write(pghbaMaster);

				BufferedWriter bwPostgresqlMaster = new BufferedWriter(
						new FileWriter(fileLocation + "\\master" + masterIP
								+ "\\postgresql.conf"));
				bwPostgresqlMaster.write(postgresqlMaster);

				bwScript.write("scp -r master"
						+ masterIP
						+ " root@"
						+ masterIP
						+ ":.\n ssh root@"
						+ masterIP
						+ " \"cd master"
						+ masterIP
						+ "; bash ./script_needs.sh ; bash ./script_postgres94.sh; bash ./installReplicationMasterPostgreSQL.sh "
						+ infoDB.get(masterIP).get("user") + " "
						+ infoDB.get(masterIP).get("password") + slavesIp
						+ "\"\n");

				bwPgHbaMaster.close();
				bwPostgresqlMaster.close();

			}

			bwScript.close();

		} catch (IOException e) {

			e.printStackTrace();
		}

		messageBox();
	}

	public String readFile(String path) throws IOException {

		String line = "";

		String fic = "";

		BufferedReader br = new BufferedReader(new FileReader(path));

		while ((line = br.readLine()) != null) {

			fic += line + "\n";
		}

		br.close();

		return fic;

	}
	
	private void messageBox() {

		MessageBox msg = new MessageBox(Display.getCurrent().getActiveShell(),
				SWT.ICON_INFORMATION | SWT.OK);
		msg.setMessage("Code generated succesfully");
		msg.open();
	}

}
