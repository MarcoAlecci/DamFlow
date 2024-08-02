<div align="center">
  <h1 align="center">DamFlow</h1>
</div>

In this repository, we host all the data and code related to our paper titled **DamFlow: Preventing a Flood of Irrelevant Data Flows in Android Apps**.

### 🗂️ Repository Organization

The repository is organized into several key directories, each serving a specific purpose:

* **📁 0_Data**  
  Contains all the datasets used in the experiments. This includes a smaller version of AndroCatSet called **AndroCatSet Mini**, which can be used to test the code easily.

* **📂 1_Src**  
  Houses all the source code, divided into two subfolders: one for **Java** and one for **Python**.
  * **Java Folder**  
    Contains the code needed to launch FlowDroid (for both backward analysis and the baseline approach). A `.jar` file is also provided for convenience.
  * **Python Folder**  
    Contains the code for the entire pipeline: Data Flow Extraction ➡️ Embedding ➡️ Anomaly Detection. The Python code is primarily provided in the form of Jupyter Notebooks to facilitate execution and collaboration.

### 📋 Requirements

##### 🗝️ API Keys
To execute the entire code, two API keys are required. They should be set in an environment file named *.env* using the following names: **ANDROZOO_API_KEY** and **OPENAI_API_KEY**.

- 🗝️ **ANDROZOO_API_KEY**: This key is necessary to download apps from the *AndroZoo* Repository, as various operations on the APK files are performed "on-the-fly," such as app download, extraction, and deletion. It can be requested here: [https://androzoo.uni.lu/access](https://androzoo.uni.lu/access)

- 🗝️ **OPENAI_API_KEY**: This key is required to utilize the Embedding models from OpenAI through their official API ([https://platform.openai.com/overview](https://platform.openai.com/overview)).

##### 🖥️ Redis Server
As the temporary data and results are too large to be stored locally, our system uses an external Redis server for efficient data processing and to avoid the overhead of storing large amounts of data locally.

You must configure your own Redis server and specify the connection details in the .env file as follows:

* REDIS_SERVER  = [SERVER]
* REDIS_PORT    = [PORT]
* REDIS_DB      = [DB_NUMBER]
* REDIS_PSW     = [PSW]

##### 🟩 Android Platforms:
In order to use DamFlow (as it is based on FlowDroid) you need to add to the .env file also the path to the Android Platforms.
* ANDROID_PATH = [path_to_android_platform]

### ⚙️ Usage

#### ☕ Java

The Java part consists of a script that relies on **FlowDroid** and **Soot** to perform backward taint analysis and extract all the dataflow pairs. A `.jar` file with dependencies is already provided for convenience. 

If you prefer to build the Maven project yourself, you will first need to build FlowDroid 2.14 by downloading the code from their official GitHub page: [FlowDroid GitHub Repository](https://github.com/secure-software-engineering/FlowDroid).

#### 🐍 Python

The code is provided in the form of Jupyter Notebooks to facilitate execution and collaboration. The notebooks are named and organized in sequential order to avoid possible mistakes. The usual pipeline includes:

* **🔍 Extraction Notebook**
* **🔢 Embedding Notebook**
* **📚 Training Notebook**
* **🧪 Testing Notebook**

In the **Extraction Notebook**, you need to specify which approach to use for extraction. The options are:

* **DamFlow**  
  `DIRECTION = "backward"`  
  `SOURCE_APPROACH = "nosources"`

* **DocFlow**  
  `DIRECTION = "forward"`  
  `SOURCE_APPROACH = "docflow"`

You also need to choose whether to analyze **AndroCatSet** or the dataset of malicious apps we created.

Additionally, you can specify your own key for REDIS by setting the parameter `REDIS_PREFIX` in every notebook.

**Important:** Ensure these values are consistent across all notebooks for the full pipeline to work properly.