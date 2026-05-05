package com.geotab.sdk.datafeed.cache;

import static com.geotab.plain.Entities.FailureModeEntity;
import static com.geotab.util.Util.apply;

import com.geotab.api.Api;
import com.geotab.model.Id;
import com.geotab.plain.objectmodel.engine.FailureMode;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link FailureMode} cache singleton. Reloads controllers periodically on demand and caches them.
 */
public final class FailureModeCache extends GeotabEntityCache<FailureMode> {

  private static final Logger log = LoggerFactory.getLogger(FailureModeCache.class);

  public FailureModeCache(Api api) {
    super(api, null);
  }

  @Override
  protected Logger getLog() {
    return log;
  }

  @Override
  protected Optional<FailureMode> fetchEntity(String id) {
    log.debug("Loading FailureMode by id {} from Geotab…", id);
    return api.callGetById(FailureModeEntity, id);
  }

  @Override
  protected Optional<List<FailureMode>> fetchAll() {
    log.debug("Loading all FailureMode from Geotab…");
    return api.callGetAll(FailureModeEntity);
  }

  @Override
  protected FailureMode createFakeCacheable(String id) {
    log.debug(
        "No FailureMode with id {} found in Geotab; creating a fake FailureMode to cache it.", id);
    return apply(new FailureMode(), f -> f.setId(new Id(id)));
  }
}
