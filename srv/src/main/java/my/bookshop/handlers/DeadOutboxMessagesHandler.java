package my.bookshop.handlers;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import com.sap.cds.CdsData;
import com.sap.cds.Result;
import com.sap.cds.ResultBuilder;
import com.sap.cds.Row;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.Predicate;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.StructuredType;
import com.sap.cds.ql.cqn.CqnPredicate;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.services.environment.CdsProperties.Outbox.OutboxServiceConfig;
import com.sap.cds.services.outbox.OutboxService;
import com.sap.cds.services.runtime.CdsRuntime;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sap.cds.ql.Delete;
import com.sap.cds.ql.Update;
import com.sap.cds.ql.cqn.AnalysisResult;
import com.sap.cds.ql.cqn.CqnAnalyzer;
import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.environment.CdsProperties;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.persistence.PersistenceService;

import cds.gen.cds.outbox.Messages;
import cds.gen.cds.outbox.Messages_;
import cds.gen.outboxdeadletterqueueservice.DeadOutboxMessagesDeleteContext;
import cds.gen.outboxdeadletterqueueservice.DeadOutboxMessagesReviveContext;
import cds.gen.outboxdeadletterqueueservice.DeadOutboxMessages_;
import cds.gen.outboxdeadletterqueueservice.OutboxDeadLetterQueueService_;

@Component
@ServiceName(OutboxDeadLetterQueueService_.CDS_NAME)
public class DeadOutboxMessagesHandler implements EventHandler {

    private final PersistenceService db;

    public DeadOutboxMessagesHandler(@Qualifier(PersistenceService.DEFAULT_NAME) PersistenceService db) {
        this.db = db;
    }

    @On(entity = DeadOutboxMessages_.CDS_NAME)
    public void readDeadOutboxMessages(CdsReadEventContext context) {
        CqnSelect cqn = context.getCqn();
        Optional<Predicate> outboxFilters = this.createOutboxFilters(context.getCdsRuntime());
        Select<StructuredType<?>> select = Select
          .from(Messages_.CDS_NAME)
          .columns(cqn.items());

        select = select.groupBy(cqn.groupBy()).excluding(cqn.excluding());
        if(cqn.having().isPresent()) {
            select = select.having(cqn.having().get());
        }
        if(cqn.search().isPresent()) {
            select.search(cqn.search().get());
        }
        if(cqn.where().isPresent()) {
            CqnPredicate where = cqn.where().get();
            if (outboxFilters.isPresent()) {
                where = outboxFilters.get().and(where);
            }
            select = select.where(where);
        } else if (outboxFilters.isPresent()) {
            select = select.where(outboxFilters.get());
        }
        select = select.orderBy(cqn.orderBy()).limit(cqn.top(), cqn.skip()).inlineCount();

        List<Row> deadMessages = this.db.run(select).list();
        context.setResult(ResultBuilder.selectedRows(deadMessages).inlineCount(deadMessages.size()).result());
    }

    private Optional<Predicate> createOutboxFilters(CdsRuntime runtime) {
        List<OutboxService> outboxServices = runtime.getServiceCatalog().getServices(OutboxService.class)
                .filter(s -> !s.getName().equals(OutboxService.INMEMORY_NAME)).toList();
        CdsProperties.Outbox outboxConfigs = runtime.getEnvironment().getCdsProperties().getOutbox();

        Predicate where = null;
        for(OutboxService service : outboxServices) {
            OutboxServiceConfig config = outboxConfigs.getService(service.getName());
            Predicate targetPredicate = CQL.get(Messages.TARGET).eq(service.getName()).and(CQL.get(Messages.ATTEMPTS).ge(config.getMaxAttempts()));

            if (where == null) {
                where = targetPredicate;
            } else {
                where = where.or(targetPredicate);
            }
        }

        return Optional.ofNullable(where);
    }

    @On
    public void reviveOutboxMessage(DeadOutboxMessagesReviveContext context) {
        CqnAnalyzer analyzer = CqnAnalyzer.create(context.getModel());
        AnalysisResult analysisResult = analyzer.analyze(context.getCqn());
        Map<String, Object> key = analysisResult.rootKeys();
        Messages deadOutboxMessage = Messages.create((String) key.get(Messages.ID));

        deadOutboxMessage.setAttempts(0);

        this.db.run(Update.entity(Messages_.class).entry(key).data(deadOutboxMessage));
        context.setCompleted();
    }

    @On
    public void deleteOutboxEntry(DeadOutboxMessagesDeleteContext context) {
        CqnAnalyzer analyzer = CqnAnalyzer.create(context.getModel());
        AnalysisResult analysisResult = analyzer.analyze(context.getCqn());
        Map<String, Object> key = analysisResult.rootKeys();

        this.db.run(Delete.from(Messages_.class).byId(key.get(Messages.ID)));
        context.setCompleted();
    }
}
