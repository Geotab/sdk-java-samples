package com.geotab.sdk.datafeed.cache;

import static com.geotab.plain.Entities.DeviceEntity;
import static com.geotab.util.Util.apply;

import com.geotab.api.Api;
import com.geotab.model.Id;
import com.geotab.plain.objectmodel.Device;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@link Device} cache singleton. Reloads controllers periodically on demand and caches them. */
public final class DeviceCache extends GeotabEntityCache<Device> {

  private static final Logger log = LoggerFactory.getLogger(DeviceCache.class);

  public DeviceCache(Api api) {
    super(api, null);
  }

  @Override
  protected Logger getLog() {
    return log;
  }

  @Override
  protected Optional<Device> fetchEntity(String id) {
    log.debug("Loading Device by id {} from Geotab…", id);
    return api.callGetById(DeviceEntity, id);
  }

  @Override
  protected Optional<List<Device>> fetchAll() {
    log.debug("Loading all Device from Geotab…");
    return api.callGetAll(DeviceEntity);
  }

  @Override
  protected Device createFakeCacheable(String id) {
    log.debug("No Device with id {} found in Geotab; creating a fake Device to cache it.", id);
    return apply(new Device(), d -> d.setId(new Id(id)));
  }
}
