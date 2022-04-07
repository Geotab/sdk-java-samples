package com.geotab.sdk.datafeed.cache;

import com.geotab.api.GeotabApi;
import com.geotab.http.request.AuthenticatedRequest;
import com.geotab.http.request.param.SearchParameters;
import com.geotab.http.response.UserListResponse;
import com.geotab.model.entity.user.Driver;
import com.geotab.model.entity.user.NoDriver;
import com.geotab.model.entity.user.UnknownDriver;
import com.geotab.model.entity.user.User;
import com.geotab.model.search.UserSearch;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Driver} cache singleton. Reloads controllers periodically on demand and caches them.
 */
public final class DriverCache extends GeotabEntityCache<Driver> {

  private static final Logger log = LoggerFactory.getLogger(DriverCache.class);

  public DriverCache(GeotabApi api) {
    super(api, NoDriver.getInstance());
  }

  @Override
  protected Logger getLog() {
    return log;
  }

  @Override
  protected Optional<Driver> fetchEntity(String id) {
    log.debug("Loading Driver by id {} from Geotab ...", id);

    AuthenticatedRequest<?> request = AuthenticatedRequest.authRequestBuilder()
        .method("Get")
        .params(SearchParameters.searchParamsBuilder()
            .search(
                UserSearch.builder()
                    .id(id)
                    .isDriver(true)
                    .build()
            )
            .typeName("User")
            .build())
        .build();

    Optional<List<? extends User>> drivers = api.call(request, UserListResponse.class);

    if (drivers.isPresent() && !drivers.get().isEmpty()) {
      log.debug("Driver by id {} loaded from Geotab.", id);
      return Optional.of((Driver) drivers.get().get(0));
    }

    return Optional.empty();
  }

  @Override
  protected Optional<List<Driver>> fetchAll() {
    log.debug("Loading all Drivers from Geotab ...");
    AuthenticatedRequest<?> request = AuthenticatedRequest.authRequestBuilder()
        .method("Get")
        .params(SearchParameters.searchParamsBuilder()
            .search(
                UserSearch.builder()
                    .isDriver(true)
                    .build()
            )
            .typeName("User")
            .build())
        .build();

    Optional<List<? extends User>> drivers = api.call(request, UserListResponse.class);

    return drivers.map(
        users -> users.stream()
            .map(Driver.class::cast)
            .collect(Collectors.toList())
    );
  }

  @Override
  protected Driver createFakeCacheable(String id) {
    log.debug("No Driver with id {} found in Geotab; creating a fake Driver to cache it.", id);
    return Driver.driverBuilder().id(id).build();
  }

  @Override
  protected boolean cacheNoEntity() {
    cache.put(NoDriver.getInstance().getId().getId(), NoDriver.getInstance());
    cache.put(UnknownDriver.getInstance().getId().getId(), UnknownDriver.getInstance());
    return true;
  }
}
