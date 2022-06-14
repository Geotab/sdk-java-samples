package com.geotab.sdk.datafeed.cache;

import com.geotab.api.Api;
import com.geotab.http.request.param.SearchParameters;
import com.geotab.model.entity.device.Device;
import com.geotab.model.entity.device.NoDevice;
import com.geotab.model.search.IdSearch;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Device} cache singleton. Reloads controllers periodically on demand and caches them.
 */
public final class DeviceCache extends GeotabEntityCache<Device> {

  private static final Logger log = LoggerFactory.getLogger(DeviceCache.class);

  public DeviceCache(Api api) {
    super(api, NoDevice.getInstance());
  }

  @Override
  protected Logger getLog() {
    return log;
  }

  @Override
  protected Optional<Device> fetchEntity(String id) {
    log.debug("Loading Device by id {} from Geotab…", id);
    Optional<List<Device>> devices = api.callGet(SearchParameters.searchParamsBuilder()
        .search(new IdSearch(id)).typeName("Device").build(), Device.class);

    if (devices.isPresent() && !devices.get().isEmpty()) {
      log.debug("Device by id {} loaded from Geotab.", id);
      return Optional.of(devices.get().get(0));
    }

    return Optional.empty();
  }

  @Override
  protected Optional<List<Device>> fetchAll() {
    log.debug("Loading all Device from Geotab…");
    return api.callGet(SearchParameters.searchParamsBuilder().typeName("Device").build(), Device.class);
  }

  @Override
  protected Device createFakeCacheable(String id) {
    log.debug("No Device with id {} found in Geotab; creating a fake Device to cache it.", id);
    return Device.builder().id(id).build();
  }
}
