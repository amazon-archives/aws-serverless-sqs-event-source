# Disclaimer

#### This repository is no longer supported. Please use natively supported [Lambda+SQS integration](https://docs.aws.amazon.com/lambda/latest/dg/with-sqs.html)

## AWS Serverless SQS Event Source

This serverless app periodically polls a given SQS queue and invokes a given lambda function to process messages. The app handles all interactions with the SQS queue and gives a simple interface for your lambda function to indicate a message was processed successfully or if it should be retried (with a configurable retry delay).

## Architecture

![App Architecture](https://github.com/awslabs/aws-serverless-sqs-event-source/raw/master/images/app-architecture.png)

1. The SQSPoller lambda function is triggered every 1 minute by a CloudWatch Events Rule.
1. If messages are received from the queue, the SQSPoller invokes the MessageProcessor lambda function (given when you deploy the application) to process a batch of messages.
1. The MessageProcessor function is expected to return a response indicating processing results of the message. Depending on the processing result, the SQSPoller will either delete the message from the queue or change the message visibility so it is retried later.

## Installation Steps

1. [Create an AWS account](https://portal.aws.amazon.com/gp/aws/developer/registration/index.html) if you do not already have one and login
1. Go to the app's page on the [Serverless Application Repository](https://serverlessrepo.aws.amazon.com/applications/arn:aws:serverlessrepo:us-east-1:077246666028:applications~aws-serverless-sqs-event-source) and click "Deploy"
1. Provide the required app parameters and deploy the app to your account.

Note - the S3 bucket used for uploads during the cloudformation stack deploy, must be in us-east-1 availability zone.

### Parameters

The app requires the following parameters:

1. MessageQueueName (required) - Name of the queue to poll.
1. MessageQueueUrl (required) - URL of the queue to poll.
1. MessageProcessorFunctionName (required) - Name of the lambda function that should be invoked to process messages from the queue. Note, this must be a function name and not a function ARN. It is assumed the function exists in the same region and is owned by the same account as the app.
1. SQSPollerMemorySize (optional) - Memory size of the SQSPoller lambda function. This is a parameter in case you have especially large messages and don't think the default memory size will be enough. Default: 512.

### MessageProcessor

The app requires the user to provide a message processor lambda function to process messages. The message processor will receive a list of SQS Message objects and is expected to return a list of message processing results in this format:

```
{
  "messageResults": [
    {
      "messageId": "processingSuccessExample",
      "status": "SUCCESS"
    },
    {
      "messageId": "processingErrorExample",
      "status": "ERROR"
    },
    {
      "messageId": "processingRetryExample",
      "status": "RETRY",
      "retryDelayInSeconds": 60
    }
  ]
}
```

Supported status values are:

1. `SUCCESS` - Indicates the message was processed successfully. In this case, the SQSPoller will remove it from the queue.
1. `RETRY` - Indicates the message processor would like the message to be retried after some time. If `retryDelayInSeconds` is specified, the SQSPoller will change the message's visibility so it is retried in that amount of time. If no retry delay is specified, it will use a default retry delay of 10 seconds.
1. `ERROR` - Indicates an error occurred while processing this message. In this case, the SQSPoller will do nothing with the message and rely on the queue's visibility timeout setting to determine when the message will be visible for retry.

If the message processor lambda function encounters an unhandled error (unsuccessful execution), the SQSPoller will do nothing with the messages sent to the lambda function, similar to how it handles the `ERROR` message result status.

#### aws-serverless-sqs-event-source-java-messageprocessor

This github repo also includes a maven convenience library to make it easier to write Java-based message processor lambda functions meant to interact with this app.

## License Summary

This sample code is made available under a modified MIT license. See the LICENSE file.
