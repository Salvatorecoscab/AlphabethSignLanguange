# make a program that deletes random images from the dataset directories
# to make the dataset smaller
import os
import random

# get the current working directory
cwd = os.getcwd()
print(cwd)

# get the path to the dataset
path = os.path.join(cwd, 'dataext')
print(path)

# get the list of directories in the dataset
dirList = os.listdir(path)
print(dirList)

# loop through the directories and delete random images until there is only 70 images left on each directory
for dir in dirList:
    # get the path to the directory
    dirPath = os.path.join(path, dir)
    print(dirPath)

    # get the list of images in the directory
    try:
        imgList = os.listdir(dirPath)
    except NotADirectoryError:
        continue
    
    print(imgList)

    # get the number of images in the directory
    numImg = len(imgList)
    print(numImg)

    # get the number of images to delete
    numImgToDelete = numImg - 100
    print(numImgToDelete)

    # loop through the images and delete random images
    for i in range(numImgToDelete):
        # get a random image
        img = random.choice(imgList)
        print(img)

        # get the path to the image
        imgPath = os.path.join(dirPath, img)
        print(imgPath)

        # delete the image
        os.remove(imgPath)

        # remove the image from the list of images
        imgList.remove(img)
        print(imgList)

