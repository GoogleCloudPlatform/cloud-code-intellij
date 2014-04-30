package ${packageName};

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;

import java.util.logging.Logger;

import javax.inject.Named;

/** An endpoint class we are exposing */
@Api(name = "${entityName}Endpoint", version = "v1", namespace = @ApiNamespace(ownerDomain = "${endpointOwnerDomain}", ownerName = "${endpointOwnerDomain}", packagePath=""))
public class ${entityType}Endpoint {

    // Make sure to add this endpoint to your web.xml file if this is a web application.

    private static final Logger LOG = Logger.getLogger(${entityType}Endpoint.class.getName());

    /**
     * This method gets the <code>${entityType}</code> object associated with the specified <code>id</code>.
     * @param id The id of the object to be returned.
     * @return The <code>${entityType}</code> associated with <code>id</code>.
     */
    @ApiMethod(name = "get${entityType}")
    public ${entityType} get${entityType}(@Named("id") Long id) {
        // Implement this function

        LOG.info("Calling get${entityType} method");
        return null;
    }

    /**
     * This inserts a new <code>${entityType}</code> object.
     * @param ${entityName} The object to be added.
     * @return The object to be added.
     */
    @ApiMethod(name = "insert${entityType}")
    public ${entityType} insert${entityType}(${entityType} ${entityName}) {
        // Implement this function

        LOG.info("Calling insert${entityType} method");
        return ${entityName};
    }
}