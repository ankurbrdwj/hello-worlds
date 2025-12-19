Core Requirements
The system should identify clients by user ID, IP address, or API key to apply appropriate limits.
The system should limit HTTP requests based on configurable rules (e.g., 100 API requests per minute per user).
When limits are exceeded, the system should reject requests with HTTP 429 and include helpful headers (rate limit remaining, reset time).
Below the line (out of scope)
Complex querying or analytics on rate limit data
Long-term persistence of rate limiting data

THe flow of developement :
1. Requirement Analysis
   - Understand the core requirements for rate limiting.
   - Identify the key components needed (client identification, request counting, limit enforcement).
2. Design
   - Core Entities:
     - Client: Represents a user or application making requests.
     - RateLimitRule: Defines the limits (e.g., max requests, time window).
     - RequestCounter: Tracks the number of requests made by each client.
   - Relationships:
     - Each Client can have multiple RateLimitRules.
     - Each RateLimitRule is associated with a RequestCounter.
3. API and Interface
4. Data Flow:
     - Incoming requests are identified by Client.
     - The RequestCounter is checked against the RateLimitRule.
     - If the limit is exceeded, respond with HTTP 429; otherwise, process the request

Defining the Core Entities
While rate limiters might seem like simple infrastructure components, 
they actually involve several important entities that we need to model properly:
Rules: The rate limiting policies that define limits for different scenarios. 
Each rule specifies parameters like requests per time window, which clients it applies to, and what endpoints it covers.
For example: "authenticated users get 1000 requests/hour" or "the search API allows 10 requests/minute per IP."
Clients: The entities being rate limited - this could be users (identified by user ID), IP addresses, API keys, or combinations thereof.
Each client has associated rate limiting state that tracks their current usage against applicable rules.
Requests: The incoming API requests that need to be evaluated against rate limiting rules. 
Each request carries context like client identity, endpoint being accessed, and timestamp that determines which rules apply and how to track usage.
These entities work together: when a Request arrives, we identify the Client, look up applicable Rules,
check current usage against those rules, and decide whether to allow or deny the request. The interaction between these entities powers our rate limiter.

1) The system should identify clients by user ID, IP address, or API key to apply appropriate limits
2) The system should limit requests based on configurable rules
3) When limits are exceeded, reject requests with HTTP 429 and helpful headers