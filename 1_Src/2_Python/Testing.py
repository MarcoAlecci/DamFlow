from App   import DataFlows
import joblib
import json
import os

# Class to manage the Testing Phase
class TestingManager:

	# Parameters
	embeddingModel  = None

	# Model & Rersults Paths
	modelPath   = None
	resultsPath = None

	# To store the pre-trained model
	model = None

	# To store the results
	results = None

	# Initializer
	def __init__(self, modelPath, resultsPath, embeddingModel):

		# Check if the Embedding Model is one of the supported types
		if embeddingModel not in ["gpt", "codebert", "sfr"]:
			print("\n--- ⚠️ Error: Unsupported embeddingModel type. Please use 'gpt', 'codebert', or 'sfr'.")
			return
		self.modelPath 	     = modelPath
		self.resultsPath     = resultsPath
		self.embeddingModel  = embeddingModel

		# Empty Result
		self.results         = AnomalyDetectionResults(None, None)
		
		# Extract directory from resultsPath and create it if it doesn't exist
		resultsDir = os.path.dirname(self.resultsPath)
		if resultsDir and not os.path.exists(resultsDir):
			os.makedirs(resultsDir)
			print(f"\n--- 📁 Created directory for resultsPath: {resultsDir}")

		# Load the model
		self.loadModel()

	def __str__(self):
		output = "\n--- ⭐ Testing Manager ⭐---\n"
		output += "--- ⚙️ Embedding Model  : {}\n".format(self.embeddingModel)
		output += "--- ✅ Model Loaded     : {}\n".format(self.model)
		return output

	# Load the pretrained model
	def loadModel(self):
		# Convert relative path to absolute path
		absolutModelPath = os.path.abspath(self.modelPath)
		print("--- ⚙️ Loading Model\n--- ⚙️ Rel: {}\n--- ⚙️ Abs: {}".format(self.modelPath, absolutModelPath))
		
		if self.modelPath.endswith('.joblib'):
			if os.path.isfile(absolutModelPath):
				self.model = joblib.load(absolutModelPath)
				print("--- ✅ Model loaded successfully.")
			else:
				print(f"--- ⚠️ Error: Model file does not exist at {absolutModelPath}")
				raise Exception("Missing Model")
		else:
			print("--- ⚠️ Error: No Model")
			raise Exception("Missing Model")

	# Get the results of one app.
	def testingAnomalyDetectionModel(self, app):
		
		print("--- 🧪 Testing")

		# Get Feature Vectors
		X = app.embeddings[self.embeddingModel]

		if X is None or (len(X) == 0):
			print("--- ⚠️ Error: No Embeddings.")
			return

		print("--- 🧪 Embedding Shape : {}".format(X.shape))

		# Predict using the loaded model
		try:
			Y = self.model.predict(X)
		except Exception as e:
			print("--- ⚠️ Error: Failed to predict labels:", e)
			return
		
		# Get Outliers (but Add full Paths)
		outliersSources = []
		outliersSinks = []
		outliersPairs = []

		for i, value in enumerate(Y):
			if value == 1:
				currentPair = app.dataFlows.pairs[i]
				outliersSources.append(currentPair["source"])
				outliersSinks.append(currentPair["sink"])
				outliersPairs.append(currentPair)
		
		# Store results in the App Object
		self.results = AnomalyDetectionResults(len(X), DataFlows(outliersSources, outliersSinks, outliersPairs))   

	
	def saveResults(self, app):

		# Data to be saved
		resultsDict = {
			"sha256"    : app.sha256,
			"pkgName"   : app.pkgName,
			"categoryID"   : app.categoryID,
		}

		# Attach Anomaly Results
		resultsDict.update(self.results.toDict())
		
		# Read existing file contents if it exists
		if os.path.exists(self.resultsPath):
			with open(self.resultsPath, 'r') as file:
				try:
					existingData = json.load(file)
				except json.JSONDecodeError:
					existingData = []
		else:
			existingData = []

		# Append the new resultsDict
		existingData.append(resultsDict)

		# Write the updated data back to the file
		with open(self.resultsPath, 'w') as file:
			json.dump(existingData, file, indent=4)

# Class to manage the results of the Anomaly Detection Phase
class AnomalyDetectionResults:

		# Number of total Data Flows inside an app
		numDataFlows  = None
		# Number of outliers
		numOutliers = None
		# Outliers Percentage
		percentageOutliers = None
		# Outliers Data Flows
		outliers = None

		def __init__(self, numDataFlows, outliers):
			if numDataFlows is None or outliers is None:
				self.numDataFlows       = None
				self.outliers           = None
				self.numOutliers        = None
				self.percentageOutliers = None
			elif numDataFlows == 0:
				self.numDataFlows       = 0
				self.outliers           = outliers
				self.numOutliers        = 0
				self.percentageOutliers = 0
			else: 
				self.numDataFlows       = numDataFlows
				self.outliers           = outliers
				self.numOutliers        = len(outliers.pairs)
				self.percentageOutliers = round((self.numOutliers / self.numDataFlows) * 100, 2)
			
		def __str__(self) -> str:
			summary = []
			summary.append("--- 🧪 Results ")
			summary.append(f"--- #️⃣ Total number of data flows : {self.numDataFlows}")
			summary.append(f"--- #️⃣ Number of outliers         : {self.numOutliers}")
			summary.append(f"--- 📊 Percentage of outliers     : {self.percentageOutliers:.2f}%")
			summary.append("--"*20)
			return "\n".join(summary)
		
		def isNone(self):
			return (self.numDataFlows 		is None and
					self.numOutliers 		is None and
					self.percentageOutliers is None and
					self.outliers 			is None)
	
		def toDict(self):
			return {
				"numDataFlows"       : self.numDataFlows,
				"numOutliers"        : self.numOutliers,
				"percentageOutliers" : self.percentageOutliers,
				"dataFlows"          : self.outliers.getAll() if self.outliers is not None else None
			}