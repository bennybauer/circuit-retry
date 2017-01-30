# Circuit-Retry
A sample java implementation of a circuit breaker ([Hystrix](https://github.com/Netflix/Hystrix)) together with a retry mechanism ([Failsafe](https://github.com/jhalterman/failsafe#retries)).
The retry is encapsulated in the http client ([`MyHttpClient`](src/main/java/com/bauer/sample/circuitretry/MyHttpClient)). When a Hystrix command is using `MyHttpClient` it isn't aware of retries. 
In fact, timeout responsibility is handled to retry and is disabled on the command.  
 
## Setup
`mvn install`

## Lessons Learned
### Hystrix
* Throwing a `HystrixTimeoutException` will be considered as `FAILURE` exuction exception and not `TIMEOUT`, unless [`execution.timeout.enabled`](https://github.com/Netflix/Hystrix/wiki/Configuration#executiontimeoutenabled) is false

### Failsafe
* There seems to be an issue with retrying `retryIf` more than once. It succeeds on first time but then the http client is hanging.
