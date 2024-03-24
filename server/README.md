 


#package local
call mvn clean compile install -Denv=local -Dmaven.test.skip=true  

#package sit
call mvn clean compile install -Denv=sit -Dmaven.test.skip=true  
call mvn clean compile install -Denv=sit -Dmaven.test.skip=true  
#package uat
call mvn clean compile install -Denv=uat -Dmaven.test.skip=true  
call mvn clean compile install -Denv=uat -Dmaven.test.skip=true  
#package prod
call mvn clean compile install -Denv=prod -Dmaven.test.skip=true
call mvn clean compile install -Denv=prod -Dmaven.test.skip=true

 