package com.geotab.sdk.datafeed.cache;

import com.geotab.api.GeotabApi;
import com.geotab.http.request.AuthenticatedRequest;
import com.geotab.http.request.param.SearchParameters;
import com.geotab.http.response.DeviceListResponse;
import com.geotab.model.entity.device.Device;
import com.geotab.model.entity.device.NoDevice;
import com.geotab.model.search.IdSearch;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

/**
 * {@link Device} cache singleton. Reloads controllers periodically on demand and caches them.
 */
@Slf4j
public final class DeviceCache extends GeotabEntityCache<Device> {

  public DeviceCache(GeotabApi api) {
    super(api, NoDevice.getInstance());
  }

  @Override
  protected Logger getLog() {
    return log;
  }

  @Override
  protected Optional<Device> fetchEntity(String id) throws Exception {
    log.debug("Loading Device by id {} from Geotab ...", id);

    AuthenticatedRequest<?> request = AuthenticatedRequest.authRequestBuilder()
        .method("Get")
        .params(SearchParameters.searchParamsBuilder()
            .search(new IdSearch(id))
            .typeName("Device")
            .build())
        .build();

    Optional<List<Device>> devices = api.call(request, DeviceListResponse.class);

    if (devices.isPresent() && !devices.get().isEmpty()) {
      log.debug("Device by id {} loaded from Geotab.", id);
      return Optional.of(devices.get().get(0));
    }

    return Optional.empty();
  }

  @Override
  protected Optional<List<Device>> fetchAll() throws Exception {
    log.debug("Loading all Device from Geotab ...");
    AuthenticatedRequest<?> request = AuthenticatedRequest.authRequestBuilder()
        .method("Get")
        .params(SearchParameters.searchParamsBuilder()
            .typeName("Device")
            .build())
        .build();

    return api.call(request, DeviceListResponse.class);
  }

  @Override
  protected Device createFakeCacheable(String id) {
    log.debug(
        "No Device with id {} found in Geotab; creating a fake Device to cache it.",
        id);
    return Device.builder().id(id).build();
  }

}
