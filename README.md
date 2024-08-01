<div align="center">
  <h1 align="center">DamFlow</h1>
</div>

In this repository, we host all the data and code related to our paper titled **Preventing a Flood of Irrelevant Data Flows in Android Apps with Backward Taint Analysis and Context-aware Anomaly Detection**.

### 🗂️ Repository Organization

The repository mainly consists of Java and Python code, with a significant number of Jupyter Notebooks.

The repository is organized according as follows:

### 📋 Requirements

##### 🗝️ API Keys
To execute the entire code, two API keys are required. They should be set in an environment file named *.env* using the following names: **ANDROZOO_API_KEY** and **OPENAI_API_KEY**.

- 🗝️ **ANDROZOO_API_KEY**: This key is necessary to download apps from the *AndroZoo* Repository, as various operations on the APK files are performed "on-the-fly," such as app download, extraction, and deletion. It can be requested here: [https://androzoo.uni.lu/access](https://androzoo.uni.lu/access)

- 🗝️ **OPENAI_API_KEY**: This key is required to utilize the Embedding models from OpenAI through their official API ([https://platform.openai.com/overview](https://platform.openai.com/overview)).

##### 🖥️ Redis Server
To efficiently manage data processing and avoid the overhead of storing large amounts of data locally, our system utilizes an external Redis server for temporary data storage.

You must configure your own Redis server and specify the connection details in the .env file as follows:

* REDIS_SERVER  = [SERVER]
* REDIS_PORT    = [PORT]
* REDIS_DB      = [DB_NUMBER]
* REDIS_PSW     = [PSW]


### ⚙️ Usage

The code is provided in the form of Jupyter Notebooks to facilitate execution and collaboration. 