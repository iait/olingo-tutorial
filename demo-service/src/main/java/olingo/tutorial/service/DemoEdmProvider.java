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
import org.apache.olingo.commons.api.edm.provider.CsdlAction;
import org.apache.olingo.commons.api.edm.provider.CsdlActionImport;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainer;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainerInfo;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlFunction;
import org.apache.olingo.commons.api.edm.provider.CsdlFunctionImport;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationPropertyBinding;
import org.apache.olingo.commons.api.edm.provider.CsdlParameter;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;
import org.apache.olingo.commons.api.edm.provider.CsdlReturnType;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;

/**
 * this class is supposed to declare the metadata of the OData service
 * it is invoked by the Olingo framework e.g. when the metadata document of the service is invoked
 * e.g. http://localhost:8080/DemoService/DemoService.svc/$metadata
 */
public class DemoEdmProvider extends CsdlAbstractEdmProvider {

    // Service Namespace
    public static final String NAMESPACE = "EIV";

    // EDM Container
    public static final String CONTAINER_NAME = "Container";
    public static final FullQualifiedName CONTAINER = 
            new FullQualifiedName(NAMESPACE, CONTAINER_NAME);

    // Entity Types Names
    public static final String ET_PRODUCT_NAME = "Product";
    public static final FullQualifiedName ET_PRODUCT_FQN = 
            new FullQualifiedName(NAMESPACE, ET_PRODUCT_NAME);
    public static final String ET_CATEGORY_NAME = "Category";
    public static final FullQualifiedName ET_CATEGORY_FQN = 
            new FullQualifiedName(NAMESPACE, ET_CATEGORY_NAME);
    public static final String ET_ADVERTISEMENT_NAME = "Advertisement";
    public static final FullQualifiedName ET_ADVERTISEMENT_FQN =
            new FullQualifiedName(NAMESPACE, ET_ADVERTISEMENT_NAME);

    // Entity Set Names
    public static final String ES_PRODUCTS_NAME = "Products";
    public static final String ES_CATEGORIES_NAME = "Categories";
    public static final String ES_ADVERTISEMENTS_NAME = "Advertisements";
  
    // Action
    public static final String ACTION_RESET = "Reset";
    public static final FullQualifiedName ACTION_RESET_FQN = 
            new FullQualifiedName(NAMESPACE, ACTION_RESET);

    // Function
    public static final String FUNCTION_COUNT_CATEGORIES = "CountCategories";
    public static final FullQualifiedName FUNCTION_COUNT_CATEGORIES_FQN = 
            new FullQualifiedName(NAMESPACE, FUNCTION_COUNT_CATEGORIES);

    // Function/Action Parameters
    public static final String PARAMETER_AMOUNT = "Amount";

    @Override
    public List<CsdlSchema> getSchemas() {

        // create Schema
        CsdlSchema schema = new CsdlSchema();
        schema.setNamespace(NAMESPACE);
    
        // add EntityTypes
        List<CsdlEntityType> entityTypes = new ArrayList<>();
        entityTypes.add(getEntityType(ET_PRODUCT_FQN));
        entityTypes.add(getEntityType(ET_CATEGORY_FQN));
        entityTypes.add(getEntityType(ET_ADVERTISEMENT_FQN));
        schema.setEntityTypes(entityTypes);
    
        // add EntityContainer
        schema.setEntityContainer(getEntityContainer());
        
        // add actions
        List<CsdlAction> actions = new ArrayList<>();
        actions.addAll(getActions(ACTION_RESET_FQN));
        schema.setActions(actions);

        // add functions
        List<CsdlFunction> functions = new ArrayList<>();
        functions.addAll(getFunctions(FUNCTION_COUNT_CATEGORIES_FQN));
        schema.setFunctions(functions);
    
        // finally
        List<CsdlSchema> schemas = new ArrayList<>();
        schemas.add(schema);
    
        return schemas;
    }

    @Override
    public List<CsdlFunction> getFunctions(final FullQualifiedName functionName) {
        
        if (functionName.equals(FUNCTION_COUNT_CATEGORIES_FQN)) {
            
            // Create the parameter for the function
            CsdlParameter parameterAmount = new CsdlParameter()
                    .setName(PARAMETER_AMOUNT)
                    .setNullable(false)
                    .setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
    
            // Create the return type of the function
            CsdlReturnType returnType = new CsdlReturnType()
                    .setCollection(true)
                    .setType(ET_CATEGORY_FQN);
    
            // Create the function
            CsdlFunction function = new CsdlFunction()
                    .setName(FUNCTION_COUNT_CATEGORIES_FQN.getName())
                    .setParameters(Arrays.asList(parameterAmount))
                    .setReturnType(returnType);
    
            // It is allowed to overload functions, so we have to provide a list of functions for each function name
            return Arrays.asList(function);
        }

        return null;
    }
    
    @Override
    public CsdlFunctionImport getFunctionImport(FullQualifiedName entityContainer, String functionImportName) {
        
        if (entityContainer.equals(CONTAINER)) {
            if (functionImportName.equals(FUNCTION_COUNT_CATEGORIES_FQN.getName())) {
              return new CsdlFunctionImport()
                        .setName(functionImportName)
                        .setFunction(FUNCTION_COUNT_CATEGORIES_FQN)
                        .setEntitySet(ES_CATEGORIES_NAME)
                        .setIncludeInServiceDocument(true);
            }
        }

        return null;
    }

    @Override
    public List<CsdlAction> getActions(final FullQualifiedName actionName) {
      
        if (actionName.equals(ACTION_RESET_FQN)) {

            // Create parameters
            CsdlParameter parameter = new CsdlParameter()
                    .setName(PARAMETER_AMOUNT)
                    .setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
            List<CsdlParameter> parameters = Arrays.asList(parameter);
    
            // Create the Csdl Action
            CsdlAction action = new CsdlAction()
                    .setName(ACTION_RESET_FQN.getName())
                    .setParameters(parameters);
    
            // It is allowed to overload actions, so we have to provide a list of Actions for each action name
            return Arrays.asList(action);
        }

        return null;
    }

    @Override
    public CsdlActionImport getActionImport(final FullQualifiedName entityContainer, final String actionImportName) {
        
        if (entityContainer.equals(CONTAINER)) {
            if (actionImportName.equals(ACTION_RESET_FQN.getName())) {
              return new CsdlActionImport()
                      .setName(actionImportName)
                      .setAction(ACTION_RESET_FQN);
            }
        }

        return null;
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
            
        } else if (entityTypeName.equals(ET_ADVERTISEMENT_FQN)) {
            
            // Properties
            CsdlProperty id = new CsdlProperty()
                    .setName("ID")
                    .setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
            CsdlProperty name = new CsdlProperty()
                    .setName("Name")
                    .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
            CsdlProperty airDate = new CsdlProperty()
                    .setName("AirDate")
                    .setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName());
            
            // Key
            CsdlPropertyRef keyRef = new CsdlPropertyRef()
                    .setName("ID");
            
            // Entity type
            return new CsdlEntityType()
                    .setName(ET_ADVERTISEMENT_NAME)
                    .setProperties(Arrays.asList(id, name, airDate))
                    .setKey(Arrays.asList(keyRef))
                    .setHasStream(true);
            
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
                
            } else if (entitySetName.equals(ES_ADVERTISEMENTS_NAME)) {
                
                return new CsdlEntitySet()
                        .setName(ES_ADVERTISEMENTS_NAME)
                        .setType(ET_ADVERTISEMENT_FQN);
                
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
        entitySets.add(getEntitySet(CONTAINER, ES_ADVERTISEMENTS_NAME));
    
        // create EntityContainer
        CsdlEntityContainer entityContainer = new CsdlEntityContainer();
        entityContainer.setName(CONTAINER_NAME);
        entityContainer.setEntitySets(entitySets);
        
        // create function imports
        List<CsdlFunctionImport> functionImports = new ArrayList<>();
        functionImports.add(getFunctionImport(CONTAINER, FUNCTION_COUNT_CATEGORIES));

        // create action imports
        List<CsdlActionImport> actionImports = new ArrayList<CsdlActionImport>();
        actionImports.add(getActionImport(CONTAINER, ACTION_RESET));

        entityContainer.setFunctionImports(functionImports);
        entityContainer.setActionImports(actionImports);
    
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
