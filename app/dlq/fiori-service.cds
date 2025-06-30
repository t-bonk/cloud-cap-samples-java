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
