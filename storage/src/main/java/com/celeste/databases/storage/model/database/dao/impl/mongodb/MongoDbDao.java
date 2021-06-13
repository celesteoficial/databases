package com.celeste.databases.storage.model.database.dao.impl.mongodb;

import com.celeste.databases.core.adapter.impl.gson.GsonAdapter;
import com.celeste.databases.core.model.database.dao.exception.ValueNotFoundException;
import com.celeste.databases.core.model.database.provider.exception.FailedConnectionException;
import com.celeste.databases.core.util.Reflection;
import com.celeste.databases.storage.model.database.dao.AbstractStorageDao;
import com.celeste.databases.storage.model.database.provider.impl.mongodb.MongoDb;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import java.util.Arrays;
import lombok.SneakyThrows;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class MongoDbDao<T> extends AbstractStorageDao<MongoDb, T> {

  private final MongoCollection<Document> collection;

  public MongoDbDao(final MongoDb storage, final Class<T> entity) throws FailedConnectionException {
    super(storage, entity);

    this.collection = createCollection();
  }

  @SafeVarargs
  @Override
  public final void save(final T... entities) throws FailedConnectionException {
    save(Arrays.asList(entities));
  }

  @Override
  public void save(final List<T> entities) throws FailedConnectionException {
    try {
      final ReplaceOptions options = new ReplaceOptions().upsert(true);

      for (final T entity : entities) {
        final Object key = getEntity().getKey(entity);

        final Bson bson = Filters.eq(key);
        final Document document = serialize(entity);

        collection.replaceOne(bson, document, options);
      }
    } catch (Exception exception) {
      throw new FailedConnectionException(exception);
    }
  }

  @SafeVarargs
  @Override
  public final void delete(final T... entities) throws FailedConnectionException {
    try {
      for (final T entity : entities) {
        final Object key = getEntity().getKey(entity);
        final Bson bson = Filters.eq(key);

        collection.deleteOne(bson);
      }
    } catch (Exception exception) {
      throw new FailedConnectionException(exception);
    }
  }

  @Override
  public void delete(final List<T> entities) throws FailedConnectionException {
    for (T entity : entities) {
      delete(entity);
    }
  }

  @Override
  public boolean contains(final Object key) throws FailedConnectionException {
    try {
      final Bson bson = Filters.eq(key);

      return collection.countDocuments(bson) > 1;
    } catch (Exception exception) {
      throw new FailedConnectionException(exception);
    }
  }

  @Override
  public T find(final Object key) throws ValueNotFoundException, FailedConnectionException {
    try {
      final Bson bson = Filters.eq(key);
      final Document document = collection.find(bson).first();

      if (document == null) {
        throw new ValueNotFoundException("Value not found");
      }

      return deserialize(document);
    } catch (ValueNotFoundException exception) {
      throw new ValueNotFoundException(exception);
    } catch (Exception exception) {
      throw new FailedConnectionException(exception);
    }
  }

  @Override
  public List<T> findAll() throws FailedConnectionException {
    try {
      final List<T> entities = new ArrayList<>();

      try (MongoCursor<Document> cursor = collection.find().cursor()) {
        while (cursor.hasNext()) {
          final Document document = cursor.next();
          entities.add(deserialize(document));
        }
      }

      return entities;
    } catch (Exception exception) {
      throw new FailedConnectionException(exception);
    }
  }

  private MongoCollection<Document> createCollection() throws FailedConnectionException {
    try {
      final MongoDatabase database = getDatabase().getDatabase();
      final String collection = getEntity().getName();

      for (final String name : database.listCollectionNames()) {
        if (collection.equalsIgnoreCase(name)) {
          return database.getCollection(collection);
        }
      }

      database.createCollection(collection);
      return database.getCollection(collection);
    } catch (Exception exception) {
      throw new FailedConnectionException(exception);
    }
  }

  @SneakyThrows
  private Document serialize(final T entity) {
    final Map<String, Object> values = getEntity().getValues(entity);
    final Document document = new Document();

    document.putAll(values);
    return document;
  }

  @SneakyThrows
  private T deserialize(final Document document) {
    final Map<String, Field> values = getEntity().getValues();
    final T entity = Reflection.getDcConstructor(getClazz()).newInstance();

    for (final Entry<String, Field> entry : values.entrySet()) {
      Object object = document.getOrDefault(entry.getKey(), null);

      if (object instanceof Document) {
        final Class<?> clazz = Reflection.getClazz(entry.getValue());
        final String json = ((Document) object).toJson();

        object = GsonAdapter.getInstance().deserialize(json, clazz);
      }

      entry.getValue().set(entity, object);
    }

    return entity;
  }

}
