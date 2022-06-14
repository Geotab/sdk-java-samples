package com.geotab.sdk.datafeed.cache;

import com.geotab.api.Api;
import com.geotab.http.request.param.SearchParameters;
import com.geotab.model.entity.controller.Controller;
import com.geotab.model.entity.controller.NoController;
import com.geotab.model.search.ControllerSearch;
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
    super(api, NoController.getInstance());
  }

  @Override
  protected Logger getLog() {
    return log;
  }

  @Override
  protected Optional<Controller> fetchEntity(String id) {
    log.debug("Loading Controller by id {} from Geotab…", id);
    Optional<List<Controller>> controllers = api.callGet(SearchParameters.searchParamsBuilder()
        .search(ControllerSearch.builder().id(id).build()).typeName("Controller").build(), Controller.class);

    if (controllers.isPresent() && !controllers.get().isEmpty()) {
      log.debug("Controller by id {} loaded from Geotab.", id);
      return Optional.of(controllers.get().get(0));
    }

    return Optional.empty();
  }

  @Override
  protected Optional<List<Controller>> fetchAll() {
    log.debug("Loading all Controllers from Geotab…");
    return api.callGet(SearchParameters.searchParamsBuilder().typeName("Controller").build(), Controller.class);
  }

  @Override
  protected Controller createFakeCacheable(String id) {
    log.debug("No Controller with id {} found in Geotab; creating a fake Controller to cache it.", id);
    return Controller.builder().id(id).build();
  }
}
