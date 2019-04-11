package olingo.tutorial.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.batch.BatchFacade;
import org.apache.olingo.server.api.deserializer.batch.BatchOptions;
import org.apache.olingo.server.api.deserializer.batch.BatchRequestPart;
import org.apache.olingo.server.api.deserializer.batch.ODataResponsePart;
import org.apache.olingo.server.api.processor.BatchProcessor;

import olingo.tutorial.data.Storage;

public class DemoBatchProcessor implements BatchProcessor {
    
    private OData odata;
    private Storage storage;

    public DemoBatchProcessor(Storage storage) {
        this.storage = storage;
    }

    @Override
        public void init(OData odata, ServiceMetadata serviceMetadata) {
        this.odata = odata;
    }

    @Override
    public void processBatch(BatchFacade facade, ODataRequest request, ODataResponse response)
            throws ODataApplicationException, ODataLibraryException {
        
        // 1 - Extract the boundary
        String boundary = facade.extractBoundaryFromContentType(request.getHeader(HttpHeader.CONTENT_TYPE));
        
        // 2 - Parse batch parts
        BatchOptions options = BatchOptions.with()
                .rawBaseUri(request.getRawBaseUri())
                .rawServiceResolutionUri(request.getRawServiceResolutionUri())
                .build();
        List<BatchRequestPart> batchParts = odata.createFixedFormatDeserializer()
                .parseBatchRequest(request.getBody(), boundary, options);
        
        // 3 - Execute batch parts
        List<ODataResponsePart> responses = new ArrayList<>();
        for (BatchRequestPart batchRequestPart : batchParts) {
            ODataResponsePart partResponse = facade.handleBatchRequest(batchRequestPart);
            responses.add(partResponse);
        }
        
        // 4 - Serialize the response
        String newBoundary = "batch_" + UUID.randomUUID().toString();
        InputStream serializedResponse = odata.createFixedFormatSerializer()
                .batchResponse(responses, newBoundary);
        response.setHeader(HttpHeader.CONTENT_TYPE, ContentType.MULTIPART_MIXED + ";boundary=" + newBoundary);
        response.setContent(serializedResponse);
        response.setStatusCode(HttpStatusCode.ACCEPTED.getStatusCode());
    }

    @Override
    public ODataResponsePart processChangeSet(BatchFacade facade, List<ODataRequest> requests)
            throws ODataApplicationException, ODataLibraryException {
        List<ODataResponse> responses = new ArrayList<>();
        
        try {
            storage.beginTransaction();
            
            for (ODataRequest request : requests) {
                ODataResponse response = facade.handleODataRequest(request);
                int statusCode = response.getStatusCode();
                if (statusCode < 400) {
                    responses.add(response);
                } else {
                    storage.rollbackTranscation();
                    return new ODataResponsePart(response, false);
                }
            }
            storage.commitTransaction();
            return new ODataResponsePart(responses, true);
        } catch (Exception e) {
            storage.rollbackTranscation();
            throw e;
        }
    }

}


