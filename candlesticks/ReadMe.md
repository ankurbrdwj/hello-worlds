#  Coding Challenge

Your task is to build a system that enables users to view price histories.
It will receive updates from a partner service, transform these updates and provide the aggregated data through an endpoint.

## Content
- Intro and terminology
- Setup
- Future Development Discussion

## Intro and terminology
#  Coding Challenge

Your task is to build a system that enables users to view price histories.
It will receive updates from a partner service, transform these updates and provide the aggregated data through an endpoint.

## Content
- Intro and terminology
- Setup
- Future Development Discussion

## Intro and terminology

#### Instruments and Quotes
Every asset that can be traded is represented by an “instrument”, which has a unique identifier (ISIN).
Each time the instrument price changes, an update message called “quote” is broadcasted for this instrument to inform about the change.

#### What is a candlestick?
A [candlestick](https://en.wikipedia.org/wiki/Candlestick_chart) is a representation that describes the price movement for a given instrument in a fixed amount of time, usually one minute.
We will be using a simplified version of candlesticks for this challenge.

![chart](https://t4.ftcdn.net/jpg/02/79/65/79/360_F_279657943_FALhJZ6g4shXyfqMIRifp1l6lhiwhbwm.jpg)

The basic idea is that we don't need to know about _all_ prices changes within a given timeframe.
Usually we want them grouped in 1 minute chunks, because we are more interested in some key data points within any given minute.
In theory, a candlestick “contains” all quotes, where the timestamp of the quote is higher than the openTimestamp and lower than the closeTimestamp (`openTimestamp <= quoteTimestamp < closeTimestamp`).
However, for each candle for each given minute, we only present the following data points to the user:
- the first quotes price, that was received (openPrice)
- the last quotes, that was received (closePrice)
- the highest quote price that was observed (highPrice)
- the lowest quote price that was observed (lowPrice)
- the timestamp when the candlestick was opened (openTimestamp)
- the timestamp when the candlestick was closed (closeTimestamp)

##### Example
Assume the following (simplified) data was received for an instrument:
```
@2019-03-05 13:00:05 price: 10
@2019-03-05 13:00:06 price: 11
@2019-03-05 13:00:13 price: 15
@2019-03-05 13:00:19 price: 11
@2019-03-05 13:00:32 price: 13
@2019-03-05 13:00:49 price: 12
@2019-03-05 13:00:57 price: 12
@2019-03-05 13:01:00 price: 9
```
The resulting minute candle would have these attributes:
```
openTimestamp: 2019-03-05 13:00:00
openPrice: 10
highPrice: 15
lowPrice: 10
closePrice: 12
closeTimestamp: 13:01:00
```
Note that the last received quote with a price of 9 is not part of this candlestick anymore, but belongs to the new candlestick.

### Input data
The input data is received through a websocket stream from a partner service.
The code provides handles connecting and consuming the stream (`Streams.kt`).
There are two types of input messages:

- Instrument additions/deletions, which adds or removes an instrument from our catalogue
- Instrument price updates, giving the most recent price for a specific instrument


### Output (Aggregated-Price History)
The output (and the main task of this challenge) is the aggregated price history endpoint.
It should provide a 30 minutes quotes history in the form of minute candlesticks (check information below) for any requested instrument.

End-users of the service are interested in a specific instruments price history, and they want it in a format that is easy to read.
Hence we should provide candlesticks.
To get these candlesticks, the user needs to provide the instrument id (ISIN) as a query parameter (e.g. `http://localhost:9000/candlesticks?isin={ISIN}`).

The system only needs to return the candlesticks for the last 30 minutes, including the most recent prices.
If there weren't any quotes received for more than a minute, instead of missing candlesticks in the 30 minute window, values from the previous candle are reused.

The endpoint stub for fetching candlesticks is already provided in `Server.kt`. Fully implementing the body of that method is the main task.

**Your task is to implement a service that will consume the stream data and make it reachable through the API in the format mentioned above**

### Open questions

If you feel that the requirements leave you with open questions - that is on purpose and part of the challenge.
You are free to make assumptions or reach out to us, when you really need feedback.
We highly appreciate if you document the assumptions you made. We will probably ask you to argument those assumptions in the next interview steps.

# Setup

You can either choose to use the framework we provide you, or to use your own framework.
For the latter, see the specification information about the partner service at the end of this section.

## Framework Requirements
- JVM running on your local machine
- Gradle
- Docker v2
- An IDE of your choice

### Running the Partner Service

To run a partner service you can either use docker-compose. Docker v3 or above will require slight changes to the docker-compose files.
``` 
docker-compose up -d
```
or Java
```
java -jar partner-service-1.0.1-all.jar --port=8032
```

### Running the app
To run the app you can use the following gradle commands
```
./gradlew build
./gradlew test
./gradlew run
```

Once the server is running you can check the results at
```
http://localhost:9000/candlesticks?isin={ISIN}
```
Note: If you don't implement your service, you will see an exception here

### Using your own Framework

You are free to do the challenge without the code we provided.
Most likely you will need the following information to do so.

#### PartnerService
This coding challenge includes a runnable JAR (the partner service) that provides the websockets mentioned below.
If you decide to not use the given boilerplate code, you will need the following information.
Running the jar in a terminal with `-h` or `--help` will print how to use it.
The default port is `8080`, but you are free to change that.
We like `8032` and use that for the following examples.

Once started, it provides two websocket streams (`ws://localhost:8032`), plus a website preview of how the stream look (http://localhost:8032).

* `/instruments` provides the currently available instruments with their ISIN and a Description

    * when connecting to the stream, it gives all currently available Instruments
    * once connected it streams the addition/removal of instruments
    * Our partners assured us, that ISINS are unique, but can in rare cases be reused once no Instrument with that ISIN is available anymore (has been deleted, etc.)

* `/quotes` provides the most current price for an instrument every few seconds per available instrument

If you restart the PartnerService, you will have to clean up any data you might have persisted, since it will generate new ISINs and does not retain state from any previous runs.

##### /instrument Specification
The `/instruments` websocket provides all currently active instruments `onConnect`, as well as a stream of add/delete events of instruments.
When you receive a `DELETE` event, the instrument is no longer available and will not receive any more quotes (beware out of order messages on the /quotes stream)
The instruments are uniquely identified by their isin. Beware, ISINs can be reused _after_ an instrument has been deleted.
In any case, you would see a regular ADD event for this new instrument, even when it reuses an ISIN.

```
{
    // The type of the event. ADD if an instrument is ADDED
    // DELETE if an instrument is deleted
    "type": "DELETE"
    {
        //The Payload
        "data": {
            //The Description of the instrument
            "description": "elementum eos accumsan orci constituto antiopam",
            //The ISIN of this instrument
            "isin": "LS342I184454"
        }
    }
}
```

##### /quotes Specification
The `/quotes` Websocket provides prices for available instruments at an arbitrary rate.
It only streams prices for available instruments (beware out of order messages).
```
{
    // The type of the event.
    // QUOTE if an new price is available for an instrument identified by the ISIN
    "type": "QUOTE"
    {
        //The Payload
        "data": {
            //The price of the instrument with arbitray precision
            "price": 1365.25,
            //The ISIN of this instrument
            "isin": "LS342I184454"
        }
    }
}
```

## Future Development Discussion
The following questions will give you a hint on what to think about for the code review interview.

- How would you change the system to provide scaling capabilities to 50.000 (or more) available instruments, each streaming quotes between once per second and every few seconds?
- How could this system be build in a way that supports failover capabilities so that multiple instances of the system could run simultaneously?

#### Instruments and Quotes
Every asset that can be traded is represented by an “instrument”, which has a unique identifier (ISIN).
Each time the instrument price changes, an update message called “quote” is broadcasted for this instrument to inform about the change.

#### What is a candlestick?
A [candlestick](https://en.wikipedia.org/wiki/Candlestick_chart) is a representation that describes the price movement for a given instrument in a fixed amount of time, usually one minute.
We will be using a simplified version of candlesticks for this challenge.

![chart](https://t4.ftcdn.net/jpg/02/79/65/79/360_F_279657943_FALhJZ6g4shXyfqMIRifp1l6lhiwhbwm.jpg)

The basic idea is that we don't need to know about _all_ prices changes within a given timeframe.
Usually we want them grouped in 1 minute chunks, because we are more interested in some key data points within any given minute.
In theory, a candlestick “contains” all quotes, where the timestamp of the quote is higher than the openTimestamp and lower than the closeTimestamp (`openTimestamp <= quoteTimestamp < closeTimestamp`).
However, for each candle for each given minute, we only present the following data points to the user:
- the first quotes price, that was received (openPrice)
- the last quotes, that was received (closePrice)
- the highest quote price that was observed (highPrice)
- the lowest quote price that was observed (lowPrice)
- the timestamp when the candlestick was opened (openTimestamp)
- the timestamp when the candlestick was closed (closeTimestamp)

##### Example
Assume the following (simplified) data was received for an instrument:
```
@2019-03-05 13:00:05 price: 10
@2019-03-05 13:00:06 price: 11
@2019-03-05 13:00:13 price: 15
@2019-03-05 13:00:19 price: 11
@2019-03-05 13:00:32 price: 13
@2019-03-05 13:00:49 price: 12
@2019-03-05 13:00:57 price: 12
@2019-03-05 13:01:00 price: 9
```
The resulting minute candle would have these attributes:
```
openTimestamp: 2019-03-05 13:00:00
openPrice: 10
highPrice: 15
lowPrice: 10
closePrice: 12
closeTimestamp: 13:01:00
```
Note that the last received quote with a price of 9 is not part of this candlestick anymore, but belongs to the new candlestick.

### Input data
The input data is received through a websocket stream from a partner service.
The code provides handles connecting and consuming the stream (`Streams.kt`).
There are two types of input messages:

- Instrument additions/deletions, which adds or removes an instrument from our catalogue
- Instrument price updates, giving the most recent price for a specific instrument


### Output (Aggregated-Price History)
The output (and the main task of this challenge) is the aggregated price history endpoint.
It should provide a 30 minutes quotes history in the form of minute candlesticks (check information below) for any requested instrument.

End-users of the service are interested in a specific instruments price history, and they want it in a format that is easy to read.
Hence we should provide candlesticks.
To get these candlesticks, the user needs to provide the instrument id (ISIN) as a query parameter (e.g. `http://localhost:9000/candlesticks?isin={ISIN}`).

The system only needs to return the candlesticks for the last 30 minutes, including the most recent prices.
If there weren't any quotes received for more than a minute, instead of missing candlesticks in the 30 minute window, values from the previous candle are reused.

The endpoint stub for fetching candlesticks is already provided in `Server.kt`. Fully implementing the body of that method is the main task.

**Your task is to implement a service that will consume the stream data and make it reachable through the API in the format mentioned above**

### Open questions

If you feel that the requirements leave you with open questions - that is on purpose and part of the challenge.
You are free to make assumptions or reach out to us, when you really need feedback.
We highly appreciate if you document the assumptions you made. We will probably ask you to argument those assumptions in the next interview steps.

# Setup

You can either choose to use the framework we provide you, or to use your own framework.
For the latter, see the specification information about the partner service at the end of this section.

## Framework Requirements
- JVM running on your local machine
- Gradle
- Docker v2
- An IDE of your choice

### Running the Partner Service

To run a partner service you can either use docker-compose. Docker v3 or above will require slight changes to the docker-compose files.
``` 
docker-compose up -d
```
or Java
```
java -jar partner-service-1.0.1-all.jar --port=8032
```

### Running the app
To run the app you can use the following gradle commands
```
./gradlew build
./gradlew test
./gradlew run
```

Once the server is running you can check the results at
```
http://localhost:9000/candlesticks?isin={ISIN}
```
Note: If you don't implement your service, you will see an exception here

### Using your own Framework

You are free to do the challenge without the code we provided.
Most likely you will need the following information to do so.

#### PartnerService
This coding challenge includes a runnable JAR (the partner service) that provides the websockets mentioned below.
If you decide to not use the given boilerplate code, you will need the following information.
Running the jar in a terminal with `-h` or `--help` will print how to use it.
The default port is `8080`, but you are free to change that.
We like `8032` and use that for the following examples.

Once started, it provides two websocket streams (`ws://localhost:8032`), plus a website preview of how the stream look (http://localhost:8032).

* `/instruments` provides the currently available instruments with their ISIN and a Description

    * when connecting to the stream, it gives all currently available Instruments
    * once connected it streams the addition/removal of instruments
    * Our partners assured us, that ISINS are unique, but can in rare cases be reused once no Instrument with that ISIN is available anymore (has been deleted, etc.)

* `/quotes` provides the most current price for an instrument every few seconds per available instrument

If you restart the PartnerService, you will have to clean up any data you might have persisted, since it will generate new ISINs and does not retain state from any previous runs.

##### /instrument Specification
The `/instruments` websocket provides all currently active instruments `onConnect`, as well as a stream of add/delete events of instruments.
When you receive a `DELETE` event, the instrument is no longer available and will not receive any more quotes (beware out of order messages on the /quotes stream)
The instruments are uniquely identified by their isin. Beware, ISINs can be reused _after_ an instrument has been deleted.
In any case, you would see a regular ADD event for this new instrument, even when it reuses an ISIN.

```
{
    // The type of the event. ADD if an instrument is ADDED
    // DELETE if an instrument is deleted
    "type": "DELETE"
    {
        //The Payload
        "data": {
            //The Description of the instrument
            "description": "elementum eos accumsan orci constituto antiopam",
            //The ISIN of this instrument
            "isin": "LS342I184454"
        }
    }
}
```

##### /quotes Specification
The `/quotes` Websocket provides prices for available instruments at an arbitrary rate.
It only streams prices for available instruments (beware out of order messages).
```
{
    // The type of the event.
    // QUOTE if an new price is available for an instrument identified by the ISIN
    "type": "QUOTE"
    {
        //The Payload
        "data": {
            //The price of the instrument with arbitray precision
            "price": 1365.25,
            //The ISIN of this instrument
            "isin": "LS342I184454"
        }
    }
}
```

## Future Development Discussion
The following questions will give you a hint on what to think about for the code review interview.

- How would you change the system to provide scaling capabilities to 50.000 (or more) available instruments, each streaming quotes between once per second and every few seconds?
- How could this system be build in a way that supports failover capabilities so that multiple instances of the system could run simultaneously?

Instrument Lifecycle

Step-by-Step Lifecycle

┌─────────────────────────────────────────────────────────────────────────────┐
│                         INSTRUMENT LIFECYCLE                                 │
└─────────────────────────────────────────────────────────────────────────────┘

Step 1: BIRTH (ADD Event)
─────────────────────────
/instruments WebSocket
│
▼
┌──────────────────┐
│ { "type": "ADD", │
│   "data": {      │
│     "isin": "X1",│
│     "description"│
│   }              │
│ }                │
└──────────────────┘
│
▼
Instrument "X1" is now ACTIVE
✓ Can receive quotes


Step 2: LIFE (QUOTE Events)
───────────────────────────
/quotes WebSocket
│
▼
┌──────────────────┐      ┌──────────────────┐      ┌──────────────────┐
│ { "type":"QUOTE",│      │ { "type":"QUOTE",│      │ { "type":"QUOTE",│
│   "data": {      │ ───► │   "data": {      │ ───► │   "data": {      │
│     "isin": "X1",│      │     "isin": "X1",│      │     "isin": "X1",│
│     "price": 100 │      │     "price": 105 │      │     "price": 102 │
│   }              │      │   }              │      │   }              │
│ }                │      │ }                │      │ }                │
└──────────────────┘      └──────────────────┘      └──────────────────┘
t=0                       t=1                       t=2


Step 3: DEATH (DELETE Event)
────────────────────────────
/instruments WebSocket
│
▼
┌────────────────────┐
│ { "type": "DELETE",│
│   "data": {        │
│     "isin": "X1",  │
│     "description"  │
│   }                │
│ }                  │
└────────────────────┘
│
▼
Instrument "X1" is now DEAD
✗ Should NOT receive more quotes


Step 4: REBIRTH (Optional - ISIN Reuse)
───────────────────────────────────────
/instruments WebSocket
│
▼
┌──────────────────┐
│ { "type": "ADD", │
│   "data": {      │
│     "isin": "X1",│  ← Same ISIN, NEW instrument!
│     "description"│
│   }              │
│ }                │
└──────────────────┘
│
▼
Instrument "X1" is ACTIVE again (fresh start)

  ---
Race Conditions

Race Condition 1: Quote Before ADD

Timeline:
─────────────────────────────────────────────────────────►

/quotes:      QUOTE(X1, 100)
│
/instruments:      │         ADD(X1)
│              │
▼              ▼
┌─────────────────────────────┐
│ PROBLEM: Quote arrives for  │
│ ISIN that doesn't exist yet │
└─────────────────────────────┘

Impact:
- Quote cannot be linked to instrument
- Quote might be lost or cause error

Race Condition 2: Quote After DELETE

Timeline:
─────────────────────────────────────────────────────────►

/instruments:  DELETE(X1)
│
/quotes:           │         QUOTE(X1, 150)
│              │
▼              ▼
┌─────────────────────────────┐
│ PROBLEM: Quote arrives for  │
│ ISIN that was just deleted  │
└─────────────────────────────┘

Impact:
- Quote for "dead" instrument
- Should be ignored/discarded

Race Condition 3: ADD After DELETE (ISIN Reuse)

Timeline:
─────────────────────────────────────────────────────────►

/instruments:  ADD(X1)    DELETE(X1)    ADD(X1)
│            │            │
/quotes:         │   QUOTE    │   QUOTE    │   QUOTE
│   (X1,100) │   (X1,150) │   (X1,200)
▼            ▼            ▼
┌─────────────────────────────────────────┐
│ PROBLEM: Which "X1" does middle quote  │
│ belong to? Old instrument or new one?  │
└─────────────────────────────────────────┘

Impact:
- Historical data corruption
- Wrong candlesticks for reused ISIN

Race Condition 4: Duplicate ADD

Timeline:
─────────────────────────────────────────────────────────►

/instruments:  ADD(X1)       ADD(X1)
│              │
▼              ▼
┌─────────────────────────────┐
│ PROBLEM: Same ISIN added    │
│ twice without DELETE        │
└─────────────────────────────┘

Impact:
- Duplicate instrument records
- Database constraint violation

  ---
Summary Table

| Race Condition     | Cause            | Impact              | Solution                                            |
  |--------------------|------------------|---------------------|-----------------------------------------------------|
| Quote Before ADD   | WebSocket timing | Quote lost/error    | Buffer quotes, wait for ADD                         |
| Quote After DELETE | Async streams    | Invalid quote saved | Check instrument exists before saving               |
| ISIN Reuse         | Business logic   | Data corruption     | Clear old data on DELETE, track instrument versions |
| Duplicate ADD      | Network retry    | DB constraint error | Use UPSERT or check exists                          |

  ---
Recommended Handling

// In QuotesHandler - before saving quote
public void handleQuote(String isin, double price) {
// Race Condition 1 & 2: Check instrument exists
if (!instrumentService.instrumentExists(isin)) {
log.warn("Quote for unknown/deleted ISIN: {}", isin);
return; // Discard quote
}

      quoteService.saveQuote(isin, price);
}

// In InstrumentsHandler - on DELETE
public void handleDelete(String isin) {
// Race Condition 3: Clear history on delete
candlestickService.clearHistory(isin);
instrumentService.deleteInstrument(isin);
}

Would you like me to implement these safeguards in your code?

> do you think active column i s best sttrategy?

⏺ Great question! Let's analyze different strategies:

Strategy Comparison

1. Active Column (Soft Delete)

@Entity
public class Instrument {
String isin;
String description;
boolean active = true;  // ← soft delete flag
}

| Pros                     | Cons                                         |
  |--------------------------|----------------------------------------------|
| Preserves history        | Must filter all queries: WHERE active = true |
| Audit trail              | Database grows indefinitely                  |
| Simple to implement      | ISIN reuse: old & new share same record?     |
| Handles Race Condition 2 | Ambiguous: is inactive = deleted or paused?  |

Problem with ISIN Reuse:
ADD(X1) → active=true
DELETE(X1) → active=false
ADD(X1) → active=true  ← Same row reactivated? Or new row?

  ---
2. Version/Generation Column (Better for ISIN Reuse)

@Entity
public class Instrument {
String isin;
String description;
int generation = 1;  // ← increments on each ADD after DELETE
boolean active = true;
}

| Pros                                     | Cons                             |
  |------------------------------------------|----------------------------------|
| Handles ISIN reuse cleanly               | More complex                     |
| Full history per "lifetime"              | Quotes need generation reference |
| Clear separation of instrument instances |                                  |

ADD(X1)    → isin=X1, generation=1, active=true
DELETE(X1) → isin=X1, generation=1, active=false
ADD(X1)    → isin=X1, generation=2, active=true  ← NEW record

  ---
3. Timestamp-based (Valid From/To)

@Entity
public class Instrument {
String isin;
String description;
Instant validFrom;
Instant validTo;  // null = currently active
}

| Pros                                 | Cons                      |
  |--------------------------------------|---------------------------|
| Temporal queries easy                | Complex joins             |
| Full audit history                   | Need to check time ranges |
| Industry standard for financial data |                           |

  ---
4. Hard Delete + In-Memory Cache (Current Approach)

// What you have now
Map<String, String> instruments = new ConcurrentHashMap<>();

void delete(String isin) {
instruments.remove(isin);  // Memory only
// DB record stays (orphaned)
}

| Pros         | Cons                                |
  |--------------|-------------------------------------|
| Simple       | DB/Cache out of sync                |
| Fast lookups | Orphaned quotes in DB               |
|              | ISIN reuse causes duplicate records |

  ---
My Recommendation

For this challenge (30-min candlesticks, ISIN reuse possible):

