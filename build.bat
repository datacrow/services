rd _classes /S /Q
del standard_services_pack.jar
call ant
del ../datacrow/services/standard_services_pack.jar
copy standard_services_pack.jar ..\datacrow\services\