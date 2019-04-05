package olingo.tutorial.data;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmKeyPropertyRef;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResourceFunction;

import olingo.tutorial.service.DemoEdmProvider;
import olingo.tutorial.util.Util;

public class Storage {
    
    private static final String MEDIA_PROPERTY_NAME = "$value";

    private List<Entity> productList;
    private List<Entity> categoryList;
    private List<Entity> advertisementList;

    public Storage() {
        productList = new ArrayList<>();
        categoryList = new ArrayList<>();
        advertisementList = new ArrayList<>();
        initProductSampleData();
        initCategorySampleData();
        initAdvertisementSampleData();
    }

    /* PUBLIC FACADE */
    
    public byte[] readMedia(Entity entity) {
        return (byte[]) entity.getProperty(MEDIA_PROPERTY_NAME).asPrimitive();
    }

    public void updateMedia(Entity entity, String mediaContentType, byte[] data) {
        entity.getProperties().remove(entity.getProperty(MEDIA_PROPERTY_NAME));
        entity.addProperty(new Property(null, MEDIA_PROPERTY_NAME, ValueType.PRIMITIVE, data));
        entity.setMediaContentType(mediaContentType);
    }

    public EntityCollection readEntitySetData(EdmEntitySet edmEntitySet) 
            throws ODataApplicationException{

        // actually, this is only required if we have more than one Entity Sets
        if (edmEntitySet.getName().equals(DemoEdmProvider.ES_PRODUCTS_NAME)) {
            return getProducts();
        } else if (edmEntitySet.getName().equals(DemoEdmProvider.ES_CATEGORIES_NAME)) {
            return getCategories();
        }

        return null;
    }

    public Entity createMediaEntity(
            EdmEntityType edmEntityType, String mediaContentType, final byte[] data) {

        if (edmEntityType.getName().equals(DemoEdmProvider.ET_ADVERTISEMENT_NAME)) {
            Entity entity = new Entity();
            entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, UUID.randomUUID()));
            entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, null));
            entity.addProperty(new Property(null, "AirDate", ValueType.PRIMITIVE, null));

            entity.setMediaContentType(mediaContentType);
            entity.addProperty(new Property(null, MEDIA_PROPERTY_NAME, ValueType.PRIMITIVE, data));

            advertisementList.add(entity);
            return entity;
        }

        return null;
    }

    public Entity readEntityData(EdmEntitySet edmEntitySet, List<UriParameter> keyParams) 
            throws ODataApplicationException{

        EdmEntityType edmEntityType = edmEntitySet.getEntityType();

        if (edmEntityType.getName().equals(DemoEdmProvider.ET_PRODUCT_NAME)) {
            return getProduct(edmEntityType, keyParams);
        } else if (edmEntityType.getName().equals(DemoEdmProvider.ET_CATEGORY_NAME)) {
            return getCategory(edmEntityType, keyParams);
        }

        return null;
    }

    public Entity createEntityData(EdmEntitySet edmEntitySet, Entity entityToCreate) {

        EdmEntityType edmEntityType = edmEntitySet.getEntityType();

        // actually, this is only required if we have more than one Entity Type
        if (edmEntityType.getName().equals(DemoEdmProvider.ET_PRODUCT_NAME)) {
          return createProduct(edmEntityType, entityToCreate);
        }

        return null;
      }
    
    /**
     * This method is invoked for PATCH or PUT requests
     * */
    public void updateEntityData(EdmEntitySet edmEntitySet, List<UriParameter> keyParams, Entity updateEntity,
        HttpMethod httpMethod) throws ODataApplicationException {

      EdmEntityType edmEntityType = edmEntitySet.getEntityType();

      // actually, this is only required if we have more than one Entity Type
      if (edmEntityType.getName().equals(DemoEdmProvider.ET_PRODUCT_NAME)) {
        updateProduct(edmEntityType, keyParams, updateEntity, httpMethod);
      }
    }

    public void deleteEntityData(EdmEntitySet edmEntitySet, List<UriParameter> keyParams)
        throws ODataApplicationException {

      EdmEntityType edmEntityType = edmEntitySet.getEntityType();

      // actually, this is only required if we have more than one Entity Type
      if (edmEntityType.getName().equals(DemoEdmProvider.ET_PRODUCT_NAME)) {
        deleteProduct(edmEntityType, keyParams);
      }
    }

    /*  INTERNAL */
    private EntityCollection getCategories() {
        EntityCollection retEntitySet = new EntityCollection();
        retEntitySet.getEntities().addAll(categoryList);
        return retEntitySet;
    }
    
    private EntityCollection getProducts() {
        EntityCollection retEntitySet = new EntityCollection();
        retEntitySet.getEntities().addAll(productList);
        return retEntitySet;
    }

    private Entity getCategory(EdmEntityType edmEntityType, List<UriParameter> keyParams) 
            throws ODataApplicationException {
        EntityCollection entitySet = getCategories();
        
        Entity requestedEntity = Util.findEntity(edmEntityType, entitySet, keyParams);

        if (requestedEntity == null){
            // this variable is null if our data doesn't contain an entity for the requested key
            throw new ODataApplicationException("Entity for requested key doesn't exist",
                                       HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
        }

        return requestedEntity;
    }

    private Entity getProduct(EdmEntityType edmEntityType, List<UriParameter> keyParams) 
            throws ODataApplicationException {

        // the list of entities at runtime
        EntityCollection entitySet = getProducts();

        /*  generic approach  to find the requested entity */
        Entity requestedEntity = Util.findEntity(edmEntityType, entitySet, keyParams);

        if (requestedEntity == null){
            // this variable is null if our data doesn't contain an entity for the requested key
            throw new ODataApplicationException("Entity for requested key doesn't exist",
                    HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
        }

        return requestedEntity;
     }
    
    private boolean productIdExists(int id) {

        for (Entity entity : this.productList) {
          Integer existingID = (Integer) entity.getProperty("ID").getValue();
          if (existingID.intValue() == id) {
            return true;
          }
        }

        return false;
      }
    
    private Entity createProduct(EdmEntityType edmEntityType, Entity entity) {

        // the ID of the newly created product entity is generated automatically
        int newId = 1;
        while (productIdExists(newId)) {
          newId++;
        }

        Property idProperty = entity.getProperty("ID");
        if (idProperty != null) {
          idProperty.setValue(ValueType.PRIMITIVE, Integer.valueOf(newId));
        } else {
          // as of OData v4 spec, the key property can be omitted from the POST request body
          entity.getProperties().add(new Property(null, "ID", ValueType.PRIMITIVE, newId));
        }
        entity.setId(createId(entity, "ID"));
        this.productList.add(entity);

        return entity;

      }
    
    private void updateProduct(EdmEntityType edmEntityType, List<UriParameter> keyParams, Entity receivedEntity,
            HttpMethod httpMethod) throws ODataApplicationException {

          Entity existingEntity = getProduct(edmEntityType, keyParams);
          if (existingEntity == null) {
            throw new ODataApplicationException("Entity not found", HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
          }

          // loop over all properties and replace the values with the values of the given payload
          // Note: ignoring ComplexType, as we don't have it in our odata model
          List<Property> existingProperties = existingEntity.getProperties();
          for (Property existingProp : existingProperties) {
            String propName = existingProp.getName();

            // ignore the key properties, they aren't updateable
            if (isKey(edmEntityType, propName)) {
              continue;
            }

            Property updateProperty = receivedEntity.getProperty(propName);
            // the request payload might not consider ALL properties, so it can be null
            if (updateProperty == null) {
              // if a property has NOT been added to the request payload
              // depending on the HttpMethod, our behavior is different
              if (httpMethod.equals(HttpMethod.PATCH)) {
                // as of the OData spec, in case of PATCH, the existing property is not touched
                continue; // do nothing
              } else if (httpMethod.equals(HttpMethod.PUT)) {
                // as of the OData spec, in case of PUT, the existing property is set to null (or to default value)
                existingProp.setValue(existingProp.getValueType(), null);
                continue;
              }
            }

            // change the value of the properties
            existingProp.setValue(existingProp.getValueType(), updateProperty.getValue());
          }
        }

        private void deleteProduct(EdmEntityType edmEntityType, List<UriParameter> keyParams)
            throws ODataApplicationException {

          Entity productEntity = getProduct(edmEntityType, keyParams);
          if (productEntity == null) {
            throw new ODataApplicationException("Entity not found", HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
          }

          this.productList.remove(productEntity);
        }

     /* HELPER */
    private void initProductSampleData() {

        Entity entity = new Entity();

        entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 1));
        entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Notebook Basic 15"));
        entity.addProperty(new Property(null, "Description", ValueType.PRIMITIVE,
            "Notebook Basic, 1.7GHz - 15 XGA - 1024MB DDR2 SDRAM - 40GB"));
        entity.setType(DemoEdmProvider.ET_PRODUCT_FQN.getFullQualifiedNameAsString());
        entity.setId(createId(entity, "ID"));
        productList.add(entity);

        entity = new Entity();
        entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 2));
        entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Notebook Professional 17"));
        entity.addProperty(new Property(null, "Description", ValueType.PRIMITIVE,
            "Notebook Professional, 2.8GHz - 15 XGA - 8GB DDR3 RAM - 500GB"));
        entity.setType(DemoEdmProvider.ET_PRODUCT_FQN.getFullQualifiedNameAsString());
        entity.setId(createId(entity, "ID"));
        productList.add(entity);

        entity = new Entity();
        entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 3));
        entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "1UMTS PDA"));
        entity.addProperty(new Property(null, "Description", ValueType.PRIMITIVE,
            "Ultrafast 3G UMTS/HSDPA Pocket PC, supports GSM network"));
        entity.setType(DemoEdmProvider.ET_PRODUCT_FQN.getFullQualifiedNameAsString());
        entity.setId(createId(entity, "ID"));
        productList.add(entity);

        entity = new Entity();
        entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 4));
        entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Comfort Easy"));
        entity.addProperty(new Property(null, "Description", ValueType.PRIMITIVE,
            "32 GB Digital Assitant with high-resolution color screen"));
        entity.setType(DemoEdmProvider.ET_PRODUCT_FQN.getFullQualifiedNameAsString());
        entity.setId(createId(entity, "ID"));
        productList.add(entity);

        entity = new Entity();
        entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 5));
        entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Ergo Screen"));
        entity.addProperty(new Property(null, "Description", ValueType.PRIMITIVE,
            "19 Optimum Resolution 1024 x 768 @ 85Hz, resolution 1280 x 960"));
        entity.setType(DemoEdmProvider.ET_PRODUCT_FQN.getFullQualifiedNameAsString());
        entity.setId(createId(entity, "ID"));
        productList.add(entity);

        entity = new Entity();
        entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 6));
        entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Flat Basic"));
        entity.addProperty(new Property(null, "Description", ValueType.PRIMITIVE,
            "Optimum Hi-Resolution max. 1600 x 1200 @ 85Hz, Dot Pitch: 0.24mm"));
        entity.setType(DemoEdmProvider.ET_PRODUCT_FQN.getFullQualifiedNameAsString());
        entity.setId(createId(entity, "ID"));
        productList.add(entity);
    }

    private void initCategorySampleData() {

        Entity entity = new Entity();
        
        entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 1));
        entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Notebooks"));
        entity.setType(DemoEdmProvider.ET_CATEGORY_FQN.getFullQualifiedNameAsString());
        entity.setId(createId(entity, "ID"));
        categoryList.add(entity);
        
        entity = new Entity();
        entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 2));
        entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Organizers"));
        entity.setType(DemoEdmProvider.ET_CATEGORY_FQN.getFullQualifiedNameAsString());
        entity.setId(createId(entity, "ID"));
        categoryList.add(entity);
        
        entity = new Entity();
        entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 3));
        entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Monitors"));
        entity.setType(DemoEdmProvider.ET_CATEGORY_FQN.getFullQualifiedNameAsString());
        entity.setId(createId(entity, "ID"));
        categoryList.add(entity);
    }

    private void initAdvertisementSampleData() {
        Entity entity = new Entity();
        entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, UUID.fromString("f89dee73-af9f-4cd4-b330-db93c25ff3c7")));
        entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Old School Lemonade Store, Retro Style"));
        entity.addProperty(new Property(null, "AirDate", ValueType.PRIMITIVE, Timestamp.valueOf("2012-11-07 00:00:00")));
        entity.addProperty(new Property(null, MEDIA_PROPERTY_NAME, ValueType.PRIMITIVE, "Super content".getBytes()));
        entity.setMediaContentType(ContentType.parse("text/plain").toContentTypeString());
        advertisementList.add(entity);

        entity = new Entity();
        entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, UUID.fromString("db2d2186-1c29-4d1e-88ef-a127f521b9c67")));
        entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Early morning start, need coffee"));
        entity.addProperty(new Property(null, "AirDate", ValueType.PRIMITIVE, Timestamp.valueOf("2000-02-29 00:00:00")));
        entity.addProperty(new Property(null, MEDIA_PROPERTY_NAME, ValueType.PRIMITIVE, "Super content2".getBytes()));
        entity.setMediaContentType(ContentType.parse("text/plain").toContentTypeString());
        advertisementList.add(entity);
    }

    public EntityCollection getRelatedEntityCollection(Entity sourceEntity, EdmEntityType targetEntityType) {
        EntityCollection navigationTargetEntityCollection = new EntityCollection();

        FullQualifiedName relatedEntityFqn = targetEntityType.getFullQualifiedName();
        String sourceEntityFqn = sourceEntity.getType();

        if (sourceEntityFqn.equals(DemoEdmProvider.ET_PRODUCT_FQN.getFullQualifiedNameAsString())
                && relatedEntityFqn.equals(DemoEdmProvider.ET_CATEGORY_FQN)) {
            // relation Products->Category (result all categories)
            int productID = (Integer) sourceEntity.getProperty("ID").getValue();
            if (productID == 1 || productID == 2) {
                navigationTargetEntityCollection.getEntities().add(categoryList.get(0));
            } else if (productID == 3 || productID == 4) {
                navigationTargetEntityCollection.getEntities().add(categoryList.get(1));
            } else if (productID == 5 || productID == 6) {
                navigationTargetEntityCollection.getEntities().add(categoryList.get(2));
            }
        } else if (sourceEntityFqn.equals(DemoEdmProvider.ET_CATEGORY_FQN.getFullQualifiedNameAsString())
                && relatedEntityFqn.equals(DemoEdmProvider.ET_PRODUCT_FQN)) {
            // relation Category->Products (result all products)
            int categoryID = (Integer) sourceEntity.getProperty("ID").getValue();
            if (categoryID == 1) {
                // the first 2 products are notebooks
                navigationTargetEntityCollection.getEntities().addAll(productList.subList(0, 2));
            } else if (categoryID == 2) {
                // the next 2 products are organizers
                navigationTargetEntityCollection.getEntities().addAll(productList.subList(2, 4));
            } else if (categoryID == 3) {
                // the first 2 products are monitors
                navigationTargetEntityCollection.getEntities().addAll(productList.subList(4, 6));
            }
        }

        if (navigationTargetEntityCollection.getEntities().isEmpty()) {
            return null;
        }

        return navigationTargetEntityCollection;
    }

    private URI createId(Entity entity, String idPropertyName) {
        return createId(entity, idPropertyName, null);
    }

    private URI createId(Entity entity, String idPropertyName, String navigationName) {
        try {
            StringBuilder sb = new StringBuilder(getEntitySetName(entity)).append("(");
            final Property property = entity.getProperty(idPropertyName);
            sb.append(property.asPrimitive()).append(")");
            if (navigationName != null) {
                sb.append("/").append(navigationName);
            }
            return new URI(sb.toString());
        } catch (URISyntaxException e) {
            throw new ODataRuntimeException("Unable to create (Atom) id for entity: " + entity, e);
        }
    }
          
    private String getEntitySetName(Entity entity) {
        if (DemoEdmProvider.ET_CATEGORY_FQN.getFullQualifiedNameAsString().equals(entity.getType())) {
            return DemoEdmProvider.ES_CATEGORIES_NAME;
        } else if(DemoEdmProvider.ET_PRODUCT_FQN.getFullQualifiedNameAsString().equals(entity.getType())) {
            return DemoEdmProvider.ES_PRODUCTS_NAME;
        }
        return null;
    }
    
    private boolean isKey(EdmEntityType edmEntityType, String propertyName) {
        List<EdmKeyPropertyRef> keyPropertyRefs = edmEntityType.getKeyPropertyRefs();
        for (EdmKeyPropertyRef propRef : keyPropertyRefs) {
          String keyPropertyName = propRef.getName();
          if (keyPropertyName.equals(propertyName)) {
            return true;
          }
        }
        return false;
    }
    
    public EntityCollection readFunctionImportCollection(
            UriResourceFunction uriResourceFunction, ServiceMetadata serviceMetadata) 
                    throws ODataApplicationException {

        if (DemoEdmProvider.FUNCTION_COUNT_CATEGORIES.equals(uriResourceFunction.getFunctionImport().getName())) {
            // Get the parameter of the function
            UriParameter parameterAmount = uriResourceFunction.getParameters().get(0);

            // Try to convert the parameter to an Integer.
            // We have to take care, that the type of parameter fits to its EDM declaration
            int amount;
            try {
                amount = Integer.parseInt(parameterAmount.getText());
            } catch(NumberFormatException e) {
                throw new ODataApplicationException("Type of parameter Amount must be Edm.Int32",
                        HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
            }

            EdmEntityType productEntityType = serviceMetadata.getEdm().getEntityType(DemoEdmProvider.ET_PRODUCT_FQN);
            List<Entity> resultEntityList = new ArrayList<>();

            // Loop over all categories and check how many products are linked
            for (Entity category : categoryList) {
                EntityCollection products = getRelatedEntityCollection(category, productEntityType);
                if (products.getEntities().size() == amount) {
                    resultEntityList.add(category);
                }
            }

            EntityCollection resultCollection = new EntityCollection();
            resultCollection.getEntities().addAll(resultEntityList);
            return resultCollection;
        } else {
            throw new ODataApplicationException("Function not implemented", 
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
        }
    }

    public Entity readFunctionImportEntity(
            UriResourceFunction uriResourceFunction, ServiceMetadata serviceMetadata) 
                    throws ODataApplicationException {

        EntityCollection entityCollection = readFunctionImportCollection(uriResourceFunction, serviceMetadata);
        EdmEntityType edmEntityType = (EdmEntityType) uriResourceFunction.getFunction().getReturnType().getType();

        return Util.findEntity(edmEntityType, entityCollection, uriResourceFunction.getKeyPredicates());
    }
    
    public void resetDataSet(int amount) {
        // Replace the old lists with empty ones
        productList = new ArrayList<>();
        categoryList = new ArrayList<>();

        // Create new sample data
        initProductSampleData();
        initCategorySampleData();

        // Truncate the lists
        if (amount < productList.size()) {
            productList = productList.subList(0, amount);
            // Products 0, 1 are linked to category 0
            // Products 2, 3 are linked to category 1
            // Products 4, 5 are linked to category 2
            categoryList = categoryList.subList(0, (amount + 1) / 2);
        }
    }

    public void resetDataSet() {
        resetDataSet(Integer.MAX_VALUE);
    }
}

