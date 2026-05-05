package com.geotab.sdk.datafeed.cache;

import static com.geotab.plain.Entities.UnitOfMeasureEntity;

import com.geotab.api.Api;
import com.geotab.model.Id;
import com.geotab.plain.objectmodel.engine.UnitOfMeasure;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link UnitOfMeasure} cache singleton. Reloads controllers periodically on demand and caches
 * them.
 */
public final class UnitOfMeasureCache extends GeotabEntityCache<UnitOfMeasure> {

  private static final Logger log = LoggerFactory.getLogger(UnitOfMeasureCache.class);

  public UnitOfMeasureCache(Api api) {
    super(api, null);
  }

  @Override
  protected Logger getLog() {
    return log;
  }

  @Override
  protected Optional<UnitOfMeasure> fetchEntity(String id) {
    log.debug("Loading UnitOfMeasure by id {} from Geotab…", id);
    return api.callGetById(UnitOfMeasureEntity, id);
  }

  @Override
  protected Optional<List<UnitOfMeasure>> fetchAll() {
    log.debug("Loading all UnitOfMeasures from Geotab…");
    return api.callGetAll(UnitOfMeasureEntity);
  }

  @Override
  protected UnitOfMeasure createFakeCacheable(String id) {
    log.debug("No UnitOfMeasure with id {} found in Geotab; creating a fake UnitOfMeasure to cache it.", id);
    UnitOfMeasure out = new UnitOfMeasure();
    out.setId(new Id(id));
    return out;
  }
}
