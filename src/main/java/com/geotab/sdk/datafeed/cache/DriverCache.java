package com.geotab.sdk.datafeed.cache;

import static com.geotab.plain.Entities.DriverEntity;
import static com.geotab.util.Util.apply;

import com.geotab.api.Api;
import com.geotab.model.Id;
import com.geotab.plain.objectmodel.Driver;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@link Driver} cache singleton. Reloads controllers periodically on demand and caches them. */
public final class DriverCache extends GeotabEntityCache<Driver> {

  private static final Logger log = LoggerFactory.getLogger(DriverCache.class);

  public DriverCache(Api api) {
    super(api, null);
  }

  @Override
  protected Logger getLog() {
    return log;
  }

  @Override
  protected Optional<Driver> fetchEntity(String id) {
    log.debug("Loading Driver by id {} from Geotab…", id);
    return api.callGetById(DriverEntity, id);
  }

  @Override
  protected Optional<List<Driver>> fetchAll() {
    log.debug("Loading all Drivers from Geotab…");
    return api.callGetAll(DriverEntity);
  }

  @Override
  protected Driver createFakeCacheable(String id) {
    log.debug("No Driver with id {} found in Geotab; creating a fake Driver to cache it.", id);
    return apply(new Driver(), d -> d.setId(new Id(id)));
  }
}
