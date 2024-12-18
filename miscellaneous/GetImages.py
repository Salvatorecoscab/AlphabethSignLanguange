import cv2
import os
import threading
import time
import re
import sys
person = "Salvador"
letter=sys.argv[1]
# Custom sorting function
def extract_number(filename):
    # Extract number from the filename using regex
    match = re.search(r'_(\d+)\.jpg', filename)
    return int(match.group(1)) if match else 0


# Configuration
# letter = "A"
n = 10  # Save one frame every n frames
capture_duration = 3  # Duration in seconds

# Create the output folder structure
output_folder = f'./data/{letter}'
# verify if the folder exists
current_frame = 0
if not os.path.exists(output_folder):
    os.makedirs(output_folder, exist_ok = True)
else:
    # get last frame
    arr = os.listdir(output_folder)
    file_list_sorted = sorted(arr, key=extract_number)
    # Print sorted list
    print("Sorted file list:", file_list_sorted)
    lastfile=file_list_sorted[-1]
    # Regex to extract the number
    pattern = r"_(\d+)\.jpg"

    # Find the match
    match = re.search(pattern, lastfile)
    print(match.group(1))
    
    if match:
        current_frame = int(match.group(1))  # Skip the leading underscore
        print(f"Extracted current_frame: {current_frame}")
        current_frame+=1

    else:
        print("Number not found.")




# Open the camera or video source (use 0 for the default webcam)
camera_index = 0  # Change to 1, 2, etc., for other cameras()
cam = cv2.VideoCapture(camera_index)

if not cam.isOpened():
    print("Error: Could not access the camera or video stream.")
    exit()

# Get frames per second (FPS) if available
fps = cam.get(cv2.CAP_PROP_FPS)
print(f"FPS: {fps}")
time.sleep(1)
# Flag to signal when to stop the capture
stop_capture = False

def stop_after_duration(duration):
    global stop_capture
    time.sleep(duration)
    stop_capture = True

# Start the timer thread
timer_thread = threading.Thread(target=stop_after_duration, args=(capture_duration,))
timer_thread.start()

count = 0

try:
    print(f"Press 'q' to quit manually, or the program will stop after {capture_duration} seconds.")

    while not stop_capture:
        # Read a frame from the camera
        ret, frame = cam.read()

        if not ret:
            print("Error: Failed to capture frame. Exiting.")
            break

        # Save every nth frame
        if count % n == 0:
            file_name = os.path.join(output_folder, f"{person}_{letter}_{current_frame}.jpg")
            print(f"Saving: {file_name}")
            cv2.imwrite(file_name, frame)
            current_frame += 1

        count += 1

        # Display the live video feed (optional)
        cv2.imshow('Live Video', frame)

        # Exit the loop if 'q' is pressed
        if cv2.waitKey(1) & 0xFF == ord('q'):
            break

except KeyboardInterrupt:
    print("Interrupted by user. Exiting...")

finally:
    # Release resources
    cam.release()
    cv2.destroyAllWindows()
    print("Video capture stopped.")
