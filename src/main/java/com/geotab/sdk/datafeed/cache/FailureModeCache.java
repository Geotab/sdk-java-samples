package com.geotab.sdk.datafeed.cache;

import com.geotab.api.Api;
import com.geotab.http.request.param.SearchParameters;
import com.geotab.model.entity.failuremode.FailureMode;
import com.geotab.model.entity.failuremode.NoFailureMode;
import com.geotab.model.search.IdSearch;
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
    super(api, NoFailureMode.getInstance());
  }

  @Override
  protected Logger getLog() {
    return log;
  }

  @Override
  protected Optional<FailureMode> fetchEntity(String id) {
    log.debug("Loading FailureMode by id {} from Geotab…", id);
    Optional<List<FailureMode>> failureModes = api.callGet(SearchParameters.searchParamsBuilder()
        .search(new IdSearch(id)).typeName("FailureMode").build(), FailureMode.class);

    if (failureModes.isPresent() && !failureModes.get().isEmpty()) {
      log.debug("FailureMode by id {} loaded from Geotab.", id);
      return Optional.of(failureModes.get().get(0));
    }

    return Optional.empty();
  }

  @Override
  protected Optional<List<FailureMode>> fetchAll() {
    log.debug("Loading all FailureMode from Geotab…");
    return api.callGet(SearchParameters.searchParamsBuilder().typeName("FailureMode").build(), FailureMode.class);
  }

  @Override
  protected FailureMode createFakeCacheable(String id) {
    log.debug("No FailureMode with id {} found in Geotab; creating a fake FailureMode to cache it.", id);
    return FailureMode.builder().id(id).build();
  }
}
