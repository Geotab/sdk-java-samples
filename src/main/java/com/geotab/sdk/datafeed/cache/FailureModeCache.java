package com.geotab.sdk.datafeed.cache;

import com.geotab.api.GeotabApi;
import com.geotab.http.request.AuthenticatedRequest;
import com.geotab.http.request.param.SearchParameters;
import com.geotab.http.response.FailureModeListResponse;
import com.geotab.model.entity.failuremode.FailureMode;
import com.geotab.model.entity.failuremode.NoFailureMode;
import com.geotab.model.search.IdSearch;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

/**
 * {@link FailureMode} cache singleton. Reloads controllers periodically on demand and caches them.
 */
@Slf4j
public final class FailureModeCache extends GeotabEntityCache<FailureMode> {

  public FailureModeCache(GeotabApi api) {
    super(api, NoFailureMode.getInstance());
  }

  @Override
  protected Logger getLog() {
    return log;
  }

  @Override
  protected Optional<FailureMode> fetchEntity(String id) throws Exception {
    log.debug("Loading FailureMode by id {} from Geotab ...", id);

    AuthenticatedRequest<?> request = AuthenticatedRequest.authRequestBuilder()
        .method("Get")
        .params(SearchParameters.searchParamsBuilder()
            .search(new IdSearch(id))
            .typeName("FailureMode")
            .build())
        .build();

    Optional<List<FailureMode>> failureModes = api.call(request, FailureModeListResponse.class);

    if (failureModes.isPresent() && !failureModes.get().isEmpty()) {
      log.debug("FailureMode by id {} loaded from Geotab.", id);
      return Optional.of(failureModes.get().get(0));
    }

    return Optional.empty();
  }

  @Override
  protected Optional<List<FailureMode>> fetchAll() throws Exception {
    log.debug("Loading all FailureMode from Geotab ...");
    AuthenticatedRequest<?> request = AuthenticatedRequest.authRequestBuilder()
        .method("Get")
        .params(SearchParameters.searchParamsBuilder()
            .typeName("FailureMode")
            .build())
        .build();

    return api.call(request, FailureModeListResponse.class);
  }

  @Override
  protected FailureMode createFakeCacheable(String id) {
    log.debug("No FailureMode with id {} found in Geotab; creating a fake FailureMode to cache it.", id);
    return FailureMode.failureModeBuilder().id(id).build();
  }
}
