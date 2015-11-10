#
# Copyright 2014 Modeliosoft
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

def createTraceabilityLink(el, target):
 	modelingSession.getModel().createDependency(el, target, "ModelerModule", "trace")
 				
def isCreated(package,el):
	for database in package.getCompositionChildren() :
		for element in database.getCompositionChildren():
			dependencies = element.getDependsOnDependency()
			if len(dependencies)>0 :
				for dependency in dependencies :
					if dependency.isStereotyped('ModelerModule','trace') :
						if dependency.getDependsOn() == el :						
							return dependency.getImpacted()
	return None
 				
business = selectedElements.get(0)
software = business.getOwner()
datamodel = modelingSession.getModel().createPackage('Business datamodel', software,'PersistentProfile','DataModel')
database = modelingSession.getModel().createPackage('Business database', datamodel,'PostgreSQLModeler','PsqlDatabase')
database.addStereotype('PersistentProfile','DataModel')

for child in business.getCompositionChildren() : 
	if child.isStereotyped('PersistentProfile','Entity'):
		entity = isCreated(datamodel,child)
		if  entity == None :
			entity = modelingSession.getModel().createClass(child.getName(),database,  'PersistentProfile', 'Entity')
			createTraceabilityLink(entity,child)
		for attribut in  child.getOwnedAttribute() :
			attribute = modelingSession.getModel().createAttribute(attribut.getName(),attribut.getType(),entity)
			attribute.setMultiplicityMin(attribut.getMultiplicityMin())
			attribute.setMultiplicityMax(attribut.getMultiplicityMax())
			createTraceabilityLink(attribute, attribut)
			if attribut.isStereotyped('PersistentProfile','Identifier'):
				attribute.addStereotype('PersistentProfile','Identifier')
			else:
				attribute.addStereotype('PersistentProfile','PersistentProperty')
				
			#if attribut.isStereotyped('JuniperIDE','HorizontalPartitioning'):
				#shardingKey = modelingSession.getModel().createClass('ShardingKey', collection,  'MongoDBModeler', 'ShardingKey')
				#dependency = modelingSession.getModel().createDependency(shardingKey, attribute, 'MongoDBModeler','Sharding')
			#for dependency in attribut.getImpactedDependency() : 
			#if dependency.getImpacted().isStereotyped('JuniperIDE','vertical partitionning') :
				
		for associationEnd in child.getOwnedEnd() :
			element = isCreated(datamodel,associationEnd.getOppositeOwner().getOwner())
			if  element != None :
				aggregation= associationEnd.getAggregation()
				endName=associationEnd.getName()
				multiplicityMin = associationEnd.getMultiplicityMin()
				multiplicityMax = associationEnd.getMultiplicityMax()
				association = modelingSession.getModel().createAssociation(entity,element,element.getName())
				association.getEnd().get(1).setAggregation(aggregation)
				association.getEnd().get(1).setName(endName)
				association.getEnd().get(1).setMultiplicityMin(multiplicityMin)
				association.getEnd().get(1).setMultiplicityMax(multiplicityMax)
				
			else :
				element = modelingSession.getModel().createClass(associationEnd.getOppositeOwner().getOwner().getName(), database,  'PersistentProfile', 'Entity')
				createTraceabilityLink(element,associationEnd.getOppositeOwner().getOwner())
				aggregation= associationEnd.getAggregation()
				endName=associationEnd.getName()
				multiplicityMin = associationEnd.getMultiplicityMin()
				multiplicityMax = associationEnd.getMultiplicityMax()
				association = modelingSession.getModel().createAssociation(entity,element,element.getName())
				association.getEnd().get(1).setAggregation(aggregation)
				association.getEnd().get(1).setName(endName)
				association.getEnd().get(1).setMultiplicityMin(multiplicityMin)
				association.getEnd().get(1).setMultiplicityMax(multiplicityMax)					