using from '@sap/cds/srv/outbox';

@requires: 'internal-user'
@path: 'dlq'
service OutboxDeadLetterQueueService {

  @readonly
  entity DeadOutboxMessages as projection on cds.outbox.Messages
    actions {
      action revive();
      action delete();
    };

}
