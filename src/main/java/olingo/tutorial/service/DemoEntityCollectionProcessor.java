/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package olingo.tutorial.service;

import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.processor.EntityCollectionProcessor;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.apache.olingo.server.api.uri.queryoption.CountOption;
import org.apache.olingo.server.api.uri.queryoption.SkipOption;
import org.apache.olingo.server.api.uri.queryoption.TopOption;

import olingo.tutorial.data.Storage;
import olingo.tutorial.util.Util;

/**
 * This class is invoked by the Olingo framework when the the OData service is invoked order to display a list/collection of data (entities).
 * This is the case if an EntitySet is requested by the user.
 * Such an example URL would be:
 * http://localhost:8080/ExampleService1/ExampleService1.svc/Products
 */
public class DemoEntityCollectionProcessor implements EntityCollectionProcessor {

    private OData odata;
    private ServiceMetadata serviceMetadata;
  
    private Storage storage;

    public DemoEntityCollectionProcessor(Storage storage) {
        this.storage = storage;
    }
  
    // our processor is initialized with the OData context object
    @Override
    public void init(OData odata, ServiceMetadata serviceMetadata) {
        this.odata = odata;
        this.serviceMetadata = serviceMetadata;
    }

    // this method is called, when the user fires a request to an EntitySet
    // in our example, the URL would be:
    // http://localhost:8080/DemoService/DemoService.svc/Products
    public void readEntityCollection(
            ODataRequest request, 
            ODataResponse response, 
            UriInfo uriInfo, 
            ContentType responseFormat) 
                    throws ODataApplicationException, SerializerException {

        EdmEntitySet responseEntitySet;
        EntityCollection responseEntityCollection;
        
        // 1st we have retrieve the requested EntitySet from the uriInfo object 
        // (representation of the parsed service URI)
        List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
        int segmentCount = resourcePaths.size();
        
        UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
        EdmEntitySet startEntitySet = uriResourceEntitySet.getEntitySet();
        
        // 2nd: fetch the data from backend for this request
        if (segmentCount == 1) { 
            responseEntitySet = startEntitySet;
        
            responseEntityCollection = storage.readEntitySetData(responseEntitySet);
            
        } else if (segmentCount == 2) {
            
            List<UriParameter> keyParams = uriResourceEntitySet.getKeyPredicates();
            Entity sourceEntity = storage.readEntityData(startEntitySet, keyParams);
            
            UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) resourcePaths.get(1);
            EdmNavigationProperty navigationProperty = uriResourceNavigation.getProperty();
            EdmEntityType targetEntityType = navigationProperty.getType();
            responseEntitySet = Util.getNavigationTargetEntitySet(startEntitySet, navigationProperty);
            
            responseEntityCollection = storage.getRelatedEntityCollection(sourceEntity, targetEntityType);
        } else {
            throw new ODataApplicationException("Not supported", 
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
        }
        
        // 3rd: create a serializer based on the requested format (json)
        ODataSerializer serializer = odata.createSerializer(responseFormat);
        
        // 4th: Now serialize the content: transform from the EntitySet object to InputStream
        EdmEntityType responseEntityType = responseEntitySet.getEntityType();
        ContextURL contextUrl = ContextURL.with().entitySet(responseEntitySet).build();
    
        // 5th query params
        EntityCollection finalEntityCollection = new EntityCollection();
        List<Entity> entityList = responseEntityCollection.getEntities();
        // count
        CountOption countOption = uriInfo.getCountOption();
        if (countOption != null) {
            if (countOption.getValue()) {
                finalEntityCollection.setCount(entityList.size());
            }
        }
        // skip
        SkipOption skipOption = uriInfo.getSkipOption();
        if (skipOption != null) {
            int skipNumber = skipOption.getValue();
            if (skipNumber >= 0) {
                if(skipNumber <= entityList.size()) {
                    entityList = entityList.subList(skipNumber, entityList.size());
                } else {
                    entityList.clear();
                }
            } else {
                throw new ODataApplicationException("Invalid value for $skip", 
                        HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
            }
        }
        // top
        TopOption topOption = uriInfo.getTopOption();
        if (topOption != null) {
            int topNumber = topOption.getValue();
            if (topNumber >= 0) {
                if (topNumber < entityList.size()) {
                    entityList = entityList.subList(0, topNumber);
                }
            } else {
                throw new ODataApplicationException("Invalid value for $top", 
                        HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
            }
        }
        
        finalEntityCollection.getEntities().addAll(entityList);
        
        final String id = request.getRawBaseUri() + "/" + responseEntitySet.getName();
        EntityCollectionSerializerOptions opts = EntityCollectionSerializerOptions.with()
                .id(id)
                .contextURL(contextUrl)
                .count(countOption)
                .build();
        SerializerResult serializedResult = serializer.entityCollection(
                serviceMetadata, responseEntityType, finalEntityCollection, opts);
    
        // Finally: configure the response object: set the body, headers and status code
        response.setContent(serializedResult.getContent());
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
  }

}
