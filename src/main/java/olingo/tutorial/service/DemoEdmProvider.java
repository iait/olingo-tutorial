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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAbstractEdmProvider;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainer;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainerInfo;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationPropertyBinding;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;

/**
 * this class is supposed to declare the metadata of the OData service
 * it is invoked by the Olingo framework e.g. when the metadata document of the service is invoked
 * e.g. http://localhost:8080/ExampleService1/ExampleService1.svc/$metadata
 */
public class DemoEdmProvider extends CsdlAbstractEdmProvider {

  // Service Namespace
  public static final String NAMESPACE = "EIV";

  // EDM Container
  public static final String CONTAINER_NAME = "Container";
  public static final FullQualifiedName CONTAINER = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);

  // Entity Types Names
  public static final String ET_PRODUCT_NAME = "Product";
  public static final FullQualifiedName ET_PRODUCT_FQN = new FullQualifiedName(NAMESPACE, ET_PRODUCT_NAME);
  public static final String ET_CATEGORY_NAME = "Category";
  public static final FullQualifiedName ET_CATEGORY_FQN = new FullQualifiedName(NAMESPACE, ET_CATEGORY_NAME);

  // Entity Set Names
  public static final String ES_PRODUCTS_NAME = "Products";
  public static final String ES_CATEGORIES_NAME = "Categories";

  @Override
  public List<CsdlSchema> getSchemas() {

    // create Schema
    CsdlSchema schema = new CsdlSchema();
    schema.setNamespace(NAMESPACE);

    // add EntityTypes
    List<CsdlEntityType> entityTypes = new ArrayList<>();
    entityTypes.add(getEntityType(ET_PRODUCT_FQN));
    entityTypes.add(getEntityType(ET_CATEGORY_FQN));
    schema.setEntityTypes(entityTypes);

    // add EntityContainer
    schema.setEntityContainer(getEntityContainer());

    // finally
    List<CsdlSchema> schemas = new ArrayList<>();
    schemas.add(schema);

    return schemas;
  }


    @Override
    public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) {

        // this method is called for one of the EntityTypes that are configured in the Schema
        if (entityTypeName.equals(ET_PRODUCT_FQN)) {
    
            // Properties
            CsdlProperty id = new CsdlProperty()
                    .setName("ID")
                    .setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
            CsdlProperty name = new CsdlProperty()
                    .setName("Name")
                    .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
            CsdlProperty  description = new CsdlProperty()
                    .setName("Description")
                    .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
            
            // Key
            CsdlPropertyRef propertyRef = new CsdlPropertyRef()
                    .setName("ID");
            
            // Navigation
            CsdlNavigationProperty navigationProperty = new CsdlNavigationProperty()
                    .setName("Category")
                    .setType(ET_CATEGORY_FQN)
                    .setNullable(false)
                    .setPartner("Products");
            
            // Entity type
            return new CsdlEntityType()
                    .setName(ET_PRODUCT_NAME)
                    .setProperties(Arrays.asList(id, name , description))
                    .setKey(Arrays.asList(propertyRef))
                    .setNavigationProperties(Arrays.asList(navigationProperty));
            
        } else if (entityTypeName.equals(ET_CATEGORY_FQN)) {
            
            // Properties
            CsdlProperty id = new CsdlProperty()
                    .setName("ID")
                    .setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
            CsdlProperty name = new CsdlProperty()
                    .setName("Name")
                    .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
            
            // Key
            CsdlPropertyRef keyRef = new CsdlPropertyRef()
                    .setName("ID");
            
            // Navigation
            CsdlNavigationProperty navigationProperty = new CsdlNavigationProperty()
                    .setName("Products")
                    .setType(ET_PRODUCT_FQN)
                    .setCollection(true)
                    .setPartner("Category");
            
            // Entity type
            return new CsdlEntityType()
                    .setName(ET_CATEGORY_NAME)
                    .setProperties(Arrays.asList(id, name))
                    .setKey(Arrays.asList(keyRef))
                    .setNavigationProperties(Arrays.asList())
                    .setNavigationProperties(Arrays.asList(navigationProperty));
        }

        return null;
    }

    @Override
    public CsdlEntitySet getEntitySet(FullQualifiedName entityContainer, String entitySetName) {

        if (entityContainer.equals(CONTAINER)) {
            
            if (entitySetName.equals(ES_PRODUCTS_NAME)) {
                
                CsdlNavigationPropertyBinding binding = new CsdlNavigationPropertyBinding()
                        .setPath("Category")
                        .setTarget(ES_CATEGORIES_NAME);
                
                return new CsdlEntitySet()
                        .setName(ES_PRODUCTS_NAME)
                        .setType(ET_PRODUCT_FQN)
                        .setNavigationPropertyBindings(Arrays.asList(binding));
                
            } else if (entitySetName.equals(ES_CATEGORIES_NAME)) {
                
                CsdlNavigationPropertyBinding binding = new CsdlNavigationPropertyBinding()
                        .setPath("Products")
                        .setTarget(ES_PRODUCTS_NAME);
                
                return new CsdlEntitySet()
                        .setName(ES_CATEGORIES_NAME)
                        .setType(ET_CATEGORY_FQN)
                        .setNavigationPropertyBindings(Arrays.asList(binding));
            }
        }
        return null;
    }

  @Override
  public CsdlEntityContainer getEntityContainer() {

    // create EntitySets
    List<CsdlEntitySet> entitySets = new ArrayList<>();
    entitySets.add(getEntitySet(CONTAINER, ES_PRODUCTS_NAME));
    entitySets.add(getEntitySet(CONTAINER, ES_CATEGORIES_NAME));

    // create EntityContainer
    CsdlEntityContainer entityContainer = new CsdlEntityContainer();
    entityContainer.setName(CONTAINER_NAME);
    entityContainer.setEntitySets(entitySets);

    return entityContainer;
  }

  @Override
  public CsdlEntityContainerInfo getEntityContainerInfo(FullQualifiedName entityContainerName) {

    // This method is invoked when displaying the service document at e.g. 
      // http://localhost:8080/DemoService/DemoService.svc
    if(entityContainerName == null || entityContainerName.equals(CONTAINER)){
      CsdlEntityContainerInfo entityContainerInfo = new CsdlEntityContainerInfo();
      entityContainerInfo.setContainerName(CONTAINER);
      return entityContainerInfo;
    }

    return null;
  }
}
