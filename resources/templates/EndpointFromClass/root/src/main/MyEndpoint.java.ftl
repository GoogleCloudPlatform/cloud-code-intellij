package ${packageName};

<#if isObjectified>
  <#include "ObjectifyImports.java.ftl">
<#else>
  <#include "StubImports.java.ftl">
</#if>


<#if isObjectified>
/**
 * WARNING: This generated code is intended as a sample or starting point for using a
 * Google Cloud Endpoints RESTful API with an Objectify entity. It provides no data access
 * restrictions and no data validation.
 *
 * DO NOT deploy this code unchanged as part of a real application to real users.
 */
<#else>
/** An endpoint class we are exposing */
</#if>
@Api(
  name = "${entityName}Api",
  version = "v1",
  resource = "${entityName}",
  namespace = @ApiNamespace(
    ownerDomain = "${endpointOwnerDomain}",
    ownerName = "${endpointOwnerDomain}",
    packagePath=""
  )
)
public class ${entityType}Endpoint {

  private static final Logger logger = Logger.getLogger(${entityType}Endpoint.class.getName());

  <#if isObjectified>
    <#include "ObjectifyImpl.java.ftl">
  <#else>
    <#include "StubImpl.java.ftl">
  </#if>
}