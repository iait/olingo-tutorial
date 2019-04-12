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
package myservice.mynamespace.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Link;
import org.apache.olingo.commons.api.edm.Edm;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;

public class TransactionalEntityManager {
    
    // entidades actuales por entitySetName
    private Map<String, List<Entity>> entities = new HashMap<>();
    
    // backup luego de iniciar una transacción
    private Map<String, List<Entity>> backupEntities = new HashMap<>();
    
    // mapa con las entidades ya copiadas
    private Map<String, IdentityHashMap<Entity, Entity>> copyMap = new HashMap<>();
    
    private boolean isInTransaction = false;
    
    private Edm edm;
    
    public TransactionalEntityManager(final Edm edm) {
        this.edm = edm;
    }

    public List<Entity> getEntityCollection(final String entitySetName) {
        if (!entities.containsKey(entitySetName)) {
            entities.put(entitySetName, new ArrayList<>());
        }
        
        return entities.get(entitySetName);
    }
    
    public void beginTransaction() throws ODataApplicationException {
        if (!isInTransaction) {
            isInTransaction = true;
            copyCurrentState();
        } else {
            throw new ODataApplicationException("Transaction already in progress", 
                    HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
        }
    }
    
    public void rollbackTransaction() throws ODataApplicationException {
        if(isInTransaction) {
            entities = backupEntities;
            backupEntities = new HashMap<String, List<Entity>>();
            isInTransaction = false;
        } else {
            throw new ODataApplicationException("No transaction in progress", 
                    HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
        }
    }
    
    public void commitTransaction() throws ODataApplicationException {
        if(isInTransaction) {
            backupEntities.clear();
            isInTransaction = false;
        } else {
            throw new ODataApplicationException("No transaction in progress", 
                    HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
        }
    }
    
    private void copyCurrentState() {
        // comienza creación de backup
        // limpia mapas de copias de entities y backup
        copyMap.clear();
        backupEntities.clear();
        
        for (Entry<String, List<Entity>> entry : entities.entrySet()) {
            String entitySetName = entry.getKey();
            List<Entity> srcEntities = entry.getValue();
            List<Entity> bkEntities = new ArrayList<>();
            for (Entity srcEntity : srcEntities) {
                Entity bkEntity = copyEntityRecursively(entitySetName, srcEntity);
                bkEntities.add(bkEntity);
            }
            backupEntities.put(entitySetName, bkEntities);
        }
    }
    
    private Entity copyEntityRecursively(String edmEntitySet, Entity srcEntity) {
        // Check if entity is already copied
        if (containsEntityInCopyMap(edmEntitySet, srcEntity)) {
            return getEntityFromCopyMap(edmEntitySet, srcEntity);
        } else {
            final Entity newEntity = copyEntity(srcEntity);
            addEntityToCopyMap(edmEntitySet, srcEntity, newEntity);
            
            // Create nested entities recursively
            for(final Link link : srcEntity.getNavigationLinks()) {
                newEntity.getNavigationLinks().add(copyLink(edmEntitySet, link));
            }
            
            return newEntity;
        }
    }

    private Link copyLink(String entitySetName, final Link link) {
        final Link newLink = new Link();
        newLink.setBindingLink(link.getBindingLink());
        newLink.setBindingLinks(new ArrayList<String>(link.getBindingLinks()));
        newLink.setHref(link.getHref());
        newLink.setMediaETag(link.getMediaETag());
        newLink.setRel(link.getRel());
        newLink.setTitle(link.getTitle());
        newLink.setType(link.getType());
        
        EdmEntitySet entitySet = edm.getEntityContainer().getEntitySet(entitySetName);
        
        // Single navigation link
        if(link.getInlineEntity() != null) {
            final EdmEntitySet linkedEdmEntitySet = (EdmEntitySet) entitySet.getRelatedBindingTarget(link.getTitle());
            newLink.setInlineEntity(copyEntityRecursively(linkedEdmEntitySet.getName(), link.getInlineEntity()));
        }            
        
        // Collection navigation link
        if(link.getInlineEntitySet() != null) {
            final EdmEntitySet linkedEdmEntitySet = (EdmEntitySet) entitySet.getRelatedBindingTarget(link.getTitle());
            final EntityCollection inlineEntitySet = link.getInlineEntitySet();
            final EntityCollection newInlineEntitySet = new EntityCollection();
            newInlineEntitySet.setBaseURI(inlineEntitySet.getBaseURI());
            newInlineEntitySet.setCount(inlineEntitySet.getCount());
            newInlineEntitySet.setDeltaLink(inlineEntitySet.getDeltaLink());
            newInlineEntitySet.setId(inlineEntitySet.getId());
            newInlineEntitySet.setNext(inlineEntitySet.getNext());
            
            for(final Entity inlineEntity : inlineEntitySet.getEntities()) {
                newInlineEntitySet.getEntities().add(copyEntityRecursively(linkedEdmEntitySet.getName(), inlineEntity));
            }
            
            newLink.setInlineEntitySet(newInlineEntitySet);
        }
        
        return newLink;
    }

    private Entity copyEntity(final Entity entity) {
        final Entity newEntity = new Entity();
        newEntity.setBaseURI(entity.getBaseURI());
        newEntity.setEditLink(entity.getEditLink());
        newEntity.setETag(entity.getETag());
        newEntity.setId(entity.getId());
        newEntity.setMediaContentSource(entity.getMediaContentSource());
        newEntity.setMediaContentType(entity.getMediaContentType());
        newEntity.setSelfLink(entity.getSelfLink());
        newEntity.setMediaETag(entity.getMediaETag());
        newEntity.setType(entity.getType());
        newEntity.getProperties().addAll(entity.getProperties());
        
        return newEntity;
    }

    private void addEntityToCopyMap(final String entitySetName, final Entity srcEntity, final Entity destEntity) {
        if(!copyMap.containsKey(entitySetName)) {
            copyMap.put(entitySetName, new IdentityHashMap<Entity, Entity>());
        }
        
        copyMap.get(entitySetName).put(srcEntity, destEntity);
    }
    
    private boolean containsEntityInCopyMap(final String entitySetName, final Entity srcEntity) {
        return getEntityFromCopyMap(entitySetName, srcEntity) != null;
    }
    
    private Entity getEntityFromCopyMap(final String entitySetName, final Entity srcEntity) {
        if(!copyMap.containsKey(entitySetName)) {
            return null;
        }
        
        return copyMap.get(entitySetName).get(srcEntity);
    }
}
