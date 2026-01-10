
We bill our customers on a weekly basis. Each bill charges for transactions that occurred during a specific
**billing period**, which is identified by a **year** and a **period number**, as follows:

- Billing periods are numbered consecutively within a year.
- The first billing period starts on January 1st with period number 1.
- Each **Saturday** marks the beginning of a new billing period.
- Each **1st of a month** marks the beginning of a new billing period.

You can refer to [sample calendar for 2 months of 2019](calendar.md) to ensure correct understanding of the above.
We have 2 main features within this code:

* given a (local) date, return a period containing that date
* obtain all periods in a year - this is also exposed as a REST service.