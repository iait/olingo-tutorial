package olingo.tutorial.service;

import java.util.Locale;
import java.util.Map;

import org.apache.olingo.commons.api.data.Parameter;
import org.apache.olingo.commons.api.edm.EdmAction;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.deserializer.DeserializerResult;
import org.apache.olingo.server.api.deserializer.ODataDeserializer;
import org.apache.olingo.server.api.processor.ActionVoidProcessor;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResourceAction;

import olingo.tutorial.data.Storage;

public class DemoActionVoidProcessor implements ActionVoidProcessor {

    private Storage storage;
    private OData odata;
    
    public DemoActionVoidProcessor(Storage storage) {
        this.storage = storage;
    }
    
    @Override
    public void init(OData odata, ServiceMetadata serviceMetadata) {
        this.odata = odata;
    }

    @Override
    public void processActionVoid(
            ODataRequest request, 
            ODataResponse response, 
            UriInfo uriInfo,
            ContentType requestFormat) 
                    throws ODataApplicationException, ODataLibraryException {

        UriResourceAction uriActionResource = (UriResourceAction) uriInfo.getUriResourceParts().get(0);
        if (uriActionResource.getActionImport().getFullQualifiedName().equals(DemoEdmProvider.ACTION_RESET_FQN)) {
            EdmAction action = uriActionResource.getAction();
            
            // Los parámetros en una action están en el body, hay que deserializarlo
            ODataDeserializer deserializer = odata.createDeserializer(requestFormat);
            DeserializerResult deserializerResult = deserializer.actionParameters(request.getBody(), action);
            Map<String, Parameter> actionParameters = deserializerResult.getActionParameters();
            Parameter param = actionParameters.get(DemoEdmProvider.PARAMETER_AMOUNT);
            if (param == null) {
                storage.resetDataSet();
            } else {
                Integer amount = (Integer) param.asPrimitive();
                storage.resetDataSet(amount);
            }
            
            response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
            
        } else {
            throw new ODataApplicationException("Not supported", 
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
        }
        
    }

}
