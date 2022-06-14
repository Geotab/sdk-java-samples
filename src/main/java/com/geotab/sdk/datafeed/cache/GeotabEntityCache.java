package com.geotab.sdk.datafeed.cache;

import com.geotab.api.Api;
import com.geotab.model.entity.Entity;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;

/**
 * Base {@link Entity} cache.
 */
public abstract class GeotabEntityCache<T extends Entity> {

  protected LoadingCache<String, T> cache;

  protected Api api;

  protected T noEntity;

  protected GeotabEntityCache(Api api, T noEntity) {
    this.api = api;
    this.noEntity = noEntity;
    this.cache = CacheBuilder.newBuilder()
        .maximumSize(600)
        //.expireAfterWrite(12, TimeUnit.HOURS)
        .build(new CacheLoader<String, T>() {
          @Override
          public T load(String key) {
            Optional<T> entity = fetchEntity(key);
            return entity.orElseGet(() -> createFakeCacheable(key));
          }
        });
  }

  /**
   * Get class logger.
   *
   * @return The logger.
   */
  protected abstract Logger getLog();

  /**
   * Load entity by id from Geotab.
   *
   * @param id The entity id.
   * @return The Entity.
   */
  protected abstract Optional<T> fetchEntity(String id);

  /**
   * Load all entities from Geotab.
   *
   * @return All entities.
   */
  protected abstract Optional<List<T>> fetchAll();

  /**
   * In the extreme unlike scenario when the entity is not found in Geotab system by the id, then create a fake entity
   * of the required type.
   *
   * @param id The entity id.
   * @return Fake entity with the provided id.
   */
  protected abstract T createFakeCacheable(String id);

  /**
   * Cache the NoEntity instance.
   *
   * @return Whether the operation succeeded or not.
   */
  protected boolean cacheNoEntity() {
    if (noEntity != null) {
      cache.put(noEntity.getId().getId(), noEntity);
    }
    return true;
  }

  /**
   * Get entity by id.
   *
   * @param id The entity id.
   * @return The Entity.
   */
  public T get(String id) {
    if (id == null || id.isEmpty()) {
      return noEntity;
    }

    getLog().debug("Get entity with id = {}", id);

    try {
      return cache.get(id);
    } catch (Exception e) {
      getLog().error("Can not load for id {}", id, e);
    }

    return noEntity;
  }

  /**
   * Invalidate/flush all cached entities.
   *
   * @return Whether the operation succeeded or not.
   */
  public synchronized boolean flush() {
    if (cache != null) {
      getLog().debug("Removing all from cache…");

      try {
        cache.invalidateAll();
      } catch (Exception e) {
        getLog().error(".flush() {}", cache, e);
        return false;
      }

      getLog().debug("Cache invalidated.");
    }
    return true;
  }

  /**
   * Invalidate cache and reload all entities from Geotab.
   *
   * @return Whether the operation succeeded or not.
   */
  public synchronized boolean reloadAll() {
    getLog().debug("Reloading cache…");

    boolean reloaded = flush();

    try {
      Optional<List<T>> entities = fetchAll();
      if (entities.isPresent() && !entities.get().isEmpty()) {
        for (T entity : entities.get()) {
          cache.put(entity.getId().getId(), entity);
        }
      }
    } catch (Exception exception) {
      getLog().error("Failed to reload entities - ", exception);
      reloaded = false;
    }

    reloaded = reloaded && cacheNoEntity();

    getLog().debug("Cache was{} reloaded", reloaded ? "" : " not");

    return reloaded;
  }
}
