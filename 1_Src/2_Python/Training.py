from   sklearn.model_selection import train_test_split
from   sklearn.svm             import OneClassSVM
from   sklearn.preprocessing   import StandardScaler
from   sklearn.ensemble 	   import IsolationForest
import numpy                as np
import joblib
import torch # type: ignore
import random 
import time
import os

# Class to manage the training phase
class TrainingManager:

	# Parameters
	embeddingModel  = None

	# Keep track of the number of apps loaded
	numApps = None

	# Numerical Embeddings to train the model.
	embeddings = None

	# To store the results of the training.
	trainingResults = None

	# Initializer
	def __init__(self, embeddingModel):

		# Check if the Embedding Model is one of the supported types
		if embeddingModel not in ["gpt", "codebert", "sfr"]:
			print("\n--- ⚠️ Error: Unsupported embeddingModel type. Please use 'gpt', 'codebert', or 'sfr'.")
			return
		self.embeddingModel  = embeddingModel
		self.numApps = 0
		self.embeddings      = np.array([])
		self.trainingResults = None

	# Info about the Training
	def __str__(self):
		result = (
			"\n--- ⭐ Training Manager ⭐---\n"
			"--- ⚙️ Embedding Model  : {}\n".format(self.embeddingModel) +
			"--- #️⃣ Number of apps loaded  : {}\n".format(self.numApps) +
			"--- 📐 Feature Vectors Shape [Num Data Flow Pairs, Length of Feature Vectors]\n"
			"--- 📐 Shape : {}\n".format(self.embeddings.shape)
		)
		return result

	def loadEmbeddingsFromApp(self, appEmbeddings):
		if appEmbeddings is None:
			print("--- ❌ Error: appEmbeddings is None. Cannot load embeddings ")
			return  

		if self.numApps == 0:
			self.embeddings = appEmbeddings
		else:
			if appEmbeddings.shape[1] != self.embeddings.shape[1]:
				print("--- ❌ Dimensions mismatch: Cannot stack embeddings. Check failed.")
				return  
			self.embeddings = np.vstack((self.embeddings, appEmbeddings))

		self.numApps += 1

		print("--- ✅ Training Data Loaded", flush=True)

	# Train a new Anomaly Detection Model
	def trainAnomalyDetectionModel(self, modelPath):
		print("--- 🦾 TRAINING 🦾 ---")

	 	# Select the correct Feature Vectors
		X = self.embeddings

		# Print message if no embeddings
		if len(X) == 0:
			print("\n--- ⚠️ Error: Embeddings Not Loaded")
			raise FileNotFoundError("--- ⚠️ Error:  Embeddings Not Loaded")

		# Start
		startTime = time.time()
		print("--- ⏳ Start at    :", time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(startTime)))

		# Create the One-Class SVM model with mid-way parameters
		model = OneClassSVM(kernel='rbf',
									gamma=0.001,
									cache_size=500,
									tol=0.001,          
									nu=0.005,
									).fit(X)


		# Count Outliers
		Y = model.predict(X)
		numOutliers = np.count_nonzero(Y == -1)

		# End
		endTime = time.time()
		print("--- ⏳ Finished at :", time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(endTime)))
		print("--- ⏳ Time elapsed: {:.2f} seconds".format(endTime - startTime))
		print(f"--- 🧾 Number of Outliers Detected: {numOutliers}")


		# Check if the directory exists, create it if not
		if not os.path.exists(os.path.dirname(modelPath)):
			try:
				os.makedirs(os.path.dirname(modelPath))
				print(f"--- 💾 Created directory: {os.path.dirname(modelPath)}")
			except OSError as exc: 
				print(exc)
		
		# Save the model
		joblib.dump(model, modelPath)
		print("--- 💾 Model saved :", modelPath)

		self.trainingResults = TrainingResults(model, modelPath, X.shape, Y, numOutliers)

	
# Class to store the results of Training
class TrainingResults:

	# The Trained Model
	model       = None
	# Wherte the model is saved
	modelPath   = None
	# Input Shape
	inputShape  = None

	# List of embeddings representing outliers pairs
	labels       = None
	numOutliers  = None

	def __init__(self, model, modelPath, inputShape, labels, numOutliers):
		self.model       = model
		self.modelPath   = modelPath
		self.inputShape  = inputShape
		self.labels      = labels
		self.numOutliers = numOutliers


	def __str__(self):
		percentage = (self.numOutliers / self.inputShape[0]) * 100 if self.inputShape[0] else 0
		output = "\n--- 🚀 Training Results 🚀 ---\n"
		output += "--- 📦 Model                 : {}\n".format(self.model)
		output += "--- 💾 Model Path            : {}\n".format(self.modelPath)
		output += "--- 📐 Input Size            : {}\n".format(self.inputShape)
		output += "--- ⭕ Number of Outliers    : {} ({:.2f}%)\n".format(self.numOutliers, percentage)
		output += "-" * 30 + "\n\n"
		return output
