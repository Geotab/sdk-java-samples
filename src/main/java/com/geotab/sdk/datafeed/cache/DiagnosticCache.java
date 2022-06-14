package com.geotab.sdk.datafeed.cache;

import com.geotab.api.Api;
import com.geotab.http.request.param.SearchParameters;
import com.geotab.model.entity.diagnostic.BasicDiagnostic;
import com.geotab.model.entity.diagnostic.Diagnostic;
import com.geotab.model.entity.diagnostic.NoDiagnostic;
import com.geotab.model.search.IdSearch;
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
      Api api,
      ControllerCache controllerCache,
      UnitOfMeasureCache unitOfMeasureCache
  ) {
    super(api, NoDiagnostic.getInstance());
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
    Optional<List<Diagnostic>> diagnostics = api.callGet(SearchParameters.searchParamsBuilder()
        .search(new IdSearch(id)).typeName("Diagnostic").build(), Diagnostic.class);

    if (diagnostics.isPresent() && !diagnostics.get().isEmpty()) {
      log.debug("Diagnostic by id {} loaded from Geotab.", id);
      return Optional.of(diagnostics.get().get(0));
    }

    return Optional.empty();
  }

  @Override
  protected Optional<List<Diagnostic>> fetchAll() {
    log.debug("Loading all Diagnostic from Geotab…");
    return api.callGet(SearchParameters.searchParamsBuilder().typeName("Diagnostic").build(), Diagnostic.class);
  }

  @Override
  protected Diagnostic createFakeCacheable(String id) {
    log.debug(
        "No Diagnostic with id {} found in Geotab; creating a fake Diagnostic to cache it.",
        id);
    return BasicDiagnostic.basicDiagnosticBuilder().id(id).build();
  }

  @Override
  public Diagnostic get(String id) {
    Diagnostic diagnostic = super.get(id);

    if (diagnostic.getController() != null) {
      diagnostic.setController(controllerCache.get(diagnostic.getController().getId().getId()));
    }

    if (diagnostic.getUnitOfMeasure() != null) {
      diagnostic
          .setUnitOfMeasure(unitOfMeasureCache.get(diagnostic.getUnitOfMeasure().getId().getId()));
    }

    return diagnostic;
  }
}
