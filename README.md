# OrangeHRM Automation Testing

## Project Overview

This project is an **Automation Testing Framework for OrangeHRM** built using **Selenium WebDriver**. It automates key functionalities of the OrangeHRM web application to validate UI behavior, functionality, and regression scenarios.

The framework is designed to improve testing efficiency, reduce manual effort, and provide reliable test execution for HRMS workflows.

---

## Tech Stack

* **Java**
* **Selenium WebDriver**
* **TestNG**
* **Maven**
* **Page Object Model (POM)**
* **Extent Reports / TestNG Reports**

---

## Features Covered

* Login functionality testing
* Employee management test cases
* Navigation and UI validation
* Form submission testing
* Positive and negative test scenarios
* Regression testing support
* Report generation after execution

---

## Project Structure

```text
src/
 ┣ main/
 ┣ test/
 ┣ pages/
 ┣ utilities/
 ┣ testcases/
pom.xml
testing.xml
test-output/
README.md
```

---

## Prerequisites

Before running this project, make sure you have installed:

* Java JDK 8 or above
* Maven
* IDE (Eclipse / IntelliJ)
* Chrome Browser
* ChromeDriver (compatible version)

---

## Installation & Setup

1. Clone the repository:

```bash
git clone https://github.com/Meet-Vijayshree/Automated-Orange.git
```

2. Navigate to the project directory:

```bash
cd Automated-Orange
```

3. Install dependencies:

```bash
mvn clean install
```

---

## Running Tests

Execute tests using:

```bash
mvn test
```

Or run using TestNG XML:

```bash
mvn test -DsuiteXmlFile=testing.xml
```

---

## Reporting

After execution, reports can be found in:

* `test-output/`
* Extent Reports (if configured)

---

## Objective

The main objective of this project is to automate OrangeHRM application testing using Selenium and improve software quality through reusable and maintainable test scripts.

---

## Author

**Meet Vijayshree**
