package com.geotab.sdk.datafeed.cache;

import static com.geotab.plain.Entities.DiagnosticEntity;
import static com.geotab.util.Util.apply;

import com.geotab.api.Api;
import com.geotab.model.Id;
import com.geotab.plain.objectmodel.engine.Diagnostic;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Diagnostic} cache singleton. Reloads controllers periodically on demand and caches them.
 */
public final class DiagnosticCache extends GeotabEntityCache<Diagnostic> {

  private static final Logger log = LoggerFactory.getLogger(DiagnosticCache.class);
  private ControllerCache controllerCache;
  private UnitOfMeasureCache unitOfMeasureCache;

  public DiagnosticCache(
      Api api, ControllerCache controllerCache, UnitOfMeasureCache unitOfMeasureCache) {
    super(api, null);
    this.controllerCache = controllerCache;
    this.unitOfMeasureCache = unitOfMeasureCache;
  }

  @Override
  protected Logger getLog() {
    return log;
  }

  @Override
  protected Optional<Diagnostic> fetchEntity(String id) {
    log.debug("Loading Diagnostic by id {} from Geotab…", id);
    return api.callGetById(DiagnosticEntity, id);
  }

  @Override
  protected Optional<List<Diagnostic>> fetchAll() {
    log.debug("Loading all Diagnostic from Geotab…");
    return api.callGetAll(DiagnosticEntity);
  }

  @Override
  protected Diagnostic createFakeCacheable(String id) {
    log.debug(
        "No Diagnostic with id {} found in Geotab; creating a fake Diagnostic to cache it.", id);
    return apply(new Diagnostic(), d -> d.setId(new Id(id)));
  }

  @Override
  public Diagnostic get(String id) {
    Diagnostic diagnostic = super.get(id);

    if (diagnostic == null) {
      return null;
    }

    if (diagnostic.controller != null && diagnostic.controller.getId() != null) {
      diagnostic.controller = controllerCache.get(diagnostic.controller.getId().getId());
    }

    if (diagnostic.unitOfMeasure != null && diagnostic.unitOfMeasure.getId() != null) {
      diagnostic.unitOfMeasure = unitOfMeasureCache.get(diagnostic.unitOfMeasure.getId().getId());
    }

    return diagnostic;
  }
}
