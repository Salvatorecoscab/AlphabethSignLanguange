import cv2
import time
import requests
import threading
import tkinter as tk
from PIL import Image, ImageTk

URL = "http://alphabethsignlanguange.uw.r.appspot.com/classify"
IMAGE_PATH = "testCase.png"

def resize_image(image, height):
    h, w = image.shape[:2]
    scale = height / h
    new_width = int(w * scale)
    return cv2.resize(image, (new_width, height))

def capture_and_process():
    ret, frame = cap.read()
    if not ret:
        print("Error capturing the image")
        return
    threading.Thread(target=process_request, args=(frame,)).start()

def process_request(frame):
    resized_image = resize_image(frame, 240)
    cv2.imwrite(IMAGE_PATH, resized_image)
    with open(IMAGE_PATH, "rb") as img_file:
        response = requests.post(URL, files={"image": img_file})
    if response.status_code == 200:
        display_result(response.json())
    else:
        print(f"Request error: {response.status_code}")

def display_result(data):
    gesture_class = data.get('class')
    finger_gesture = data.get('finger_gesture')
    if gesture_class and finger_gesture:
        result_text.set(f"Letter: {gesture_class}")
    else:
        result_text.set("No class detected")

def display_camera():
    ret, frame = cap.read()
    if ret:
        frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        img_tk = ImageTk.PhotoImage(Image.fromarray(frame_rgb))
        video_label.config(image=img_tk)
        video_label.image = img_tk
    root.after(30, display_camera)

root = tk.Tk()
root.title("Gesture Recognition")

result_text = tk.StringVar()
result_label = tk.Label(root, textvariable=result_text, font=("Arial", 16))
result_label.pack()

video_label = tk.Label(root)
video_label.pack()

cap = cv2.VideoCapture(0)
if not cap.isOpened():
    print("Error accessing the camera")
    exit()

def classification():
    capture_and_process()
    root.after(2000, classification)

display_camera()
classification()

root.mainloop()

cap.release()
