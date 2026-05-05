package com.geotab.sdk.datafeed.cache;

import static com.geotab.plain.Entities.ControllerEntity;
import static com.geotab.util.Util.apply;

import com.geotab.api.Api;
import com.geotab.model.Id;
import com.geotab.plain.objectmodel.engine.Controller;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Controller} cache singleton. Reloads controllers periodically on demand and caches them.
 */
public final class ControllerCache extends GeotabEntityCache<Controller> {

  private static final Logger log = LoggerFactory.getLogger(ControllerCache.class);

  public ControllerCache(Api api) {
    super(api, null);
  }

  @Override
  protected Logger getLog() {
    return log;
  }

  @Override
  protected Optional<Controller> fetchEntity(String id) {
    log.debug("Loading Controller by id {} from Geotab…", id);
    return api.callGetById(ControllerEntity, id);
  }

  @Override
  protected Optional<List<Controller>> fetchAll() {
    log.debug("Loading all Controllers from Geotab…");
    return api.callGetAll(ControllerEntity);
  }

  @Override
  protected Controller createFakeCacheable(String id) {
    log.debug("No Controller with id {} found in Geotab; creating a fake Controller to cache it.", id);
    return apply(new Controller(), c -> c.setId(new Id(id)));
  }
}
