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

from org.modelio.metamodel.uml.statik import Package
server = selectedElements.get(0)
serverName=server.getName();
software = server.getOwner() 
  
datamodel = modelingSession.getModel().createPackage(serverName+' Data Model', software,'PersistentProfile','DataModel')
datamodel.removeStereotypes('JavaDesigner','JavaPackage')
modelingSession.getModel().createDependency(server, datamodel, 'JuniperIDE', 'Stores')
diagram=modelingSession.getModel().createStaticDiagram('Data model diagram', datamodel, 'PostgreSQLModeler', 'PostgreSQLDataModelDiagram')
#style = Modelio.getInstance().getDiagramService().getStyle("dataDiagramStyle");
#diagramHandler = Modelio.getInstance().getDiagramService().getDiagramHandle(diagram);
#diagramHandler.getDiagramNode().setStyle(style);
#diagramHandler.save();
#diagramHandler.close();
createdElement=datamodel 