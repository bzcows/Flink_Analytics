# Flink Dynamic CEP Demo - AI VIBE-CODING 
​
THis is project that built using AI-VIBE coding. It is inspired by a real project is https://getindata.com/blog/dynamic-sql-processing-with-apache-flink/.

I wanted to see if I can build the same project (almost) purely using AI and Vibe-Coding. I used Cline/RooCode + DeepSeek V3, Cluade Sonnet 3.5 and Sonnet 3.7 
to build this app using ONLY prompts. I did not write a single line of code myself (fyi- I am full-time developer) as an experiment to see what can be done using Vibe coding. I started on an empty folder, and prompted the LLM to create the project structure and the tech stack and continue to prompt it with details, starting with defining what the input message atttributes are etc.. THIS WAS PURE ENLISH PROMPTS!!!

It took me many hours(and many tokens/$$) to get it to a point where I thought this was good enough as an experiment to upload. 

​
## Project Overview
​
This project simulates and manages transactions by leveraging a robust Java-based backend and a React-based frontend.
​
The backend is responsible for real-time data ingestion, processing, and distribution using technologies like Apache Kafka, Apache Flink, and WebSockets.
​
The frontend provides an interactive user interface built with React, enabling users to visualize and manage transaction data effectively. The user can submit SQL materialize aggregations and pattern matching queries and the backend will run the queries on the streaming data from the kafka topic using an flink local stream env (minicluster).

No checkpointing is not enabled.
​
## Technology Choices
​
**Backend:**
- Java with Spring Boot
- Maven for build management
- Apache Kafka for messaging
- Apache Flink for stream processing
- WebSockets for real-time communication
​
**Frontend:**
- React for building the user interface
- npm for dependency management
​
## Building the Project
​
**Backend:**
1. Navigate to the backend directory:
```bash
cd backend
```
2. Build the project using Maven:
```bash
mvn clean package
```
This will generate a runnable JAR file in the target folder.
​
**Frontend:**
1. Navigate to the frontend directory:
```bash
cd frontend
```
2. Install dependencies:
```bash
npm install
```
3. Run the development server:
```bash
npm start
```
For a production build, use:
```bash
npm run build
```
​
## Running the Project
​
**Backend:**
Run the generated JAR file from the target folder:
```bash
java -jar target/cc-aggregator-backend-1.0.0.jar
```
​
**Frontend:**
Execute the following command to start the frontend development server:
```bash
npm start
```
Access the application at [http://localhost:3000](http://localhost:3000).
​
## Additional Information
​
- Logs are available in the backend/logs directory.
- Configuration properties can be found in backend/src/main/resources/application.properties.
- WebSocket support is enabled for real-time data updates.
​
## Conclusion
​
This project demonstrates a scalable and efficient solution for aggregating transaction data with real-time processing capabilities.
Its robust technology stack ensures high performance and reliability.
Explore, build, and extend the application to fit your needs.
