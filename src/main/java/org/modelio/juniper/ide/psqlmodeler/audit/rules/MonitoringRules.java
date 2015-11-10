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
package org.modelio.juniper.ide.psqlmodeler.audit.rules;

import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.modelio.metamodel.uml.infrastructure.Dependency;
import org.modelio.metamodel.uml.infrastructure.ModelElement;
import org.modelio.metamodel.uml.statik.AggregationKind;
import org.modelio.metamodel.uml.statik.AssociationEnd;
import org.modelio.metamodel.uml.statik.Attribute;
import org.modelio.metamodel.uml.statik.Classifier;
import org.modelio.metamodel.uml.statik.Instance;
import org.modelio.metamodel.uml.statik.Package;
import org.modelio.metamodel.uml.statik.NameSpace;
import org.modelio.modelvalidator.engine.IModelValidator;
import org.modelio.modelvalidator.engine.impl.AbstractSimplifiedRule;
import org.modelio.temp.audit.service.AuditSeverity;
import org.modelio.vcore.smkernel.mapi.MObject;

public class MonitoringRules {

	public static void init(IModelValidator modelValidator) {

		modelValidator.registerRule(
				new AbstractSimplifiedRule("POSTGRES_004a",
						org.modelio.metamodel.uml.statik.Class.class) {
					@Override
					public boolean check(MObject obj, List<Object> linked) {
						MObject el = obj;
						if (((ModelElement) el).isStereotyped(
								"PostgreSQLModeler", "PostgreSQLServer")) {
							if (((ModelElement) el).getTagValue(
									"PostgreSQLModeler", "user") == null
									|| ((ModelElement) el).getTagValue(
											"PostgreSQLModeler", "password") == null) {
								return false;
							}
						}
						return true;
					}
				}, AuditSeverity.AuditError,
				"PostgreSQL Server tag values should be filled.", "help");

		modelValidator
				.registerRule(
						new AbstractSimplifiedRule("POSTGRES_004b",
								org.modelio.metamodel.uml.statik.Class.class) {
							@Override
							public boolean check(MObject obj,
									List<Object> linked) {
								MObject el = obj;
								if (((ModelElement) el)
										.isStereotyped("PostgreSQLModeler",
												"PostgreSQLServer")) {
									if (((NameSpace) el).getRepresenting()
											.size() < 1) {

										return false;
									}
								}
								return true;
							}
						},
						AuditSeverity.AuditError,
						"PostgreSQL server should have either a single instance or a replicated instances",
						"help");

		modelValidator
				.registerRule(
						new AbstractSimplifiedRule("POSTGRES_004c",
								org.modelio.metamodel.uml.statik.Class.class) {
							@Override
							public boolean check(MObject obj,
									List<Object> linked) {
								MObject el = obj;
								if (((ModelElement) el)
										.isStereotyped("PostgreSQLModeler",
												"PostgreSQLServer")) {

									if (((NameSpace) el).getRepresenting()
											.size() > 1) {
										int nbMaster = 0;
										int nbStandby = 0;
										for (Instance instance : ((NameSpace) el)
												.getRepresenting()) {
											if (instance.isStereotyped(
													"PostgreSQLModeler",
													"Master")) {
												nbMaster++;
											} else if (instance.isStereotyped(
													"PostgreSQLModeler",
													"StandBy")) {
												nbStandby++;
											}
										}

										return nbMaster == 1 && nbStandby >= 1;

									}

								}
								return true;
							}
						},
						AuditSeverity.AuditError,
						"PostgreSQL server should have exactly one master and at least one standby on replicated mode",
						"help");

		modelValidator
				.registerRule(
						new AbstractSimplifiedRule(
								"POSTGRES_004d",
								org.modelio.metamodel.uml.statik.AssociationEnd.class) {
							@Override
							public boolean check(MObject obj,
									List<Object> linked) {
								ModelElement el = (ModelElement) obj;
								if (el.isStereotyped("PostgreSQLModeler",
										"PostgreSQLServer")) {
									EList<Dependency> dependencies = el
											.getDependsOnDependency();
									if (dependencies == null) {
										return false;
									} else {
										for (Dependency dependency : dependencies) {
											if (dependency.isStereotyped(
													"JuniperIDE", "Stores")) {
												return true;
											}
										}
										return false;
									}
								}
								return true;
							}
						},
						AuditSeverity.AuditWarning,
						"PostgreSQLServer is not associated with a data schema",
						"help");

		modelValidator
				.registerRule(
						new AbstractSimplifiedRule(
								"POSTGRES_005",
								org.modelio.metamodel.uml.statik.BindableInstance.class) {
							@Override
							public boolean check(MObject obj,
									List<Object> linked) {
								MObject el = obj;
								if (((ModelElement) el).isStereotyped(
										"PostgreSQLModeler", "Master")) {
									if (!(((Instance) el).getBase()
											.isStereotyped("PostgreSQLModeler",
													"PostgreSQLServer"))) {
										return false;
									}
								} else if (((ModelElement) el).isStereotyped(
										"PostgreSQLModeler", "StandBy")) {
									if (!(((Instance) el).getBase()
											.isStereotyped("PostgreSQLModeler",
													"PostgreSQLServer"))) {
										return false;
									}
								}
								return true;
							}
						},
						AuditSeverity.AuditError,
						"Master or Slave instances should have a PostgreSQL server as base",
						"help");

		modelValidator.registerRule(new AbstractSimplifiedRule("POSTGRES_006a",
				org.modelio.metamodel.uml.statik.Class.class) {
			@Override
			public boolean check(MObject obj, List<Object> linked) {
				MObject el = obj;
				if (((ModelElement) el).isStereotyped("PersistentProfile",
						"Entity")) {
					if (((Classifier) el).getOwnedAttribute().size() == 0 && ((Classifier) el).getOwnedEnd().size()<1) {
						return false;
					}

				}
				return true;
			}
		}, AuditSeverity.AuditWarning, "Entities should have attribute(s) or/and association(s)",
				"help");

		modelValidator.registerRule(
				new AbstractSimplifiedRule("POSTGRES_006b",
						org.modelio.metamodel.uml.statik.Class.class) {
					@Override
					public boolean check(MObject obj, List<Object> linked) {
						MObject el = obj;
						if (((ModelElement) el).isStereotyped(
								"PersistentProfile", "Entity")
								&& !((ModelElement) el).isStereotyped(
										"MongoDBModeler", "Document")) {

							if (!((NameSpace) el).getOwner().isStereotyped(
									"PostgreSQLModeler", "PsqlDatabase")) {
								return false;
							}

						}
						return true;
					}
				}, AuditSeverity.AuditWarning,
				"Entities should be in Database package", "help");

		modelValidator.registerRule(
				new AbstractSimplifiedRule("POSTGRES_006c",
						org.modelio.metamodel.uml.statik.AssociationEnd.class) {
					@Override
					public boolean check(MObject obj, List<Object> linked) {
						AssociationEnd el = (AssociationEnd) obj;
						if (el.getAggregation().equals(
								AggregationKind.KINDISCOMPOSITION)) {
							if (el.isStereotyped("PostgreSQLModeler", "Xml")
									|| el.isStereotyped("PostgreSQLModeler",
											"Json")
									|| el.isStereotyped("PostgreSQLModeler",
											"Hstore")) {
								return true;

							}

							return false;
						}
						return true;
					}
				},
				AuditSeverity.AuditWarning,
				"Composition associationEnds that are not stereotyped Xml, Json or Hstore will be ignored",
				"help");

		modelValidator.registerRule(
				new AbstractSimplifiedRule("POSTGRES_006d",
						org.modelio.metamodel.uml.statik.AssociationEnd.class) {
					@Override
					public boolean check(MObject obj, List<Object> linked) {
						AssociationEnd el = (AssociationEnd) obj;
						if (el.getAggregation().equals(
								AggregationKind.KINDISASSOCIATION)) {
							for(Attribute attribute : el.getOpposite().getOwner().getOwnedAttribute()){
								if(attribute.isStereotyped("PersistentProfile", "Identifier")){
									return true;
								}
							}

							return false;
						}
						return true;
					}
				}, AuditSeverity.AuditWarning,
				"AssociationEnd should pointing an Entity with primary key ", "help");

		modelValidator.registerRule(
				new AbstractSimplifiedRule("POSTGRES_007a",
						org.modelio.metamodel.uml.statik.Package.class) {
					@Override
					public boolean check(MObject obj, List<Object> linked) {
						ModelElement el = (ModelElement) obj;
						if (el.isStereotyped("PostgreSQLModeler", "PsqlDatabase")) {
							if (!el.isStereotyped("PersistentProfile",
									"DataModel")) {
								return false;
							}
						}
						return true;
					}
				}, AuditSeverity.AuditWarning,
				"The package Database should be stereotyped DataModel", "help");

		modelValidator.registerRule(
				new AbstractSimplifiedRule("POSTGRES_007b",
						org.modelio.metamodel.uml.statik.Package.class) {
					@Override
					public boolean check(MObject obj, List<Object> linked) {
						ModelElement el = (ModelElement) obj;
						if (el.isStereotyped("PostgreSQLModeler", "PsqlDatabase")) {
							if ((!((NameSpace) el).getOwner().isStereotyped(
									"PersistentProfile", "DataModel"))
									|| ((NameSpace) el).getOwner()
											.isStereotyped("PostgreSQLModeler",
													"PsqlDatabase")) {
								return false;
							}
						}
						return true;
					}
				}, AuditSeverity.AuditWarning,
				"The package Database should be in package DataModel", "help");

		modelValidator.registerRule(new AbstractSimplifiedRule("POSTGRES_007c",
				org.modelio.metamodel.uml.statik.Package.class) {
			@Override
			public boolean check(MObject obj, List<Object> linked) {
				ModelElement el = (ModelElement) obj;
				if (el.isStereotyped("PostgreSQLModeler", "PsqlDatabase")) {
					if (el.getCompositionChildren().isEmpty()) {
						return false;
					}
				}
				return true;
			}
		}, AuditSeverity.AuditWarning, "The package Database is empty", "help");

		modelValidator.registerRule(
				new AbstractSimplifiedRule("POSTGRES_007d",
						org.modelio.metamodel.uml.statik.Package.class) {
					@SuppressWarnings("unchecked")
					@Override
					public boolean check(MObject obj, List<Object> linked) {
						ModelElement el = (ModelElement) obj;
						if (el.isStereotyped("PostgreSQLModeler", "PsqlDatabase")) {

							List<MObject> childs = (List<MObject>) el
									.getCompositionChildren();
							for (MObject child : childs) {
								if (child instanceof Package)
									return false;
							}
						}
						return true;
					}
				}, AuditSeverity.AuditWarning,
				"The Database package should not contain package", "help");
	}
}
