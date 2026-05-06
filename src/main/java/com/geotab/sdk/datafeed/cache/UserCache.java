package com.geotab.sdk.datafeed.cache;

import static com.geotab.plain.Entities.UserEntity;
import static com.geotab.util.Util.apply;

import com.geotab.api.Api;
import com.geotab.model.Id;
import com.geotab.plain.objectmodel.User;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@link User} cache singleton. Reloads controllers periodically on demand and caches them. */
public final class UserCache extends GeotabEntityCache<User> {

  private static final Logger log = LoggerFactory.getLogger(UserCache.class);

  public UserCache(Api api) {
    super(api, null);
  }

  @Override
  protected Logger getLog() {
    return log;
  }

  @Override
  protected Optional<User> fetchEntity(String id) {
    log.debug("Loading User by id {} from Geotab…", id);
    return api.callGetById(UserEntity, id);
  }

  @Override
  protected Optional<List<User>> fetchAll() {
    log.debug("Loading all Users from Geotab…");
    return api.callGetAll(UserEntity);
  }

  @Override
  protected User createFakeCacheable(String id) {
    log.debug("No User with id {} found in Geotab; creating a fake User to cache it.", id);
    return apply(new User(), d -> d.setId(new Id(id)));
  }
}
