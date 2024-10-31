import shutil
import os

# Delete a file
def deleteFile(filePath):
    os.remove(filePath)

# Delete a folder
def deleteFolder(folderPath):
    try:
        shutil.rmtree(folderPath)
    except Exception as e:
        print("--- ⚠️ Error deleting folder '{}': {}".format(folderPath, e))