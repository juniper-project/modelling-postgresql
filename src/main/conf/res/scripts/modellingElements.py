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



    
juniper = selectedElements.get(0)
childs = [child for child in juniper.getCompositionChildren() if child.isStereotyped('JuniperIDE','SoftwareArchitectureModel')] 
for childSoft in childs :
    servers= [server for server in childSoft.getCompositionChildren() if server.isStereotyped('PostgreSQLModeler','PostgreSQLServer')]
    if len(servers) !=0 :
        interface=modelingSession.getModel().createInterface('IRelationalDatabase',childSoft)

for server in servers :
	
	operation = modelingSession.getModel().createOperation('getConnectionString',server)
	modelingSession.getModel().createIOParameter('database', modelingSession.getModel().getUmlTypes().getSTRING(), operation)
	modelingSession.getModel().createReturnParameter('database', modelingSession.getModel().getUmlTypes().getSTRING(), operation)
    
	getUser = modelingSession.getModel().createOperation('getUser',server)
	modelingSession.getModel().createReturnParameter('user', modelingSession.getModel().getUmlTypes().getSTRING(), getUser)
	getPassword = modelingSession.getModel().createOperation('getPassword',server)
	modelingSession.getModel().createReturnParameter('password', modelingSession.getModel().getUmlTypes().getSTRING(), getPassword)
    
	attributeUser=modelingSession.getModel().createAttribute('user', modelingSession.getModel().getUmlTypes().getSTRING(), server)
	attributeUser.setValue(server.getTagValue('PostgreSQLModeler','user'))
	attributePassword=modelingSession.getModel().createAttribute('password', modelingSession.getModel().getUmlTypes().getSTRING(), server)
	attributePassword.setValue(server.getTagValue('PostgreSQLModeler','password'))
	attributeIp=modelingSession.getModel().createAttribute('ip', modelingSession.getModel().getUmlTypes().getSTRING(), server)
	hosts = server.getRepresenting()
	if len(hosts) >1 :
		for host in hosts :
			if host.isStereotyped('PostgreSQLModeler','Master') :
				ip=host.getCluster().getTagValue('JuniperIDE','ip')
				attributeIp.setValue(ip)
	else :
		ip=hosts.get(0).getCluster().getTagValue('JuniperIDE','ip')
	 	attributeIp.setValue(ip)
	 	
	modelingSession.getModel().createInterfaceRealization(server,interface)
    
    

    