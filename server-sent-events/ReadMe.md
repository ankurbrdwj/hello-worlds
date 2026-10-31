home page
Products
Cards
Solutions
Resources
Developers
Sales:+49 30 54453778 1

globe simple global world earth international icon
Login
Get started
Tech at Pliant
4 min read
Building Real-Time, Reliable Notifications with Server-Side Events: A Case Study from Fintech
In this post, we’ll explore how we designed and implemented a scalable, event-driven architecture based on Server-Side Events (SSE) to handle these kinds of user-facing workflows in real time.

Daniel Susumu Ribeiro Yamamoto, Senior Software Engineer
Daniel Susumu Ribeiro Yamamoto
on 6/27/2025
Building Real-Time, Reliable Notifications with Server-Sent Events: A Case Study from Fintech
A Scalable Pattern for User-Initiated Workflows
At Pliant, user experience and security are critical—especially when handling card transactions that require identity verification. In this post, we’ll explore how we designed and implemented a scalable, event-driven architecture based on Server-Side Events (SSE) to handle these kinds of user-facing workflows in real time. Although our initial use case was 3DS biometric verification in a fintech environment, the solution is broadly reusable across many domains.

This system not only improved speed and reliability, but also cut costs and introduced a scalable, reusable foundation for real-time communication across our platform.

The Problem: Delivering Time-Sensitive Events to Users
Imagine this scenario: your system needs to send a time-sensitive request to a user, and you want to ensure the following:

It’s delivered instantly, across any active device or browser tab.

It’s retryable and persistent—even if the user isn’t connected at the moment.

It works with real users in real environments (tab switches, flaky networks, mobile fallback).

It’s secure, auditable, and scalable across service instances.

Why We Chose Server-Side Events
When building real-time systems, the first question is always: WebSockets or SSE?

In our case, SSE was the clear choice:

The communication pattern was unidirectional—we needed to push updates from the server to the client, not the other way around.

SSE is simpler to operate at scale, with automatic reconnection support in browsers.

It reduces the risk of client-driven resource exhaustion, helping us avoid unintentional DDoS-like behaviors.

Native support in browsers means less friction for our frontend teams.

We didn’t need full-duplex communication or custom protocols—SSE gave us the right balance of simplicity, reliability, and performance.

System Overview
Our SSE implementation is part of the notification-service, which consumes events from Kafka and delivers them to users via open HTTP connections.
Real-Time SSE Notification Architecture
How It Works
1. Establishing the SSE Connection
   Clients (typically browser tabs) initiate a connection via a GET request.

We validate the member, register a connection in memory, and send an initial "ping".

Then we check the database for pending SseRequest events that might have not been sent or have been missed (e.g., during page reloads or network instability).

2. Emitting Real-Time Events
   When a real-time event is triggered, we:

Generate an SseRequest object containing:

type: the event type (e.g., BIOMETRIC_AUTH_REQUEST)

payload: user-specific data

expiresAt: time limit for delivery

persistent: indicates whether it should be re-sent on reconnect

These events are stored in the database, then delivered via the open SSE connection. If delivery fails, the connection is cleaned up, and the event remains for retry.
The anatomy of a SseRequest
3. Clean Failure Handling
   If the SSE connection drops or times out:

The notifier is removed from memory

Events remain in the DB to still be sent if marked as persistent

Clients can reconnect and resume seamlessly

Real-Time 3DS Verification Flow
Our first major use of SSE was integrating it into our 3DS biometric verification flow.

Here’s how the process works:

The process is initiated by a third party

The notification-service picks up the event, stores an SseRequest, and emits the event to the client via SSE

The user approves the request using a Security Key

The response is sent asynchronously and an event is emitted

The event is used to close verification modals on other devices or tabs
Biometric 3DS Flow: Step-by-Step Timeline
Lessons Learned
Implementing real-time 3DS verification taught us several key lessons:

Simple > complex: SSE solved our needs without overengineering

Event persistence is critical for reliability and observability

Fallback delivery paths are essential when supporting multi-device flows

Kafka fan-out patterns enabled event delivery across multiple service instances

Final Thoughts
Real-time feedback is critical for security workflows like 3DS. By using Server-Side Events, we built a system that is fast, reliable, and easy to operate. More importantly, we built it with extensibility in mind — laying a strong foundation for other use cases that need live event delivery.

If your team is considering adding real-time functionality, consider SSE as a powerful, low-complexity alternative to WebSockets.



