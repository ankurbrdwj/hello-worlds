# Simple Web Crawler

This project contains a simple web webCrawler written in Java and Spring Boot. Given a starting URL, the webCrawler will visit each URL it finds on the same domain.

## Problem Statement

We'd like you to write a simple web webCrawler in a programming language you're familiar with. Given a starting URL, the webCrawler should visit each URL it finds on the same domain. It should print each URL visited, and a list of links found on that page. The webCrawler should be limited to one subdomain - so when you start with https://monzo.com/, it would crawl all pages on the monzo.com website, but not follow external links, for example to facebook.com or community.monzo.com.

We would like to see your own implementation of a web webCrawler. Please do not use frameworks like scrapy or go-colly which handle all the crawling behind the scenes or someone else's code. You are welcome to use libraries to handle things like HTML parsing.

Ideally, write it as you would a production piece of code. This exercise is not meant to show us whether you can write code – we are more interested in how you design software. This means that we care less about a fancy UI or sitemap format, and more about how your program is structured: the trade-offs you've made, what behaviour the program exhibits, and your use of concurrency, test coverage, and so on.

## How to Run

The application is a command-line tool. You can run it from the command line, passing the seed URL as an argument.

```bash
java -jar webcrawler.jar --webCrawler.seed-url=https://monzo.com
```

If the `seed-url` is not provided, the application will exit with an error message.

## Design and Structure

The application is built with production-readiness in mind, focusing on modularity, testability, and maintainability.

*   **Dependency Injection:** The application uses Spring Boot's dependency injection to manage components. This makes the code more modular and easier to test.
*   **Factory Pattern:** A `CrawlerFactory` is used to create `Crawler` instances. This decouples the `CrawlerRunner` from the creation of the `Crawler`, allowing for more flexible and testable code.
*   **Interfaces:** The `Frontier` is defined by an interface, allowing for different implementations (e.g., in-memory, persistent) to be swapped in easily.
*   **Test Coverage:** The project includes JUnit tests to verify the behavior of the components. Mockito is used to create mock objects, allowing for isolated testing of individual classes.

### Core Components

*   `CrawlerRunner`: The main entry point of the application. It parses command-line arguments and initiates the crawling process.
*   `Crawler`: The core crawling logic. It takes a seed URL, fetches the page, extracts links, and follows them within the same domain.
*   `Frontier`: Manages the queue of URLs to be visited.
*   `CrawlerFactory`: Creates instances of the `Crawler`.
