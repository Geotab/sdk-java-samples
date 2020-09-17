package com.geotab.sdk.datafeed.cache;

import com.geotab.api.GeotabApi;
import com.geotab.http.request.AuthenticatedRequest;
import com.geotab.http.request.param.SearchParameters;
import com.geotab.http.response.UnitOfMeasureListResponse;
import com.geotab.model.entity.unitofmeasure.UnitOfMeasure;
import com.geotab.model.entity.unitofmeasure.UnitOfMeasureNone;
import com.geotab.model.search.IdSearch;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

/**
 * {@link UnitOfMeasure} cache singleton. Reloads controllers periodically on demand and caches
 * them.
 */
@Slf4j
public final class UnitOfMeasureCache extends GeotabEntityCache<UnitOfMeasure> {

  public UnitOfMeasureCache(GeotabApi api) {
    super(api, UnitOfMeasureNone.getInstance());
  }

  @Override
  protected Logger getLog() {
    return log;
  }

  @Override
  protected Optional<UnitOfMeasure> fetchEntity(String id) throws Exception {
    log.debug("Loading UnitOfMeasure by id {} from Geotab ...", id);

    AuthenticatedRequest<?> request = AuthenticatedRequest.authRequestBuilder()
        .method("Get")
        .params(SearchParameters.searchParamsBuilder()
            .search(new IdSearch(id))
            .typeName("UnitOfMeasure")
            .build())
        .build();

    Optional<List<UnitOfMeasure>> unitOfMeasures = api
        .call(request, UnitOfMeasureListResponse.class);

    if (unitOfMeasures.isPresent() && !unitOfMeasures.get().isEmpty()) {
      log.debug("UnitOfMeasure by id {} loaded from Geotab.", id);
      return Optional.of(unitOfMeasures.get().get(0));
    }

    return Optional.empty();
  }

  @Override
  protected Optional<List<UnitOfMeasure>> fetchAll() throws Exception {
    log.debug("Loading all UnitOfMeasures from Geotab ...");
    AuthenticatedRequest<?> request = AuthenticatedRequest.authRequestBuilder()
        .method("Get")
        .params(SearchParameters.searchParamsBuilder()
            .typeName("UnitOfMeasure")
            .build())
        .build();

    return api.call(request, UnitOfMeasureListResponse.class);
  }

  @Override
  protected UnitOfMeasure createFakeCacheable(String id) {
    log.debug(
        "No UnitOfMeasure with id {} found in Geotab; creating a fake UnitOfMeasure to cache it.",
        id);
    return UnitOfMeasure.builder().id(id).build();
  }

}
