from flask import Flask, request, jsonify
import cv2 as cv
import csv
import numpy as np
import mediapipe as mp
from model import KeyPointClassifier
from model import PointHistoryClassifier
from collections import deque, Counter
import copy
from app import calc_bounding_rect, calc_landmark_list, pre_process_landmark, pre_process_point_history

app = Flask(__name__)

# Load models once (similar to your main script)
keypoint_classifier = KeyPointClassifier()
point_history_classifier = PointHistoryClassifier()

# Read labels
with open('model/keypoint_classifier/keypoint_classifier_label.csv', encoding='utf-8-sig') as f:
    keypoint_classifier_labels = csv.reader(f)
    keypoint_classifier_labels = [row[0] for row in keypoint_classifier_labels]
with open('model/point_history_classifier/point_history_classifier_label.csv', encoding='utf-8-sig') as f:
    point_history_classifier_labels = csv.reader(f)
    point_history_classifier_labels = [row[0] for row in point_history_classifier_labels]

# Initialize mediapipe
mp_hands = mp.solutions.hands
hands = mp_hands.Hands(static_image_mode=True, max_num_hands=1, min_detection_confidence=0.7, min_tracking_confidence=0.5)

# Coordinate history
history_length = 16
point_history = deque(maxlen=history_length)
finger_gesture_history = deque(maxlen=history_length)

@app.route("/classify", methods=["POST"])
def classify_image():
    # Retrieve image from request
    file = request.files["image"]
    np_img = np.frombuffer(file.read(), np.uint8)
    image = cv.imdecode(np_img, cv.IMREAD_COLOR)
    image = cv.flip(image, 1)  # Mirror display
    debug_image = copy.deepcopy(image)

    # Detection implementation
    image = cv.cvtColor(image, cv.COLOR_BGR2RGB)
    image.flags.writeable = False
    results = hands.process(image)
    image.flags.writeable = True

    if results.multi_hand_landmarks is not None:
        for hand_landmarks, handedness in zip(results.multi_hand_landmarks, results.multi_handedness):
            # Bounding box calculation
            brect = calc_bounding_rect(debug_image, hand_landmarks)
            # Landmark calculation
            landmark_list = calc_landmark_list(debug_image, hand_landmarks)

            # Conversion to relative coordinates / normalized coordinates
            pre_processed_landmark_list = pre_process_landmark(landmark_list)
            pre_processed_point_history_list = pre_process_point_history(debug_image, point_history)

            # Hand sign classification
            hand_sign_id = keypoint_classifier(pre_processed_landmark_list)
            if hand_sign_id == 8:  # "i" sign
                point_history.append(landmark_list[8])
            else:
                point_history.append([0, 0])

            # Finger gesture classification
            finger_gesture_id = 0
            point_history_len = len(pre_processed_point_history_list)
            if point_history_len == (history_length * 2):
                finger_gesture_id = point_history_classifier(pre_processed_point_history_list)

            # Calculates the gesture IDs in the latest detection
            finger_gesture_history.append(finger_gesture_id)
            most_common_fg_id = Counter(finger_gesture_history).most_common()

            prediction = {
                "class": keypoint_classifier_labels[hand_sign_id],
                "location": brect,
                "finger_gesture": point_history_classifier_labels[most_common_fg_id[0][0]]
            }

            return jsonify(prediction)

    return jsonify({"error": "No hand detected"})

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)