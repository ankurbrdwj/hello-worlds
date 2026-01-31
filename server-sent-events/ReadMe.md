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

A Note from the CTO:

"Thanks for reading! This is the very first post in our new engineering blog, where we’ll be sharing how we’re building Pliant—from the architectural choices and technical challenges to the small wins that make a big difference. We’re excited to give you a peek behind the curtain and hope you’ll stick around for what’s coming next.

Thank you again for joining us at the start of this journey, and stay pliant!"

Alex Korotkykh, CTO at Pliant
Daniel Susumu Ribeiro Yamamoto, Senior Software Engineer
Daniel Susumu Ribeiro Yamamoto
LinkedIn icon
Senior Software Engineer
Table of content
A Scalable Pattern for User-Initiated Workflows
The Problem: Delivering Time-Sensitive Events to Users
Why We Chose Server-Side Events
System Overview
How It Works
Real-Time 3DS Verification Flow
Lessons Learned
Final Thoughts
Recent blog posts
All blog posts
Virtual Credit Cards for Employees: What You Need to Know
A modern virtual card solution for employees is secure, transparent, and saves time for management, employees, and accountants through streamlined digital processes.

Credit cards
11 min read
Virtual Credit Cards for Employees
TMC-tailored virtual credit cards: Cashback at your fingertips
Travel Management Companies (TMCs) make business travel easy for their clients. A powerful modern corporate credit card solution ensures that TMCs’ internal processes and operations run equally smoothly.

Travel
5 min read
How TMCs benefit from a modern credit card solution
What is a Card Issuance Provider? And who would benefit from issuing their own credit cards?
In the constantly evolving landscape of financial services, card issuers are playing an increasingly important role. But what exactly does a card issuance provider do? Simply put, these entities are the engine behind the creation and management of payment cards. They offer businesses the tools to launch their own branded cards, either physically or digitally, opening up new opportunities for revenue, customer engagement, and financial management.

CaaS
5 min read
A card issuance provider is an entity that enables institutions to create, manage, and issue payment cards—including credit, debit, and prepaid cards. This enablement is known as a card issuing service.
Could Your Company Issue Credit Cards? 3 Industries That Could Benefit from Cards-as-a-Service
If you’re looking to expand your profit margins, and your customer base, by adding financial services to your portfolio, a credit card issued and branded by your company is certainly a goal to aspire to. However, without any experience of offering financial services, you might be wondering about the best way to issue credit cards and bolster your revenue streams. Fortunately, Cards-as-a-Service (CaaS) is the simple, effective option that brings your own card program within reach. Let’s look at the industries best suited to issue credit cards and whether your company could benefit too.

CaaS
9 min read
Industries Suited to Issuing Credit Cards - Pliant
What Is Embedded Finance? And How Could It Benefit Your Business?
As TechCrunch so aptly put it, embedded finance is having a moment: banking, payments, and more are being continually integrated into the apps and platforms you already use. You’d be forgiven for thinking that controlling the means of payment would be a goldmine because, well… it is. In fact, more companies than ever are aiming to blur the lines between product and payment, and if you’ve found this post, yours might be among them.

CaaS
11 min read
What Is Embedded Finance? Pliant
previous
next
Payment Apps
Discover Payment Apps
Real-time monitoring
Receipt management
Spend control
Accounting automations
Multi-currency accounts
Benefits
Integrations
Pro API
Discover Pliant Pro API
Card issuance & management
Global bank transfers
Transaction insights
Accounting optimization
Member management
Integrations
Custom integrations
CaaS
Discover Cards-as-a-Service
Card issuance & management
Global bank transfers
Advanced data capabilities
Ready-made UI
Compliance & security
Dedicated support
CaaS API
Integrations
Card OS
Discover Card OS
Accounting automation & integrations
Next-generation financial infrastructure
Modular architecture & detailed customization
Scalable back-office tools
Flexible integration
Cards
Physical cards
Premium cards
Virtual cards
Single-use cards
Travel purchasing cards
Fleet cards
Benefit cards
Insurance claim cards
Solutions
Corporations
E-commerce
Marketing agencies
Resellers
SaaS
Travel
ERP
Invoice management
Travel expense management
Specialised lending
Insurance payments
Customer stories
Resources
Pricing
Help center
Blog
Events
Exchange rates
FAQ
Developers
Company
About Pliant
CareersHIRING
Press
Contact
Follow us on
linkedin
Pliant's Youtube channel
Download on the App Store
Download Pliant on the App Store
Download Pliant App on the Google Play Store
Get Pliant mobile app on Google Play
© 2020 – 2026 Pliant GmbH

Pliant is certified as a Payment Card Industry (PCI) Data Security Standard service provider and has achieved ISO Certificate 27001-2022.

Pliant offers its service in both the EEA and the UK. In the EEA, the cards are issued by Pliant Oy, identified by business ID 3266913-9, under a license from Visa Europe Limited. Pliant Oy is recognized as an authorized e-money payment institution and is subject to supervision by the Finnish Financial Supervisory Authority. In the UK, Visa cards are issued by Transact Payments Limited. Transact Payments Limited is authorized and regulated by the Gibraltar Financial Services Commission.

Imprint
Privacy Policy
Privacy Settings

globe simple global world earth international icon
Global (English)

How to build real-time notifications | Pliant









