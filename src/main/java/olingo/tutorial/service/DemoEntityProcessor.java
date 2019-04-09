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

import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.ContextURL.Suffix;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.deserializer.DeserializerException;
import org.apache.olingo.server.api.deserializer.ODataDeserializer;
import org.apache.olingo.server.api.processor.EntityProcessor;
import org.apache.olingo.server.api.processor.MediaEntityProcessor;
import org.apache.olingo.server.api.serializer.EntitySerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceFunction;
import org.apache.olingo.server.api.uri.UriResourceNavigation;

import olingo.tutorial.data.Storage;
import olingo.tutorial.util.Util;

public class DemoEntityProcessor implements EntityProcessor, MediaEntityProcessor {

    private OData odata;
    private ServiceMetadata serviceMetadata;
    private Storage storage;

    public DemoEntityProcessor(Storage storage) {
        this.storage = storage;
    }

    public void init(OData odata, ServiceMetadata serviceMetadata) {
        this.odata = odata;
        this.serviceMetadata = serviceMetadata;
    }

    @Override
    public void readEntity(
            ODataRequest request, 
            ODataResponse response, 
            UriInfo uriInfo, 
            ContentType responseFormat)
                    throws ODataApplicationException, SerializerException {

        UriResource firstResourceSegment = uriInfo.getUriResourceParts().get(0);

        if (firstResourceSegment instanceof UriResourceEntitySet) {
            readEntityInternal(request, response, uriInfo, responseFormat);
        } else if (firstResourceSegment instanceof UriResourceFunction) {
            readFunctionImport(request, response, uriInfo, responseFormat);
        } else {
            throw new ODataApplicationException("Not implemented",
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
        }
    }

    private void readEntityInternal(
            ODataRequest request, 
            ODataResponse response, 
            UriInfo uriInfo, 
            ContentType responseFormat)
                    throws ODataApplicationException, SerializerException {

        EdmEntitySet responseEntitySet;
        Entity responseEntity;

        // 1. analyze resource uri
        List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
        int segmentCount = resourcePaths.size();

        UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
        EdmEntitySet startEntitySet = uriResourceEntitySet.getEntitySet();
        
        List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
        Entity sourceEntity = storage.readEntityData(startEntitySet, keyPredicates);

        // 2. retrieve the data from backend
        if (segmentCount == 1) {
            responseEntitySet = startEntitySet;
            responseEntity = sourceEntity;
            
        } else if (segmentCount == 2) {
            
            UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) resourcePaths.get(1);
            EdmNavigationProperty navigationProperty = uriResourceNavigation.getProperty();
            responseEntitySet = Util.getNavigationTargetEntitySet(startEntitySet, navigationProperty);
            EdmEntityType targetEntityType = navigationProperty.getType();
            
            EntityCollection relatedEntities = storage.getRelatedEntityCollection(sourceEntity, targetEntityType);
            assert relatedEntities.getCount() == 1;
            responseEntity = relatedEntities.getEntities().get(0);
            
        } else {
            throw new ODataApplicationException("Not supported", 
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
        }

        // 3. serialize
        EdmEntityType responseEntityType = responseEntitySet.getEntityType();

        ContextURL contextUrl = ContextURL.with().entitySet(responseEntitySet).suffix(ContextURL.Suffix.ENTITY).build();
        // expand and select currently not supported
        EntitySerializerOptions options = EntitySerializerOptions.with().contextURL(contextUrl).build();

        ODataSerializer serializer = this.odata.createSerializer(responseFormat);
        SerializerResult serializerResult = serializer.entity(serviceMetadata, responseEntityType, responseEntity, options);
        InputStream entityStream = serializerResult.getContent();

        //4. configure the response object
        response.setContent(entityStream);
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    }

    private void readFunctionImport(
            ODataRequest request, 
            ODataResponse response, 
            UriInfo uriInfo, 
            ContentType responseFormat) 
                    throws ODataApplicationException, SerializerException {
        
        UriResourceFunction uriResourseFunction = (UriResourceFunction) uriInfo.getUriResourceParts().get(0);
        Entity entity = storage.readFunctionImportEntity(uriResourseFunction, serviceMetadata);
        
        EdmEntityType edmType = (EdmEntityType) uriResourseFunction.getFunction().getReturnType().getType();
        ContextURL contextURL = ContextURL.with().type(edmType).build();
        EntitySerializerOptions serializerOptions = EntitySerializerOptions.with().contextURL(contextURL).build();
        ODataSerializer serializer = odata.createSerializer(responseFormat);
        SerializerResult serializerResult = serializer.entity(serviceMetadata, edmType, entity, serializerOptions);
        
        response.setContent(serializerResult.getContent());
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
        
    }

    public void createEntity(
            ODataRequest request, 
            ODataResponse response, 
            UriInfo uriInfo, 
            ContentType requestFormat, 
            ContentType responseFormat)
                    throws ODataApplicationException, DeserializerException, SerializerException {
        
        // 1. Retrieve the entity type from the URI
        UriResourceEntitySet uriResourceEntitySet = Util.getUriResourceEntitySet(uriInfo);
        EdmEntitySet entitySet = uriResourceEntitySet.getEntitySet();
        EdmEntityType entityType = entitySet.getEntityType();

        // 2. create the data in backend
        InputStream reqInputStream = request.getBody();
        ODataDeserializer deserializer = odata.createDeserializer(requestFormat);
        Entity entity = deserializer.entity(reqInputStream, entityType).getEntity();
        Entity createdEntity = storage.createEntityData(entitySet, entity);

        // 3. serialize the response (we have to return the created entity)
        ContextURL contextUrl = ContextURL.with().entitySet(entitySet).build();
        EntitySerializerOptions options = EntitySerializerOptions.with().contextURL(contextUrl).build();
        ODataSerializer serializer = odata.createSerializer(responseFormat);
        SerializerResult serializedResponse = serializer.entity(serviceMetadata, entityType, createdEntity, options);

        // 4. configure the response object
        response.setContent(serializedResponse.getContent());
        response.setStatusCode(HttpStatusCode.CREATED.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    }


    public void updateEntity(
            ODataRequest request, 
            ODataResponse response, 
            UriInfo uriInfo, 
            ContentType requestFormat, 
            ContentType responseFormat)
                    throws ODataApplicationException, DeserializerException, SerializerException {
        
        // 1. Retrieve the entity set which belongs to the requested entity
        UriResourceEntitySet uriResourceEntitySet = Util.getUriResourceEntitySet(uriInfo);
        EdmEntitySet entitySet = uriResourceEntitySet.getEntitySet();
        EdmEntityType entityType = entitySet.getEntityType();

        // 2. update the data in backend
        InputStream requestInputStream = request.getBody();
        ODataDeserializer deserializer = odata.createDeserializer(requestFormat);
        Entity requestEntity = deserializer.entity(requestInputStream, entityType).getEntity();
        List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
        // Note that this updateEntity()-method is invoked for both PUT or PATCH operations
        HttpMethod httpMethod = request.getMethod();
        storage.updateEntityData(entitySet, keyPredicates, requestEntity, httpMethod);

        // 3. configure the response object
        response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
    }

    public void deleteEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo) 
            throws ODataApplicationException {
        // 1. Retrieve the entity set
        UriResourceEntitySet uriResourceEntitySet = Util.getUriResourceEntitySet(uriInfo);
        EdmEntitySet entitySet = uriResourceEntitySet.getEntitySet();

        // 2. delete the data backend
        List<UriParameter> keyParams = uriResourceEntitySet.getKeyPredicates();
        storage.deleteEntityData(entitySet, keyParams);

        // 3. configure the response object
        response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
    }

    @Override
    public void readMediaEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo,
            ContentType responseFormat) 
                    throws ODataApplicationException, ODataLibraryException {
        
        UriResourceEntitySet uriResourceEntitySet = Util.getUriResourceEntitySet(uriInfo);
        EdmEntitySet entitySet = uriResourceEntitySet.getEntitySet();

        Entity entity = storage.readEntityData(entitySet, uriResourceEntitySet.getKeyPredicates());

        byte[] mediaContent = storage.readMedia(entity);
        InputStream responseContent = odata.createFixedFormatSerializer().binary(mediaContent);

        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setContent(responseContent);
        response.setHeader(HttpHeader.CONTENT_TYPE, entity.getMediaContentType());
    }

    @Override
    public void createMediaEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, 
            ContentType requestFormat, ContentType responseFormat)
                    throws ODataApplicationException, ODataLibraryException {

        EdmEntitySet entitySet = Util.getUriResourceEntitySet(uriInfo).getEntitySet();
        // the whole body of the request contains the content of the media entity
        byte[] mediaContent = odata.createFixedFormatDeserializer().binary(request.getBody());

        Entity entity = storage.createMediaEntity(entitySet.getEntityType(),
                requestFormat.toContentTypeString(), mediaContent);

        ContextURL contextUrl = ContextURL.with().entitySet(entitySet).suffix(Suffix.ENTITY).build();
        EntitySerializerOptions opts = EntitySerializerOptions.with().contextURL(contextUrl).build();
        SerializerResult serializerResult = odata.createSerializer(responseFormat)
                .entity(serviceMetadata, entitySet.getEntityType(), entity, opts);

        String location = request.getRawBaseUri() + '/'
                + odata.createUriHelper().buildCanonicalURL(entitySet, entity);

        response.setContent(serializerResult.getContent());
        response.setStatusCode(HttpStatusCode.CREATED.getStatusCode());
        response.setHeader(HttpHeader.LOCATION, location);
        response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    }

    @Override
    public void updateMediaEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo,
            ContentType requestFormat, ContentType responseFormat)
                    throws ODataApplicationException, ODataLibraryException {

        UriResourceEntitySet uriResourceEntitySet = Util.getUriResourceEntitySet(uriInfo);
        EdmEntitySet entitySet = uriResourceEntitySet.getEntitySet();

        Entity entity = storage.readEntityData(entitySet, uriResourceEntitySet.getKeyPredicates());

        byte[] mediaContent = odata.createFixedFormatDeserializer().binary(request.getBody());
        storage.updateMedia(entity, requestFormat.toContentTypeString(), mediaContent);

        response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
    }

    @Override
    public void deleteMediaEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo)
            throws ODataApplicationException, ODataLibraryException {
        /*
         * In this tutorial, the content of the media entity is stored in a special property.
         * So no additional steps to delete the content of the media entity are necessary.
         *
         * A real service may store the content on the file system. So we have to take care to
         * delete external files too.
         *
         * DELETE request to /Advertisements(ID) will be dispatched to the deleteEntity(...) method
         * DELETE request to /Advertisements(ID)/$value will be dispatched to the deleteMediaEntity(...) method
         *
         * So it is a good idea handle deletes in a central place.
         */
        deleteEntity(request, response, uriInfo);
    }

}
