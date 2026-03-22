# VaultDocs

VaultDocs is a Spring Boot application designed to handle document ingestion and provide intelligent Question & Answer (Q&A) capabilities based on the uploaded documents.

## Features

* **Document Ingestion:** Upload and index documents seamlessly using the `IngestionService`.
* **Intelligent Q&A:** Query your indexed documents using natural language questions or specific operational commands via the `QnAService`.
* **RESTful API:** Easy-to-use endpoints for seamless integration with front-end clients or other external services.

## Prerequisites

* Java 17+ (or your configured Java version)
* Maven or Gradle
* **Ollama** installed and running locally (defaults to `http://localhost:11434`).
* The following Ollama models must be pulled locally:
    * `llama3.2` (used for intelligent Q&A and operations)
    * `nomic-embed-text` (used for generating vector embeddings)

*To pull the required models, run the following commands in your terminal:*
 ```bash
     ollama pull llama3.2
     ollama pull nomic-embed-text
 ```

## API Endpoints

## API Endpoints

### 1. Upload Document
Uploads a file to be processed and indexed by the system.

* **URL:** `/vaultdocs/upload`
* **Method:** `POST`
* **Content-Type:** `multipart/form-data`
* **Parameters:**
  * `file` (MultipartFile, required): The document to be uploaded and indexed.

**Example cURL:**
```bash
  curl -X POST -F "file=@/path/to/your/document.pdf" http://localhost:8080/vaultdocs/upload
```

### 2. Ask a Question / Perform an Operation
Query the indexed documents. You must provide either a `question` or an `operation`, but not both simultaneously.

* **URL:** `/vaultdocs/ask`
* **Method:** `GET`
* **Parameters:** 
  * `question` (String, optional): A natural language question to ask against the documents.
  * `operation` (String, optional): A specific operational command to execute.

**Example cURL:**
```bash
  curl -X GET "http://localhost:8080/vaultdocs/ask?question=What is the summary of this document?"
```

## Running the Application

1. Navigate to the project root directory.
2. Start the application using your build tool (Maven example below):
   ```bash
   mvn spring-boot:run
   ```