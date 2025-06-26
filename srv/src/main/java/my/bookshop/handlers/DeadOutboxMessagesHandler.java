package my.bookshop.handlers;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sap.cds.ql.Delete;
import com.sap.cds.ql.Update;
import com.sap.cds.ql.cqn.AnalysisResult;
import com.sap.cds.ql.cqn.CqnAnalyzer;
import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.environment.CdsProperties;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.persistence.PersistenceService;

import cds.gen.cds.outbox.Messages;
import cds.gen.cds.outbox.Messages_;
import cds.gen.outboxdeadletterqueueservice.DeadOutboxMessages;
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

    @After(service = PersistenceService.DEFAULT_NAME, entity = DeadOutboxMessages_.CDS_NAME)
    public void filterDeadEntries(CdsReadEventContext context) {
        CdsProperties.Outbox outboxConfigs = context.getCdsRuntime().getEnvironment().getCdsProperties().getOutbox();
        List<DeadOutboxMessages> deadEntries = context
                .getResult()
                .listOf(DeadOutboxMessages.class)
                .stream()
                .filter(entry -> entry.getAttempts() >= outboxConfigs.getService(entry.getTarget()).getMaxAttempts())
                .toList();

        context.setResult(deadEntries);
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
