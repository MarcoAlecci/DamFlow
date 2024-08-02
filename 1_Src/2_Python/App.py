# Imports
from dotenv      import load_dotenv
import numpy     as np
import subprocess
import requests
import psutil
import random
import json
import time
import os
# Own Imports
import Utils

# Class representing an App under analysis,
class App:

	# App Info
	sha256  = None
	pkgName = None
	categoryID = None

	# Where the APK file is stored
	apkPath = None

	# Data Flows extracted
	dataFlows = None

	# Embeddings
	embeddings = None

	# Anomaly Detection Results
	anomalyDetectionResults = None

	# Init Method
	def __init__(self, sha256, pkgName = None, categoryID = None):
		self.sha256  = sha256
		self.pkgName = pkgName
		self.categoryID = categoryID

		# Initialize the embeddings dictionary with the specified keys
		self.embeddings = {
			'gpt'       : None,
			'codebert'  : None,
			'sfr'       : None
		}

		# Init
		self.anomalyDetectionResults = None

	# Download APK File  from AndroZoo.
	# path: where the APK file will be stored.
	def downloadAPK(self, path):

		# Max number of Retries for AndroZoo
		MAX_RETRIES = 10

		# Load AndroZoo API KEY
		load_dotenv()

		# Check if the Apk File already exist
		if os.path.exists(path + '{}.apk'.format(self.sha256)):
			print("--- 📤 APK file with SHA256 already exists.")
			self.apkPath = path + '{}.apk'.format(self.sha256)
			return

		# Construct the URL for downloading the APK
		apkUrl = "https://androzoo.uni.lu/api/download?apikey={}&sha256={}".format(os.getenv("ANDROZOO_API_KEY"), self.sha256)

		retries = 0
		while retries < MAX_RETRIES:
			print("--- 🔄 Tentive N: {}".format(retries))
			# Make a GET request to download the APK file
			req = requests.get(apkUrl, allow_redirects=True)
			
			# Check for HTTP errors like 502 or 503
			if req.status_code in [502, 503]:
				print(f"--- ❌ Error: Received status code {req.status_code}. Retrying in 30 seconds...")
				retries += 1
				time.sleep(30)
				continue
			elif req.status_code != 200:
				print(f"--- ❌ Error: Received unexpected status code {req.status_code}.")
				return
			else:
				# Save the downloaded content to the specified file path
				with open(path + '{}.apk'.format(self.sha256), "wb") as apkFile:
					apkFile.write(req.content)

				# Store the apkPath
				self.apkPath = path + '{}.apk'.format(self.sha256)
				print("--- 📤 APK file downloaded and saved to {}".format(self.apkPath))
				return
		
		print(f"--- ❌ Error: Failed to download APK after {MAX_RETRIES} attempts.")

	# Delete the APK file
	def deleteAPK(self):
		print("--- 🗑️ Delete APK")
		os.remove(self.apkPath)


	# Extract Data Flow Pairs
	# tmpPath           : where to temporaly store the results.
	# javaExtractorPath : path to the .jar file of the extractor.
	# androidPath       : path to Android Platforms
	# direction         : direction of the taint analysis (forward or backward)
	# sourcesApproach   : sources to be used (docflow or nosources)
	# timeout           : timeout to be used for the analysis
	def extractDataFlows(self, tmpPath, javaExtractorPath, androidPath, direction, sourcesApproach, timeout):
		#1. Download the app
		print("--- 📥 Downloading APK")
		self.downloadAPK(tmpPath)

		# 2. Java Extractor
		self.launchJavaExtractor(javaExtractorPath, androidPath, direction, sourcesApproach, timeout)

		# 3. Read Output files containing Results
		self.loadJsonExtractionResults()

		#4. Delete everything
		# Check if APK file exists before calling deleteAPK
		if os.path.exists(self.apkPath):
			self.deleteAPK()
		# Check if JSONL file exists before calling deleteFile
		if os.path.exists(self.apkPath.replace(".apk",".json")):
			Utils.deleteFile(self.apkPath.replace(".apk",".json"))

	# Launch Java Extractor
	def launchJavaExtractor(self, javaExtractorPath, androidPath, direction, sourcesApproach, timeout = 1):
		# Java Extractor
		command = 'java -Xmx24g -Xss1g -jar {} -a {} -p {} -d {} -s {} '.format(javaExtractorPath, self.apkPath, androidPath, direction, sourcesApproach)
		print("--- 💻 Executing: {}".format(command))
	
		process = subprocess.Popen(command.split(), stdout=subprocess.PIPE, stderr=subprocess.PIPE)

		print("--- ⏲️ Timeout: {} s\n".format(timeout))
		try:
			output, error = process.communicate(timeout=timeout)
			
			# Check return code
			returnCode = process.returncode
			if returnCode == 0:
				print("\n+++ START of Output +++")
				print(output.decode("utf-8"))
				print("+++ END of Output +++\n")
			else:
				if error is not None:
					print("\n+++ START of Logging/Error +++")
					print(error.decode("utf-8"))
					print("+++ END of Logging/Error +++\n")
				else:
					print("No error output captured.")
		except subprocess.TimeoutExpired:
			print("--- ⚠️ Timeout Reached.")
			if os.path.exists(self.apkPath):
				self.deleteAPK()
			process.kill()
			raise TimeoutError("Timeout reached while executing the command.")

	# Read Results
	def loadJsonExtractionResults(self):
		resultsPath = self.apkPath.replace(".apk", ".json")

		if not os.path.exists(resultsPath):
			print("--- ⚠️ Results file not available.\n")
			raise FileNotFoundError("Results file '{}' not found.".format(resultsPath))

		with open(resultsPath, 'r') as file:
			data = json.load(file)  # Read the whole JSON file
			sources = data.get('sources', [])
			sinks = data.get('sinks', [])
			pairs = data.get('pairs', [])

			self.dataFlows = DataFlows(sources, sinks, pairs)


	### REDIS ### 
	# To a JSON String
	def toJsonString(self):
		# Convert object attributes to a dictionary
		jsonDict = {
			"sha256"    : self.sha256,
			"pkgName"   : self.pkgName,
			"categoryID"   : self.categoryID,
			"dataFlows" : self.dataFlows.getAll() if self.dataFlows is not None else None,
			"embeddings": self.embeddings
		}
		# Serialize dictionary to JSON string
		return json.dumps(jsonDict)

	# Download DataFlows From Redis
	def downloadDataFlowsFromRedis(self, redisClient):

		def printMemoryUsage():
			process = psutil.Process(os.getpid())
			mem_info = process.memory_info()
			total_mem = psutil.virtual_memory().total
			percent_usage = (mem_info.rss / total_mem) * 100
			print(f"--- 💾 Memory Usage: {mem_info.rss / 1024 ** 2:.2f} MB [{percent_usage:.2f}% of total]", flush=True)

		# Download Extraction Results
		resultJsonData = redisClient.downloadJsonData(redisClient.resultsKey, self.sha256)

		if resultJsonData is not None:
			print("--- ✅ Data Flows Availaible on Redis", flush=True)

			# Print memory usage
			printMemoryUsage()

			self.dataFlows = DataFlows(resultJsonData.get("sources", []),
										resultJsonData.get("sinks", []),
										resultJsonData.get("pairs", []), 
										)
				
			print("--- ⚙️ Data Flows Pairs : {}".format(len(resultJsonData.get("pairs", []))))

		else:
			print("--- ❌ Data Flows Unavailaible on Redis", flush=True)

	# Download Embeddings from Redis for each pair of the Data Flows Object.
	def downloadPairsEmbeddingsFromRedis(self, redisClient, embeddingModel):

		# Check if the Embedding Model is one of the supported types
		if embeddingModel not in ["gpt", "codebert", "sfr"]:
			print("--- ⚠️ Error: Unsupported embeddingModel type. Please use 'gpt', 'codebert', or 'sfr'.")
			return

		embeddingModelRedisKey = redisClient.projectKey + "." + embeddingModel

		# If there are no DataFlow s...
		if self.dataFlows is None:
			print("--- ⚠️ Data Flows not present.\n")
			return
		else:
			# To store all the embeddings
			pairEmbeddings = []

			# For each Data Flow Pair
			for pair in self.dataFlows.pairs:
				# Get Source and Sink Embeddings
				source = pair['source']
				srcEmbString = redisClient.downloadString(embeddingModelRedisKey, source)
				if srcEmbString is None:
					print("--- ⚠️ Embedding not present on Redis Server.")
					return
				srcEmb = np.array([float(x) for x in srcEmbString.split(',')])
			
				sink = pair['sink']
				sinkEmbString = redisClient.downloadString(embeddingModelRedisKey, sink)
				if sinkEmbString is None:
					print("--- ⚠️ Embedding not present on Redis Server.")
					return
				sinkEmb = np.array([float(x) for x in sinkEmbString.split(',')])
			
				# Concatenate Source and Sink
				pairEmb = np.concatenate((srcEmb, sinkEmb))
				
				# Add to Embedding list
				pairEmbeddings.append(pairEmb)

			self.embeddings[embeddingModel] = np.array(pairEmbeddings)

		print("--- ✅ PAIRS Embeddings Loaded From Redis --> {}".format(embeddingModel), flush=True)


# Class to manage DataFlows extracted from an App
class DataFlows:

	# Fields after Data Flows Extraction
	sources = []
	sinks   = []
	pairs   = []

	def __init__(self, sources=[], sinks=[], pairs=[]):
		self.sources = sources
		self.sinks = sinks
		self.pairs = pairs

	def __str__(self) -> str:
		print("\n--- ⭐ Summary ⭐ ---")
		print(f"--- #️⃣ Number of sources          : {len(self.sources)}")
		print(f"--- #️⃣ Number of sinks            : {len(self.sinks)}")
		print(f"--- #️⃣ Number of data flows pairs : {len(self.pairs)}")
		return ""

	# Get Dictionary
	def getAll(self):
		return {
			'sources': self.sources,
			'sinks': self.sinks,
			'pairs': self.pairs,
		}
	
	# Convert to JSON String
	def toJsonString(self):
		return json.dumps(self.getAll())
	
	# Check if all lists are empty
	def isEmpty(self):
		if (len(self.sources) == 0 and (self.sinks) == 0 and len(self.pairs) == 0):
			print("--- ⚠️ Empty Data Flows")
			return True
		else:
			return False