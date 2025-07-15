using OutboxDeadLetterQueueService from '../../srv/outbox-dead-letter-queue';

annotate OutboxDeadLetterQueueService.DeadOutboxMessages with @(UI : {
    LineItem : [
        {
          Value: ID,
          Label : '{i18n>ID}'
        },
        {
          Value: timestamp,
          Label : '{i18n>timestamp}'
        },
        {
          Value: target,
          Label : '{i18n>target}'
        },
        {
          Value: msg,
          Label : '{i18n>msg}'
        },
        {
          Value: attempts,
          Label : '{i18n>attempts}'
        },
        {
          Value: lastError,
          Label : '{i18n>lastError}'
        },
        {
          Value: lastAttemptTimestamp,
          Label : '{i18n>lastAttemptTimestamp}'
        },
        {
          $Type : 'UI.DataFieldForAction',
          Action: 'OutboxDeadLetterQueueService.revive',
          Label : 'Revive'
        },
        {
          $Type : 'UI.DataFieldForAction',
          Action: 'OutboxDeadLetterQueueService.delete',
          Label : 'Delete'
        }
    ],
    HeaderInfo : {
        TypeName : '{i18n>DeadOutboxMessage}',
        TypeNamePlural: '{i18n>DeadOutboxMessages}',
        Title : {Value : ID},
        Description : {Value : target}
    },
    PresentationVariant : {
        Text : 'Default',
        Visualizations : ['@UI.LineItem']
    }
});

annotate OutboxDeadLetterQueueService.DeadOutboxMessages with actions {
  @(SideEffects : {
      //TargetEntities : ['OutboxDeadLetterQueueService.DeadOutboxMessages']
      TargetProperties : ['in/attempts']
      //TargetEntities : []
  }) revive;
  @(SideEffects : {
      //TargetEntities : ['OutboxDeadLetterQueueService.DeadOutboxMessages']
      //TargetProperties : [in]
      // Marcel Wächter fragen, falls es nicht funktioniert
      TargetEntities : ['OutboxDeadLetterQueueService.EntityContainer/DeadOutboxMessages']
  }) delete;
}
