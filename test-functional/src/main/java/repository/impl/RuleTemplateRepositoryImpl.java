package repository.impl;

import com.github.longdt.vertxorm.repository.postgresql.AbstractCrudRepository;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import model.RuleTemplate;
import repository.RuleTemplateRepository;

public class RuleTemplateRepositoryImpl extends AbstractCrudRepository<Long, RuleTemplate> implements RuleTemplateRepository {
    @Override
    public void doSomething(Handler<AsyncResult<Long>> resultHandler) {

    }
}
